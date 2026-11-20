package com.indutime.backend.service;

import com.indutime.backend.database.model.CNCMachineEntity;
import com.indutime.backend.database.model.OperationEntity;
import com.indutime.backend.database.model.OperationTemperatureEntity;
import com.indutime.backend.database.repository.ICNCMachineRepository;
import com.indutime.backend.database.repository.OperationRepository;
import com.indutime.backend.database.repository.OperationTemperatureRepository;
import com.indutime.backend.dto.CNCMachineRequestDTO;
import jakarta.persistence.EntityManager;
import com.indutime.backend.dto.OperationResponseDTO;
import com.indutime.backend.dto.OperationSummaryResponseDTO;
import com.indutime.backend.dto.OperationTelemetryRequestDTO;
import com.indutime.backend.enums.CNCStatus;
import com.indutime.backend.enums.OperationStatus;
import com.indutime.backend.exception.BadRequestException;
import com.indutime.backend.exception.ConflictException;
import com.indutime.backend.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do OperationService")
class OperationServiceTest {

    private static final OffsetDateTime START =
            OffsetDateTime.parse("2026-11-19T08:00:00-03:00");
    private static final OffsetDateTime FIRST_MEASUREMENT =
            OffsetDateTime.parse("2026-11-19T08:20:00-03:00");
    private static final BigDecimal TEMPERATURE = new BigDecimal("63.45");

    @Mock
    private OperationRepository operationRepository;

    @Mock
    private OperationTemperatureRepository temperatureRepository;

    @Mock
    private ICNCMachineRepository machineRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private OperationService operationService;

    private CNCMachineEntity machine;

