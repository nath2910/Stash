package backend.service;

import backend.config.StripeProperties;
import backend.dto.CheckoutRequest;
import backend.entity.User;
import backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.stripe.Stripe;
import com.stripe.model.Customer;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.SubscriptionListParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionListParams;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BillingService {
  public static final String TERMS_VERSION = "2026-09-08";
  private final StripeProperties props;
  private final UserRepository users;
  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;
  private final Cache<String, Price> prices = Caffeine.newBuilder().maximumSize(2)
      .expireAfterWrite(Duration.ofMinutes(5)).build();

  public BillingService(StripeProperties props, UserRepository users, JdbcTemplate jdbc, ObjectMapper mapper) {
    this.props = props;
    this.users = users;
    this.jdbc = jdbc;
    this.mapper = mapper;
    if (present(props.getSecretKey())) Stripe.apiKey = props.getSecretKey();
  }

  public boolean isConfigured() {
    return present(props.getSecretKey()) && present(props.getPriceId())
        && present(props.getSuccessUrl()) && present(props.getWebhookSecret());
  }

  public record Plan(String id, long amount, String currency, String interval, boolean available) {}

  public List<Plan> plans() throws Exception {
    requireConfigured();
    List<Plan> result = new ArrayList<>();
    for (String plan : List.of("monthly", "annual")) {
      if (!present(priceId(plan))) continue;
      Price price = priceFor(plan);
      result.add(new Plan(plan, price.getUnitAmount(), price.getCurrency(),
          price.getRecurring().getInterval(), props.isSalesEnabled()));
    }
    return result;
  }

  private Price priceFor(String plan) throws Exception {
    String id = priceId(plan);
    if (!present(id)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formule indisponible");
    Price price = prices.getIfPresent(id);
    if (price == null) {
      price = Price.retrieve(id);
      prices.put(id, price);
    }
    String interval = "annual".equals(plan) ? "year" : "month";
    if (!Boolean.TRUE.equals(price.getActive()) || price.getUnitAmount() == null || price.getUnitAmount() <= 0
        || !"eur".equals(price.getCurrency()) || !"inclusive".equals(price.getTaxBehavior())
        || price.getRecurring() == null || !interval.equals(price.getRecurring().getInterval())
        || !Long.valueOf(1).equals(price.getRecurring().getIntervalCount())) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Tarif TTC non configuré");
    }
    return price;
  }

  @Transactional(rollbackFor = Exception.class)
  public Session createCheckout(User principal, CheckoutRequest request) throws Exception {
    requireConfigured();
    if (request == null || !Boolean.TRUE.equals(request.termsAccepted())
        || !("monthly".equals(request.plan()) || "annual".equals(request.plan()))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formule et acceptation des CGV requises");
    }
    if (!props.isSalesEnabled()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Souscriptions bientôt disponibles");
    User user = lockUser(principal);
    if (!user.isEmailVerified()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vérifiez votre email");
    Price price = priceFor(request.plan());
    String customerId = ensureCustomer(user);
    if (subscriptions(customerId).stream().anyMatch(s -> !terminal(s.getStatus()))) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Un abonnement existe déjà. Gérez-le depuis votre compte.");
    }
    // A user lock serializes checkout, cancellation, deletion and webhook processing across replicas.
    for (Session open : openSessions(customerId)) {
      if (TERMS_VERSION.equals(open.getMetadata().get("terms_version"))
          && price.getId().equals(open.getMetadata().get("price_id"))) return open;
      open.expire();
    }
    long acceptedAt = Instant.now().getEpochSecond();
    var params = SessionCreateParams.builder()
        .setMode(SessionCreateParams.Mode.SUBSCRIPTION).setCustomer(customerId)
        .setClientReferenceId(String.valueOf(user.getId()))
        .setSuccessUrl(successUrl()).setCancelUrl(present(props.getCancelUrl()) ? props.getCancelUrl() : props.getSuccessUrl())
        .putMetadata("terms_version", TERMS_VERSION).putMetadata("terms_accepted_at", String.valueOf(acceptedAt))
        .putMetadata("price_id", price.getId())
        .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
            .putMetadata("terms_version", TERMS_VERSION).putMetadata("terms_accepted_at", String.valueOf(acceptedAt)).build())
        .addLineItem(SessionCreateParams.LineItem.builder().setPrice(price.getId()).setQuantity(1L).build()).build();
    return Session.create(params, RequestOptions.builder().setIdempotencyKey(
        "checkout:" + user.getId() + ":" + price.getId() + ":" + acceptedAt / 1800).build());
  }

  private String ensureCustomer(User user) throws Exception {
    if (present(user.getStripeCustomerId())) return user.getStripeCustomerId();
    Customer customer = Customer.create(CustomerCreateParams.builder().setEmail(user.getEmail())
        .setName((user.getFirstName() + " " + user.getLastName()).trim())
        .putMetadata("user_id", String.valueOf(user.getId())).build(),
        RequestOptions.builder().setIdempotencyKey("customer:" + user.getId()).build());
    user.setStripeCustomerId(customer.getId());
    users.save(user);
    return customer.getId();
  }

  public com.stripe.model.billingportal.Session createPortal(User user) throws Exception {
    requireConfigured();
    if (!present(user.getStripeCustomerId())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Aucun compte de facturation");
    return com.stripe.model.billingportal.Session.create(com.stripe.param.billingportal.SessionCreateParams.builder()
        .setCustomer(user.getStripeCustomerId()).setReturnUrl(props.getSuccessUrl().split("\\?")[0]).build());
  }

  @Transactional(rollbackFor = Exception.class)
  public void handleWebhook(String payload, String signature) {
    requireConfigured();
    com.stripe.model.Event event;
    try {
      event = Webhook.constructEvent(payload, signature, props.getWebhookSecret());
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Signature ou événement invalide");
    }
    String type = event.getType();
    if (type == null || !(type.startsWith("customer.subscription.") || type.equals("checkout.session.completed")
        || type.equals("invoice.paid") || type.equals("invoice.payment_failed"))) return;
    try {
      // Read verified JSON: SDK API-version mismatch must never silently discard a payment event.
      var customer = mapper.readTree(payload).path("data").path("object").path("customer");
      String customerId = customer.isTextual() ? customer.asText() : customer.path("id").asText();
      User user = users.lockByStripeCustomerId(customerId).orElse(null);
      if (user == null) return; // Never link a payment account by an email from an event.
      int inserted = jdbc.update("INSERT INTO billing_webhook_receipts(event_id) VALUES (?) ON CONFLICT DO NOTHING", event.getId());
      if (inserted == 0) return;
      // Stripe does not guarantee delivery order: read current provider state while holding the lock.
      syncCurrent(user, subscriptions(customerId));
    } catch (Exception ex) {
      // Roll back the receipt and return a retryable error.
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Synchronisation paiement temporairement indisponible");
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public String refreshStatus(User principal) {
    requireConfigured();
    if (!present(principal.getStripeCustomerId())) return principal.getSubscriptionStatus();
    User user = lockUser(principal);
    try {
      syncCurrent(user, subscriptions(user.getStripeCustomerId()));
      copyState(user, principal);
      return user.getSubscriptionStatus();
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Synchronisation paiement temporairement indisponible");
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public void cancelAtPeriodEnd(User principal) throws Exception {
    requireConfigured();
    User user = lockUser(principal);
    if (!present(user.getStripeCustomerId())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Aucun abonnement");
    for (Subscription subscription : subscriptions(user.getStripeCustomerId())) {
      if (!terminal(subscription.getStatus())) {
        subscription.update(SubscriptionUpdateParams.builder().setCancelAtPeriodEnd(true).build());
      }
    }
    syncCurrent(user, subscriptions(user.getStripeCustomerId()));
    copyState(user, principal);
  }

  /** Account deletion holds the same user lock as checkout and webhooks. Stripe invoices are retained. */
  public void cancelForAccountDeletion(User user) throws Exception {
    if (!present(user.getStripeCustomerId())) return;
    requireConfigured();
    for (Session open : openSessions(user.getStripeCustomerId())) open.expire();
    for (Subscription subscription : subscriptions(user.getStripeCustomerId())) {
      if (!terminal(subscription.getStatus())) subscription.cancel();
    }
  }

  private List<Session> openSessions(String customer) throws Exception {
    var collection = Session.list(SessionListParams.builder().setCustomer(customer)
        .setStatus(SessionListParams.Status.OPEN).setLimit(100L).build());
    if (Boolean.TRUE.equals(collection.getHasMore())) throw new IllegalStateException("Checkout limit exceeded");
    return collection.getData();
  }

  private List<Subscription> subscriptions(String customer) throws Exception {
    var collection = Subscription.list(SubscriptionListParams.builder().setCustomer(customer)
        .setStatus(SubscriptionListParams.Status.ALL).setLimit(100L).build());
    if (Boolean.TRUE.equals(collection.getHasMore())) throw new IllegalStateException("Subscription limit exceeded");
    return collection.getData();
  }

  private void syncCurrent(User user, List<Subscription> all) {
    Subscription selected = all.stream().filter(this::knownPrice)
        .max(Comparator.comparingInt((Subscription s) -> accessStatus(s.getStatus()) ? 2 : terminal(s.getStatus()) ? 0 : 1)
            .thenComparing(s -> s.getCreated() == null ? 0L : s.getCreated())).orElse(null);
    user.setSubscriptionStatus(selected == null ? "inactive" : selected.getStatus());
    user.setStripeSubscriptionId(selected == null ? null : selected.getId());
    user.setSubscriptionCurrentPeriodEnd(selected == null || selected.getCurrentPeriodEnd() == null ? null
        : OffsetDateTime.ofInstant(Instant.ofEpochSecond(selected.getCurrentPeriodEnd()), ZoneOffset.UTC));
    user.setSubscriptionCancelAtPeriodEnd(selected != null && Boolean.TRUE.equals(selected.getCancelAtPeriodEnd()));
    users.save(user);
  }

  private boolean knownPrice(Subscription sub) {
    return sub.getItems() != null && sub.getItems().getData().stream().anyMatch(item -> item.getPrice() != null
        && (item.getPrice().getId().equals(props.getPriceId()) || item.getPrice().getId().equals(props.getAnnualPriceId())));
  }
  private void copyState(User source, User target) {
    target.setSubscriptionStatus(source.getSubscriptionStatus());
    target.setStripeSubscriptionId(source.getStripeSubscriptionId());
    target.setSubscriptionCurrentPeriodEnd(source.getSubscriptionCurrentPeriodEnd());
    target.setSubscriptionCancelAtPeriodEnd(source.isSubscriptionCancelAtPeriodEnd());
  }
  private User lockUser(User principal) {
    if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    return users.lockById(principal.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  }
  private String priceId(String plan) { return "monthly".equals(plan) ? props.getPriceId() : "annual".equals(plan) ? props.getAnnualPriceId() : null; }
  private boolean terminal(String status) { return "canceled".equals(status) || "incomplete_expired".equals(status); }
  private boolean accessStatus(String status) { return "active".equals(status) || "trialing".equals(status); }
  private boolean present(String value) { return value != null && !value.isBlank(); }
  private void requireConfigured() { if (!isConfigured()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Paiement indisponible"); }
  private String successUrl() {
    String base = props.getSuccessUrl();
    return base.contains("{CHECKOUT_SESSION_ID}") ? base : base + (base.contains("?") ? "&" : "?") + "session_id={CHECKOUT_SESSION_ID}";
  }
}
