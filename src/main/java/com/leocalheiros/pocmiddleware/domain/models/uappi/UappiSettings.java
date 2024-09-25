package com.leocalheiros.pocmiddleware.domain.models.uappi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "uappi-settings")
public class UappiSettings {
    private UappiHubSettings hub;
    private UappiHubSettings platform;
}
