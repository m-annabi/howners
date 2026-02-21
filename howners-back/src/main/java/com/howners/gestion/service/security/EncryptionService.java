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
            ensureActiveKeyExists();
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

            String encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());

            EncryptionKey encryptionKey = EncryptionKey.builder()
                    .keyAlias("key-" + UUID.randomUUID().toString().substring(0, 8))
                    .encryptedKey(encodedKey)
                    .algorithm("AES-256-GCM")
                    .active(true)
                    .build();

            encryptionKeyRepository.save(encryptionKey);
        } catch (Exception e) {
            log.error("Failed to create encryption key: {}", e.getMessage());
            throw new RuntimeException("Failed to create encryption key", e);
        }
    }

    private SecretKey decodeKey(String encodedKey) {
        byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
        return new SecretKeySpec(keyBytes, "AES");
    }
}
