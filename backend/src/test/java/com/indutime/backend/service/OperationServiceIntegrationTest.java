package com.indutime.backend.service;

import com.indutime.backend.database.model.CNCMachineEntity;
import com.indutime.backend.database.repository.ICNCMachineRepository;
import com.indutime.backend.database.repository.OperationTemperatureRepository;
import com.indutime.backend.dto.OperationResponseDTO;
import com.indutime.backend.dto.OperationTelemetryRequestDTO;
import com.indutime.backend.enums.CNCLocation;
import com.indutime.backend.enums.CNCManufacturer;
import com.indutime.backend.enums.CNCModel;
import com.indutime.backend.enums.CNCStatus;
import com.indutime.backend.enums.OperationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Integração de persistência de operações")
class OperationServiceIntegrationTest {

    private static final Integer RASPBERRY_MACHINE_ID = 987_654;
    private static final OffsetDateTime START =
            OffsetDateTime.parse("2026-11-19T08:00:00-03:00");

    @Autowired
    private OperationService operationService;

    @Autowired
    private ICNCMachineRepository machineRepository;

    @Autowired
    private OperationTemperatureRepository temperatureRepository;

    @Test
    @DisplayName("Persiste offsets, idempotência, encerramento e histórico no H2")
    void shouldPersistOperationLifecycleAndTemperatureHistory() {
        CNCMachineEntity machine = machineRepository.save(CNCMachineEntity.builder()
                .raspberryMachineId(RASPBERRY_MACHINE_ID)
                .displayName("Torno de integração")
                .model(CNCModel.HAAS_ST_10)
                .manufacturer(CNCManufacturer.HAAS)
                .location(CNCLocation.SETOR_USINAGEM)
                .status(CNCStatus.ONLINE)
                .deleted(false)
                .running(false)
                .totalOperatingSeconds(0L)
                .build());

        UUID firstEventId = UUID.randomUUID();
        OffsetDateTime firstMeasuredAt = START.plusMinutes(20);
        OperationTelemetryRequestDTO startRequest = request(
                firstEventId,
                true,
                null,
                OffsetDateTime.parse("2026-11-19T12:00:00-03:00"),
                firstMeasuredAt,
                "63.45"
        );

        OperationTelemetryResult first = operationService.receiveTelemetry(startRequest);
        OperationTelemetryResult replay = operationService.receiveTelemetry(startRequest);

        assertTrue(first.created());
        assertFalse(replay.created());
        assertEquals(first.operation().id(), replay.operation().id());
        assertEquals(machine.getId(), first.operation().machineId());

        OffsetDateTime actualEnd = START.plusHours(1);
        OperationTelemetryRequestDTO stopRequest = request(
                UUID.randomUUID(),
                false,
                actualEnd,
                OffsetDateTime.parse("2026-11-19T12:00:00-03:00"),
                actualEnd,
                "59.12"
        );
        OperationTelemetryResult stopped = operationService.receiveTelemetry(stopRequest);
        OperationResponseDTO details = operationService.getOperation(
                stopped.operation().id()
        );

        assertEquals(OperationStatus.CONCLUIDA, details.status());
        assertEquals(START, details.operationStartedAt());
        assertEquals(actualEnd, details.actualEndAt());
        assertEquals(OffsetDateTime.parse("2026-11-19T12:00:00-03:00"),
                details.scheduledEndAt());
        assertEquals(2, details.temperatureReadings().size());
        assertEquals(firstMeasuredAt, details.temperatureReadings().get(0).measuredAt());
        assertEquals(3_600L, machineRepository.findById(machine.getId())
                .orElseThrow().getTotalOperatingSeconds());
        assertEquals(2, temperatureRepository
                .findAllByOperation_IdOrderByMeasuredAtAsc(details.id()).size());

        assertEquals(1, operationService.listOperations(
                RASPBERRY_MACHINE_ID,
                START.minusDays(1),
                actualEnd.plusDays(1),
                OperationStatus.CONCLUIDA
        ).size());
    }

    private OperationTelemetryRequestDTO request(
            UUID eventId,
            boolean running,
            OffsetDateTime actualEndAt,
            OffsetDateTime scheduledEndAt,
            OffsetDateTime measuredAt,
            String temperature) {
        return OperationTelemetryRequestDTO.builder()
                .eventId(eventId)
                .raspberryMachineId(RASPBERRY_MACHINE_ID)
                .operationStartedAt(START)
                .running(running)
                .actualEndAt(actualEndAt)
                .scheduledEndAt(scheduledEndAt)
                .temperature(new BigDecimal(temperature))
                .measuredAt(measuredAt)
                .build();
    }
}
