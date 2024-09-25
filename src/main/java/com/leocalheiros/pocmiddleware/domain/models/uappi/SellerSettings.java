package com.leocalheiros.pocmiddleware.domain.models.uappi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
public class SellerSettings {
    private SellerKeys keys;
}
