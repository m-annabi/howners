package com.howners.gestion.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "storage")
@Data
public class StorageProperties {

    private String type = "s3";
    private S3Properties s3 = new S3Properties();

    @Data
    public static class S3Properties {
        private String endpoint = "http://localhost:9000";
        private String accessKey = "minioadmin";
        private String secretKey = "minioadmin123";
        private String bucket = "howners-documents";
        private String region = "us-east-1";
    }
}
