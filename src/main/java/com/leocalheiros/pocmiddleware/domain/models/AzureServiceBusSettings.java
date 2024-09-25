package com.leocalheiros.pocmiddleware.domain.models;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "azure.servicebus")
public class AzureServiceBusSettings {
    private String connectionString;
}
