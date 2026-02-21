package com.howners.gestion.service.storage;

import com.howners.gestion.config.StorageProperties;
import com.howners.gestion.service.security.EncryptionService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties storageProperties;
    private final EncryptionService encryptionService;

    @Value("${storage.encryption.enabled:false}")
    private boolean encryptionEnabled;

    /**
     * Initialise le bucket S3 s'il n'existe pas
     */
    public void initBucket() {
        String bucketName = storageProperties.getS3().getBucket();

        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            s3Client.headBucket(headBucketRequest);
            log.info("Bucket '{}' already exists", bucketName);
        } catch (NoSuchBucketException e) {
            try {
                log.info("Creating bucket '{}'", bucketName);
                CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                        .bucket(bucketName)
                        .build();
                s3Client.createBucket(createBucketRequest);
                log.info("Bucket '{}' created successfully", bucketName);
            } catch (Exception createError) {
                log.error("Error creating bucket: {}", createError.getMessage());
                // Don't throw - allow app to start even if bucket creation fails
            }
        } catch (Exception e) {
            log.error("Error checking bucket: {}", e.getMessage());
            // Don't throw - allow app to start even if bucket check fails
        }
    }

    /**
     * Upload un fichier vers S3/MinIO
     */
    public String uploadFile(byte[] fileBytes, String fileName, String contentType) {
        String bucketName = storageProperties.getS3().getBucket();
        String key = generateKey(fileName);

        try {
            byte[] dataToUpload = encryptionEnabled ? encryptionService.encrypt(fileBytes) : fileBytes;

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(dataToUpload));
            log.info("File uploaded successfully: {} (encrypted: {})", key, encryptionEnabled);

            return key;

        } catch (Exception e) {
            log.error("Error uploading file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    /**
     * Télécharge un fichier depuis S3/MinIO
     */
    public byte[] downloadFile(String key) throws IOException {
        String bucketName = storageProperties.getS3().getBucket();

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            InputStream inputStream = s3Client.getObject(getObjectRequest);
            byte[] rawData = inputStream.readAllBytes();
            return encryptionEnabled ? encryptionService.decrypt(rawData) : rawData;

        } catch (NoSuchKeyException e) {
            log.error("File not found: {}", key);
            throw new IOException("File not found: " + key);
        } catch (Exception e) {
            log.error("Error downloading file: {}", e.getMessage(), e);
            throw new IOException("Failed to download file", e);
        }
    }

    /**
     * Supprime un fichier de S3/MinIO
     */
    public void deleteFile(String key) {
        String bucketName = storageProperties.getS3().getBucket();

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted successfully: {}", key);

        } catch (Exception e) {
            log.error("Error deleting file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    /**
     * Génère une URL présignée pour télécharger un fichier
     * L'URL est valide pendant 7 jours
     */
    public String generatePresignedUrl(String key) {
        String bucketName = storageProperties.getS3().getBucket();

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofDays(7)) // URL valide 7 jours
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            String url = presignedRequest.url().toString();

            log.debug("Generated presigned URL for key: {} (valid for 7 days)", key);
            return url;

        } catch (Exception e) {
            log.error("Error generating presigned URL for key: {}", key, e);
            // Fallback to simple URL if presigning fails
            String endpoint = storageProperties.getS3().getEndpoint();
            return String.format("%s/%s/%s", endpoint, bucketName, key);
        }
    }

    /**
     * Génère une clé unique pour un fichier
     */
    private String generateKey(String fileName) {
        String uuid = UUID.randomUUID().toString();
        return String.format("%s/%s", uuid.substring(0, 2), uuid + "_" + fileName);
    }
}
