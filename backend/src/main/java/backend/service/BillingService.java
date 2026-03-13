package backend.service;

import backend.config.StripeProperties;
import backend.entity.User;
import backend.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionCollection;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.SubscriptionListParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
public class BillingService {

  private static final Logger log = LoggerFactory.getLogger(BillingService.class);

  private final StripeProperties props;
  private final UserRepository userRepository;

  public BillingService(StripeProperties props, UserRepository userRepository) {
    this.props = props;
    this.userRepository = userRepository;
    if (props.getSecretKey() != null && !props.getSecretKey().isBlank()) {
      Stripe.apiKey = props.getSecretKey();
    }
  }

  public boolean isConfigured() {
    return props.getSecretKey() != null && !props.getSecretKey().isBlank()
        && props.getPriceId() != null && !props.getPriceId().isBlank()
        && props.getSuccessUrl() != null && !props.getSuccessUrl().isBlank();
  }

  public String ensureCustomer(User user) throws Exception {
    if (user.getStripeCustomerId() != null && !user.getStripeCustomerId().isBlank()) {
      return user.getStripeCustomerId();
    }
    CustomerCreateParams params = CustomerCreateParams.builder()
        .setEmail(user.getEmail())
        .setName((user.getFirstName() + " " + user.getLastName()).trim())
        .putMetadata("user_id", String.valueOf(user.getId()))
        .build();
    Customer customer = Customer.create(params);
    user.setStripeCustomerId(customer.getId());
    userRepository.save(user);
    return customer.getId();
  }

  public Session createCheckout(User user, String promoCode, String discordId) throws Exception {
    if (!isConfigured()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe non configuré");
    }
    log.info("Billing: create checkout for user={} promo={} discord={}",
        user.getId(),
        promoCode != null && !promoCode.isBlank() ? "***" : "(none)",
        discordId != null && !discordId.isBlank() ? "***" : "(none)");

    String customerId = ensureCustomer(user);
    if (discordId != null && !discordId.isBlank()) {
      user.setDiscordId(discordId.trim());
      userRepository.save(user);
    }

    SessionCreateParams.Builder builder = SessionCreateParams.builder()
        .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
        .setCustomer(customerId)
        .setSuccessUrl(resolveSuccessUrlWithSessionId())
        .setCancelUrl(props.getCancelUrl() != null ? props.getCancelUrl() : props.getSuccessUrl())
        .addLineItem(
            SessionCreateParams.LineItem.builder()
                .setPrice(props.getPriceId())
                .setQuantity(1L)
                .build()
        );

    if (promoCode != null && !promoCode.isBlank()) {
      builder.addDiscount(SessionCreateParams.Discount.builder().setPromotionCode(promoCode).build());
    }

    Session session = Session.create(builder.build());
    return session;
  }

  public com.stripe.model.billingportal.Session createPortal(User user) throws Exception {
    if (!isConfigured()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe non configuré");
    }
    log.info("Billing: create portal for user={}", user.getId());
    String customerId = ensureCustomer(user);
    com.stripe.param.billingportal.SessionCreateParams params = com.stripe.param.billingportal.SessionCreateParams.builder()
        .setCustomer(customerId)
        .setReturnUrl(props.getSuccessUrl())
        .build();
    return com.stripe.model.billingportal.Session.create(params);
  }

  public void handleWebhook(String payload, String sigHeader) {
    if (!isConfigured() || props.getWebhookSecret() == null || props.getWebhookSecret().isBlank()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Webhook non configuré");
    }

    Event event;
    try {
      event = Webhook.constructEvent(payload, sigHeader, props.getWebhookSecret());
    } catch (SignatureVerificationException e) {
      log.warn("Billing: webhook signature invalid");
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Signature webhook invalide");
    }

    String type = event.getType();
    EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
    if (!deserializer.getObject().isPresent()) return;

    log.info("Billing: webhook type={}", type);
    switch (type) {
      case "customer.subscription.created":
      case "customer.subscription.updated":
      case "customer.subscription.deleted":
        Subscription sub = (Subscription) deserializer.getObject().get();
        syncSubscription(sub);
        break;
      case "checkout.session.completed":
        Session session = (Session) deserializer.getObject().get();
        // Optionally handle immediately: fetch subscription id from session
        if (session.getSubscription() != null) {
          try {
            Subscription s = Subscription.retrieve(session.getSubscription());
            syncSubscription(s);
          } catch (Exception ignore) {
            log.warn("Billing: unable to retrieve subscription from checkout session {}", session.getId());
          }
        }
        break;
      default:
        log.debug("Billing: webhook ignored type={}", type);
        break;
    }
  }

  /**
   * Poll Stripe to refresh the stored subscription status when webhook delivery is absent
   * (typique en dev/local). Returns the (possibly updated) status string.
   */
  public String refreshStatus(User user) {
    if (!isConfigured()) return user.getSubscriptionStatus();
    try {
      String customerId = ensureCustomer(user);
      SubscriptionListParams params = SubscriptionListParams.builder()
          .setCustomer(customerId)
          .setLimit(1L)
          .build();
      SubscriptionCollection coll = Subscription.list(params);
      Subscription sub = coll.getData().isEmpty() ? null : coll.getData().get(0);
      if (sub != null) {
        syncSubscription(sub);
        return sub.getStatus();
      }
    } catch (Exception e) {
      // ignore, fallback to stored value
    }
    return user.getSubscriptionStatus();
  }

  private String resolveSuccessUrlWithSessionId() {
    String base = props.getSuccessUrl();
    if (base == null || base.isBlank()) return "";
    if (base.contains("{CHECKOUT_SESSION_ID}")) return base;
    String separator = base.contains("?") ? "&" : "?";
    return base + separator + "session_id={CHECKOUT_SESSION_ID}";
  }

  private void syncSubscription(Subscription sub) {
    String customerId = sub.getCustomer();
    if (customerId == null) return;
    Optional<User> opt = userRepository.findByStripeCustomerId(customerId);
    User user = opt.orElse(null);
    // Fallback: find by email on customer if not linked yet
    Customer cust = sub.getCustomerObject();
    if (user == null && cust != null) {
      String email = cust.getEmail();
      if (email != null && !email.isBlank()) {
        user = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
        if (user != null) {
          user.setStripeCustomerId(customerId);
        }
      }
    }
    if (user == null) return;
    String status = sub.getStatus(); // active | past_due | canceled | incomplete...
    user.setSubscriptionStatus(status != null ? status : "inactive");
    if (sub.getCurrentPeriodEnd() != null) {
      OffsetDateTime end = OffsetDateTime.ofInstant(Instant.ofEpochSecond(sub.getCurrentPeriodEnd()), ZoneOffset.UTC);
      user.setSubscriptionCurrentPeriodEnd(end);
    }
    userRepository.save(user);
    log.info("Billing: synced subscription customer={} status={}", customerId, user.getSubscriptionStatus());
  }
}
