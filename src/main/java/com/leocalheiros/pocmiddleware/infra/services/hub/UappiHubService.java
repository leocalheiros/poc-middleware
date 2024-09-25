package com.leocalheiros.pocmiddleware.infra.services.hub;

import com.leocalheiros.pocmiddleware.application.dtos.responses.DefaultResponse;
import com.leocalheiros.pocmiddleware.application.dtos.responses.UpdateProductPriceResponse;
import com.leocalheiros.pocmiddleware.config.FeignConfig;
import com.leocalheiros.pocmiddleware.domain.models.AuthorizationToken;
import com.leocalheiros.pocmiddleware.domain.models.uappi.TokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "uappiHubService", url = "${uappi-settings.hub.url}", configuration = FeignConfig.class)
public interface UappiHubService {

    @PutMapping("/products/price-batch")
    DefaultResponse updateProductPrice(@RequestHeader("Authorization") String bearerToken,
                                       @RequestBody UpdateProductPriceResponse prices);

    @PostMapping("/auth")
    TokenResponse getToken(@RequestBody AuthorizationToken payload);

    @GetMapping("/ping")
    void validateToken(@RequestHeader("Authorization") String token);
}
