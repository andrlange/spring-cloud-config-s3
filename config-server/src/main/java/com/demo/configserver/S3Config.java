package com.demo.configserver;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.util.logging.Logger;

/**
 * S3 Configuration for connecting to MinIO (DellEMC ECS simulation).
 * <p>
 * This configuration sets up the S3Client to work with MinIO running locally,
 * which simulates DellEMC ECS Storage behavior for development and testing.
 * <p>
 * This configuration is used as a fallback when VCAP_SERVICES is not available.
 */
@Configuration
@ConditionalOnMissingBean(VcapServicesConfiguration.S3ServiceInfo.class)
public class S3Config {

    private static final Logger LOGGER = Logger.getLogger(S3Config.class.getName());

    @Value("${spring.cloud.config.server.aws-s3.endpoint}")
    private String s3Endpoint;

    @Value("${spring.cloud.config.server.aws-s3.access-key}")
    private String accessKey;

    @Value("${spring.cloud.config.server.aws-s3.secret-key}")
    private String secretKey;

    @Value("${spring.cloud.config.server.aws-s3.region:us-east-1}")
    private String region;

    @Bean
    public S3Client s3Client() {
        LOGGER.info("Configuring S3 client for endpoint: " + s3Endpoint);
        LOGGER.info("Configuring S3 client for region: " + Region.of(region));
        
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
        
        S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(true) // Required for MinIO
                .build();

        S3Client s3Client = S3Client.builder()
                .endpointOverride(URI.create(s3Endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .region(Region.of(region))
                .serviceConfiguration(s3Config)
                .build();

        LOGGER.info("S3 client configured successfully");
        return s3Client;
    }
}