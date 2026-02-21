package com.howners.gestion.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class S3Config {

    private final StorageProperties storageProperties;

    @Bean
    public S3Client s3Client() {
        StorageProperties.S3Properties s3Props = storageProperties.getS3();

        log.info("Configuring S3 client with endpoint: {}", s3Props.getEndpoint());

        return S3Client.builder()
                .endpointOverride(URI.create(s3Props.getEndpoint()))
                .region(Region.of(s3Props.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                s3Props.getAccessKey(),
                                s3Props.getSecretKey()
                        )
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)  // Required for MinIO
                        .build())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        StorageProperties.S3Properties s3Props = storageProperties.getS3();

        log.info("Configuring S3 presigner with endpoint: {}", s3Props.getEndpoint());

        return S3Presigner.builder()
                .endpointOverride(URI.create(s3Props.getEndpoint()))
                .region(Region.of(s3Props.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                s3Props.getAccessKey(),
                                s3Props.getSecretKey()
                        )
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)  // Required for MinIO
                        .build())
                .build();
    }
}
