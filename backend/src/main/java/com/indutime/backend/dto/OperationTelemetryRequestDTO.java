package com.indutime.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Schema(description = "Telemetria operacional enviada pela Raspberry Pi")
public class OperationTelemetryRequestDTO {

    @NotNull
    @Schema(description = "Identificador único do evento, reutilizado em novas tentativas",
            example = "d739e601-33dd-4a50-a012-fc3f114293d0", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID eventId;

    @NotNull
    @Positive
    @Schema(description = "ID externo configurado na Raspberry, não é o ID interno do banco",
            example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer raspberryMachineId;

    @NotNull
    @Schema(description = "Instante original de início da operação, com offset ISO-8601",
            example = "2026-11-19T08:00:00-03:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private OffsetDateTime operationStartedAt;

    @NotNull
    @Schema(description = "Indica se a operação ainda está em andamento",
            example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean running;

    @Schema(description = "Instante real de encerramento; obrigatório quando running=false",
            example = "2026-11-19T12:03:00-03:00")
    private OffsetDateTime actualEndAt;

    @Schema(description = "Previsão de encerramento, independente do horário real",
            example = "2026-11-19T12:00:00-03:00")
    private OffsetDateTime scheduledEndAt;

    @NotNull
    @Schema(description = "Temperatura medida em graus Celsius",
            example = "63.45", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal temperature;

    @NotNull
    @Schema(description = "Instante da coleta da temperatura, com offset ISO-8601",
            example = "2026-11-19T08:20:00-03:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private OffsetDateTime measuredAt;

    @AssertTrue(message = "Os horários da operação são inconsistentes.")
    @JsonIgnore
    public boolean isTemporalDataConsistent() {
        if (operationStartedAt == null || running == null) {
            return true;
        }

        if (scheduledEndAt != null && scheduledEndAt.isBefore(operationStartedAt)) {
            return false;
        }

        if (measuredAt != null && measuredAt.isBefore(operationStartedAt)) {
            return false;
        }

        if (Boolean.TRUE.equals(running)) {
            return actualEndAt == null;
        }

        return actualEndAt != null && !actualEndAt.isBefore(operationStartedAt);
    }
}
