package com.leocalheiros.pocmiddleware.domain.models;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "integration-settings")
public class IntegrationSettings {
    private IntegrationParametersSettings updateStock;
    private IntegrationParametersSettings updateProductPrice;
    private IntegrationParametersSettings updateOrderStatus;
    private IntegrationParametersSettings updateRetailerCredit;
    private IntegrationParametersSettings saveRecommendationList;
}