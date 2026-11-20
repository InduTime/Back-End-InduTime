package com.indutime.backend.controller;

import com.indutime.backend.dto.OperationResponseDTO;
import com.indutime.backend.dto.OperationSummaryResponseDTO;
import com.indutime.backend.dto.OperationTelemetryRequestDTO;
import com.indutime.backend.enums.OperationStatus;
import com.indutime.backend.service.OperationService;
import com.indutime.backend.service.OperationTelemetryResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@Tag(
        name = "Operações CNC",
        description = "Recebimento de telemetria e consulta ao histórico das operações CNC"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/operations")
public class OperationController {

    private final OperationService operationService;

    @PostMapping("/telemetry")
    @Operation(
            summary = "Receber telemetria operacional",
            description = "Inicia ou atualiza uma operação quando running=true e a encerra "
                    + "quando running=false. Cada eventId é idempotente. Os horários devem "
                    + "usar ISO-8601 com offset, como 2026-11-19T08:20:00-03:00. "
                    + "Uma operação criada retorna 201; continuidade ou repetição retorna 200."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Operação criada"),
            @ApiResponse(responseCode = "200", description = "Telemetria processada ou repetida sem duplicidade"),
            @ApiResponse(responseCode = "400", description = "Payload inválido"),
            @ApiResponse(responseCode = "404", description = "Máquina Raspberry não encontrada"),
            @ApiResponse(responseCode = "409", description = "Máquina desativada ou operação inconsistente")
    })
    public ResponseEntity<OperationSummaryResponseDTO> receiveTelemetry(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Payload de operação com timestamps ISO-8601 e offset",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = "{\"eventId\":\"d739e601-33dd-4a50-a012-fc3f114293d0\",\"raspberryMachineId\":1,\"operationStartedAt\":\"2026-11-19T08:00:00-03:00\",\"running\":true,\"actualEndAt\":null,\"scheduledEndAt\":\"2026-11-19T12:00:00-03:00\",\"temperature\":63.45,\"measuredAt\":\"2026-11-19T08:20:00-03:00\"}")
                    )
            )
            @Valid @RequestBody OperationTelemetryRequestDTO request) {

        OperationTelemetryResult result = operationService.receiveTelemetry(request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.operation());
    }

    @GetMapping
    @Operation(
            summary = "Listar operações",
            description = "Retorna resumos de operações. startDate e endDate filtram "
                    + "inclusivamente o instante de início da operação."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Filtro ou intervalo de datas inválido")
    })
    public List<OperationSummaryResponseDTO> listOperations(
            @Parameter(description = "ID externo da máquina na Raspberry", example = "1")
            @RequestParam(required = false) Integer raspberryMachineId,
            @Parameter(description = "Início do período ISO-8601 com offset", example = "2026-11-19T00:00:00-03:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @Parameter(description = "Fim do período ISO-8601 com offset", example = "2026-11-20T00:00:00-03:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @Parameter(description = "Status da operação")
            @RequestParam(required = false) OperationStatus status) {

        return operationService.listOperations(
                raspberryMachineId, startDate, endDate, status
        );
    }

    @GetMapping("/{operationId}")
    @Operation(
            summary = "Consultar detalhes da operação",
            description = "Retorna a máquina associada e todas as temperaturas registradas "
                    + "em ordem cronológica. operationDate é derivada do início da operação."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operação encontrada"),
            @ApiResponse(responseCode = "404", description = "Operação não encontrada")
    })
    public OperationResponseDTO getOperation(
            @Parameter(description = "ID interno da operação", example = "12")
            @PathVariable Integer operationId) {

        return operationService.getOperation(operationId);
    }
}
