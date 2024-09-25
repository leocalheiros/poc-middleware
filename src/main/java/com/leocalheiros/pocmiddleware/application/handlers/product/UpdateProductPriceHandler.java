package com.leocalheiros.pocmiddleware.application.handlers.product;

import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leocalheiros.pocmiddleware.application.dtos.requests.UpdateProductPriceRequest;
import com.leocalheiros.pocmiddleware.application.dtos.responses.DefaultResponse;
import com.leocalheiros.pocmiddleware.application.dtos.responses.UpdateProductPriceResponse;
import com.leocalheiros.pocmiddleware.application.handlers.base.BaseBatchHandler;
import com.leocalheiros.pocmiddleware.domain.enums.IntegrationType;
import com.leocalheiros.pocmiddleware.domain.enums.Status;
import com.leocalheiros.pocmiddleware.domain.models.AzureServiceBusSettings;
import com.leocalheiros.pocmiddleware.domain.models.Integration;
import com.leocalheiros.pocmiddleware.domain.models.IntegrationSettings;
import com.leocalheiros.pocmiddleware.infra.repository.BaseMongoRepository;
import com.leocalheiros.pocmiddleware.infra.services.hub.UappiHubServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UpdateProductPriceHandler extends BaseBatchHandler<UpdateProductPriceRequest> {
    private static final Logger logger = LoggerFactory.getLogger(UpdateProductPriceHandler.class);
    private final BaseMongoRepository baseMongoRepository;
    private final ObjectMapper objectMapper;
    private final UappiHubServiceImpl uappiHubService;

    public UpdateProductPriceHandler(
            AzureServiceBusSettings azureServiceBusSettings,
            BaseMongoRepository baseMongoRepository,
            ObjectMapper objectMapper,
            UappiHubServiceImpl uappiHubService,
            IntegrationSettings integrationSettings) {
        super(azureServiceBusSettings.getConnectionString(),
                integrationSettings.getUpdateProductPrice().getQueueName(),
                baseMongoRepository,
                objectMapper,
                integrationSettings.getUpdateProductPrice().getType(),
                integrationSettings.getUpdateProductPrice().getBatchLimit());
        this.baseMongoRepository = baseMongoRepository;
        this.objectMapper = objectMapper;
        this.uappiHubService = uappiHubService;
    }

    @Scheduled(fixedRateString = "#{@integrationSettings.updateProductPrice.timerPeriod}")
    public void scheduledTask() {
        logger.info("Scheduled task executed.");
        super.onTimerComplete();
    }

    @Override
    protected void messageHandler(ServiceBusReceivedMessageContext context) {
        String messageBody = context.getMessage().getBody().toString();
        try {
            UpdateProductPriceRequest dto = objectMapper.readValue(messageBody, UpdateProductPriceRequest.class);

            Integration integration = new Integration();
            integration.setStatus(Status.PENDING);
            integration.setType(IntegrationType.PRODUCTPRICEUPDATE);
            integration.setDocumentNumber(context.getMessage().getTo());
            integration.setObject(objectMapper.writeValueAsString(dto));
            logger.info("Message received in UpdateProductPriceHandler: {}", messageBody);
            baseMongoRepository.save(integration);
        } catch (JsonProcessingException e) {
            logger.error("Error processing JSON: {}", e.getMessage());
        }
    }

    @Override
    public DefaultResponse executeIntegration(List<UpdateProductPriceRequest> list, String documentNumber) {
        UpdateProductPriceResponse payload = new UpdateProductPriceResponse(list);
        return uappiHubService.updateProductPrice(payload, documentNumber);
    }

    @Override
    protected Class<UpdateProductPriceRequest> getGenericClass() {
        return UpdateProductPriceRequest.class;
    }
}

