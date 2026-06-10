package com.howners.gestion.service.ai;

import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.dto.ai.DraftLeaseRequest;
import com.howners.gestion.dto.ai.DraftLeaseResponse;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Brouillon de bail assisté par IA. Si {@code openai.api-key} est fourni, on
 * appelle GPT-4o ; sinon on retourne un brouillon mock construit à partir des
 * données du rental — utile pour tester le parcours sans clé.
 *
 * Notes business avant production :
 *  - Le contenu généré doit être validé par un avocat. Le disclaimer le rappelle.
 *  - Pour la France, il faut ajuster les prompts pour rester conforme à la loi
 *    de 89 (durées minimales, dépôt, état des lieux, etc.).
 *  - Considérer un cache (ratio coût/utilité) car la même propriété + locataire
 *    donne sensiblement le même bail.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiLeaseService {

    private final RentalRepository rentalRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${openai.api-key:}")
    private String openaiApiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String openaiModel;

    @Transactional(readOnly = true)
    public DraftLeaseResponse draft(DraftLeaseRequest request) {
        Rental rental = rentalRepository.findById(request.rentalId())
                .orElseThrow(() -> new ResourceNotFoundException("Rental not found"));

        boolean hasKey = openaiApiKey != null && !openaiApiKey.isBlank();
        String content = hasKey ? callOpenAi(rental, request) : mockDraft(rental, request);

        return new DraftLeaseResponse(
                content,
                hasKey ? openaiModel : "mock",
                "Ce brouillon est généré automatiquement. Faites-le relire par un professionnel du droit avant signature."
        );
    }

    private String mockDraft(Rental r, DraftLeaseRequest req) {
        boolean furnished = Boolean.TRUE.equals(req.furnished());
        int months = req.leaseMonths() != null ? req.leaseMonths() : (furnished ? 12 : 36);
        String petsLine = Boolean.TRUE.equals(req.petsAllowed())
                ? "Les animaux familiers sont autorisés dans la limite des dispositions du règlement de copropriété."
                : "Tout animal nécessite l'accord préalable écrit du bailleur.";

        StringBuilder sb = new StringBuilder();
        sb.append("CONTRAT DE BAIL D'HABITATION ").append(furnished ? "MEUBLÉE" : "NON MEUBLÉE").append("\n\n");
        sb.append("ARTICLE 1 — OBJET DU CONTRAT\n")
          .append("Le Bailleur loue au Locataire le logement situé : ")
          .append(r.getProperty().getName()).append("\n\n");
        sb.append("ARTICLE 2 — DURÉE\n")
          .append("Le présent bail est conclu pour une durée de ").append(months).append(" mois.\n\n");
        sb.append("ARTICLE 3 — LOYER & CHARGES\n")
          .append("Loyer mensuel : ").append(r.getMonthlyRent()).append(" EUR.\n");
        if (r.getCharges() != null) sb.append("Charges : ").append(r.getCharges()).append(" EUR.\n");
        sb.append("\n");
        sb.append("ARTICLE 4 — DÉPÔT DE GARANTIE\n")
          .append("Le locataire verse un dépôt de garantie de ")
          .append(r.getDepositAmount() != null ? r.getDepositAmount() : r.getMonthlyRent())
          .append(" EUR.\n\n");
        sb.append("ARTICLE 5 — PRÉAVIS\n")
          .append("Le locataire peut résilier à tout moment en respectant un préavis de ")
          .append(req.noticeMonths() != null ? req.noticeMonths() : (furnished ? 1 : 3))
          .append(" mois.\n\n");
        sb.append("ARTICLE 6 — ANIMAUX\n").append(petsLine).append("\n\n");
        if (req.customClauses() != null && !req.customClauses().isBlank()) {
            sb.append("ARTICLE 7 — CLAUSES PARTICULIÈRES\n").append(req.customClauses()).append("\n\n");
        }
        sb.append("[Brouillon mock — branchez OPENAI_API_KEY pour générer une version complète et personnalisée.]");
        return sb.toString();
    }

    private String callOpenAi(Rental r, DraftLeaseRequest req) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            String system = "Tu es un juriste français spécialisé en droit du bail (loi 89-462). "
                    + "Tu rédiges des baux conformes, clairs et équilibrés. "
                    + "Respecte impérativement les durées minimales, les obligations du bailleur et du locataire, "
                    + "et les clauses interdites.";

            String user = "Rédige le bail à partir des informations suivantes :\n"
                    + "- Bien : " + r.getProperty().getName() + " (" + r.getProperty().getCity() + ")\n"
                    + "- Loyer mensuel : " + r.getMonthlyRent() + " EUR\n"
                    + "- Charges : " + r.getCharges() + " EUR\n"
                    + "- Dépôt : " + r.getDepositAmount() + " EUR\n"
                    + "- Meublé : " + Boolean.TRUE.equals(req.furnished()) + "\n"
                    + "- Durée demandée : " + req.leaseMonths() + " mois\n"
                    + "- Préavis demandé : " + req.noticeMonths() + " mois\n"
                    + "- Animaux autorisés : " + Boolean.TRUE.equals(req.petsAllowed()) + "\n"
                    + (req.customClauses() != null
                            ? "- Clauses particulières souhaitées : " + req.customClauses() + "\n"
                            : "");

            Map<String, Object> body = Map.of(
                    "model", openaiModel,
                    "temperature", 0.2,
                    "messages", new Object[]{
                            Map.of("role", "system", "content", system),
                            Map.of("role", "user", "content", user)
                    }
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            Map<?, ?> resp = restTemplate.postForObject(
                    "https://api.openai.com/v1/chat/completions", entity, Map.class);
            if (resp != null && resp.get("choices") instanceof java.util.List<?> choices && !choices.isEmpty()) {
                Object first = choices.get(0);
                if (first instanceof Map<?, ?> firstMap && firstMap.get("message") instanceof Map<?, ?> msg) {
                    Object content = msg.get("content");
                    if (content instanceof String s) return s;
                }
            }
        } catch (Exception e) {
            log.warn("OpenAI call failed, falling back to mock: {}", e.getMessage());
        }
        return mockDraft(r, req);
    }
}
