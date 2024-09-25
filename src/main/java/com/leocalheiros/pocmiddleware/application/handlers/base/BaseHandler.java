package com.leocalheiros.pocmiddleware.application.handlers.base;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class BaseHandler {
    private final Logger logger = LoggerFactory.getLogger(BaseHandler.class);

    private final ServiceBusProcessorClient processorClient;

    protected BaseHandler(String connectionString, String queueName) {
        ServiceBusClientBuilder builder = new ServiceBusClientBuilder().connectionString(connectionString);
        this.processorClient = builder
                .processor()
                .queueName(queueName)
                .processMessage(this::messageHandler)
                .processError(this::errorHandler)
                .buildProcessorClient();

        processorClient.start();
        logger.info("Processor started for queue: {}", processorClient.getQueueName());
    }

    @PreDestroy
    public void stop() {
        processorClient.close();
        logger.info("Processor stopped for queue: {}", processorClient.getQueueName());
    }

    protected abstract void messageHandler(ServiceBusReceivedMessageContext context);

    private void errorHandler(ServiceBusErrorContext context) {
        logger.error("Erro ao processar mensagem", context.getException());
    }
}
