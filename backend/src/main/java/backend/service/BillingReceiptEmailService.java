package backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/** Sends a receipt only after Stripe confirms an invoice was paid. */
@Service
public class BillingReceiptEmailService {
  private static final Logger log = LoggerFactory.getLogger(BillingReceiptEmailService.class);
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)
      .withZone(ZoneId.of("Europe/Paris"));

  private final JavaMailSender mailSender;
  private final Environment environment;

  public BillingReceiptEmailService(JavaMailSender mailSender, Environment environment) {
    this.mailSender = mailSender;
    this.environment = environment;
  }

  public void send(backend.entity.User user, JsonNode invoice) {
    if (user == null || user.getEmail() == null || user.getEmail().isBlank()) return;
    try {
      long amountCents = invoice.path("amount_paid").asLong(0L);
      String currency = invoice.path("currency").asText("eur").toUpperCase(Locale.ROOT);
      String amount = String.format(Locale.FRENCH, "%.2f %s", amountCents / 100.0, currency);
      String invoiceNumber = invoice.path("number").asText("");
      String invoiceId = invoice.path("id").asText("");
      long created = invoice.path("created").asLong(Instant.now().getEpochSecond());
      String date = DATE_FORMAT.format(Instant.ofEpochSecond(created));
      String hostedInvoiceUrl = invoice.path("hosted_invoice_url").asText("");

      String from = environment.getProperty("app.password-reset.mail-from", "");
      if (from.isBlank()) from = environment.getProperty("spring.mail.username", "");
      if (from.isBlank()) {
        log.warn("Payment receipt email skipped: sender address is missing");
        return;
      }

      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setTo(user.getEmail());
      helper.setFrom(from, "MyStash");
      helper.setSubject("Confirmation de paiement MyStash");
      helper.setText(
          "Bonjour " + safe(user.getFirstName()) + ",\n\n"
              + "Ton paiement MyStash a bien été confirmé.\n\n"
              + "Offre : Abonnement mensuel\nMontant : " + amount + "\nDate : " + date
              + (invoiceNumber.isBlank() ? "" : "\nRéférence : " + invoiceNumber)
              + "\n\nMerci pour ta confiance.\nL'équipe MyStash",
          html(user, amount, date, invoiceNumber, invoiceId, hostedInvoiceUrl));
      mailSender.send(message);
      log.info("Payment receipt email sent for invoice {}", invoiceId);
    } catch (Exception ex) {
      // The payment is already confirmed; email delivery must not trigger webhook retries.
      log.warn("Payment receipt email failed for {}", user.getEmail(), ex);
    }
  }

  private String html(backend.entity.User user, String amount, String date, String number, String id, String invoiceUrl) {
    String invoiceLink = invoiceUrl.isBlank() ? "" : "<p style=\"margin:24px 0 0\"><a href=\""
        + safe(invoiceUrl) + "\" style=\"display:inline-block;background:#635bff;color:#fff;text-decoration:none;font-weight:700;border-radius:7px;padding:12px 18px\">Voir la facture</a></p>";
    return """
        <!doctype html><html><body style="margin:0;background:#f6f9fc;font-family:Arial,sans-serif;color:#3c4257;padding:40px 16px">
        <table role="presentation" width="100%%"><tr><td align="center"><table role="presentation" width="100%%" style="max-width:620px;background:#fff;border-radius:14px;padding:46px 52px;box-shadow:0 1px 2px rgba(60,66,87,.08)">
        <tr><td style="font-size:22px;font-weight:800;color:#0f172a">MyStash</td></tr>
        <tr><td><h1 style="margin:34px 0 0;font-size:25px;color:#30313d">Paiement confirmé</h1><p style="font-size:17px;line-height:1.6;color:#4f566b">Bonjour %s, ton abonnement est bien actif.</p>
        <table role="presentation" width="100%%" style="margin:28px 0 0;border-top:1px solid #e6ebf1;border-bottom:1px solid #e6ebf1;padding:18px 0;font-size:16px;line-height:2"><tr><td>Offre</td><td align="right"><b>Abonnement mensuel</b></td></tr><tr><td>Montant</td><td align="right"><b>%s</b></td></tr><tr><td>Date</td><td align="right">%s</td></tr>%s</table>
        %s<p style="margin:30px 0 0;font-size:14px;line-height:1.6;color:#697386">Merci pour ta confiance. Tu peux maintenant accéder à ton espace MyStash.</p></td></tr></table><p style="font-size:13px;color:#697386">MyStash - mystash.fr</p></td></tr></table></body></html>
        """.formatted(safe(user.getFirstName()), amount, date,
        number.isBlank() ? "" : "<tr><td>Référence</td><td align=\"right\">" + safe(number) + "</td></tr>", invoiceLink);
  }

  private String safe(String value) {
    if (value == null) return "";
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&#39;");
  }
}
