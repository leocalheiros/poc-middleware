package com.leocalheiros.pocmiddleware.infra.services.hub.impl;

import com.leocalheiros.pocmiddleware.application.dtos.responses.DefaultResponse;
import com.leocalheiros.pocmiddleware.application.dtos.responses.UpdateProductPriceResponse;
import com.leocalheiros.pocmiddleware.domain.models.AuthorizationToken;
import com.leocalheiros.pocmiddleware.domain.models.uappi.UappiSettings;
import com.leocalheiros.pocmiddleware.infra.resilience.ResilientApiClientBase;
import com.leocalheiros.pocmiddleware.infra.services.hub.UappiHubService;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

@Service
public class UappiHubServiceImpl extends ResilientApiClientBase {
    private final UappiHubService uappiHubService;
    private final StringRedisTemplate redisTemplate;
    private final UappiSettings uappiSettings;
    private final Logger logger = LoggerFactory.getLogger(UappiHubServiceImpl.class);

    public UappiHubServiceImpl(CircuitBreaker circuitBreaker,
                               CircuitBreaker circuiteBreakerHalfOpen,
                               Retry retryPolicy,
                               UappiHubService uappiHubService,
                               StringRedisTemplate redisTemplate,
                               UappiSettings uappiSettings) {
        super(circuitBreaker, circuiteBreakerHalfOpen, retryPolicy);

        this.uappiHubService = uappiHubService;
        this.redisTemplate = redisTemplate;
        this.uappiSettings = uappiSettings;
    }

    public DefaultResponse updateProductPrice(UpdateProductPriceResponse payload, String documentNumber) {
        return executeGenericHandling(() -> {
            var token = getToken(documentNumber);
            return uappiHubService.updateProductPrice(token, payload);
        });
    }

    public String getToken(String documentNumber) {
        var key = "token_hub_" + documentNumber;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();

        var token = ops.get(key);
        if (token != null) {
            validateToken(documentNumber, token, key);
        }

        var sellerSettings = uappiSettings.getHub().getSellers().get(documentNumber);
        if (sellerSettings == null) {
            logger.warn("Configuração do seller não encontrada para o documento: {}", documentNumber);
            return null;
        }

        var tokenPayload = new AuthorizationToken(sellerSettings.getKeys().getApiKey(),
                sellerSettings.getKeys().getSecretKey());
        var tokenResponse = uappiHubService.getToken(tokenPayload);
        if (tokenResponse == null) {
            return "";
        }

        ops.set(key, tokenResponse.getToken());
        logger.info("Token obtido: {}", tokenResponse.getToken());
        return "Bearer " + tokenResponse.getToken();
    }

    private void validateToken(String documentNumber, String token, String redisKey) {
        try {
            uappiHubService.validateToken(token);
        } catch (FeignException.Unauthorized e) {
            redisTemplate.delete(redisKey);
            getToken(documentNumber);
        }
    }
}

