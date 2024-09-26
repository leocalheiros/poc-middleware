package com.leocalheiros.pocmiddleware.infra.resilience;

import com.azure.core.exception.HttpRequestException;
import com.leocalheiros.pocmiddleware.application.dtos.responses.DefaultResponse;
import com.leocalheiros.pocmiddleware.config.CircuitBreakerSettings;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.vavr.control.Try;

import java.net.HttpURLConnection;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import static io.vavr.API.println;

public abstract class ResilientApiClientBase {
    protected Retry retryPolicy;
    protected CircuitBreaker circuitBreaker;
    protected CircuitBreaker circuitBreakerHalfOpen;
    private int halfOpenFailures;

    protected ResilientApiClientBase() {
        createRetryPolicy();
        createCircuitBreakerPolicy();
        createHalfOpenPolicy();
    }

    private void createRetryPolicy() {
        int retryCount = 3;

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(retryCount)
                .waitDuration(Duration.ofSeconds(2)) // Exponential backoff
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

        retryPolicy = Retry.of("retryPolicy", retryConfig);
        retryPolicy.getEventPublisher().onRetry(event ->
                println(GetDatetimeNow() + " - Attempt " + event.getNumberOfRetryAttempts() +
                        " failed. Retrying in " + event.getWaitInterval().getSeconds() +
                        " seconds..."));
    }

    private void createCircuitBreakerPolicy() {
//        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
//                .failureRateThreshold(50)
//                .waitDurationInOpenState(Duration.ofSeconds(60))
//                .slidingWindowSize(3)
//                .recordException(ex -> ex instanceof FeignException || ex instanceof HttpRequestException || ex instanceof Exception)
//                .build();

        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig
                .custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .slidingWindowSize(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .permittedNumberOfCallsInHalfOpenState(3)
                .recordException(ex -> ex instanceof FeignException || ex instanceof HttpRequestException || ex instanceof Exception).build();

        circuitBreaker = CircuitBreaker.of("circuitBreaker", circuitBreakerConfig);
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> {
                    if (event.getStateTransition() == CircuitBreaker.StateTransition.CLOSED_TO_OPEN) {
                        onBreak(circuitBreakerConfig.getWaitIntervalFunctionInOpenState());
                    } else if (event.getStateTransition() == CircuitBreaker.StateTransition.OPEN_TO_HALF_OPEN) {
                        onHalfOpen();
                    } else if (event.getStateTransition() == CircuitBreaker.StateTransition.HALF_OPEN_TO_CLOSED) {
                        onReset();
                    }
                });
    }

    private void createHalfOpenPolicy() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold((float) (1.0 * 1 / 5))
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .slidingWindowSize(3)
                .recordException(ex -> ex instanceof FeignException || ex instanceof HttpRequestException || ex instanceof Exception)
                .build();

        circuitBreakerHalfOpen = CircuitBreaker.of("circuitBreaker", circuitBreakerConfig);
        circuitBreakerHalfOpen.getEventPublisher()
                .onStateTransition(event -> {
                    if (event.getStateTransition() == CircuitBreaker.StateTransition.OPEN_TO_HALF_OPEN) {
                        if (halfOpenFailures < 1) return;
                        println(GetDatetimeNow() + " - Half-Open circuit failed " + halfOpenFailures + " times and will be reopened.");
                    }
                });
    }

    private void onBreak(IntervalFunction duration) {
        long waitDurationInMillis = duration.apply(1);
        long waitDurationInSeconds = Duration.ofMillis(waitDurationInMillis).getSeconds();
        println(GetDatetimeNow() + " - Circuit opened for " + waitDurationInSeconds + " seconds due to failures.");
    }

    private void onReset() {
        println(GetDatetimeNow() + " - Circuit closed again, operating normally.");
        halfOpenFailures = 0;
    }

    private void onHalfOpen() {
        println(GetDatetimeNow() + " - Circuit in Half-Open state, testing connectivity...");
        halfOpenFailures = 0;
    }

    protected <T> T executeWithResilience(Supplier<T> action) {
        // Callable<T> decorated = CircuitBreaker.decorateCallable(circuitBreaker, Retry.decorateCallable(retryPolicy, action::get));
        Callable<T> decorated = Retry.decorateCallable(retryPolicy,
                CircuitBreaker.decorateCallable(circuitBreaker,
                        CircuitBreaker.decorateCallable(circuitBreakerHalfOpen, action::get)
                ));

        return Try.ofCallable(decorated)
                .getOrElseThrow(throwable -> new RuntimeException(GetDatetimeNow() + " - Operation failed after retries"));
    }

    protected <T extends DefaultResponse> T ExecuteGenericHandling(Supplier<T> action) {
        try {
            return executeWithResilience(action);
        } catch (Exception ex) {
            println(ex.getMessage());

            Throwable innerEx = ex.getCause() != null ? ex.getCause() : ex;
            T response = (T) new DefaultResponse();
            response.setId(-1);

            if (innerEx instanceof FeignException) {
                response.setError(((FeignException) innerEx).getMessage());
                response.setStatusCode(((FeignException) innerEx).status());
            } else if (innerEx instanceof HttpRequestException) {
                response.setError(innerEx.getMessage());
                response.setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
            } else {
                response.setError(innerEx.getMessage());
                response.setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
            }
            return response;
        }
    }

    private String GetDatetimeNow() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/M/yyyy HH:mm:ss");
        return now.format(formatter);
    }
}
