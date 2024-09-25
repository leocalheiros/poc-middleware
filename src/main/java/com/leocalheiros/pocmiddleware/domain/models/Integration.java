package com.leocalheiros.pocmiddleware.domain.models;

import com.leocalheiros.pocmiddleware.domain.enums.IntegrationType;
import com.leocalheiros.pocmiddleware.domain.enums.Status;
import jakarta.validation.constraints.Max;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Document(collection = "integration")
public class Integration {
    @Id
    private ObjectId id;

    @Max(20)
    private String documentNumber = "";

    @Max(20)
    private String requestParams;

    private Status status;
    private long batchId;
    private IntegrationType type;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    @Max(1000)
    private String object = "";

    @Max(1000)
    private String error;
}


