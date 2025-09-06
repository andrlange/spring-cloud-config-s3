package com.demo.configserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

/**
 * Custom Environment Repository that reads configuration from S3.
 * 
 * This implementation simulates DellEMC ECS Storage behavior by reading
 * configuration files from an S3-compatible storage system (MinIO).
 */
@Component
public class S3ConfigEnvironmentRepository implements EnvironmentRepository {

    private static final Logger LOGGER = Logger.getLogger(S3ConfigEnvironmentRepository.class.getName());
    
    private final S3Client s3Client;
    private final String bucketName;
    private final Yaml yaml = new Yaml();
    
    @Autowired(required = false)
    private VcapServicesConfiguration.S3ServiceInfo s3ServiceInfo;

    public S3ConfigEnvironmentRepository(S3Client s3Client, 
                                       @Value("${spring.cloud.config.server.aws-s3.bucket:#{null}}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        LOGGER.info("Initialized S3ConfigEnvironmentRepository with bucket: " + getBucketName());
    }
    
    /**
     * Gets the bucket name from VCAP services if available, otherwise uses configured value.
     */
    private String getBucketName() {
        if (s3ServiceInfo != null && s3ServiceInfo.getBucketName() != null) {
            return s3ServiceInfo.getBucketName();
        }
        return bucketName;
    }

    @Override
    public Environment findOne(String application, String profile, String label) {
        LOGGER.info(String.format("Finding configuration for application=%s, profile=%s, label=%s", 
                application, profile, label));
        
        Environment environment = new Environment(application, profile.split(","));
        
        // Load application-specific configuration
        String applicationConfigKey = String.format("%s-%s.yml", application, profile);
        loadConfigFromS3(environment, applicationConfigKey, String.format("%s-%s", application, profile));
        
        // Load global application configuration if exists
        String globalConfigKey = "application.yml";
        loadConfigFromS3(environment, globalConfigKey, "application");
        
        LOGGER.info(String.format("Loaded %d property sources for %s-%s", 
                environment.getPropertySources().size(), application, profile));
        
        return environment;
    }

    private void loadConfigFromS3(Environment environment, String key, String name) {
        try {
            String currentBucket = getBucketName();
            LOGGER.info("Attempting to load configuration from S3: " + key + " (bucket: " + currentBucket + ")");
            
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(currentBucket)
                    .key(key)
                    .build();

            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
            String content = new String(s3Object.readAllBytes(), StandardCharsets.UTF_8);
            
            LOGGER.info("Successfully loaded configuration from S3: " + key);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = yaml.load(content);
            
            if (properties != null) {
                Map<String, Object> flatProperties = flattenProperties(properties);
                PropertySource propertySource = new PropertySource(name, flatProperties);
                environment.add(propertySource);
                
                LOGGER.info(String.format("Added %d properties from %s", flatProperties.size(), key));
            }
            
        } catch (NoSuchKeyException e) {
            LOGGER.warning("Configuration file not found in S3: " + key);
        } catch (IOException e) {
            LOGGER.severe("Error reading configuration from S3: " + key + ", error: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.severe("Unexpected error loading configuration from S3: " + key + ", error: " + e.getMessage());
        }
    }

    /**
     * Flattens nested YAML properties into Spring Boot compatible flat properties.
     */
    private Map<String, Object> flattenProperties(Map<String, Object> properties) {
        Map<String, Object> flatProperties = new HashMap<>();
        flattenPropertiesRecursive(properties, "", flatProperties);
        return flatProperties;
    }

    @SuppressWarnings("unchecked")
    private void flattenPropertiesRecursive(Map<String, Object> properties, String prefix, Map<String, Object> result) {
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map) {
                flattenPropertiesRecursive((Map<String, Object>) value, key, result);
            } else {
                result.put(key, value);
            }
        }
    }
}