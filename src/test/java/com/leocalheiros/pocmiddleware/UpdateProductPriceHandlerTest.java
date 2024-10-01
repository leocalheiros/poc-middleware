package com.leocalheiros.pocmiddleware;

import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leocalheiros.pocmiddleware.application.dtos.requests.UpdateProductPriceRequest;
import com.leocalheiros.pocmiddleware.application.dtos.responses.DefaultResponse;
import com.leocalheiros.pocmiddleware.application.dtos.responses.UpdateProductPriceResponse;
import com.leocalheiros.pocmiddleware.application.handlers.product.UpdateProductPriceHandler;
import com.leocalheiros.pocmiddleware.domain.models.AzureServiceBusSettings;
import com.leocalheiros.pocmiddleware.domain.models.Integration;
import com.leocalheiros.pocmiddleware.domain.enums.IntegrationType;
import com.leocalheiros.pocmiddleware.domain.models.IntegrationParametersSettings;
import com.leocalheiros.pocmiddleware.domain.models.IntegrationSettings;
import com.leocalheiros.pocmiddleware.infra.repository.BaseMongoRepository;
import com.leocalheiros.pocmiddleware.infra.services.hub.UappiHubServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;

class UpdateProductPriceHandlerTest {

    @Mock
    private BaseMongoRepository baseMongoRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private UappiHubServiceImpl uappiHubService;

    @Mock
    private AzureServiceBusSettings azureServiceBusSettings;

    @Mock
    private IntegrationSettings integrationSettings;

    @Mock
    private IntegrationParametersSettings integrationParametersSettings;


    private UpdateProductPriceHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(azureServiceBusSettings.getConnectionString()).thenReturn("Endpoint=sb://example-connectionstring.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=example-key");
        when(integrationParametersSettings.getQueueName()).thenReturn("fake-queue-name");
        when(integrationParametersSettings.getBatchLimit()).thenReturn(100);
        when(integrationParametersSettings.getType()).thenReturn(IntegrationType.PRODUCTPRICEUPDATE);
        when(integrationSettings.getUpdateProductPrice()).thenReturn(integrationParametersSettings);

        handler = new UpdateProductPriceHandler(
                azureServiceBusSettings,
                baseMongoRepository,
                objectMapper,
                uappiHubService,
                integrationSettings
        );
    }

    @Test
    void testMessageHandler_ShouldInsertIntegration_WhenMessageIsValid() throws JsonProcessingException {
        // Arrange
        var messageBody = "{\"productId\":\"123\",\"price\":10.0}";
        ServiceBusReceivedMessage message = mock(ServiceBusReceivedMessage.class);
        ServiceBusReceivedMessageContext context = mock(ServiceBusReceivedMessageContext.class);

        when(context.getMessage()).thenReturn(message);
        when(message.getBody()).thenReturn(BinaryData.fromString(messageBody));
        UpdateProductPriceRequest request = new UpdateProductPriceRequest();
        when(objectMapper.readValue(anyString(), eq(UpdateProductPriceRequest.class))).thenReturn(request);

        // Act
        handler.messageHandler(context);

        // Assert
        verify(baseMongoRepository, times(1)).save(any(Integration.class));
    }

    @Test
    void testExecuteIntegration_ShouldCallUpdateProductPrice_WhenValidRequests() {
        // Arrange
        List<UpdateProductPriceRequest> requestList = List.of(new UpdateProductPriceRequest());
        String documentNumber = "123";
        DefaultResponse expectedResponse = new DefaultResponse();
        when(uappiHubService.updateProductPrice(any(UpdateProductPriceResponse.class), anyString()))
                .thenReturn(expectedResponse);

        // Act
        DefaultResponse response = handler.executeIntegration(requestList, documentNumber);

        // Assert
        verify(uappiHubService, times(1)).updateProductPrice(any(UpdateProductPriceResponse.class), eq(documentNumber));
        assertEquals(expectedResponse, response);
    }

    @Test
    void testMessageHandler_ShouldLogError_WhenJsonProcessingExceptionOccurs() throws JsonProcessingException {
        // Arrange
        String messageBody = "{\"invalid_json\":\"}";
        ServiceBusReceivedMessage message = mock(ServiceBusReceivedMessage.class);
        ServiceBusReceivedMessageContext context = mock(ServiceBusReceivedMessageContext.class);

        when(context.getMessage()).thenReturn(message);
        when(message.getBody()).thenReturn(BinaryData.fromString(messageBody));
        when(objectMapper.readValue(anyString(), eq(UpdateProductPriceRequest.class)))
                .thenThrow(new JsonProcessingException("JSON error") {});

        // Act
        handler.messageHandler(context);

        // Assert
        verify(baseMongoRepository, never()).save(any(Integration.class));
    }
}