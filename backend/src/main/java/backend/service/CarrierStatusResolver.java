package backend.service;

import backend.entity.ParcelStatus;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class CarrierStatusResolver {

  private static final List<StatusRule> STATUS_RULES = List.of(
      new StatusRule(
          ParcelStatus.DELIVERED,
          List.of(
              new Phrase("votre colis a ete livre", 98),
              new Phrase("colis a ete livre", 96),
              new Phrase("remis au destinataire", 95),
              new Phrase("livraison effectuee", 95),
              new Phrase("livraison terminee", 94),
              new Phrase("delivery completed", 92),
              new Phrase("successfully delivered", 92),
              new Phrase("parcel delivered", 90),
              new Phrase("a ete distribue", 90),
              new Phrase("remis dans votre boite aux lettres", 89),
              new Phrase("colis retire", 88),
              new Phrase("retire par le destinataire", 88),
              new Phrase("colis livre", 88),
              new Phrase("delivered", 80),
              new Phrase("livre", 74),
              new Phrase("distribue", 72),
              new Phrase("remis", 70)
          )
      ),
      new StatusRule(
          ParcelStatus.EXCEPTION,
          List.of(
              new Phrase("delivery exception", 92),
              new Phrase("echec de livraison", 90),
              new Phrase("delivery failed", 90),
              new Phrase("retour expediteur", 88),
              new Phrase("returned to sender", 88),
              new Phrase("adresse incomplete", 86),
              new Phrase("incident de livraison", 84),
              new Phrase("anomalie de livraison", 84),
              new Phrase("incident", 74),
              new Phrase("anomalie", 72),
              new Phrase("retard", 70),
              new Phrase("impossible", 70),
              new Phrase("refuse", 70),
              new Phrase("perdu", 70)
          )
      ),
      new StatusRule(
          ParcelStatus.OUT_FOR_DELIVERY,
          List.of(
              new Phrase("out for delivery", 96),
              new Phrase("en cours de livraison", 96),
              new Phrase("delivery today", 92),
              new Phrase("livraison aujourd hui", 90),
              new Phrase("sera livre aujourd hui", 90),
              new Phrase("depart en livraison", 88),
              new Phrase("distribution en cours", 88),
              new Phrase("loaded on delivery vehicle", 88),
              new Phrase("with the delivery courier", 86),
              new Phrase("driver is on the way", 84),
              new Phrase("envoi en cours de livraison", 84),
              new Phrase("livraison imminente", 82)
          )
      ),
      new StatusRule(
          ParcelStatus.IN_TRANSIT,
          List.of(
              new Phrase("livraison ce jour", 86),
              new Phrase("available for pickup", 84),
              new Phrase("disponible au point relais", 84),
              new Phrase("disponible en point relais", 84),
              new Phrase("colis disponible au point relais", 84),
              new Phrase("disponible dans votre point relais", 84),
              new Phrase("mis a disposition", 82),
              new Phrase("point de retrait", 80),
              new Phrase("shipment in transit", 84),
              new Phrase("colis en transit", 82),
              new Phrase("in transit", 80),
              new Phrase("on the way", 76),
              new Phrase("pris en charge", 76),
              new Phrase("arrive en agence", 74),
              new Phrase("arrived at sorting center", 74),
              new Phrase("tri effectue", 74),
              new Phrase("en cours d acheminement", 74),
              new Phrase("achemine", 72),
              new Phrase("site logistique", 72),
              new Phrase("agence", 70),
              new Phrase("en transit", 70),
              new Phrase("depose", 68),
              new Phrase("en livraison", 68)
          )
      ),
      new StatusRule(
          ParcelStatus.REGISTERED,
          List.of(
              new Phrase("shipment information received", 82),
              new Phrase("etiquette creee", 80),
              new Phrase("label created", 80),
              new Phrase("bordereau cree", 80),
              new Phrase("pre-advice", 78),
              new Phrase("annonce au transporteur", 76),
              new Phrase("preparation de votre colis", 76),
              new Phrase("preparation chez l expediteur", 74),
              new Phrase("colis en preparation", 74),
              new Phrase("prepare chez l expediteur", 74),
              new Phrase("information recue", 72),
              new Phrase("information recueillie", 72)
          )
      )
  );

  private CarrierStatusResolver() {
  }

  static ParcelStatus resolve(String... values) {
    return best(values)
        .map(StatusMatch::status)
        .orElse(ParcelStatus.UNKNOWN);
  }

  static Optional<StatusMatch> best(String... values) {
    if (values == null || values.length == 0) {
      return Optional.empty();
    }

    StatusMatch best = null;
    for (String value : values) {
      if (value == null || value.isBlank()) {
        continue;
      }
      StatusMatch candidate = bestForSingleValue(value);
      if (candidate != null && (best == null || candidate.score() > best.score())) {
        best = candidate;
      }
    }
    return Optional.ofNullable(best);
  }

  static String normalizeForMatch(String value) {
    String lowercase = value == null ? "" : value.toLowerCase(Locale.ROOT);
    return Normalizer.normalize(lowercase, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "");
  }

  private static StatusMatch bestForSingleValue(String value) {
    String normalized = normalizeForMatch(value);
    StatusMatch best = null;
    for (StatusRule rule : STATUS_RULES) {
      for (Phrase phrase : rule.phrases()) {
        if (!normalized.contains(phrase.value())) {
          continue;
        }
        StatusMatch candidate = new StatusMatch(rule.status(), phrase.value(), phrase.score());
        if (best == null || candidate.score() > best.score()) {
          best = candidate;
        }
      }
    }
    return best;
  }

  record StatusMatch(ParcelStatus status, String phrase, int score) {
  }

  private record StatusRule(ParcelStatus status, List<Phrase> phrases) {
  }

  private record Phrase(String value, int score) {
  }
}
