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
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import static io.vavr.API.println;

public abstract class ResilientApiClientBase {
    protected Retry retryPolicy;
    protected CircuitBreaker circuitBreaker;
    private int halfOpenFailures;

    protected ResilientApiClientBase() {
        createRetryPolicy();
        createCircuitBreakerPolicy();
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
                println("Attempt " + event.getNumberOfRetryAttempts() +
                        " failed. Retrying in " + event.getWaitInterval().getSeconds() +
                        " seconds..."));
    }

    private void createCircuitBreakerPolicy() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .slidingWindowSize(3)
                .recordException(ex -> ex instanceof FeignException || ex instanceof HttpRequestException || ex instanceof Exception)
                .build();

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

    private void onBreak(IntervalFunction duration) {
        long waitDurationInMillis = duration.apply(1);
        long waitDurationInSeconds = Duration.ofMillis(waitDurationInMillis).getSeconds();
        println("Circuit opened for " + waitDurationInSeconds + " seconds due to failures.");
    }

    private void onReset() {
        println("Circuit closed again, operating normally.");
        halfOpenFailures = 0;
    }

    private void onHalfOpen() {
        println("Circuit in Half-Open state, testing connectivity...");
        halfOpenFailures = 0;
    }

    protected <T> T executeWithResilience(Supplier<T> action) {
        // Callable<T> decorated = CircuitBreaker.decorateCallable(circuitBreaker, Retry.decorateCallable(retryPolicy, action::get));
        Callable<T> decorated = Retry.decorateCallable(retryPolicy, CircuitBreaker.decorateCallable(circuitBreaker, action::get));

        return Try.ofCallable(decorated)
                .getOrElseThrow(throwable -> new RuntimeException("Operation failed after retries"));
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
}
