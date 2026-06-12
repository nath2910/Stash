package backend.service;

import backend.dto.LegalProfileResponse;
import backend.dto.LegalProfileUpdateRequest;
import backend.dto.UserMapper;
import backend.entity.BusinessActivityType;
import backend.entity.BusinessRegime;
import backend.entity.DeclarationFrequency;
import backend.entity.DeclaredRevenueThreshold;
import backend.entity.LegalProfileType;
import backend.entity.TaxCategory;
import backend.entity.User;
import backend.entity.VatStatus;
import backend.repository.UserRepository;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LegalProfileService {

  private final UserRepository userRepository;

  public LegalProfileService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public LegalProfileResponse getLegalProfile(Long userId) {
    return toResponse(findUser(userId));
  }

  @Transactional
  public LegalProfileResponse updateLegalProfile(Long userId, LegalProfileUpdateRequest request) {
    if (request == null) {
      throw badRequest("Profil administratif manquant");
    }

    User user = findUser(userId);
    LegalProfileType profileType = parseRequiredEnum(
        LegalProfileType.class,
        request.legalProfileType(),
        "legalProfileType"
    );

    user.setLegalProfileType(profileType);

    if (profileType == LegalProfileType.MICRO_ENTREPRISE_UNDER_200K_YEAR) {
      String siret = trimToNull(request.siret());
      if (siret == null) {
        throw badRequest("Le SIRET est obligatoire pour une micro-entreprise");
      }
      if (!siret.matches("\\d{14}")) {
        throw badRequest("Le SIRET doit contenir exactement 14 chiffres");
      }

      user.setSiret(siret);
      user.setTaxCategory(parseOptionalEnum(TaxCategory.class, request.taxCategory(), TaxCategory.BIC, "taxCategory"));
      user.setBusinessRegime(parseOptionalEnum(
          BusinessRegime.class,
          request.businessRegime(),
          BusinessRegime.MICRO_ENTREPRISE,
          "businessRegime"
      ));
      user.setBusinessActivityType(parseOptionalEnum(
          BusinessActivityType.class,
          request.businessActivityType(),
          BusinessActivityType.ACHAT_REVENTE,
          "businessActivityType"
      ));
      user.setDeclaredRevenueThreshold(parseOptionalEnum(
          DeclaredRevenueThreshold.class,
          request.declaredRevenueThreshold(),
          DeclaredRevenueThreshold.UNDER_200K_YEAR,
          "declaredRevenueThreshold"
      ));
      user.setVatNumber(trimToNull(request.vatNumber()));
      user.setVatStatus(parseOptionalEnum(VatStatus.class, request.vatStatus(), VatStatus.UNKNOWN, "vatStatus"));
      user.setDeclarationFrequency(parseOptionalEnum(
          DeclarationFrequency.class,
          request.declarationFrequency(),
          DeclarationFrequency.UNKNOWN,
          "declarationFrequency"
      ));
    } else {
      clearMicroProfileFields(user);
    }

    user.setLegalProfileCompleted(true);
    user.setLegalProfileUpdatedAt(OffsetDateTime.now());
    return toResponse(userRepository.save(user));
  }

  private LegalProfileResponse toResponse(User user) {
    return UserMapper.toLegalProfile(user);
  }

  private User findUser(Long userId) {
    if (userId == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable");
    }
    return userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
  }

  private void clearMicroProfileFields(User user) {
    user.setSiret(null);
    user.setTaxCategory(null);
    user.setBusinessRegime(null);
    user.setBusinessActivityType(null);
    user.setDeclaredRevenueThreshold(null);
    user.setVatNumber(null);
    user.setVatStatus(null);
    user.setDeclarationFrequency(null);
  }

  private <T extends Enum<T>> T parseRequiredEnum(Class<T> enumType, String value, String fieldName) {
    String text = trimToNull(value);
    if (text == null) {
      throw badRequest("Valeur manquante pour " + fieldName);
    }
    return parseEnum(enumType, text, fieldName);
  }

  private <T extends Enum<T>> T parseOptionalEnum(Class<T> enumType, String value, T fallback, String fieldName) {
    String text = trimToNull(value);
    if (text == null) {
      return fallback;
    }
    return parseEnum(enumType, text, fieldName);
  }

  private <T extends Enum<T>> T parseEnum(Class<T> enumType, String value, String fieldName) {
    try {
      return Enum.valueOf(enumType, value.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw badRequest("Valeur invalide pour " + fieldName);
    }
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private ResponseStatusException badRequest(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}
