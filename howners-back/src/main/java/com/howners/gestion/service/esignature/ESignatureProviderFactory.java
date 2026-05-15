package com.howners.gestion.service.esignature;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Factory pour obtenir le bon provider de signature électronique.
 *
 * Fallback automatique : si {@code esignature.provider=docusign} mais que les
 * credentials DocuSign ne sont pas configurés, on retombe sur le provider
 * interne (canvas HTML5) plutôt que de planter à l'envoi. Le mode "internal"
 * peut aussi être choisi explicitement via {@code esignature.provider=internal}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ESignatureProviderFactory {

    private final DocuSignProvider docuSignProvider;
    private final InternalProvider internalProvider;

    @Value("${esignature.provider:docusign}")
    private String providerName;

    @Value("${docusign.private-key:}")
    private String docusignPrivateKey;

    @Value("${docusign.integration-key:}")
    private String docusignIntegrationKey;

    public ESignatureProvider getProvider() {
        log.debug("Getting e-signature provider: {}", providerName);

        return switch (providerName.toLowerCase()) {
            case "internal" -> internalProvider;
            case "docusign" -> docuSignIfConfigured();
            default -> {
                log.warn("Unknown provider '{}', falling back to internal canvas", providerName);
                yield internalProvider;
            }
        };
    }

    private ESignatureProvider docuSignIfConfigured() {
        boolean configured = docusignPrivateKey != null && !docusignPrivateKey.isBlank()
                && docusignIntegrationKey != null && !docusignIntegrationKey.isBlank();
        if (!configured) {
            log.warn("DocuSign credentials missing — falling back to internal canvas provider for this send.");
            return internalProvider;
        }
        return docuSignProvider;
    }

    /**
     * Nom du provider effectif (utilisé lorsque ContractESignatureService persiste
     * le champ {@code provider} sur ContractSignatureRequest).
     */
    public String getProviderName() {
        ESignatureProvider effective = getProvider();
        return effective instanceof InternalProvider ? "INTERNAL" : "DOCUSIGN";
    }
}
