package backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {
  private String secretKey;
  private String priceId;
  private String webhookSecret;
  private String successUrl;
  private String cancelUrl;

  public String getSecretKey() {
    return secretKey;
  }
  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  public String getPriceId() {
    return priceId;
  }
  public void setPriceId(String priceId) {
    this.priceId = priceId;
  }

  public String getWebhookSecret() {
    return webhookSecret;
  }
  public void setWebhookSecret(String webhookSecret) {
    this.webhookSecret = webhookSecret;
  }

  public String getSuccessUrl() {
    return successUrl;
  }
  public void setSuccessUrl(String successUrl) {
    this.successUrl = successUrl;
  }

  public String getCancelUrl() {
    return cancelUrl;
  }
  public void setCancelUrl(String cancelUrl) {
    this.cancelUrl = cancelUrl;
  }
}
