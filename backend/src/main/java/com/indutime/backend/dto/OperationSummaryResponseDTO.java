package com.indutime.backend.dto;

import com.indutime.backend.enums.OperationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Schema(description = "Resumo de uma operação CNC")
public record OperationSummaryResponseDTO(
        Integer id,
        Integer machineId,
        Integer raspberryMachineId,
        String machineName,
        LocalDate operationDate,
        OffsetDateTime operationStartedAt,
        OffsetDateTime actualEndAt,
        OffsetDateTime scheduledEndAt,
        OperationStatus status) {
}
