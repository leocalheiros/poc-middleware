package com.leocalheiros.pocmiddleware.infra.repository;

import com.leocalheiros.pocmiddleware.domain.enums.IntegrationType;
import com.leocalheiros.pocmiddleware.domain.enums.Status;
import com.leocalheiros.pocmiddleware.domain.models.Integration;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BaseMongoRepository extends MongoRepository<Integration, String> {
    List<Integration> findByStatusAndType(Status status, IntegrationType type);
}
