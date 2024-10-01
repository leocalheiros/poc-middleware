package com.leocalheiros.pocmiddleware.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "resilience4j.retry.instances.retry-policy")
public class RetrySettings {
    private int maxAttempts;
    private int waitDuration;
    private List<Class<? extends Throwable>> retryException;
}