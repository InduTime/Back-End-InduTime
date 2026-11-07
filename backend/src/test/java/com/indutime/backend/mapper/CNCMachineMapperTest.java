package com.indutime.backend.utils;

import com.indutime.backend.database.model.CNCMachineEntity;
import com.indutime.backend.dto.CNCMachineRequestDTO;
import com.indutime.backend.dto.CNCMachineResponseDTO;
import com.indutime.backend.enums.CNCLocation;
import com.indutime.backend.enums.CNCManufacturer;
import com.indutime.backend.enums.CNCModel;
import com.indutime.backend.enums.CNCStatus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes do CNCMachineMapper")
class CNCMachineMapperTest {

    private final CNCMachineMapper mapper = new CNCMachineMapper();

    @Test
    @DisplayName("Deve converter todos os campos da entidade para ResponseDTO")
    void shouldMapAllEntityFieldsToResponse() {

        // Arrange
        CNCMachineEntity machineEntity = CNCMachineEntity.builder()
                .id(1)
                .displayName("Torno CNC Principal")
                .model(CNCModel.HAAS_ST_10)
                .manufacturer(CNCManufacturer.HAAS)
                .serialNumber("CNC-2026-001")
                .location(CNCLocation.SETOR_USINAGEM)
                .status(CNCStatus.ONLINE)
                .build();

        // Act
        CNCMachineResponseDTO response =
                mapper.convertToResponseDTO(machineEntity);

        // Assert
        assertNotNull(response);

        assertAll(
                () -> assertEquals(machineEntity.getId(), response.getId()),
                () -> assertEquals(machineEntity.getDisplayName(), response.getDisplayName()),
                () -> assertEquals(machineEntity.getModel(), response.getModel()),
                () -> assertEquals(machineEntity.getManufacturer(), response.getManufacturer()),
                () -> assertEquals(machineEntity.getSerialNumber(), response.getSerialNumber()),
                () -> assertEquals(machineEntity.getLocation(), response.getLocation()),
                () -> assertEquals(machineEntity.getStatus(), response.getStatus())
        );
    }

    @Test
    @DisplayName("Deve converter entidade para ResponseDTO quando o número de série for nulo")
    void shouldMapEntityWithNullSerialNumber() {

        // Arrange
        CNCMachineEntity machineEntity = CNCMachineEntity.builder()
                .id(2)
                .displayName("Torno CNC Secundário")
                .model(CNCModel.DMG_MORI_CLX_350)
                .manufacturer(CNCManufacturer.DMG_MORI)
                .serialNumber(null)
                .location(CNCLocation.SETOR_MANUTENCAO)
                .status(CNCStatus.OFFLINE)
                .build();

        // Act
        CNCMachineResponseDTO response =
                mapper.convertToResponseDTO(machineEntity);

        // Assert
        assertNotNull(response);

        assertAll(
                () -> assertEquals(machineEntity.getId(), response.getId()),
                () -> assertEquals(machineEntity.getDisplayName(), response.getDisplayName()),
                () -> assertEquals(machineEntity.getModel(), response.getModel()),
                () -> assertEquals(machineEntity.getManufacturer(), response.getManufacturer()),
                () -> assertNull(response.getSerialNumber()),
                () -> assertEquals(machineEntity.getLocation(), response.getLocation()),
                () -> assertEquals(machineEntity.getStatus(), response.getStatus())
        );
    }

    @Test
    @DisplayName("Deve atualizar todos os campos permitidos da máquina")
    void shouldUpdateAllAllowedFields() {

        // Arrange
        CNCMachineEntity machineEntity = CNCMachineEntity.builder()
                .id(1)
                .displayName("Torno Antigo")
                .model(CNCModel.HAAS_ST_10)
                .manufacturer(CNCManufacturer.HAAS)
                .serialNumber("CNC-ANTIGO")
                .location(CNCLocation.SETOR_USINAGEM)
                .status(CNCStatus.ONLINE)
                .deleted(true)
                .build();

        CNCMachineRequestDTO requestDTO = new CNCMachineRequestDTO();

        requestDTO.setDisplayName("Torno Atualizado");
        requestDTO.setModel(CNCModel.DMG_MORI_CLX_350);
        requestDTO.setManufacturer(CNCManufacturer.DMG_MORI);
        requestDTO.setSerialNumber("CNC-2026-002");
        requestDTO.setLocation(CNCLocation.SETOR_MANUTENCAO);

        // Act
        mapper.updateEntity(machineEntity, requestDTO);

        // Assert
        assertAll(
                // Verify fields that should be updated
                () -> assertEquals("Torno Atualizado", machineEntity.getDisplayName()),
                () -> assertEquals(CNCModel.DMG_MORI_CLX_350, machineEntity.getModel()),
                () -> assertEquals(CNCManufacturer.DMG_MORI, machineEntity.getManufacturer()),
                () -> assertEquals("CNC-2026-002", machineEntity.getSerialNumber()),
                () -> assertEquals(CNCLocation.SETOR_MANUTENCAO, machineEntity.getLocation()),

                // Verify fields that should remain unchanged
                () -> assertEquals(1, machineEntity.getId()),
                () -> assertEquals(CNCStatus.ONLINE, machineEntity.getStatus()),
                () -> assertTrue(machineEntity.isDeleted())
        );
    }
}