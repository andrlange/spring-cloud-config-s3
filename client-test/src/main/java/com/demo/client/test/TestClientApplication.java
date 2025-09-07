package com.demo.client.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Test Environment Client Application.
 * <p>
 * This Spring Boot application connects to the Config Server to retrieve
 * configuration specific to the test environment. It demonstrates how
 * applications can consume centralized configuration from S3-backed
 * Config Server.
 */
@SpringBootApplication
@EnableConfigurationProperties(DemoProperties.class)
public class TestClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestClientApplication.class, args);
    }
}