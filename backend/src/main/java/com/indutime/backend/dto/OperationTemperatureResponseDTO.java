package com.indutime.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Leitura histórica de temperatura associada a uma operação")
public record OperationTemperatureResponseDTO(
        Integer id,
        UUID eventId,
        BigDecimal temperature,
        OffsetDateTime measuredAt,
        boolean running) {
}
