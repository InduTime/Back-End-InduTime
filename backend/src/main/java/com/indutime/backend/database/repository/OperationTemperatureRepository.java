package com.indutime.backend.database.repository;

import com.indutime.backend.database.model.OperationTemperatureEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OperationTemperatureRepository
        extends JpaRepository<OperationTemperatureEntity, Integer> {

    Optional<OperationTemperatureEntity> findByEventId(UUID eventId);

    List<OperationTemperatureEntity> findAllByOperation_IdOrderByMeasuredAtAsc(
            Integer operationId);
}
