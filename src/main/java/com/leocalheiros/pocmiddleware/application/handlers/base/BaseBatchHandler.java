package com.leocalheiros.pocmiddleware.application.handlers.base;

import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leocalheiros.pocmiddleware.application.dtos.responses.DefaultResponse;
import com.leocalheiros.pocmiddleware.domain.enums.IntegrationType;
import com.leocalheiros.pocmiddleware.domain.enums.Status;
import com.leocalheiros.pocmiddleware.domain.models.Integration;
import com.leocalheiros.pocmiddleware.infra.repository.BaseMongoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public abstract class BaseBatchHandler<T> extends BaseHandler {
    private final Logger logger = LoggerFactory.getLogger(BaseBatchHandler.class);

    private final BaseMongoRepository baseMongoRepository;
    private final ObjectMapper objectMapper;
    private final IntegrationType integrationType;
    private final int batchSize;

    protected BaseBatchHandler(
            String connectionString,
            String queueName,
            BaseMongoRepository baseMongoRepository,
            ObjectMapper objectMapper,
            IntegrationType integrationType,
            int batchSize) {
        super(connectionString, queueName);
        this.baseMongoRepository = baseMongoRepository;
        this.objectMapper = objectMapper;
        this.integrationType = integrationType;
        this.batchSize = batchSize;
    }

    public abstract DefaultResponse executeIntegration(List<T> list, String documentNumber);

    protected void onTimerComplete() {
        try {
            List<Integration> integrationsList = baseMongoRepository.findByStatusAndType(Status.PENDING, integrationType);

            if (integrationsList.isEmpty()) return;

            var groupedDocumentNumbers = integrationsList.stream()
                    .map(Integration::getDocumentNumber)
                    .distinct()
                    .toList();

            for (String documentNumber : groupedDocumentNumbers) {
                List<Integration> integrationsEntities = integrationsList.stream()
                        .filter(x -> x.getDocumentNumber().equals(documentNumber))
                        .limit(batchSize)
                        .toList();

                List<T> integrationsPayload = integrationsEntities.stream()
                        .map(integration -> {
                            try {
                                return objectMapper.readValue(integration.getObject(), getGenericClass());
                            } catch (JsonProcessingException e) {
                                logger.error("Error deserializing object: {}", e.getMessage());
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .toList();

                if (integrationsPayload.isEmpty()) return;

                DefaultResponse response = executeIntegration(integrationsPayload, documentNumber);

                updateIntegrations(integrationsEntities, response);
            }
        } catch (Exception e) {
            logger.error("Error in onTimerComplete: {}", e.getMessage());
        }
    }

    private void updateIntegrations(List<Integration> integrationsEntities, DefaultResponse response) {
        for (Integration integration : integrationsEntities) {
            integration.setBatchId(response.getId());
            integration.setStatus(response.getId() >= 0 ? Status.DONE : Status.PENDING);
            integration.setUpdatedAt(LocalDateTime.now());
            integration.setError(response.getId() < 0 ? response.getError() : "");

            baseMongoRepository.save(integration);
        }
    }

    protected abstract Class<T> getGenericClass();

    @Override
    protected void messageHandler(ServiceBusReceivedMessageContext context) {

    }
}
