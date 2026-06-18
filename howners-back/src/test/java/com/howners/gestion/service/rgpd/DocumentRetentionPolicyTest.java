package com.howners.gestion.service.rgpd;

import com.howners.gestion.domain.document.DocumentType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentRetentionPolicyTest {

    @Test
    void conserveLesDocumentsAObligationLegale() {
        assertThat(DocumentRetentionPolicy.isRetainedOnErasure(DocumentType.CONTRACT)).isTrue();
        assertThat(DocumentRetentionPolicy.isRetainedOnErasure(DocumentType.INVOICE)).isTrue();
        assertThat(DocumentRetentionPolicy.isRetainedOnErasure(DocumentType.RECEIPT)).isTrue();
        assertThat(DocumentRetentionPolicy.isRetainedOnErasure(DocumentType.INVENTORY)).isTrue();
        assertThat(DocumentRetentionPolicy.isRetainedOnErasure(DocumentType.MISE_EN_DEMEURE)).isTrue();
        assertThat(DocumentRetentionPolicy.isRetainedOnErasure(DocumentType.SIGNATURE)).isTrue();
        assertThat(DocumentRetentionPolicy.isRetainedOnErasure(DocumentType.PHOTOS)).isTrue();
    }

    @Test
    void supprimeLesPiecesPersonnelles() {
        assertThat(DocumentRetentionPolicy.isRetainedOnErasure(DocumentType.ID_CARD)).isFalse();
        assertThat(DocumentRetentionPolicy.isRetainedOnErasure(DocumentType.IDENTITY)).isFalse();
        assertThat(DocumentRetentionPolicy.isRetainedOnErasure(DocumentType.PROOF_INCOME)).isFalse();
        assertThat(DocumentRetentionPolicy.isRetainedOnErasure(DocumentType.PROOF_OF_INCOME)).isFalse();
        assertThat(DocumentRetentionPolicy.isRetainedOnErasure(DocumentType.PROOF_ADDRESS)).isFalse();
        assertThat(DocumentRetentionPolicy.isRetainedOnErasure(DocumentType.PROOF_OF_RESIDENCE)).isFalse();
        assertThat(DocumentRetentionPolicy.isRetainedOnErasure(DocumentType.BANK_STATEMENT)).isFalse();
        assertThat(DocumentRetentionPolicy.isRetainedOnErasure(DocumentType.TAX_NOTICE)).isFalse();
        assertThat(DocumentRetentionPolicy.isRetainedOnErasure(DocumentType.EMPLOYMENT_CONTRACT)).isFalse();
    }

    @Test
    void supprimeParDefautLesTypesNonListes() {
        // OTHER et null = pièce personnelle par défaut (protecteur de la vie privée).
        assertThat(DocumentRetentionPolicy.isRetainedOnErasure(DocumentType.OTHER)).isFalse();
        assertThat(DocumentRetentionPolicy.isRetainedOnErasure(null)).isFalse();
    }
}
