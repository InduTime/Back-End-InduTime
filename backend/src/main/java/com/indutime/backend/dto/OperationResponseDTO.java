package com.indutime.backend.dto;

import com.indutime.backend.enums.OperationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Detalhes de uma operação CNC e suas temperaturas históricas")
public record OperationResponseDTO(
        Integer id,
        Integer machineId,
        Integer raspberryMachineId,
        String machineName,
        LocalDate operationDate,
        OffsetDateTime operationStartedAt,
        OffsetDateTime actualEndAt,
        OffsetDateTime scheduledEndAt,
        OperationStatus status,
        List<OperationTemperatureResponseDTO> temperatureReadings) {
}
