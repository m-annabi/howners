package com.howners.gestion.service.esignature;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Factory pour obtenir le bon provider de signature électronique
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ESignatureProviderFactory {

    private final DocuSignProvider docuSignProvider;

    @Value("${esignature.provider:docusign}")
    private String providerName;

    /**
     * Retourne le provider configuré
     */
    public ESignatureProvider getProvider() {
        log.debug("Getting e-signature provider: {}", providerName);

        return switch (providerName.toLowerCase()) {
            case "docusign" -> docuSignProvider;
            // case "hellosign" -> helloSignProvider;
            // case "adobesign" -> adobeSignProvider;
            default -> {
                log.warn("Unknown provider '{}', falling back to DocuSign", providerName);
                yield docuSignProvider;
            }
        };
    }

    /**
     * Retourne le nom du provider actuel
     */
    public String getProviderName() {
        return providerName.toUpperCase();
    }
}