    @BeforeEach
    void setUp() {
        machine = CNCMachineEntity.builder()
                .id(41)
                .raspberryMachineId(1)
                .displayName("Torno principal")
                .status(CNCStatus.ONLINE)
                .deleted(false)
                .running(false)
                .totalOperatingSeconds(0L)
                .build();

        lenient().when(machineRepository.save(any(CNCMachineEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(operationRepository.save(any(OperationEntity.class)))
                .thenAnswer(invocation -> {
                    OperationEntity operation = invocation.getArgument(0);
                    if (operation.getId() == null) {
                        operation.setId(10);
                    }
                    return operation;
                });
        lenient().when(temperatureRepository.save(any(OperationTemperatureEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Cria uma operação e localiza a máquina pelo ID externo da Raspberry")
    void shouldCreateOperationAndUseRaspberryMachineId() {
        OperationTelemetryRequestDTO request = runningRequest(UUID.randomUUID(), FIRST_MEASUREMENT);
        prepareNewEvent(request);

        OperationTelemetryResult result = operationService.receiveTelemetry(request);

        assertTrue(result.created());
        assertEquals(10, result.operation().id());
        assertEquals(1, result.operation().raspberryMachineId());
        assertEquals(41, result.operation().machineId());
        assertEquals(START.toLocalDate(), result.operation().operationDate());
        assertTrue(machine.getRunning());
        assertEquals(START, machine.getOperationStartedAt());
        assertEquals(FIRST_MEASUREMENT, machine.getLastTelemetryAt());
        assertEquals(TEMPERATURE, machine.getTemperature());

        verify(machineRepository).findByRaspberryMachineIdForUpdate(1);
        verify(operationRepository).save(argThat(saved ->
                saved.getMachine().getId().equals(41)
                        && saved.getStartDate().isEqual(START)
                        && saved.getStatus() == OperationStatus.EM_ANDAMENTO));
        verify(temperatureRepository).save(any(OperationTemperatureEntity.class));
    }

    @Test
    @DisplayName("Atualiza uma operação aberta sem criar outra")
    void shouldContinueExistingOperation() {
        UUID firstEvent = UUID.randomUUID();
        OperationEntity operation = openOperation();
        machine.setRunning(true);
        machine.setOperationStartedAt(START);
        machine.setLastTelemetryAt(FIRST_MEASUREMENT);

        OperationTelemetryRequestDTO request = runningRequest(
                UUID.randomUUID(), FIRST_MEASUREMENT.plusMinutes(20)
        );
        when(machineRepository.findByRaspberryMachineIdForUpdate(1))
                .thenReturn(Optional.of(machine));
        when(temperatureRepository.findByEventId(request.getEventId()))
                .thenReturn(Optional.empty());
        when(operationRepository.findAllByMachine_IdAndStatus(
                41, OperationStatus.EM_ANDAMENTO))
                .thenReturn(List.of(operation));

        OperationTelemetryResult result = operationService.receiveTelemetry(request);

        assertFalse(result.created());
        assertTrue(machine.getOperationStartedAt().isEqual(START));
        verify(operationRepository).save(same(operation));
        verify(temperatureRepository).save(any(OperationTemperatureEntity.class));
        assertNotEquals(firstEvent, request.getEventId());
    }

    @Test
    @DisplayName("Registra várias temperaturas na mesma operação")
    void shouldRecordMultipleTemperaturesForOneOperation() {
        OperationEntity operation = openOperation();
        machine.setRunning(true);
        machine.setOperationStartedAt(START);
        machine.setLastTelemetryAt(FIRST_MEASUREMENT);

        OperationTelemetryRequestDTO first = runningRequest(
                UUID.randomUUID(), FIRST_MEASUREMENT.plusMinutes(1)
        );
        OperationTelemetryRequestDTO second = runningRequest(
                UUID.randomUUID(), FIRST_MEASUREMENT.plusMinutes(2)
        );
        when(machineRepository.findByRaspberryMachineIdForUpdate(1))
                .thenReturn(Optional.of(machine));
        when(temperatureRepository.findByEventId(any(UUID.class)))
                .thenReturn(Optional.empty());
        when(operationRepository.findAllByMachine_IdAndStatus(
                41, OperationStatus.EM_ANDAMENTO))
                .thenReturn(List.of(operation));

        operationService.receiveTelemetry(first);
        operationService.receiveTelemetry(second);

        verify(operationRepository, times(2)).save(same(operation));
        verify(temperatureRepository, times(2))
                .save(any(OperationTemperatureEntity.class));
        assertEquals(second.getMeasuredAt(), machine.getLastTelemetryAt());
    }

    @Test
    @DisplayName("Encerra uma operação e soma seu tempo operacional uma única vez")
    void shouldCloseOperationAndAccumulateOperatingSeconds() {
        OperationEntity operation = openOperation();
        machine.setRunning(true);
        machine.setOperationStartedAt(START);
        machine.setLastTelemetryAt(FIRST_MEASUREMENT);
        machine.setTotalOperatingSeconds(90L);
        OffsetDateTime actualEnd = START.plusHours(1);
        OperationTelemetryRequestDTO request = stoppedRequest(
                UUID.randomUUID(), actualEnd, actualEnd
        );
        prepareOpenOperation(request, operation);

        OperationTelemetryResult result = operationService.receiveTelemetry(request);

        assertEquals(OperationStatus.CONCLUIDA, operation.getStatus());
        assertEquals(actualEnd, operation.getEndDate());
        assertEquals(3_690L, machine.getTotalOperatingSeconds());
        assertFalse(machine.getRunning());
        assertNull(machine.getOperationStartedAt());
        assertEquals(actualEnd, result.operation().actualEndAt());
    }

    @Test
    @DisplayName("Mantém previsão e encerramento real como horários diferentes")
    void shouldKeepScheduledAndActualEndSeparate() {
        OperationEntity operation = openOperation();
        OffsetDateTime scheduled = START.plusHours(4);
        OffsetDateTime actual = START.plusHours(4).plusMinutes(3);
        operation.setScheduledEndDate(scheduled);
        operation.setScheduledEndOffsetSeconds(scheduled.getOffset().getTotalSeconds());
        machine.setRunning(true);
        machine.setOperationStartedAt(START);
        machine.setLastTelemetryAt(FIRST_MEASUREMENT);
        OperationTelemetryRequestDTO request = stoppedRequest(
                UUID.randomUUID(), actual, actual
        );
        request.setScheduledEndAt(scheduled);
        prepareOpenOperation(request, operation);

        OperationTelemetryResult result = operationService.receiveTelemetry(request);

        assertEquals(actual, result.operation().actualEndAt());
        assertEquals(scheduled, result.operation().scheduledEndAt());
        assertNotEquals(result.operation().actualEndAt(), result.operation().scheduledEndAt());
    }

    @Test
    @DisplayName("Conta a duração pelo instante, mesmo que o offset mude")
    void shouldCalculateDurationAcrossOffsets() {
        OperationEntity operation = openOperation();
        machine.setRunning(true);
        machine.setOperationStartedAt(START);
        machine.setLastTelemetryAt(FIRST_MEASUREMENT);
        OffsetDateTime actualEnd = OffsetDateTime.parse("2026-11-19T12:00:00Z");
        OperationTelemetryRequestDTO request = stoppedRequest(
                UUID.randomUUID(), actualEnd, actualEnd
        );
        prepareOpenOperation(request, operation);

        operationService.receiveTelemetry(request);

        assertEquals(3_600L, machine.getTotalOperatingSeconds());
        assertEquals(-10_800, operation.getStartOffsetSeconds());
        assertEquals(0, operation.getEndOffsetSeconds());
    }

    @Test
    @DisplayName("Impede mais de uma operação aberta para a máquina")
    void shouldRejectMultipleOpenOperations() {
        OperationTelemetryRequestDTO request = runningRequest(UUID.randomUUID(), FIRST_MEASUREMENT);
        prepareMachine(request);
        when(operationRepository.findAllByMachine_IdAndStatus(
                41, OperationStatus.EM_ANDAMENTO))
                .thenReturn(List.of(openOperation(), openOperation()));

        assertThrows(ConflictException.class,
                () -> operationService.receiveTelemetry(request));

        verify(operationRepository, never()).save(any(OperationEntity.class));
    }

    @Test
    @DisplayName("Rejeita encerramento anterior ao início")
    void shouldRejectEndBeforeStart() {
        OperationTelemetryRequestDTO request = stoppedRequest(
                UUID.randomUUID(), START.minusSeconds(1), START
        );

        assertThrows(BadRequestException.class,
                () -> operationService.receiveTelemetry(request));

        verifyNoInteractions(machineRepository);
    }

    @Test
    @DisplayName("Retorna 404 de serviço quando a máquina Raspberry não existe")
    void shouldRejectUnknownMachine() {
        OperationTelemetryRequestDTO request = runningRequest(UUID.randomUUID(), FIRST_MEASUREMENT);
        when(machineRepository.findByRaspberryMachineIdForUpdate(1))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> operationService.receiveTelemetry(request));
    }

    @Test
    @DisplayName("Rejeita telemetria de máquina desativada")
    void shouldRejectDeactivatedMachine() {
        OperationTelemetryRequestDTO request = runningRequest(UUID.randomUUID(), FIRST_MEASUREMENT);
        machine.setDeleted(true);
        prepareMachine(request);
        assertThrows(ConflictException.class,
                () -> operationService.receiveTelemetry(request));
    }

    @Test
    @DisplayName("Não duplica uma leitura quando o mesmo eventId é reenviado")
    void shouldTreatRepeatedEventAsIdempotent() {
        OperationTelemetryRequestDTO request = runningRequest(UUID.randomUUID(), FIRST_MEASUREMENT);
        prepareNewEvent(request);
        AtomicReference<OperationTemperatureEntity> persisted = new AtomicReference<>();
        when(temperatureRepository.findByEventId(request.getEventId()))
                .thenAnswer(invocation -> Optional.ofNullable(persisted.get()));
        when(temperatureRepository.save(any(OperationTemperatureEntity.class)))
                .thenAnswer(invocation -> {
                    OperationTemperatureEntity reading = invocation.getArgument(0);
                    persisted.set(reading);
                    return reading;
                });

        OperationTelemetryResult first = operationService.receiveTelemetry(request);
        OperationTelemetryResult replay = operationService.receiveTelemetry(request);

        assertTrue(first.created());
        assertFalse(replay.created());
        assertEquals(first.operation().id(), replay.operation().id());
        verify(operationRepository, times(1)).save(any(OperationEntity.class));
        verify(temperatureRepository, times(1)).save(any(OperationTemperatureEntity.class));
    }

    @Test
    @DisplayName("Aceita parada CSV repetida quando não há operação aberta")
    void shouldKeepStoppedCsvTelemetryCompatibleWithoutOpenOperation() {
        UUID eventId = UUID.randomUUID();
        when(machineRepository.findByIdForUpdate(41)).thenReturn(Optional.of(machine));
        when(temperatureRepository.findByEventId(eventId)).thenReturn(Optional.empty());
        when(operationRepository.findAllByMachine_IdAndStatus(
                41, OperationStatus.EM_ANDAMENTO))
                .thenReturn(List.of());

        CNCMachineRequestDTO metadata = CNCMachineRequestDTO.builder()
                .displayName("Torno principal")
                .serialNumber("CNC-001")
                .model(com.indutime.backend.enums.CNCModel.HAAS_ST_10)
                .manufacturer(com.indutime.backend.enums.CNCManufacturer.HAAS)
                .location(com.indutime.backend.enums.CNCLocation.SETOR_USINAGEM)
                .build();

        operationService.receiveCsvTelemetry(
                41,
                1,
                metadata,
                eventId,
                FIRST_MEASUREMENT,
                false,
                null
        );
        operationService.receiveCsvTelemetry(
                41, 1, metadata, eventId, FIRST_MEASUREMENT, false, null
        );

        assertFalse(machine.getRunning());
        assertEquals(FIRST_MEASUREMENT, machine.getLastTelemetryAt());
        verify(operationRepository, never()).save(any(OperationEntity.class));
    }

    @Test
    @DisplayName("Rejeita telemetria antiga sem sobrescrever o estado atual")
    void shouldRejectOlderTelemetry() {
        OperationTelemetryRequestDTO request = runningRequest(
                UUID.randomUUID(), FIRST_MEASUREMENT.minusMinutes(1)
        );
        machine.setLastTelemetryAt(FIRST_MEASUREMENT);
        prepareMachine(request);
        assertThrows(ConflictException.class,
                () -> operationService.receiveTelemetry(request));

        assertEquals(FIRST_MEASUREMENT, machine.getLastTelemetryAt());
        assertFalse(machine.getRunning());
    }

    @Test
    @DisplayName("Aplica filtros de máquina, período e status na listagem")
    void shouldListOperationsWithFilters() {
        OperationEntity operation = openOperation();
        OffsetDateTime startFilter = START.minusDays(1);
        OffsetDateTime endFilter = START.plusDays(1);
        when(operationRepository.findAll(
                any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(operation));

        List<OperationSummaryResponseDTO> result = operationService.listOperations(
                1, startFilter, endFilter, OperationStatus.EM_ANDAMENTO
        );

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).raspberryMachineId());
        verify(operationRepository).findAll(
                any(Specification.class), any(Sort.class)
        );
    }

    @Test
    @DisplayName("Retorna detalhes da operação e temperaturas em ordem do repositório")
    void shouldReturnOperationWithTemperatureHistory() {
        OperationEntity operation = openOperation();
        OperationTemperatureEntity reading = OperationTemperatureEntity.builder()
                .id(22)
                .operation(operation)
                .temperature(TEMPERATURE)
                .measuredAt(FIRST_MEASUREMENT.withOffsetSameInstant(ZoneOffset.UTC))
                .measuredOffsetSeconds(FIRST_MEASUREMENT.getOffset().getTotalSeconds())
                .eventId(UUID.randomUUID())
                .runningAtMeasurement(true)
                .build();
        when(operationRepository.findById(10)).thenReturn(Optional.of(operation));
        when(temperatureRepository.findAllByOperation_IdOrderByMeasuredAtAsc(10))
                .thenReturn(List.of(reading));

        OperationResponseDTO response = operationService.getOperation(10);

        assertEquals(1, response.temperatureReadings().size());
        assertEquals(TEMPERATURE, response.temperatureReadings().get(0).temperature());
        assertEquals(FIRST_MEASUREMENT, response.temperatureReadings().get(0).measuredAt());
        assertEquals("Torno principal", response.machineName());
    }

    @Test
    @DisplayName("Não reabre uma operação concluída")
    void shouldNotReopenCompletedOperation() {
        OperationTelemetryRequestDTO request = runningRequest(UUID.randomUUID(), FIRST_MEASUREMENT);
        OperationEntity completed = openOperation();
        completed.setStatus(OperationStatus.CONCLUIDA);
        completed.setEndDate(START.plusHours(1));
        prepareMachine(request);
        when(operationRepository.findAllByMachine_IdAndStatus(
                41, OperationStatus.EM_ANDAMENTO))
                .thenReturn(List.of());
        when(operationRepository.findAllByMachine_IdAndStartDate(41, START))
                .thenReturn(List.of(completed));

        assertThrows(ConflictException.class,
                () -> operationService.receiveTelemetry(request));
    }

    @Test
    @DisplayName("Retorna conflito ao encerrar sem operação aberta")
    void shouldRejectClosingWithoutAnOpenOperation() {
        OffsetDateTime actualEnd = START.plusHours(1);
        OperationTelemetryRequestDTO request = stoppedRequest(
                UUID.randomUUID(), actualEnd, actualEnd
        );
        prepareMachine(request);
        when(operationRepository.findAllByMachine_IdAndStatus(
                41, OperationStatus.EM_ANDAMENTO))
                .thenReturn(List.of());
        when(operationRepository.findAllByMachine_IdAndStartDate(41, START))
                .thenReturn(List.of());

        assertThrows(ConflictException.class,
                () -> operationService.receiveTelemetry(request));
    }

    private void prepareNewEvent(OperationTelemetryRequestDTO request) {
        prepareMachine(request);
        when(operationRepository.findAllByMachine_IdAndStatus(
                41, OperationStatus.EM_ANDAMENTO))
                .thenReturn(List.of());
        when(operationRepository.findAllByMachine_IdAndStartDate(
                41, request.getOperationStartedAt()))
                .thenReturn(List.of());
    }

    private void prepareMachine(OperationTelemetryRequestDTO request) {
        when(machineRepository.findByRaspberryMachineIdForUpdate(1))
                .thenReturn(Optional.of(machine));
        when(temperatureRepository.findByEventId(request.getEventId()))
                .thenReturn(Optional.empty());
    }

    private void prepareOpenOperation(
            OperationTelemetryRequestDTO request,
            OperationEntity operation) {
        prepareMachine(request);
        when(operationRepository.findAllByMachine_IdAndStatus(
                41, OperationStatus.EM_ANDAMENTO))
                .thenReturn(List.of(operation));
    }

    private OperationEntity openOperation() {
        return OperationEntity.builder()
                .id(10)
                .machine(machine)
                .startDate(START)
                .startOffsetSeconds(START.getOffset().getTotalSeconds())
                .status(OperationStatus.EM_ANDAMENTO)
                .build();
    }

    private OperationTelemetryRequestDTO runningRequest(
            UUID eventId,
            OffsetDateTime measuredAt) {
        return OperationTelemetryRequestDTO.builder()
                .eventId(eventId)
                .raspberryMachineId(1)
                .operationStartedAt(START)
                .running(true)
                .actualEndAt(null)
                .scheduledEndAt(START.plusHours(4))
                .temperature(TEMPERATURE)
                .measuredAt(measuredAt)
                .build();
    }

    private OperationTelemetryRequestDTO stoppedRequest(
            UUID eventId,
            OffsetDateTime actualEndAt,
            OffsetDateTime measuredAt) {
        return OperationTelemetryRequestDTO.builder()
                .eventId(eventId)
                .raspberryMachineId(1)
                .operationStartedAt(START)
                .running(false)
                .actualEndAt(actualEndAt)
                .scheduledEndAt(START.plusHours(4))
                .temperature(TEMPERATURE)
                .measuredAt(measuredAt)
                .build();
    }
}
