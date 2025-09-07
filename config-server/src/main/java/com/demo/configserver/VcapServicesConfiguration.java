package com.demo.configserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.util.logging.Logger;

/**
 * VCAP Services Configuration for Cloud Foundry deployments.
 * <p>
 * This configuration reads S3 settings from VCAP_SERVICES environment variable
 * when running in Cloud Foundry, supporting both DellEMC ECS and generic S3 service bindings.
 */
@Configuration
@ConditionalOnProperty(name = "VCAP_SERVICES")
public class VcapServicesConfiguration {

    private static final Logger LOGGER = Logger.getLogger(VcapServicesConfiguration.class.getName());

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${VCAP_SERVICES}")
    private String vcapServices;

    @Value("${spring.cloud.config.server.aws-s3.service-name:s3}")
    private String serviceName;

    /**
     * S3 configuration extracted from VCAP_SERVICES.
     */
    public static class S3ServiceInfo {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucketName;
        private String region = "us-east-1";

        // Constructors
        public S3ServiceInfo() {}

        public S3ServiceInfo(String endpoint, String accessKey, String secretKey, String bucketName, String region) {
            this.endpoint = endpoint;
            this.accessKey = accessKey;
            this.secretKey = secretKey;
            this.bucketName = bucketName;
            this.region = region;
        }

        // Getters and setters
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

        public String getAccessKey() { return accessKey; }
        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }

        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

        public String getBucketName() { return bucketName; }
        public void setBucketName(String bucketName) { this.bucketName = bucketName; }

        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
    }

    @Bean
    public S3ServiceInfo s3ServiceInfo() {
        try {
            LOGGER.info("Parsing VCAP_SERVICES for S3 configuration");
            JsonNode vcapJson = objectMapper.readTree(vcapServices);
            
            // Try different service name patterns
            JsonNode serviceArray = findServiceArray(vcapJson);
            
            if (serviceArray == null || !serviceArray.isArray() || serviceArray.isEmpty()) {
                throw new RuntimeException("No S3 service found in VCAP_SERVICES");
            }
            
            JsonNode serviceInstance = serviceArray.get(0);
            JsonNode credentials = serviceInstance.get("credentials");
            
            if (credentials == null) {
                throw new RuntimeException("No credentials found in S3 service binding");
            }
            
            S3ServiceInfo serviceInfo = extractS3ServiceInfo(credentials);
            
            LOGGER.info("Successfully extracted S3 configuration from VCAP_SERVICES: " + serviceInfo.getEndpoint());
            return serviceInfo;
            
        } catch (Exception e) {
            LOGGER.severe("Failed to parse VCAP_SERVICES: " + e.getMessage());
            throw new RuntimeException("Failed to parse VCAP_SERVICES for S3 configuration", e);
        }
    }

    private JsonNode findServiceArray(JsonNode vcapJson) {
        // Try common service names for S3 services
        String[] serviceNames = {
            serviceName,           // Configured service name
            "s3",                  // Generic S3
            "dell-ecs",            // DellEMC ECS
            "ecs-s3",              // ECS S3 variant
            "object-storage",      // Generic object storage
            "aws-s3"               // AWS S3
        };
        
        for (String name : serviceNames) {
            JsonNode serviceArray = vcapJson.get(name);
            if (serviceArray != null && serviceArray.isArray() && !serviceArray.isEmpty()) {
                LOGGER.info("Found S3 service under name: " + name);
                return serviceArray;
            }
        }
        
        return null;
    }

    private S3ServiceInfo extractS3ServiceInfo(JsonNode credentials) {
        S3ServiceInfo serviceInfo = new S3ServiceInfo();
        
        // Extract endpoint - try various field names
        String endpoint = getCredentialValue(credentials, "endpoint", "uri", "url", "host");
        if (endpoint != null) {
            // Ensure endpoint has protocol
            if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
                endpoint = "https://" + endpoint;
            }
            serviceInfo.setEndpoint(endpoint);
        }
        
        // Extract access key
        serviceInfo.setAccessKey(getCredentialValue(credentials, "access-key", "accessKey", "access_key", "username"));
        
        // Extract secret key
        serviceInfo.setSecretKey(getCredentialValue(credentials, "secret-key", "secretKey", "secret_key", "password"));
        
        // Extract bucket name
        serviceInfo.setBucketName(getCredentialValue(credentials, "bucket", "bucket-name", "bucketName", "bucket_name"));
        
        // Extract region
        String region = getCredentialValue(credentials, "region", "aws-region", "awsRegion");
        if (region != null) {
            serviceInfo.setRegion(region);
        }
        
        // Validate required fields
        if (serviceInfo.getEndpoint() == null) {
            throw new RuntimeException("S3 endpoint not found in VCAP_SERVICES credentials");
        }
        if (serviceInfo.getAccessKey() == null) {
            throw new RuntimeException("S3 access key not found in VCAP_SERVICES credentials");
        }
        if (serviceInfo.getSecretKey() == null) {
            throw new RuntimeException("S3 secret key not found in VCAP_SERVICES credentials");
        }
        if (serviceInfo.getBucketName() == null) {
            throw new RuntimeException("S3 bucket name not found in VCAP_SERVICES credentials");
        }
        
        return serviceInfo;
    }

    private String getCredentialValue(JsonNode credentials, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode field = credentials.get(fieldName);
            if (field != null && !field.isNull()) {
                return field.asText();
            }
        }
        return null;
    }

    @Bean
    @Primary
    public S3Client vcapS3Client(S3ServiceInfo s3ServiceInfo) {
        LOGGER.info("Configuring S3 client from VCAP services for endpoint: " + s3ServiceInfo.getEndpoint());
        
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
                s3ServiceInfo.getAccessKey(), 
                s3ServiceInfo.getSecretKey()
        );
        
        S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(true) // Required for MinIO and ECS
                .build();

        S3Client s3Client = S3Client.builder()
                .endpointOverride(URI.create(s3ServiceInfo.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .region(Region.of(s3ServiceInfo.getRegion()))
                .serviceConfiguration(s3Config)
                .build();

        LOGGER.info("S3 client configured successfully from VCAP services");
        return s3Client;
    }
}