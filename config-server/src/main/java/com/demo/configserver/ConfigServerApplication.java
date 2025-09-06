package com.demo.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Spring Cloud Config Server application with S3 backend support.
 * 
 * This application provides configuration management for Spring Boot applications
 * using S3-compatible storage (DellEMC ECS simulation via MinIO).
 * 
 * Features:
 * - S3 backend configuration storage
 * - Environment-specific configurations (test, dev)
 * - Security with basic authentication
 * - Health checks and monitoring via actuator
 */
@SpringBootApplication(
        exclude = {
                io.awspring.cloud.autoconfigure.s3.S3AutoConfiguration.class
        }
)
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}