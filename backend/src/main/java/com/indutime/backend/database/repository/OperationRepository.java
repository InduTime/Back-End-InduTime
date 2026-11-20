package com.indutime.backend.database.repository;

import com.indutime.backend.database.model.OperationEntity;
import com.indutime.backend.enums.OperationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;

public interface OperationRepository extends JpaRepository<OperationEntity, Integer>,
        JpaSpecificationExecutor<OperationEntity> {

    @Override
    @EntityGraph(attributePaths = "machine")
    List<OperationEntity> findAll(Specification<OperationEntity> specification, Sort sort);

    List<OperationEntity> findAllByMachine_IdAndStatus(
            Integer machineId,
            OperationStatus status);

    List<OperationEntity> findAllByMachine_IdAndStartDate(
            Integer machineId,
            OffsetDateTime startDate);
}
