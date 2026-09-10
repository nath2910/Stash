package backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {
  private String secretKey;
  private String priceId;
  private String webhookSecret;
  private String successUrl;
  private String cancelUrl;
  private String annualPriceId;
  private boolean salesEnabled;
  private boolean commercialRegistrationComplete;
  public String getAnnualPriceId() { return annualPriceId; }
  public void setAnnualPriceId(String value) { annualPriceId = value; }
  public boolean isSalesEnabled() { return salesEnabled; }
  public void setSalesEnabled(boolean value) { salesEnabled = value; }
  public boolean isCommercialRegistrationComplete() { return commercialRegistrationComplete; }
  public void setCommercialRegistrationComplete(boolean value) { commercialRegistrationComplete = value; }

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
