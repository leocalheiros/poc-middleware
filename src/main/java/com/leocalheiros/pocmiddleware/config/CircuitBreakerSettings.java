package com.leocalheiros.pocmiddleware.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "circuit-breaker")
public class CircuitBreakerSettings {
    private int retryCount;
    private int failureThreshold;
    private int failurePeriod;
    private int closedTimeout;
    private int halfOpenTestRequestCount;
    private int halfOpenFailureThreshold;
}
