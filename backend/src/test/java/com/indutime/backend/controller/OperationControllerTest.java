package com.indutime.backend.controller;

import tools.jackson.databind.ObjectMapper;

import com.indutime.backend.dto.OperationResponseDTO;
import com.indutime.backend.dto.OperationSummaryResponseDTO;
import com.indutime.backend.dto.OperationTelemetryRequestDTO;
import com.indutime.backend.dto.OperationTemperatureResponseDTO;
import com.indutime.backend.enums.OperationStatus;
import com.indutime.backend.service.OperationService;
import com.indutime.backend.service.OperationTelemetryResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OperationController.class)
@DisplayName("Testes do OperationController")
class OperationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OperationService operationService;

    @Test
    @DisplayName("Retorna 201 ao registrar uma nova operação")
    void shouldReturnCreatedForNewOperation() throws Exception {
        OperationSummaryResponseDTO response = summary();
        when(operationService.receiveTelemetry(any(OperationTelemetryRequestDTO.class)))
                .thenReturn(new OperationTelemetryResult(response, true));

        mockMvc.perform(post("/v1/operations/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.raspberryMachineId").value(1))
                .andExpect(jsonPath("$.operationDate").value("2026-11-19"));

        verify(operationService).receiveTelemetry(any(OperationTelemetryRequestDTO.class));
    }

    @Test
    @DisplayName("Retorna 200 para continuidade ou repetição idempotente")
    void shouldReturnOkForExistingOperation() throws Exception {
        when(operationService.receiveTelemetry(any(OperationTelemetryRequestDTO.class)))
                .thenReturn(new OperationTelemetryResult(summary(), false));

        mockMvc.perform(post("/v1/operations/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
    }

    @Test
    @DisplayName("Rejeita payload sem temperatura com HTTP 400")
    void shouldRejectInvalidTelemetryPayload() throws Exception {
        String payload = """
                {
                  "eventId": "d739e601-33dd-4a50-a012-fc3f114293d0",
                  "raspberryMachineId": 1,
                  "operationStartedAt": "2026-11-19T08:00:00-03:00",
                  "running": true,
                  "actualEndAt": null,
                  "scheduledEndAt": null,
                  "measuredAt": "2026-11-19T08:20:00-03:00"
                }
                """;

        mockMvc.perform(post("/v1/operations/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Lista operações com os filtros recebidos")
    void shouldListOperations() throws Exception {
        when(operationService.listOperations(
                1,
                OffsetDateTime.parse("2026-11-19T00:00:00-03:00"),
                OffsetDateTime.parse("2026-11-20T00:00:00-03:00"),
                OperationStatus.CONCLUIDA
        )).thenReturn(List.of(summary()));

        mockMvc.perform(get("/v1/operations")
                        .param("raspberryMachineId", "1")
                        .param("startDate", "2026-11-19T00:00:00-03:00")
                        .param("endDate", "2026-11-20T00:00:00-03:00")
                        .param("status", "CONCLUIDA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].raspberryMachineId").value(1));

        verify(operationService).listOperations(
                1,
                OffsetDateTime.parse("2026-11-19T00:00:00-03:00"),
                OffsetDateTime.parse("2026-11-20T00:00:00-03:00"),
                OperationStatus.CONCLUIDA
        );
    }

    @Test
    @DisplayName("Consulta detalhes e temperaturas de uma operação")
    void shouldGetOperationDetails() throws Exception {
        OperationTemperatureResponseDTO reading = new OperationTemperatureResponseDTO(
                35,
                UUID.fromString("d739e601-33dd-4a50-a012-fc3f114293d0"),
                new BigDecimal("63.45"),
                OffsetDateTime.parse("2026-11-19T08:20:00-03:00"),
                true
        );
        OperationSummaryResponseDTO summary = summary();
        when(operationService.getOperation(12)).thenReturn(new OperationResponseDTO(
                summary.id(), summary.machineId(), summary.raspberryMachineId(),
                summary.machineName(), summary.operationDate(),
                summary.operationStartedAt(), summary.actualEndAt(),
                summary.scheduledEndAt(), summary.status(), List.of(reading)
        ));

        mockMvc.perform(get("/v1/operations/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.machineName").value("Torno principal"))
                .andExpect(jsonPath("$.temperatureReadings[0].temperature").value(63.45));

        verify(operationService).getOperation(12);
    }

    private OperationTelemetryRequestDTO request() {
        return OperationTelemetryRequestDTO.builder()
                .eventId(UUID.fromString("d739e601-33dd-4a50-a012-fc3f114293d0"))
                .raspberryMachineId(1)
                .operationStartedAt(OffsetDateTime.parse("2026-11-19T08:00:00-03:00"))
                .running(true)
                .scheduledEndAt(OffsetDateTime.parse("2026-11-19T12:00:00-03:00"))
                .temperature(new BigDecimal("63.45"))
                .measuredAt(OffsetDateTime.parse("2026-11-19T08:20:00-03:00"))
                .build();
    }

    private OperationSummaryResponseDTO summary() {
        return new OperationSummaryResponseDTO(
                12,
                41,
                1,
                "Torno principal",
                LocalDate.of(2026, 11, 19),
                OffsetDateTime.parse("2026-11-19T08:00:00-03:00"),
                null,
                OffsetDateTime.parse("2026-11-19T12:00:00-03:00"),
                OperationStatus.EM_ANDAMENTO
        );
    }
}
