package com.demo.client.dev;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Development Environment Client Application.
 * <p>
 * This Spring Boot application connects to the Config Server to retrieve
 * configuration specific to the development environment. It demonstrates how
 * applications can consume centralized configuration from S3-backed
 * Config Server.
 */
@SpringBootApplication
@EnableConfigurationProperties(DemoProperties.class)
public class DevClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevClientApplication.class, args);
    }
}