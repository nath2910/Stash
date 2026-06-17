package backend.dto;

import backend.entity.User;

public final class UserMapper {
  private UserMapper() {}

  public static UserMeResponse toMe(User u) {
    return new UserMeResponse(
      u.getId(),
      u.getEmail(),
      u.getFirstName(),
      u.getLastName(),
      u.getPictureUrl(),
      u.getProvider(),
      u.isEmailVerified(),
      u.getSubscriptionStatus(),
      u.getSubscriptionCurrentPeriodEnd(),
      u.isLegalProfileCompleted(),
      toLegalProfile(u)
    );
  }

  public static LegalProfileResponse toLegalProfile(User u) {
    return new LegalProfileResponse(
      u.getLegalProfileType(),
      u.getSiret(),
      u.getSiren(),
      u.getTaxCategory(),
      u.getBusinessRegime(),
      u.getBusinessActivityType(),
      u.getDeclaredRevenueThreshold(),
      u.getVatNumber(),
      u.getVatStatus(),
      u.getDeclarationFrequency(),
      u.getWithholdingTaxOption(),
      u.getVatFranchise(),
      u.getActivityStartDate(),
      u.isLegalProfileCompleted(),
      u.getLegalProfileUpdatedAt()
    );
  }
}
