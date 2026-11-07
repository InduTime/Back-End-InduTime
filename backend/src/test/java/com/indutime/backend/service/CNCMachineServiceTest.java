
package com.indutime.backend.service;

import com.indutime.backend.database.model.CNCMachineEntity;
import com.indutime.backend.database.repository.ICNCMachineRepository;
import com.indutime.backend.dto.CNCMachineRequestDTO;
import com.indutime.backend.dto.CNCMachineResponseDTO;
import com.indutime.backend.enums.CNCLocation;
import com.indutime.backend.enums.CNCManufacturer;
import com.indutime.backend.enums.CNCModel;
import com.indutime.backend.enums.CNCStatus;
import com.indutime.backend.exception.BadRequestException;
import com.indutime.backend.exception.ConflictException;
import com.indutime.backend.exception.NotFoundException;
import com.indutime.backend.utils.CNCMachineMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do CNCMachineService")
class CNCMachineServiceTest {

    @Mock
    private ICNCMachineRepository machineRepository;

    @Mock
    private CNCMachineMapper machineMapper;

    @InjectMocks
    private CNCMachineService machineService;

    private CNCMachineResponseDTO machineResponse;
    private CNCMachineRequestDTO machineRequest;
    private CNCMachineEntity machineEntity;

    @BeforeEach
    void setUp() {

        machineResponse = new CNCMachineResponseDTO();

        machineRequest = CNCMachineRequestDTO.builder()
                .displayName("Torno CNC Principal")
                .model(CNCModel.HAAS_ST_10)
                .manufacturer(CNCManufacturer.HAAS)
                .serialNumber("CNC-2026-001")
                .location(CNCLocation.SETOR_USINAGEM)
                .build();

        machineEntity = CNCMachineEntity.builder()
                .id(1)
                .displayName("Torno CNC Principal")
                .model(CNCModel.HAAS_ST_10)
                .manufacturer(CNCManufacturer.HAAS)
                .serialNumber("CNC-2026-001")
                .location(CNCLocation.SETOR_USINAGEM)
                .status(CNCStatus.ONLINE)
                .build();
    }

    // CREATE

