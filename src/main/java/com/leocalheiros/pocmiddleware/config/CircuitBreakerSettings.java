package com.leocalheiros.pocmiddleware.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "resilience4j.circuitbreaker.instances.circuit-breaker-default")
public class CircuitBreakerSettings {
    private int slidingWindowSize;
    private float failureRateThreshold;
    private int waitDurationInOpenState;
    private int permittedNumberOfCallsInHalfOpenState;
    private boolean automaticTransitionFromOpenToHalfOpenEnabled;
    private List<Class<? extends Throwable>> recordException;
}