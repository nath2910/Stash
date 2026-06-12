package backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "users", schema = "public")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "first_name", length = 100, nullable = false)
  private String firstName = "";

  @Column(name = "last_name", length = 100, nullable = false)
  private String lastName = "";

  @Column(length = 200, nullable = false, unique = true)
  private String email;

  @JsonIgnore
  @Column(nullable = false)
  private String password = "";

  @Column(length = 20, nullable = false)
  private String provider = "LOCAL"; // LOCAL | GOOGLE

  @Column(name = "provider_id", length = 255)
  private String providerId; // sub google

  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified = false;

  @Column(name = "picture_url")
  private String pictureUrl;

  @Column(name = "stripe_customer_id", length = 255)
  private String stripeCustomerId;

  @Column(name = "subscription_status", length = 32, nullable = false)
  private String subscriptionStatus = "inactive"; // inactive | active | past_due | canceled

  @Column(name = "subscription_current_period_end")
  private java.time.OffsetDateTime subscriptionCurrentPeriodEnd;

  @Column(name = "discord_id", length = 255)
  private String discordId;

  @Enumerated(EnumType.STRING)
  @Column(name = "legal_profile_type", length = 64)
  private LegalProfileType legalProfileType;

  @Column(name = "siret", length = 14)
  private String siret;

  @Enumerated(EnumType.STRING)
  @Column(name = "tax_category", length = 40)
  private TaxCategory taxCategory;

  @Enumerated(EnumType.STRING)
  @Column(name = "business_regime", length = 40)
  private BusinessRegime businessRegime;

  @Enumerated(EnumType.STRING)
  @Column(name = "business_activity_type", length = 40)
  private BusinessActivityType businessActivityType;

  @Enumerated(EnumType.STRING)
  @Column(name = "declared_revenue_threshold", length = 40)
  private DeclaredRevenueThreshold declaredRevenueThreshold;

  @Column(name = "vat_number", length = 32)
  private String vatNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "vat_status", length = 20)
  private VatStatus vatStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "declaration_frequency", length = 20)
  private DeclarationFrequency declarationFrequency;

  @Column(name = "legal_profile_completed", nullable = false)
  private boolean legalProfileCompleted = false;

  @Column(name = "legal_profile_updated_at")
  private OffsetDateTime legalProfileUpdatedAt;

  public User() {}

  public Long getId() { return id; }

  public String getFirstName() { return firstName; }
  public void setFirstName(String firstName) { this.firstName = firstName; }

  public String getLastName() { return lastName; }
  public void setLastName(String lastName) { this.lastName = lastName; }

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }

  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }

  public String getProvider() { return provider; }
  public void setProvider(String provider) { this.provider = provider; }

  public String getProviderId() { return providerId; }
  public void setProviderId(String providerId) { this.providerId = providerId; }

  public boolean isEmailVerified() { return emailVerified; }
  public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

  public String getPictureUrl() { return pictureUrl; }
  public void setPictureUrl(String pictureUrl) { this.pictureUrl = pictureUrl; }

  public String getStripeCustomerId() { return stripeCustomerId; }
  public void setStripeCustomerId(String stripeCustomerId) { this.stripeCustomerId = stripeCustomerId; }

  public String getSubscriptionStatus() { return subscriptionStatus; }
  public void setSubscriptionStatus(String subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }

  public java.time.OffsetDateTime getSubscriptionCurrentPeriodEnd() { return subscriptionCurrentPeriodEnd; }
  public void setSubscriptionCurrentPeriodEnd(java.time.OffsetDateTime subscriptionCurrentPeriodEnd) {
    this.subscriptionCurrentPeriodEnd = subscriptionCurrentPeriodEnd;
  }

  public String getDiscordId() { return discordId; }
  public void setDiscordId(String discordId) { this.discordId = discordId; }

  public LegalProfileType getLegalProfileType() { return legalProfileType; }
  public void setLegalProfileType(LegalProfileType legalProfileType) { this.legalProfileType = legalProfileType; }

  public String getSiret() { return siret; }
  public void setSiret(String siret) { this.siret = siret; }

  public TaxCategory getTaxCategory() { return taxCategory; }
  public void setTaxCategory(TaxCategory taxCategory) { this.taxCategory = taxCategory; }

  public BusinessRegime getBusinessRegime() { return businessRegime; }
  public void setBusinessRegime(BusinessRegime businessRegime) { this.businessRegime = businessRegime; }

  public BusinessActivityType getBusinessActivityType() { return businessActivityType; }
  public void setBusinessActivityType(BusinessActivityType businessActivityType) {
    this.businessActivityType = businessActivityType;
  }

  public DeclaredRevenueThreshold getDeclaredRevenueThreshold() { return declaredRevenueThreshold; }
  public void setDeclaredRevenueThreshold(DeclaredRevenueThreshold declaredRevenueThreshold) {
    this.declaredRevenueThreshold = declaredRevenueThreshold;
  }

  public String getVatNumber() { return vatNumber; }
  public void setVatNumber(String vatNumber) { this.vatNumber = vatNumber; }

  public VatStatus getVatStatus() { return vatStatus; }
  public void setVatStatus(VatStatus vatStatus) { this.vatStatus = vatStatus; }

  public DeclarationFrequency getDeclarationFrequency() { return declarationFrequency; }
  public void setDeclarationFrequency(DeclarationFrequency declarationFrequency) {
    this.declarationFrequency = declarationFrequency;
  }

  public boolean isLegalProfileCompleted() { return legalProfileCompleted; }
  public void setLegalProfileCompleted(boolean legalProfileCompleted) {
    this.legalProfileCompleted = legalProfileCompleted;
  }

  public OffsetDateTime getLegalProfileUpdatedAt() { return legalProfileUpdatedAt; }
  public void setLegalProfileUpdatedAt(OffsetDateTime legalProfileUpdatedAt) {
    this.legalProfileUpdatedAt = legalProfileUpdatedAt;
  }
}