    @Test
    @DisplayName("Deve cadastrar uma máquina com sucesso")
    void shouldCreateMachineSuccessfully() {

        when(machineRepository.existsBySerialNumber("CNC-2026-001"))
                .thenReturn(false);

        machineService.createCNCMachine(machineRequest);

        ArgumentCaptor<CNCMachineEntity> captor =
                ArgumentCaptor.forClass(CNCMachineEntity.class);

        verify(machineRepository).existsBySerialNumber("CNC-2026-001");
        verify(machineRepository).save(captor.capture());

        assertEquals("Torno CNC Principal",
                captor.getValue().getDisplayName());

        assertEquals("CNC-2026-001",
                captor.getValue().getSerialNumber());
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando o número de série já estiver cadastrado")
    void shouldThrowConflictExceptionWhenSerialNumberAlreadyExists() {

        when(machineRepository.existsBySerialNumber("CNC-2026-001"))
                .thenReturn(true);

        assertThrows(ConflictException.class, () ->
                machineService.createCNCMachine(machineRequest));

        verify(machineRepository).existsBySerialNumber("CNC-2026-001");
        verify(machineRepository, never()).save(any(CNCMachineEntity.class));
    }

    @Test
    @DisplayName("Deve cadastrar uma máquina sem número de série")
    void shouldCreateMachineWithoutSerialNumber() {

        machineRequest.setSerialNumber(null);

        machineService.createCNCMachine(machineRequest);

        ArgumentCaptor<CNCMachineEntity> captor =
                ArgumentCaptor.forClass(CNCMachineEntity.class);

        verify(machineRepository).save(captor.capture());

        assertNull(captor.getValue().getSerialNumber());
        verify(machineRepository, never()).existsBySerialNumber(anyString());
    }

    @Test
    @DisplayName("Deve converter número de série em branco para nulo")
    void shouldConvertBlankSerialNumberToNull() {

        machineRequest.setSerialNumber("  ");

        machineService.createCNCMachine(machineRequest);

        ArgumentCaptor<CNCMachineEntity> captor =
                ArgumentCaptor.forClass(CNCMachineEntity.class);

        verify(machineRepository).save(captor.capture());

        assertNull(captor.getValue().getSerialNumber());
        verify(machineRepository, never()).existsBySerialNumber(anyString());
    }

    // FIND BY ID

    @Test
    @DisplayName("Deve retornar uma máquina quando o ID existir")
    void shouldReturnMachineWhenIdExists() {

        when(machineRepository.findByIdAndDeletedFalse(1))
                .thenReturn(Optional.of(machineEntity));

        when(machineMapper.convertToResponseDTO(machineEntity))
                .thenReturn(machineResponse);

        CNCMachineResponseDTO response =
                machineService.getCNCMachineById(1);

        assertNotNull(response);
        assertSame(machineResponse, response);

        verify(machineRepository).findByIdAndDeletedFalse(1);
        verify(machineMapper).convertToResponseDTO(machineEntity);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando a máquina não existir")
    void shouldThrowNotFoundExceptionWhenIdDoesNotExist() {

        when(machineRepository.findByIdAndDeletedFalse(99))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                machineService.getCNCMachineById(99));

        verify(machineRepository).findByIdAndDeletedFalse(99);
        verifyNoInteractions(machineMapper);
    }

    // FIND ALL

    @Test
    @DisplayName("Deve retornar todas as máquinas ativas")
    void shouldReturnAllActiveMachines() {

        List<CNCMachineEntity> machines = List.of(machineEntity);

        when(machineRepository.findAllByDeletedFalse())
                .thenReturn(machines);

        when(machineMapper.convertToResponseDTO(machineEntity))
                .thenReturn(machineResponse);

        List<CNCMachineResponseDTO> response =
                machineService.getAllCNCMachine();

        assertNotNull(response);
        assertEquals(1, response.size());
        assertSame(machineResponse, response.get(0));

        verify(machineRepository).findAllByDeletedFalse();
        verify(machineMapper).convertToResponseDTO(machineEntity);
    }

    @Test
    @DisplayName("Deve retornar uma lista vazia quando não houver máquinas ativas")
    void shouldReturnEmptyListWhenNoActiveMachinesExist() {

        when(machineRepository.findAllByDeletedFalse())
                .thenReturn(List.of());

        List<CNCMachineResponseDTO> response =
                machineService.getAllCNCMachine();

        assertNotNull(response);
        assertTrue(response.isEmpty());

        verify(machineRepository).findAllByDeletedFalse();
        verifyNoInteractions(machineMapper);
    }

    // FIND BY SERIAL NUMBER

    @Test
    @DisplayName("Deve retornar uma máquina pelo número de série")
    void shouldReturnMachineBySerialNumber() {

        when(machineRepository.findBySerialNumberAndDeletedFalse("CNC-2026-001"))
                .thenReturn(Optional.of(machineEntity));

        when(machineMapper.convertToResponseDTO(machineEntity))
                .thenReturn(machineResponse);

        CNCMachineResponseDTO response =
                machineService.getCNCMachineBySerialNumber("CNC-2026-001");

        assertNotNull(response);
        assertSame(machineResponse, response);

        verify(machineRepository)
                .findBySerialNumberAndDeletedFalse("CNC-2026-001");

        verify(machineMapper).convertToResponseDTO(machineEntity);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando o número de série não for encontrado")
    void shouldThrowNotFoundExceptionWhenSerialNumberDoesNotExist() {

        when(machineRepository.findBySerialNumberAndDeletedFalse("INVALIDO"))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                machineService.getCNCMachineBySerialNumber("INVALIDO"));

        verify(machineRepository)
                .findBySerialNumberAndDeletedFalse("INVALIDO");

        verifyNoInteractions(machineMapper);
    }

    // SEARCH

    @Test
    @DisplayName("Deve pesquisar máquinas sem filtros")
    void shouldSearchMachinesWithoutFilters() {

        when(machineRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(machineEntity));

        when(machineMapper.convertToResponseDTO(machineEntity))
                .thenReturn(machineResponse);

        List<CNCMachineResponseDTO> response =
                machineService.searchCNCMachines(null, null, null, null);

        assertEquals(1, response.size());

        verify(machineRepository).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Deve filtrar máquinas por status")
    void shouldFilterMachinesByStatus() {

        when(machineRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(machineEntity));

        when(machineMapper.convertToResponseDTO(machineEntity))
                .thenReturn(machineResponse);

        List<CNCMachineResponseDTO> response =
                machineService.searchCNCMachines(
                        CNCStatus.ONLINE, null, null, null);

        assertEquals(1, response.size());

        verify(machineRepository).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Deve filtrar máquinas por localização")
    void shouldFilterMachinesByLocation() {

        when(machineRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(machineEntity));

        when(machineMapper.convertToResponseDTO(machineEntity))
                .thenReturn(machineResponse);

        List<CNCMachineResponseDTO> response =
                machineService.searchCNCMachines(
                        null, CNCLocation.SETOR_USINAGEM, null, null);

        assertEquals(1, response.size());

        verify(machineRepository).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Deve filtrar máquinas por modelo")
    void shouldFilterMachinesByModel() {

        when(machineRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(machineEntity));

        when(machineMapper.convertToResponseDTO(machineEntity))
                .thenReturn(machineResponse);

        List<CNCMachineResponseDTO> response =
                machineService.searchCNCMachines(
                        null, null, CNCModel.HAAS_ST_10, null);

        assertEquals(1, response.size());

        verify(machineRepository).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Deve filtrar máquinas por fabricante")
    void shouldFilterMachinesByManufacturer() {

        when(machineRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(machineEntity));

        when(machineMapper.convertToResponseDTO(machineEntity))
                .thenReturn(machineResponse);

        List<CNCMachineResponseDTO> response =
                machineService.searchCNCMachines(
                        null, null, null, CNCManufacturer.HAAS);

        assertEquals(1, response.size());

        verify(machineRepository).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Deve combinar múltiplos filtros na pesquisa")
    void shouldCombineMultipleSearchFilters() {

        when(machineRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(machineEntity));

        when(machineMapper.convertToResponseDTO(machineEntity))
                .thenReturn(machineResponse);

        List<CNCMachineResponseDTO> response =
                machineService.searchCNCMachines(
                        CNCStatus.ONLINE,
                        CNCLocation.SETOR_USINAGEM,
                        CNCModel.HAAS_ST_10,
                        CNCManufacturer.HAAS);

        assertEquals(1, response.size());

        verify(machineRepository).findAll(any(Specification.class));
    }

    // UPDATE MACHINE

    @Test
    @DisplayName("Deve atualizar uma máquina com sucesso")
    void shouldUpdateMachineSuccessfully() {

        when(machineRepository.findByIdAndDeletedFalse(1))
                .thenReturn(Optional.of(machineEntity));

        when(machineRepository.existsBySerialNumberAndIdNot("CNC-2026-001", 1))
                .thenReturn(false);

        when(machineRepository.save(machineEntity))
                .thenReturn(machineEntity);

        when(machineMapper.convertToResponseDTO(machineEntity))
                .thenReturn(machineResponse);

        CNCMachineResponseDTO response =
                machineService.updateCNCMachine(1, machineRequest);

        assertNotNull(response);
        assertSame(machineResponse, response);

        verify(machineMapper).updateEntity(machineEntity, machineRequest);
        verify(machineRepository).save(machineEntity);
        verify(machineMapper).convertToResponseDTO(machineEntity);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando a máquina não existir durante a atualização")
    void shouldThrowNotFoundExceptionWhenUpdatingNonExistingMachine() {

        when(machineRepository.findByIdAndDeletedFalse(99))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                machineService.updateCNCMachine(99, machineRequest));

        verify(machineRepository).findByIdAndDeletedFalse(99);
        verify(machineRepository, never()).save(any(CNCMachineEntity.class));
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando o número de série pertencer a outra máquina")
    void shouldThrowConflictExceptionWhenUpdatingWithExistingSerialNumber() {

        when(machineRepository.findByIdAndDeletedFalse(1))
                .thenReturn(Optional.of(machineEntity));

        when(machineRepository.existsBySerialNumberAndIdNot("CNC-2026-001", 1))
                .thenReturn(true);

        assertThrows(ConflictException.class, () ->
                machineService.updateCNCMachine(1, machineRequest));

        verify(machineRepository)
                .existsBySerialNumberAndIdNot("CNC-2026-001", 1);

        verify(machineRepository, never()).save(any(CNCMachineEntity.class));
    }

    @Test
    @DisplayName("Deve converter número de série em branco para nulo durante a atualização")
    void shouldConvertBlankSerialNumberToNullWhenUpdating() {

        machineRequest.setSerialNumber("  ");

        when(machineRepository.findByIdAndDeletedFalse(1))
                .thenReturn(Optional.of(machineEntity));

        when(machineRepository.save(machineEntity))
                .thenReturn(machineEntity);

        when(machineMapper.convertToResponseDTO(machineEntity))
                .thenReturn(machineResponse);

        machineService.updateCNCMachine(1, machineRequest);

        verify(machineRepository, never())
                .existsBySerialNumberAndIdNot(anyString(), anyInt());

        verify(machineMapper).updateEntity(machineEntity, machineRequest);
        verify(machineRepository).save(machineEntity);
    }

    // UPDATE STATUS

    @Test
    @DisplayName("Deve atualizar o status da máquina")
    void shouldUpdateMachineStatus() {

        when(machineRepository.findByIdAndDeletedFalse(1))
                .thenReturn(Optional.of(machineEntity));

        when(machineRepository.save(machineEntity))
                .thenReturn(machineEntity);

        when(machineMapper.convertToResponseDTO(machineEntity))
                .thenReturn(machineResponse);

        CNCMachineResponseDTO response =
                machineService.updateStatusMachine(1, CNCStatus.OFFLINE);

        assertNotNull(response);
        assertSame(machineResponse, response);

        assertEquals(CNCStatus.OFFLINE, machineEntity.getStatus());

        verify(machineRepository).save(machineEntity);
        verify(machineMapper).convertToResponseDTO(machineEntity);
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o status for nulo")
    void shouldThrowBadRequestExceptionWhenStatusIsNull() {

        when(machineRepository.findByIdAndDeletedFalse(1))
                .thenReturn(Optional.of(machineEntity));

        assertThrows(BadRequestException.class, () ->
                machineService.updateStatusMachine(1, null));

        verify(machineRepository, never()).save(any(CNCMachineEntity.class));
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando a máquina não existir durante a atualização do status")
    void shouldThrowNotFoundExceptionWhenUpdatingStatusOfNonExistingMachine() {

        when(machineRepository.findByIdAndDeletedFalse(99))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                machineService.updateStatusMachine(99, CNCStatus.ONLINE));

        verify(machineRepository).findByIdAndDeletedFalse(99);
        verify(machineRepository, never()).save(any(CNCMachineEntity.class));
    }

    // DEACTIVATE

    @Test
    @DisplayName("Deve desativar uma máquina com sucesso")
    void shouldDeactivateMachineSuccessfully() {

        when(machineRepository.findById(1))
                .thenReturn(Optional.of(machineEntity));

        machineService.deactivateCNCMachine(1);

        assertTrue(machineEntity.isDeleted());

        verify(machineRepository).findById(1);
        verify(machineRepository).save(machineEntity);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando a máquina não existir durante a desativação")
    void shouldThrowNotFoundExceptionWhenDeactivatingNonExistingMachine() {

        when(machineRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                machineService.deactivateCNCMachine(99));

        verify(machineRepository).findById(99);
        verify(machineRepository, never()).save(any(CNCMachineEntity.class));
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando a máquina já estiver desativada")
    void shouldThrowConflictExceptionWhenMachineIsAlreadyDeactivated() {

        machineEntity.setDeleted(true);

        when(machineRepository.findById(1))
                .thenReturn(Optional.of(machineEntity));

        assertThrows(ConflictException.class, () ->
                machineService.deactivateCNCMachine(1));

        verify(machineRepository).findById(1);
        verify(machineRepository, never()).save(any(CNCMachineEntity.class));
    }

    // REACTIVATE

    @Test
    @DisplayName("Deve reativar uma máquina com sucesso")
    void shouldReactivateMachineSuccessfully() {

        machineEntity.setDeleted(true);

        when(machineRepository.findById(1))
                .thenReturn(Optional.of(machineEntity));

        machineService.reactivateCNCMachine(1);

        assertFalse(machineEntity.isDeleted());

        verify(machineRepository).findById(1);
        verify(machineRepository).save(machineEntity);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando a máquina não existir durante a reativação")
    void shouldThrowNotFoundExceptionWhenReactivatingNonExistingMachine() {

        when(machineRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                machineService.reactivateCNCMachine(99));

        verify(machineRepository).findById(99);
        verify(machineRepository, never()).save(any(CNCMachineEntity.class));
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando a máquina já estiver ativa")
    void shouldThrowConflictExceptionWhenMachineIsAlreadyActive() {

        machineEntity.setDeleted(false);

        when(machineRepository.findById(1))
                .thenReturn(Optional.of(machineEntity));

        assertThrows(ConflictException.class, () ->
                machineService.reactivateCNCMachine(1));

        verify(machineRepository).findById(1);
        verify(machineRepository, never()).save(any(CNCMachineEntity.class));
    }

    // DELETE MACHINE BY ID

    @Test
    @DisplayName("Deve excluir permanentemente uma máquina CNC por ID")
    void shouldDeleteMachineByIdSuccessfully() {

        // Arrange

        when(machineRepository.findById(1))
                .thenReturn(Optional.of(machineEntity));

        // Act

        machineService.deleteCNCMachinebyId(1);

        // Assert

        verify(machineRepository).findById(1);
        verify(machineRepository).delete(machineEntity);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando a máquina não existir durante a exclusão por ID")
    void shouldThrowNotFoundExceptionWhenDeletingNonExistingMachineById() {

        // Arrange

        when(machineRepository.findById(99))
                .thenReturn(Optional.empty());

        // Act & Assert

        assertThrows(NotFoundException.class, () ->
                machineService.deleteCNCMachinebyId(99));

        verify(machineRepository).findById(99);
        verify(machineRepository, never()).delete(any(CNCMachineEntity.class));
    }

// DELETE MACHINE BY SERIAL NUMBER

    @Test
    @DisplayName("Deve excluir permanentemente uma máquina CNC pelo número de série")
    void shouldDeleteMachineBySerialNumberSuccessfully() {

        // Arrange

        when(machineRepository.findBySerialNumber("CNC-2026-001"))
                .thenReturn(Optional.of(machineEntity));

        // Act

        machineService.deleteCNCMachineSerial("CNC-2026-001");

        // Assert

        verify(machineRepository).findBySerialNumber("CNC-2026-001");
        verify(machineRepository).delete(machineEntity);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando a máquina não existir durante a exclusão por número de série")
    void shouldThrowNotFoundExceptionWhenDeletingNonExistingMachineBySerialNumber() {

        // Arrange

        when(machineRepository.findBySerialNumber("INVALIDO"))
                .thenReturn(Optional.empty());

        // Act & Assert

        assertThrows(NotFoundException.class, () ->
                machineService.deleteCNCMachineSerial("INVALIDO"));

        verify(machineRepository).findBySerialNumber("INVALIDO");
        verify(machineRepository, never()).delete(any(CNCMachineEntity.class));
    }
}
