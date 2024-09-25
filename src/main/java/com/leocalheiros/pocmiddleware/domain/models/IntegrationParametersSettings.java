package com.leocalheiros.pocmiddleware.domain.models;

import com.leocalheiros.pocmiddleware.domain.enums.IntegrationType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IntegrationParametersSettings {
    private String hash;
    private String queueName;
    private int timerPeriod;
    private IntegrationType type;
    private int batchLimit;
}
