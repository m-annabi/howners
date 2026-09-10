package com.howners.gestion.service.contract;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

/**
 * Fournit la notice d'information (droits et obligations des locataires et bailleurs), annexe
 * obligatoire du bail d'habitation (art. 3 de la loi du 6 juillet 1989). Voir T-01 de
 * AUDIT-BAUX-CONFORMITE.md : la notice était listée dans les templates mais jamais jointe.
 *
 * Le HTML est chargé une fois depuis les ressources et annexé au PDF du contrat via le
 * paramètre {@code appendixHtml} de {@link PdfService}. Le fichier ressource peut être remplacé
 * par la version officielle validée par le juriste sans changer de code.
 */
@Service
@Slf4j
public class LegalNoticeService {

    private static final String RESOURCE = "legal/notice-information.html";

    private final String noticeHtml;

    public LegalNoticeService() {
        this.noticeHtml = load();
    }

    private String load() {
        try {
            return StreamUtils.copyToString(
                    new ClassPathResource(RESOURCE).getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Ne jamais bloquer la génération d'un contrat si la ressource est absente/illisible.
            log.error("Notice d'information introuvable ({}) : le contrat sera généré sans annexe", RESOURCE, e);
            return "";
        }
    }

    /** Bloc HTML de la notice à annexer au PDF du contrat (chaîne vide si indisponible). */
    public String noticeAppendixHtml() {
        return noticeHtml;
    }
}
