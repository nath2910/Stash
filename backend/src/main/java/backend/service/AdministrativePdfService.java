package backend.service;

import backend.dto.AdministrativeProfileResponse;
import backend.dto.AdministrativeSaleLineResponse;
import backend.dto.AdministrativeSourceResponse;
import backend.dto.AdministrativeSummaryResponse;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdministrativePdfService {

  private static final int MAX_BODY_LINES_PER_PAGE = 48;
  private static final int MAX_TEXT_WIDTH = 96;

  public byte[] generate(String documentType, AdministrativeSummaryResponse summary) {
    List<String> lines = switch (documentType) {
      case "urssaf-summary" -> urssafSummary(summary);
      case "fiscal-summary" -> fiscalSummary(summary);
      case "receipts-register" -> receiptsRegister(summary);
      case "purchases-register" -> purchasesRegister(summary);
      default -> throw new IllegalArgumentException("Document administratif inconnu");
    };
    return buildPdf(lines);
  }

  private List<String> urssafSummary(AdministrativeSummaryResponse summary) {
    AdministrativeProfileResponse profile = summary.profile();
    List<String> lines = new ArrayList<>();
    lines.add("FICHE DE SAISIE URSSAF - MICRO-ENTREPRISE");
    lines.add("Ce PDF n'est pas transmis a l'URSSAF et ne remplace pas la declaration officielle.");
    lines.add("Utilisation: recopier les champs ci-dessous sur autoentrepreneur.urssaf.fr.");
    lines.add("");
    lines.add("DECLARANT");
    lines.add("Nom: " + value(profile.displayName()));
    lines.add("SIRET: " + value(profile.siret()));
    lines.add("SIREN: " + value(profile.siren()));
    lines.add("Activite: " + value(profile.mainActivity()));
    lines.add("Regime: " + value(profile.fiscalRegime()));
    lines.add("Periodicite: " + declarationFrequency(profile));
    lines.add("");
    lines.add("CHAMPS A SAISIR");
    lines.add("Periode declaree: " + date(summary.periodStart()) + " au " + date(summary.periodEnd()));
    lines.add("Categorie: Vente de marchandises / Micro-BIC");
    lines.add("Montant du chiffre d'affaires encaisse: " + money(summary.periodRevenue()));
    lines.add("Nombre de ventes retenues par l'application: " + summary.periodSaleCount());
    lines.add("");
    lines.add("REGLE DE CALCUL");
    lines.add(summary.revenueRule());
    lines.add("Date utilisee par l'application: date de vente, faute de champ date d'encaissement distinct.");
    if (summary.incompleteSaleCount() > 0) {
      lines.add("Attention: " + summary.incompleteSaleCount() + " vente(s) ont une date ou un prix incomplet.");
    }
    lines.add("");
    lines.add("CONTROLE RAPIDE");
    lines.add("1. Verifier que toutes les ventes encaissees de la periode sont presentes.");
    lines.add("2. Verifier que le montant ci-dessus correspond au brut encaisse hors TVA.");
    lines.add("3. Saisir le montant sur le portail officiel URSSAF.");
    addSources(lines, summary.sources());
    return lines;
  }

  private List<String> fiscalSummary(AdministrativeSummaryResponse summary) {
    List<String> lines = header("Recap annuel fiscal preparatoire", summary);
    lines.add("Annee: " + summary.year());
    lines.add("CA annuel brut encaisse connu dans l'application: " + money(summary.annualRevenue()));
    lines.add("Nombre de ventes annuelles retenues: " + summary.annualSaleCount());
    lines.add("Usage prevu: aide preparatoire pour la declaration annuelle, dont 2042-C-PRO si applicable.");
    lines.add("Regle: " + summary.revenueRule());
    addSales(lines, summary.sales(), "Ventes de la periode affichee");
    addSources(lines, summary.sources());
    return lines;
  }

  private List<String> receiptsRegister(AdministrativeSummaryResponse summary) {
    List<String> lines = header("Registre des recettes", summary);
    lines.add("Total recettes connues sur la periode: " + money(summary.periodRevenue()));
    lines.add("Nombre de ventes: " + summary.periodSaleCount());
    addSales(lines, summary.sales(), "Recettes");
    addSources(lines, summary.sources());
    return lines;
  }

  private List<String> purchasesRegister(AdministrativeSummaryResponse summary) {
    List<String> lines = header("Registre des achats", summary);
    lines.add("Total achats connus sur la periode: " + money(summary.periodPurchaseTotal()));
    lines.add("Nombre d'achats: " + summary.periodPurchaseCount());
    lines.add("");
    lines.add("Achats");
    if (summary.purchases().isEmpty()) {
      lines.add("Aucun achat connu sur la periode.");
    } else {
      for (AdministrativeSaleLineResponse purchase : summary.purchases()) {
        lines.add(
            date(purchase.saleDate()) + " | " + purchase.reference() + " | " + purchase.itemName()
                + " | achat " + money(purchase.purchaseAmount()) + " | "
                + purchase.category() + " | paiement " + purchase.paymentMethod()
        );
      }
    }
    addSources(lines, summary.sources());
    return lines;
  }

  private List<String> header(String title, AdministrativeSummaryResponse summary) {
    AdministrativeProfileResponse profile = summary.profile();
    List<String> lines = new ArrayList<>();
    lines.add(title);
    lines.add("Document d'aide preparatoire genere par l'application.");
    lines.add("A verifier et reporter sur les services officiels competents.");
    lines.add("Ce document n'est pas un document officiel.");
    lines.add("Periode: " + date(summary.periodStart()) + " au " + date(summary.periodEnd()));
    lines.add("Generation: " + date(LocalDate.now()));
    lines.add("");
    lines.add("Identite");
    lines.add("Nom affiche: " + value(profile.displayName()));
    lines.add("Profil: " + value(profile.profileType() == null ? null : profile.profileType().name()));
    if (profile.siret() != null && !profile.siret().isBlank()) {
      lines.add("SIRET: " + profile.siret());
      lines.add("SIREN: " + value(profile.siren()));
    }
    if (profile.address() != null && !profile.address().isBlank()) {
      lines.add("Adresse administrative: " + profile.address());
    }
    if (profile.mainActivity() != null && !profile.mainActivity().isBlank()) {
      lines.add("Activite: " + profile.mainActivity());
    }
    if (profile.fiscalRegime() != null && !profile.fiscalRegime().isBlank()) {
      lines.add("Regime fiscal indique: " + profile.fiscalRegime());
    }
    lines.add("");
    return lines;
  }

  private String declarationFrequency(AdministrativeProfileResponse profile) {
    if (profile.declarationFrequency() == null) {
      return "Non renseignee";
    }
    return switch (profile.declarationFrequency()) {
      case MONTHLY -> "Mensuelle";
      case QUARTERLY -> "Trimestrielle";
      case UNKNOWN -> "Non renseignee";
    };
  }

  private void addSales(List<String> lines, List<AdministrativeSaleLineResponse> sales, String title) {
    lines.add("");
    lines.add(title);
    if (sales.isEmpty()) {
      lines.add("Aucune vente retenue.");
      return;
    }
    for (AdministrativeSaleLineResponse sale : sales) {
      lines.add(
          date(sale.saleDate()) + " | " + sale.reference() + " | " + sale.itemName()
              + " | encaisse " + money(sale.saleAmount()) + " | categorie " + sale.category()
              + " | paiement " + sale.paymentMethod()
      );
    }
  }

  private void addSources(List<String> lines, List<AdministrativeSourceResponse> sources) {
    lines.add("");
    lines.add("Sources officielles consultees");
    for (AdministrativeSourceResponse source : sources) {
      lines.add(source.label() + " | " + source.url() + " | verifie le " + source.lastVerified());
    }
  }

  private byte[] buildPdf(List<String> rawLines) {
    List<String> lines = new ArrayList<>();
    for (String rawLine : rawLines) {
      lines.addAll(wrap(sanitize(rawLine)));
    }
    List<List<String>> pages = paginate(lines);
    List<String> objects = new ArrayList<>();
    int pageCount = pages.size();

    StringBuilder kids = new StringBuilder();
    for (int i = 0; i < pageCount; i += 1) {
      if (i > 0) kids.append(' ');
      kids.append(4 + i * 2).append(" 0 R");
    }

    objects.add("<< /Type /Catalog /Pages 2 0 R >>");
    objects.add("<< /Type /Pages /Kids [" + kids + "] /Count " + pageCount + " >>");
    objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");

    for (int i = 0; i < pageCount; i += 1) {
      int contentObject = 5 + i * 2;
      String content = pageContent(pages.get(i));
      objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R >> >> /Contents " + contentObject + " 0 R >>");
      objects.add("<< /Length " + content.length() + " >>\nstream\n" + content + "\nendstream");
    }

    StringBuilder pdf = new StringBuilder();
    List<Integer> offsets = new ArrayList<>();
    pdf.append("%PDF-1.4\n");
    for (int i = 0; i < objects.size(); i += 1) {
      offsets.add(pdf.length());
      pdf.append(i + 1).append(" 0 obj\n").append(objects.get(i)).append("\nendobj\n");
    }
    int xrefOffset = pdf.length();
    pdf.append("xref\n0 ").append(objects.size() + 1).append("\n");
    pdf.append("0000000000 65535 f \n");
    for (Integer offset : offsets) {
      pdf.append(String.format("%010d 00000 n \n", offset));
    }
    pdf.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\n");
    pdf.append("startxref\n").append(xrefOffset).append("\n%%EOF\n");
    return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
  }

  private String pageContent(List<String> pageLines) {
    StringBuilder content = new StringBuilder();
    content.append("BT\n/F1 10 Tf\n50 800 Td\n14 TL\n");
    for (String line : pageLines) {
      content.append('(').append(escape(line)).append(") Tj\nT*\n");
    }
    content.append("ET");
    return content.toString();
  }

  private List<List<String>> paginate(List<String> lines) {
    List<List<String>> pages = new ArrayList<>();
    for (int i = 0; i < lines.size(); i += MAX_BODY_LINES_PER_PAGE) {
      pages.add(lines.subList(i, Math.min(i + MAX_BODY_LINES_PER_PAGE, lines.size())));
    }
    if (pages.isEmpty()) {
      pages.add(List.of(""));
    }
    return pages;
  }

  private List<String> wrap(String text) {
    if (text.length() <= MAX_TEXT_WIDTH) {
      return List.of(text);
    }
    List<String> lines = new ArrayList<>();
    String remaining = text;
    while (remaining.length() > MAX_TEXT_WIDTH) {
      int split = remaining.lastIndexOf(' ', MAX_TEXT_WIDTH);
      if (split < 40) split = MAX_TEXT_WIDTH;
      lines.add(remaining.substring(0, split).trim());
      remaining = remaining.substring(split).trim();
    }
    lines.add(remaining);
    return lines;
  }

  private String sanitize(String value) {
    String text = value == null ? "" : value;
    text = text.replace("EUR", "EUR").replace("€", "EUR");
    text = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    return text.replaceAll("[^\\x20-\\x7E]", " ");
  }

  private String escape(String value) {
    return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
  }

  private String date(LocalDate date) {
    return date == null ? "Non renseigne" : date.toString();
  }

  private String money(BigDecimal value) {
    BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
    return safeValue.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() + " EUR";
  }

  private String value(String value) {
    return value == null || value.isBlank() ? "Non renseigne" : value;
  }
}
