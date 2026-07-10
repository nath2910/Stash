package backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
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

  @Column(name = "siren", length = 9)
  private String siren;

  @Column(name = "administrative_display_name", length = 240)
  private String administrativeDisplayName;

  @Column(name = "trade_name", length = 240)
  private String tradeName;

  @Column(name = "administrative_address", length = 500)
  private String administrativeAddress;

  @Column(name = "main_activity", length = 160)
  private String mainActivity;

  @Column(name = "fiscal_regime", length = 120)
  private String fiscalRegime;

  @Column(name = "withholding_tax_option", length = 20)
  private String withholdingTaxOption;

  @Column(name = "vat_franchise", length = 20)
  private String vatFranchise;

  @Column(name = "activity_start_date")
  private LocalDate activityStartDate;

  @Column(name = "administrative_notes", columnDefinition = "text")
  private String administrativeNotes;

  @Column(name = "administrative_verification_status", length = 80)
  private String administrativeVerificationStatus;

  @Column(name = "administrative_legal_status", length = 40)
  private String administrativeLegalStatus;

  @Column(name = "administrative_activities", length = 500)
  private String administrativeActivities;

  @Column(name = "administrative_vat_regime", length = 40)
  private String administrativeVatRegime;

  @Column(name = "administrative_declarations", length = 500)
  private String administrativeDeclarations;

  @Column(name = "business_name", length = 240)
  private String businessName;

  @Column(name = "owner_name", length = 240)
  private String ownerName;

  @Column(name = "legal_form", length = 80)
  private String legalForm;

  @Column(name = "fiscal_year_end_month")
  private Integer fiscalYearEndMonth;

  @Column(name = "fiscal_year_end_day")
  private Integer fiscalYearEndDay;

  @Column(name = "declaration_periodicity", length = 40)
  private String declarationPeriodicity;

  @Column(name = "urssaf_category", length = 120)
  private String urssafCategory;

  @Column(name = "default_vat_rate", precision = 5, scale = 2)
  private BigDecimal defaultVatRate;

  @Column(name = "admin_uses_online_platforms", nullable = false)
  private boolean adminUsesOnlinePlatforms = false;

  @Column(name = "admin_buys_for_resale", nullable = false)
  private boolean adminBuysForResale = false;

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

  public String getSiren() { return siren; }
  public void setSiren(String siren) { this.siren = siren; }

  public String getAdministrativeDisplayName() { return administrativeDisplayName; }
  public void setAdministrativeDisplayName(String administrativeDisplayName) {
    this.administrativeDisplayName = administrativeDisplayName;
  }

  public String getTradeName() { return tradeName; }
  public void setTradeName(String tradeName) { this.tradeName = tradeName; }

  public String getAdministrativeAddress() { return administrativeAddress; }
  public void setAdministrativeAddress(String administrativeAddress) {
    this.administrativeAddress = administrativeAddress;
  }

  public String getMainActivity() { return mainActivity; }
  public void setMainActivity(String mainActivity) { this.mainActivity = mainActivity; }

  public String getFiscalRegime() { return fiscalRegime; }
  public void setFiscalRegime(String fiscalRegime) { this.fiscalRegime = fiscalRegime; }

  public String getWithholdingTaxOption() { return withholdingTaxOption; }
  public void setWithholdingTaxOption(String withholdingTaxOption) {
    this.withholdingTaxOption = withholdingTaxOption;
  }

  public String getVatFranchise() { return vatFranchise; }
  public void setVatFranchise(String vatFranchise) { this.vatFranchise = vatFranchise; }

  public LocalDate getActivityStartDate() { return activityStartDate; }
  public void setActivityStartDate(LocalDate activityStartDate) {
    this.activityStartDate = activityStartDate;
  }

  public String getAdministrativeNotes() { return administrativeNotes; }
  public void setAdministrativeNotes(String administrativeNotes) {
    this.administrativeNotes = administrativeNotes;
  }

  public String getAdministrativeVerificationStatus() { return administrativeVerificationStatus; }
  public void setAdministrativeVerificationStatus(String administrativeVerificationStatus) {
    this.administrativeVerificationStatus = administrativeVerificationStatus;
  }

  public String getAdministrativeLegalStatus() { return administrativeLegalStatus; }
  public void setAdministrativeLegalStatus(String administrativeLegalStatus) {
    this.administrativeLegalStatus = administrativeLegalStatus;
  }

  public String getAdministrativeActivities() { return administrativeActivities; }
  public void setAdministrativeActivities(String administrativeActivities) {
    this.administrativeActivities = administrativeActivities;
  }

  public String getAdministrativeVatRegime() { return administrativeVatRegime; }
  public void setAdministrativeVatRegime(String administrativeVatRegime) {
    this.administrativeVatRegime = administrativeVatRegime;
  }

  public String getAdministrativeDeclarations() { return administrativeDeclarations; }
  public void setAdministrativeDeclarations(String administrativeDeclarations) {
    this.administrativeDeclarations = administrativeDeclarations;
  }

  public String getBusinessName() { return businessName; }
  public void setBusinessName(String businessName) { this.businessName = businessName; }

  public String getOwnerName() { return ownerName; }
  public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

  public String getLegalForm() { return legalForm; }
  public void setLegalForm(String legalForm) { this.legalForm = legalForm; }

  public Integer getFiscalYearEndMonth() { return fiscalYearEndMonth; }
  public void setFiscalYearEndMonth(Integer fiscalYearEndMonth) {
    this.fiscalYearEndMonth = fiscalYearEndMonth;
  }

  public Integer getFiscalYearEndDay() { return fiscalYearEndDay; }
  public void setFiscalYearEndDay(Integer fiscalYearEndDay) { this.fiscalYearEndDay = fiscalYearEndDay; }

  public String getDeclarationPeriodicity() { return declarationPeriodicity; }
  public void setDeclarationPeriodicity(String declarationPeriodicity) {
    this.declarationPeriodicity = declarationPeriodicity;
  }

  public String getUrssafCategory() { return urssafCategory; }
  public void setUrssafCategory(String urssafCategory) { this.urssafCategory = urssafCategory; }

  public BigDecimal getDefaultVatRate() { return defaultVatRate; }
  public void setDefaultVatRate(BigDecimal defaultVatRate) { this.defaultVatRate = defaultVatRate; }

  public boolean isAdminUsesOnlinePlatforms() { return adminUsesOnlinePlatforms; }
  public void setAdminUsesOnlinePlatforms(boolean adminUsesOnlinePlatforms) {
    this.adminUsesOnlinePlatforms = adminUsesOnlinePlatforms;
  }

  public boolean isAdminBuysForResale() { return adminBuysForResale; }
  public void setAdminBuysForResale(boolean adminBuysForResale) {
    this.adminBuysForResale = adminBuysForResale;
  }

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
