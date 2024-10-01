package com.leocalheiros.pocmiddleware.infra.resilience;

import com.azure.core.exception.HttpRequestException;
import com.leocalheiros.pocmiddleware.application.dtos.responses.DefaultResponse;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

import static io.vavr.API.println;

@Service
@AllArgsConstructor
public abstract class ResilientApiClientBase {
    protected Retry retryPolicy;
    protected CircuitBreaker circuitBreaker;
    protected CircuitBreaker circuitBreakerHalfOpen;
    private int halfOpenFailures;

    protected ResilientApiClientBase(
            CircuitBreaker circuitBreaker,
            CircuitBreaker circuitBreakerHalfOpen,
            Retry retryPolicy)
    {
        this.circuitBreaker = circuitBreaker;
        this.circuitBreakerHalfOpen = circuitBreakerHalfOpen;
        this.retryPolicy = retryPolicy;

        createRetryPolicy();
        createCircuitBreakerPolicy();
        createHalfOpenPolicy();
    }

    private void createRetryPolicy() {
        retryPolicy.getEventPublisher().onRetry(event ->
                println(getDatetimeNow() + " - Attempt " + event.getNumberOfRetryAttempts() +
                        " failed. Retrying in " + event.getWaitInterval().getSeconds() +
                        " seconds..."));
    }

    private void createCircuitBreakerPolicy() {
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> {
                    if (event.getStateTransition() == CircuitBreaker.StateTransition.CLOSED_TO_OPEN) {
                        onBreak(circuitBreaker.getCircuitBreakerConfig().getWaitIntervalFunctionInOpenState());
                    } else if (event.getStateTransition() == CircuitBreaker.StateTransition.OPEN_TO_HALF_OPEN) {
                        onHalfOpen();
                    } else if (event.getStateTransition() == CircuitBreaker.StateTransition.HALF_OPEN_TO_CLOSED) {
                        onReset();
                    }
                });
    }

    private void createHalfOpenPolicy() {
        circuitBreakerHalfOpen.getEventPublisher()
                .onStateTransition(event -> {
                    if (event.getStateTransition() == CircuitBreaker.StateTransition.OPEN_TO_HALF_OPEN) {
                        if (halfOpenFailures < 1) return;
                        println(getDatetimeNow() + " - Half-Open circuit failed " + halfOpenFailures + " times and will be reopened.");
                    }
                });
    }

    private void onBreak(IntervalFunction duration) {
        var waitDurationInMillis = duration.apply(1);
        var waitDurationInSeconds = Duration.ofMillis(waitDurationInMillis).getSeconds();
        println(getDatetimeNow() + " - Circuit opened for " + waitDurationInSeconds + " seconds due to failures.");
    }

    private void onReset() {
        println(getDatetimeNow() + " - Circuit closed again, operating normally.");
        halfOpenFailures = 0;
    }

    private void onHalfOpen() {
        println(getDatetimeNow() + " - Circuit in Half-Open state, testing connectivity...");
        halfOpenFailures = 0;
    }

    protected <T> T executeWithResilience(Supplier<T> action) {
        var decorated = Retry.decorateCallable(retryPolicy,
                CircuitBreaker.decorateCallable(circuitBreaker,
                        CircuitBreaker.decorateCallable(circuitBreakerHalfOpen, action::get)
                ));

        return Try.ofCallable(decorated)
                .getOrElseThrow(throwable -> new RuntimeException(getDatetimeNow() + " - Operation failed after retries"));
    }

    public <T extends DefaultResponse> T executeGenericHandling(Supplier<T> action) {
        try {
            return executeWithResilience(action);
        } catch (Exception ex) {
            println(ex.getMessage());

            var innerEx = ex.getCause() != null ? ex.getCause() : ex;
            T response = (T) new DefaultResponse();
            response.setId(-1);

            if (innerEx instanceof FeignException feignException) {
                response.setError(feignException.getMessage());
                response.setStatusCode(feignException.status());
            } else {
                response.setError(innerEx.getMessage());
                response.setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
            }
            return response;
        }
    }

    private String getDatetimeNow() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/M/yyyy HH:mm:ss");
        return now.format(formatter);
    }
}
