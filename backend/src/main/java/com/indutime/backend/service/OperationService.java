package com.indutime.backend.service;

import com.indutime.backend.database.model.CNCMachineEntity;
import com.indutime.backend.database.model.OperationEntity;
import com.indutime.backend.database.model.OperationTemperatureEntity;
import com.indutime.backend.database.repository.ICNCMachineRepository;
import com.indutime.backend.database.repository.OperationRepository;
import com.indutime.backend.database.repository.OperationTemperatureRepository;
import com.indutime.backend.dto.CNCMachineRequestDTO;
import com.indutime.backend.dto.OperationResponseDTO;
import com.indutime.backend.dto.OperationSummaryResponseDTO;
import com.indutime.backend.dto.OperationTelemetryRequestDTO;
import com.indutime.backend.dto.OperationTemperatureResponseDTO;
import com.indutime.backend.enums.OperationStatus;
import com.indutime.backend.exception.BadRequestException;
import com.indutime.backend.exception.ConflictException;
import com.indutime.backend.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OperationService {

    private static final ZoneId LEGACY_ZONE = ZoneId.of("America/Sao_Paulo");

    private final OperationRepository operationRepository;
    private final OperationTemperatureRepository temperatureRepository;
    private final ICNCMachineRepository machineRepository;

    private final EntityManager entityManager;

    @Transactional
    public OperationTelemetryResult receiveTelemetry(
            OperationTelemetryRequestDTO request) {

        validateRequest(request);

        CNCMachineEntity machine = machineRepository
                .findByRaspberryMachineIdForUpdate(request.getRaspberryMachineId())
                .orElseThrow(() -> new NotFoundException(
                        "Máquina não encontrada para o ID da Raspberry: "
                                + request.getRaspberryMachineId()
                ));
        entityManager.refresh(machine, LockModeType.PESSIMISTIC_WRITE);

        OperationTemperatureEntity duplicate = temperatureRepository
                .findByEventId(request.getEventId())
                .orElse(null);

        if (duplicate != null) {
            return handleDuplicate(request, duplicate);
        }

        ensureMachineIsActive(machine);

        return processTelemetry(
                machine,
                request.getEventId(),
                request.getOperationStartedAt(),
                request.getRunning(),
                request.getActualEndAt(),
                request.getScheduledEndAt(),
                request.getTemperature(),
                request.getMeasuredAt(),
                false
        );
    }

    /**
     * Routes the existing CSV contract through the same operation state rules
     * as the JSON endpoint. CSV has no event ID, so a stable ID is supplied by
     * the caller when a temperature is present.
     */
    @Transactional
    public void receiveCsvTelemetry(
            Integer machineId,
            Integer raspberryMachineId,
            CNCMachineRequestDTO machineMetadata,
            UUID eventId,
            OffsetDateTime measuredAt,
            boolean running,
            BigDecimal temperature) {

        CNCMachineEntity machine = machineRepository.findByIdForUpdate(machineId)
                .orElseThrow(() -> new NotFoundException(
                        "Máquina não encontrada para o ID interno: " + machineId
                ));
        entityManager.refresh(machine, LockModeType.PESSIMISTIC_WRITE);

        if (eventId != null) {
            OperationTemperatureEntity duplicate = temperatureRepository
                    .findByEventId(eventId)
                    .orElse(null);

            if (duplicate != null) {
                if (!duplicate.getOperation().getMachine().getId().equals(machineId)
                        || !sameInstant(duplicate.getMeasuredAt(), measuredAt)
                        || !sameTemperature(duplicate.getTemperature(), temperature)) {
                    throw new ConflictException(
                            "O identificador do evento CSV já foi utilizado com outros dados."
                    );
                }
                return;
            }
        }

        ensureMachineIsActive(machine);

        machine.setRaspberryMachineId(raspberryMachineId);
        machine.setDisplayName(machineMetadata.getDisplayName());
        machine.setSerialNumber(machineMetadata.getSerialNumber());
        machine.setModel(machineMetadata.getModel());
        machine.setManufacturer(machineMetadata.getManufacturer());
        machine.setLocation(machineMetadata.getLocation());

        OffsetDateTime requestedStartAt = machine.getOperationStartedAt();
        if (requestedStartAt == null && running) {
            requestedStartAt = measuredAt;
        }

        processTelemetry(
                machine,
                eventId,
                requestedStartAt,
                running,
                running ? null : measuredAt,
                null,
                temperature,
                measuredAt,
                true
        );
    }

    @Transactional(readOnly = true)
    public List<OperationSummaryResponseDTO> listOperations(
            Integer raspberryMachineId,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            OperationStatus status) {

        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BadRequestException(
                    "endDate não pode ser anterior a startDate."
            );
        }

        Specification<OperationEntity> specification =
                (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (raspberryMachineId != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(
                            root.join("machine").get("raspberryMachineId"),
                            raspberryMachineId
                    ));
        }

        if (startDate != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(
                            root.<OffsetDateTime>get("startDate"), startDate
                    ));
        }

        if (endDate != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(
                            root.<OffsetDateTime>get("startDate"), endDate
                    ));
        }

        if (status != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status));
        }

        return operationRepository.findAll(
                        specification,
                        Sort.by(Sort.Direction.DESC, "startDate")
                ).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public OperationResponseDTO getOperation(Integer operationId) {

        OperationEntity operation = operationRepository.findById(operationId)
                .orElseThrow(() -> new NotFoundException(
                        "Operação não encontrada para o ID: " + operationId
                ));

        List<OperationTemperatureResponseDTO> temperatures =
                temperatureRepository
                        .findAllByOperation_IdOrderByMeasuredAtAsc(operationId)
                        .stream()
                        .map(this::toTemperatureResponse)
                        .toList();

        OperationSummaryResponseDTO summary = toSummary(operation);

        return new OperationResponseDTO(
                summary.id(),
                summary.machineId(),
                summary.raspberryMachineId(),
                summary.machineName(),
                summary.operationDate(),
                summary.operationStartedAt(),
                summary.actualEndAt(),
                summary.scheduledEndAt(),
                summary.status(),
                temperatures
        );
    }

    private OperationTelemetryResult processTelemetry(
            CNCMachineEntity machine,
            UUID eventId,
            OffsetDateTime requestedStartAt,
            boolean running,
            OffsetDateTime actualEndAt,
            OffsetDateTime scheduledEndAt,
            BigDecimal temperature,
            OffsetDateTime measuredAt,
            boolean legacyCsv) {

        if (measuredAt == null) {
            throw new BadRequestException("measuredAt é obrigatório.");
        }

        OffsetDateTime latestTelemetryAt = machine.getLastTelemetryAt();
        if (latestTelemetryAt != null && measuredAt.isBefore(latestTelemetryAt)) {
            throw new ConflictException(
                    "A telemetria é anterior à última leitura já processada."
            );
        }

        List<OperationEntity> openOperations = operationRepository
                .findAllByMachine_IdAndStatus(
                        machine.getId(), OperationStatus.EM_ANDAMENTO
                );

        if (openOperations.size() > 1) {
            throw new ConflictException(
                    "A máquina possui mais de uma operação em andamento."
            );
        }

        OperationEntity operation = openOperations.isEmpty()
                ? null
                : openOperations.get(0);

        if (requestedStartAt == null && operation != null) {
            requestedStartAt = operation.getStartDate();
        }

        if (requestedStartAt == null && legacyCsv && !running
                && !Boolean.TRUE.equals(machine.getRunning())
                && machine.getOperationStartedAt() == null) {
            updateMachineTelemetry(machine, false, temperature, measuredAt);
            machineRepository.save(machine);
            return new OperationTelemetryResult(null, false);
        }

        if (requestedStartAt == null) {
            throw new ConflictException(
                    "Não foi possível determinar o início da operação."
            );
        }

        if (measuredAt.isBefore(requestedStartAt)) {
            throw new BadRequestException(
                    "measuredAt não pode ser anterior ao início da operação."
            );
        }

        if (scheduledEndAt != null && scheduledEndAt.isBefore(requestedStartAt)) {
            throw new BadRequestException(
                    "scheduledEndAt não pode ser anterior ao início da operação."
            );
        }

        if (running && actualEndAt != null) {
            throw new BadRequestException(
                    "actualEndAt deve ser nulo enquanto a operação estiver em andamento."
            );
        }

        if (!running && (actualEndAt == null
                || actualEndAt.isBefore(requestedStartAt))) {
            throw new BadRequestException(
                    "actualEndAt é obrigatório e não pode ser anterior ao início."
            );
        }

        boolean operationCreated = false;

        if (running) {
            if (operation != null) {
                ensureSameOperationStart(operation, requestedStartAt);
            } else {
                ensureNotReopeningCompletedOperation(machine, requestedStartAt);

                if (machine.getOperationStartedAt() != null
                        && Boolean.TRUE.equals(machine.getRunning())
                        && !sameInstant(machine.getOperationStartedAt(), requestedStartAt)) {
                    throw new ConflictException(
                            "O horário de início informado diverge da operação atual da máquina."
                    );
                }

                if (!Boolean.TRUE.equals(machine.getRunning())
                        && latestTelemetryAt != null
                        && requestedStartAt.isBefore(latestTelemetryAt)) {
                    throw new ConflictException(
                            "O início informado é anterior à última telemetria da máquina."
                    );
                }

                operation = OperationEntity.builder()
                        .machine(machine)
                        .startDate(requestedStartAt)
                        .startOffsetSeconds(requestedStartAt.getOffset().getTotalSeconds())
                        .status(OperationStatus.EM_ANDAMENTO)
                        .build();
                operationCreated = true;
            }

            machine.setRunning(true);
            machine.setOperationStartedAt(operation.getStartDate());
        } else {
            if (operation != null) {
                ensureSameOperationStart(operation, requestedStartAt);
                completeOperation(operation, machine, actualEndAt);
            } else {
                operation = findCompletedOperation(machine, requestedStartAt);

                if (operation != null) {
                    if (operation.getEndDate() == null
                            || !sameInstant(operation.getEndDate(), actualEndAt)) {
                        throw new ConflictException(
                                "A operação já foi concluída com outro horário de encerramento."
                        );
                    }
                } else if (Boolean.TRUE.equals(machine.getRunning())
                        && machine.getOperationStartedAt() != null) {
                    if (!sameInstant(machine.getOperationStartedAt(), requestedStartAt)) {
                        throw new ConflictException(
                                "O horário de início informado diverge da operação atual da máquina."
                        );
                    }

                    operation = OperationEntity.builder()
                            .machine(machine)
                            .startDate(machine.getOperationStartedAt())
                            .startOffsetSeconds(
                                    machine.getOperationStartedAt().getOffset().getTotalSeconds()
                            )
                            .status(OperationStatus.EM_ANDAMENTO)
                            .build();
                    operationCreated = true;
                    completeOperation(operation, machine, actualEndAt);
                } else if (legacyCsv) {
                    updateMachineTelemetry(machine, false, temperature, measuredAt);
                    machine.setOperationStartedAt(null);
                    machineRepository.save(machine);
                    return new OperationTelemetryResult(null, false);
                } else {
                    throw new ConflictException(
                            "Não existe operação em andamento para encerrar."
                    );
                }
            }

            machine.setRunning(false);
            machine.setOperationStartedAt(null);
        }

        if (scheduledEndAt != null) {
            operation.setScheduledEndDate(scheduledEndAt);
            operation.setScheduledEndOffsetSeconds(
                    scheduledEndAt.getOffset().getTotalSeconds()
            );
        }

        operationRepository.save(operation);
        updateMachineTelemetry(machine, running, temperature, measuredAt);

        if (eventId != null && temperature != null) {
            OperationTemperatureEntity reading = OperationTemperatureEntity.builder()
                    .operation(operation)
                    .temperature(temperature)
                    .measuredAt(measuredAt)
                    .measuredOffsetSeconds(measuredAt.getOffset().getTotalSeconds())
                    .eventId(eventId)
                    .runningAtMeasurement(running)
                    .build();

            try {
                temperatureRepository.save(reading);
            } catch (DataIntegrityViolationException duplicateEvent) {
                throw new ConflictException(
                        "eventId já foi processado por outra telemetria."
                );
            }
        }

        machineRepository.save(machine);

        return new OperationTelemetryResult(toSummary(operation), operationCreated);
    }

    private void completeOperation(
            OperationEntity operation,
            CNCMachineEntity machine,
            OffsetDateTime actualEndAt) {

        if (actualEndAt.isBefore(operation.getStartDate())) {
            throw new BadRequestException(
                    "actualEndAt não pode ser anterior ao início da operação."
            );
        }

        operation.setEndDate(actualEndAt);
        operation.setEndOffsetSeconds(actualEndAt.getOffset().getTotalSeconds());
        operation.setStatus(OperationStatus.CONCLUIDA);

        long elapsedSeconds = Duration.between(
                operation.getStartDate().toInstant(), actualEndAt.toInstant()
        ).getSeconds();

        long previousTotal = Optional.ofNullable(machine.getTotalOperatingSeconds())
                .orElse(0L);
        machine.setTotalOperatingSeconds(previousTotal + elapsedSeconds);
    }

    private void updateMachineTelemetry(
            CNCMachineEntity machine,
            boolean running,
            BigDecimal temperature,
            OffsetDateTime measuredAt) {

        machine.setRunning(running);
        machine.setLastTelemetryAt(measuredAt);

        if (temperature != null) {
            machine.setTemperature(temperature);
        }
    }

    private OperationTelemetryResult handleDuplicate(
            OperationTelemetryRequestDTO request,
            OperationTemperatureEntity duplicate) {

        OperationEntity operation = duplicate.getOperation();
        CNCMachineEntity machine = operation.getMachine();

        boolean matches = Objects.equals(
                machine.getRaspberryMachineId(), request.getRaspberryMachineId()
        )
                && sameInstant(operation.getStartDate(), request.getOperationStartedAt())
                && sameInstant(duplicate.getMeasuredAt(), request.getMeasuredAt())
                && sameTemperature(duplicate.getTemperature(), request.getTemperature())
                && duplicate.isRunningAtMeasurement() == request.getRunning();

        if (!matches) {
            throw new ConflictException(
                    "eventId já foi utilizado por uma telemetria com outros dados."
            );
        }

        if (Boolean.FALSE.equals(request.getRunning())
                && (operation.getEndDate() == null
                || !sameInstant(operation.getEndDate(), request.getActualEndAt()))) {
            throw new ConflictException(
                    "eventId já foi utilizado com outro horário real de encerramento."
            );
        }

        return new OperationTelemetryResult(toSummary(operation), false);
    }

    private void ensureNotReopeningCompletedOperation(
            CNCMachineEntity machine,
            OffsetDateTime requestedStartAt) {

        boolean alreadyCompleted = operationRepository
                .findAllByMachine_IdAndStartDate(machine.getId(), requestedStartAt)
                .stream()
                .anyMatch(operation -> operation.getStatus() == OperationStatus.CONCLUIDA);

        if (alreadyCompleted) {
            throw new ConflictException(
                    "Uma operação concluída não pode ser reaberta."
            );
        }
    }

    private OperationEntity findCompletedOperation(
            CNCMachineEntity machine,
            OffsetDateTime requestedStartAt) {

        return operationRepository
                .findAllByMachine_IdAndStartDate(machine.getId(), requestedStartAt)
                .stream()
                .filter(candidate -> candidate.getStatus() == OperationStatus.CONCLUIDA)
                .filter(candidate -> sameInstant(candidate.getStartDate(), requestedStartAt))
                .findFirst()
                .orElse(null);
    }

    private void ensureSameOperationStart(
            OperationEntity operation,
            OffsetDateTime requestedStartAt) {

        if (!sameInstant(operation.getStartDate(), requestedStartAt)) {
            throw new ConflictException(
                    "O horário de início informado diverge da operação em andamento."
            );
        }
    }

    private void ensureMachineIsActive(CNCMachineEntity machine) {
        if (machine.isDeleted()) {
            throw new ConflictException(
                    "A máquina está desativada e não pode receber telemetria."
            );
        }
    }

    private void validateRequest(OperationTelemetryRequestDTO request) {
        if (request == null
                || request.getEventId() == null
                || request.getRaspberryMachineId() == null
                || request.getRaspberryMachineId() <= 0
                || request.getOperationStartedAt() == null
                || request.getRunning() == null
                || request.getTemperature() == null
                || request.getMeasuredAt() == null
                || !request.isTemporalDataConsistent()) {
            throw new BadRequestException(
                    "Payload de telemetria inválido ou temporalmente inconsistente."
            );
        }
    }

    private OperationSummaryResponseDTO toSummary(OperationEntity operation) {

        CNCMachineEntity machine = operation.getMachine();
        OffsetDateTime startDate = restoreOffset(
                operation.getStartDate(), operation.getStartOffsetSeconds()
        );
        OffsetDateTime endDate = restoreOffset(
                operation.getEndDate(), operation.getEndOffsetSeconds()
        );
        OffsetDateTime scheduledEndDate = restoreOffset(
                operation.getScheduledEndDate(),
                operation.getScheduledEndOffsetSeconds()
        );

        return new OperationSummaryResponseDTO(
                operation.getId(),
                machine.getId(),
                machine.getRaspberryMachineId(),
                machine.getDisplayName(),
                startDate.toLocalDate(),
                startDate,
                endDate,
                scheduledEndDate,
                operation.getStatus()
        );
    }

    private OperationTemperatureResponseDTO toTemperatureResponse(
            OperationTemperatureEntity temperature) {

        return new OperationTemperatureResponseDTO(
                temperature.getId(),
                temperature.getEventId(),
                temperature.getTemperature(),
                restoreOffset(
                        temperature.getMeasuredAt(),
                        temperature.getMeasuredOffsetSeconds()
                ),
                temperature.isRunningAtMeasurement()
        );
    }

    private OffsetDateTime restoreOffset(
            OffsetDateTime value,
            Integer offsetSeconds) {

        if (value == null) {
            return null;
        }

        if (offsetSeconds != null) {
            return value.withOffsetSameInstant(ZoneOffset.ofTotalSeconds(offsetSeconds));
        }

        return value.withOffsetSameInstant(
                LEGACY_ZONE.getRules().getOffset(value.toInstant())
        );
    }

    private boolean sameInstant(OffsetDateTime first, OffsetDateTime second) {
        return first != null && second != null && first.isEqual(second);
    }

    private boolean sameTemperature(BigDecimal first, BigDecimal second) {
        return first == null ? second == null : second != null && first.compareTo(second) == 0;
    }
}
