package com.howners.gestion.service.security;

import com.howners.gestion.domain.security.EncryptionKey;
import com.howners.gestion.repository.EncryptionKeyRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    private final EncryptionKeyRepository encryptionKeyRepository;

    @Value("${storage.encryption.enabled:false}")
    private boolean encryptionEnabled;

    @Value("${storage.encryption.master-key:default-master-key-change-in-production}")
    private String masterKey;

    @PostConstruct
    public void init() {
        if (encryptionEnabled) {
            if (masterKey == null || masterKey.isBlank()
                    || "default-master-key-change-in-production".equals(masterKey)) {
                throw new IllegalStateException(
                        "storage.encryption.master-key doit être défini (valeur forte, non par défaut) "
                        + "lorsque le chiffrement de stockage est activé.");
            }
            ensureActiveKeyExists();
        }
    }

    /**
     * Clé de chiffrement de clés (KEK) dérivée de la master-key (SHA-256 → AES-256). Elle chiffre
     * la clé de données (DEK) avant stockage, pour que celle-ci ne soit jamais en clair en base.
     */
    private SecretKey kek() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(masterKey.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Impossible de dériver la clé maître", e);
        }
    }

    /** Chiffre (enveloppe) une DEK avec la KEK : [IV(12)][DEK chiffrée + tag GCM], en base64. */
    private String wrapKey(SecretKey dek) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, kek(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] enc = cipher.doFinal(dek.getEncoded());
            byte[] out = new byte[iv.length + enc.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(enc, 0, out, iv.length, enc.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new RuntimeException("Failed to wrap data key", e);
        }
    }

    public byte[] encrypt(byte[] data) {
        if (!encryptionEnabled) {
            return data;
        }

        try {
            EncryptionKey activeKey = encryptionKeyRepository.findFirstByActiveTrueOrderByCreatedAtDesc()
                    .orElseThrow(() -> new RuntimeException("No active encryption key found"));

            SecretKey secretKey = decodeKey(activeKey.getEncryptedKey());

            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(data);

            // Prepend IV to encrypted data: [IV (12 bytes)][encrypted data]
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);

            return result;
        } catch (Exception e) {
            log.error("Encryption failed: {}", e.getMessage());
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }

    public byte[] decrypt(byte[] encryptedData) {
        if (!encryptionEnabled) {
            return encryptedData;
        }

        try {
            EncryptionKey activeKey = encryptionKeyRepository.findFirstByActiveTrueOrderByCreatedAtDesc()
                    .orElseThrow(() -> new RuntimeException("No active encryption key found"));

            SecretKey secretKey = decodeKey(activeKey.getEncryptedKey());

            // Extract IV from the beginning of the data
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encryptedData, 0, iv, 0, iv.length);

            byte[] cipherText = new byte[encryptedData.length - iv.length];
            System.arraycopy(encryptedData, iv.length, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return cipher.doFinal(cipherText);
        } catch (Exception e) {
            log.error("Decryption failed: {}", e.getMessage());
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }

    @Transactional
    public void rotateKey() {
        log.info("Starting key rotation");

        // Deactivate current key
        encryptionKeyRepository.findFirstByActiveTrueOrderByCreatedAtDesc()
                .ifPresent(key -> {
                    key.setActive(false);
                    key.setRotatedAt(LocalDateTime.now());
                    encryptionKeyRepository.save(key);
                });

        // Create new key
        createNewKey();
        log.info("Key rotation completed");
    }

    private void ensureActiveKeyExists() {
        if (encryptionKeyRepository.findFirstByActiveTrueOrderByCreatedAtDesc().isEmpty()) {
            createNewKey();
            log.info("Initial encryption key created");
        }
    }

    private void createNewKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey secretKey = keyGen.generateKey();

            // La DEK est chiffrée (enveloppée) par la KEK avant persistance — jamais en clair.
            EncryptionKey encryptionKey = EncryptionKey.builder()
                    .keyAlias("key-" + UUID.randomUUID().toString().substring(0, 8))
                    .encryptedKey(wrapKey(secretKey))
                    .algorithm("AES-256-GCM")
                    .active(true)
                    .build();

            encryptionKeyRepository.save(encryptionKey);
        } catch (Exception e) {
            log.error("Failed to create encryption key: {}", e.getMessage());
            throw new RuntimeException("Failed to create encryption key", e);
        }
    }

    private SecretKey decodeKey(String stored) {
        byte[] blob = Base64.getDecoder().decode(stored);
        // Clé enveloppée = [IV(12)][DEK chiffrée + tag(16)] = 60 octets pour une DEK AES-256.
        // Repli : une DEK héritée stockée en clair fait exactement 32 octets → SecretKeySpec direct.
        if (blob.length == 32) {
            return new SecretKeySpec(blob, "AES");
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(blob, 0, iv, 0, iv.length);
            byte[] cipherText = new byte[blob.length - iv.length];
            System.arraycopy(blob, iv.length, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, kek(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new SecretKeySpec(cipher.doFinal(cipherText), "AES");
        } catch (Exception e) {
            throw new RuntimeException("Failed to unwrap data key", e);
        }
    }
}
