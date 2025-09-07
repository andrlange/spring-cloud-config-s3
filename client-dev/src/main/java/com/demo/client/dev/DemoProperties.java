package com.demo.client.dev;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the demo service.
 * <p>
 * These properties are loaded from the Config Server and bound to this
 * configuration class using Spring Boot's @ConfigurationProperties mechanism.
 */
@ConfigurationProperties(prefix = "demo")
public record DemoProperties(
    Service service,
    Common common
) {
    
    public record Service(
        String name,
        Parameters parameters,
        Database database,
        Features features
    ) {}
    
    public record Parameters(
        String a,
        String b,
        String c
    ) {}
    
    public record Database(
        String url,
        String username
    ) {}
    
    public record Features(
        Boolean featureX,
        Boolean featureY,
        Boolean featureZ
    ) {}
    
    public record Common(
        String version,
        String author,
        String created
    ) {}
}