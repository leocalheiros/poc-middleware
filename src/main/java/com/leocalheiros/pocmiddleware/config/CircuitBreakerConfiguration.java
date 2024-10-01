package com.leocalheiros.pocmiddleware.config;

import com.azure.core.exception.HttpRequestException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.HttpURLConnection;
import java.time.Duration;

@Configuration
@AllArgsConstructor
public class CircuitBreakerConfiguration {
    private CircuitBreakerSettings circuitBreakerSettings;
    private RetrySettings retrySettings;

    private CircuitBreakerRegistry circuitBreakerRegistry() {
        var config = CircuitBreakerConfig.custom()
                .slidingWindowSize(circuitBreakerSettings.getSlidingWindowSize())
                .failureRateThreshold(circuitBreakerSettings.getFailureRateThreshold())
                .waitDurationInOpenState(
                        Duration.ofSeconds(circuitBreakerSettings.getWaitDurationInOpenState()))
                .permittedNumberOfCallsInHalfOpenState(circuitBreakerSettings.getPermittedNumberOfCallsInHalfOpenState())
                .automaticTransitionFromOpenToHalfOpenEnabled(circuitBreakerSettings.isAutomaticTransitionFromOpenToHalfOpenEnabled())
                .build();

        return CircuitBreakerRegistry.of(config);
    }

    private RetryRegistry retryRegistry() {
        var config = RetryConfig.custom()
                .maxAttempts(retrySettings.getMaxAttempts())
                .waitDuration(Duration.ofSeconds(retrySettings.getWaitDuration())) // Exponential backoff
                .retryOnException(ex -> {
                    if (ex instanceof FeignException) {
                        int statusCode = ((FeignException) ex).status();
                        return statusCode == HttpURLConnection.HTTP_UNAUTHORIZED
                                || statusCode < HttpURLConnection.HTTP_BAD_REQUEST
                                || statusCode >= HttpURLConnection.HTTP_INTERNAL_ERROR;
                    }
                    return ex instanceof HttpRequestException;
                })
                .build();

        return RetryRegistry.of(config);
    }

    @Bean
    public CircuitBreaker circuitBreaker() {
        return circuitBreakerRegistry().circuitBreaker("circuit-breaker-default");
    }

    @Bean
    public Retry retry() {
        return retryRegistry().retry("retry-policy");
    }
}
