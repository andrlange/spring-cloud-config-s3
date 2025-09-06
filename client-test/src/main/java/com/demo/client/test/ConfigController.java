package com.demo.client.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controller for displaying configuration information.
 * 
 * Provides both web pages and REST endpoints to view the configuration
 * values loaded from the Config Server.
 */
@Controller
public class ConfigController {

    private final DemoProperties demoProperties;

    @Autowired
    public ConfigController(DemoProperties demoProperties) {
        this.demoProperties = demoProperties;
    }

    /**
     * Main configuration display page.
     */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("properties", demoProperties);
        model.addAttribute("environment", "TEST");
        model.addAttribute("timestamp", LocalDateTime.now());
        return "config-display";
    }

    /**
     * REST endpoint for configuration data.
     */
    @GetMapping("/api/config")
    @ResponseBody
    public Map<String, Object> getConfig() {
        return Map.of(
            "environment", "TEST",
            "service", demoProperties.service(),
            "common", demoProperties.common(),
            "timestamp", LocalDateTime.now()
        );
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    @ResponseBody
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "environment", "TEST",
            "configLoaded", demoProperties != null,
            "timestamp", LocalDateTime.now()
        );
    }
}