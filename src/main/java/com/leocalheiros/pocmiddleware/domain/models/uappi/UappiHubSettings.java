package com.leocalheiros.pocmiddleware.domain.models.uappi;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class UappiHubSettings {
    private String url;
    private String appToken;
    private Map<String, SellerSettings> sellers;
}
