
package com.indutime.backend.controller;

import tools.jackson.databind.ObjectMapper;

import com.indutime.backend.dto.CNCMachineRequestDTO;
import com.indutime.backend.dto.CNCMachineResponseDTO;
import com.indutime.backend.enums.CNCLocation;
import com.indutime.backend.enums.CNCManufacturer;
import com.indutime.backend.enums.CNCModel;
import com.indutime.backend.enums.CNCStatus;
import com.indutime.backend.service.CNCMachineService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CNCMachineController.class)
@DisplayName("Testes do CNCMachineController")
class CNCMachineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CNCMachineService machineService;

    private CNCMachineRequestDTO createRequest() {

        return CNCMachineRequestDTO.builder()
                .displayName("Torno CNC Principal")
                .model(CNCModel.HAAS_ST_10)
                .manufacturer(CNCManufacturer.HAAS)
                .serialNumber("CNC-2026-001")
                .location(CNCLocation.SETOR_USINAGEM)
                .build();
    }

    private CNCMachineResponseDTO createResponse() {

        CNCMachineResponseDTO response = new CNCMachineResponseDTO();

        response.setId(1);
        response.setDisplayName("Torno CNC Principal");
        response.setModel(CNCModel.HAAS_ST_10);
        response.setManufacturer(CNCManufacturer.HAAS);
        response.setSerialNumber("CNC-2026-001");
        response.setLocation(CNCLocation.SETOR_USINAGEM);
        response.setStatus(CNCStatus.ONLINE);

        return response;
    }

    // CREATE MACHINE

    @Test
    @DisplayName("Deve cadastrar uma máquina CNC com sucesso")
    void shouldCreateMachineSuccessfully() throws Exception {

        // Arrange

        CNCMachineRequestDTO request = createRequest();

        // Act & Assert

        mockMvc.perform(post("/v1/cncmachine")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(machineService).createCNCMachine(any(CNCMachineRequestDTO.class));
    }

    // FIND BY ID

    @Test
    @DisplayName("Deve buscar uma máquina CNC por ID")
    void shouldGetMachineByIdSuccessfully() throws Exception {

        // Arrange

        when(machineService.getCNCMachineById(1))
                .thenReturn(createResponse());

        // Act & Assert

        mockMvc.perform(get("/v1/cncmachine/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.displayName").value("Torno CNC Principal"))
                .andExpect(jsonPath("$.serialNumber").value("CNC-2026-001"));

        verify(machineService).getCNCMachineById(1);
    }

    // FIND ALL

    @Test
    @DisplayName("Deve listar todas as máquinas CNC")
    void shouldGetAllMachinesSuccessfully() throws Exception {

        // Arrange

        when(machineService.getAllCNCMachine())
                .thenReturn(List.of(createResponse()));

        // Act & Assert

        mockMvc.perform(get("/v1/cncmachine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1));

        verify(machineService).getAllCNCMachine();
    }

    // FIND BY SERIAL NUMBER

    @Test
    @DisplayName("Deve buscar uma máquina pelo número de série")
    void shouldGetMachineBySerialNumberSuccessfully() throws Exception {

        // Arrange

        when(machineService.getCNCMachineBySerialNumber("CNC-2026-001"))
                .thenReturn(createResponse());

        // Act & Assert

        mockMvc.perform(get("/v1/cncmachine/serial/CNC-2026-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serialNumber").value("CNC-2026-001"));

        verify(machineService).getCNCMachineBySerialNumber("CNC-2026-001");
    }

    // SEARCH MACHINES

    @Test
    @DisplayName("Deve pesquisar máquinas utilizando filtros")
    void shouldSearchMachinesSuccessfully() throws Exception {

        // Arrange

        when(machineService.searchCNCMachines(
                CNCStatus.ONLINE,
                CNCLocation.SETOR_USINAGEM,
                CNCModel.HAAS_ST_10,
                CNCManufacturer.HAAS
        )).thenReturn(List.of(createResponse()));

        // Act & Assert

        mockMvc.perform(get("/v1/cncmachine/search")
                        .param("status", "ONLINE")
                        .param("location", "SETOR_USINAGEM")
                        .param("model", "HAAS_ST_10")
                        .param("manufacturer", "HAAS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(machineService).searchCNCMachines(
                CNCStatus.ONLINE,
                CNCLocation.SETOR_USINAGEM,
                CNCModel.HAAS_ST_10,
                CNCManufacturer.HAAS
        );
    }

    // UPDATE STATUS

    @Test
    @DisplayName("Deve atualizar o status de uma máquina CNC")
    void shouldUpdateMachineStatusSuccessfully() throws Exception {

        // Arrange

        when(machineService.updateStatusMachine(1, CNCStatus.MANUTENCAO))
                .thenReturn(createResponse());

        // Act & Assert

        mockMvc.perform(patch("/v1/cncmachine/1/status")
                        .param("status", "MANUTENCAO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(machineService).updateStatusMachine(1, CNCStatus.MANUTENCAO);
    }

    // UPDATE MACHINE

    @Test
    @DisplayName("Deve atualizar os dados de uma máquina CNC")
    void shouldUpdateMachineSuccessfully() throws Exception {

        // Arrange

        CNCMachineRequestDTO request = createRequest();

        when(machineService.updateCNCMachine(eq(1), any(CNCMachineRequestDTO.class)))
                .thenReturn(createResponse());

        // Act & Assert

        mockMvc.perform(put("/v1/cncmachine/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(machineService).updateCNCMachine(
                eq(1),
                any(CNCMachineRequestDTO.class)
        );
    }

    // DEACTIVATE MACHINE

    @Test
    @DisplayName("Deve desativar uma máquina CNC")
    void shouldDeactivateMachineSuccessfully() throws Exception {

        // Act & Assert

        mockMvc.perform(delete("/v1/cncmachine/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(machineService).deactivateCNCMachine(1);
    }

    // REACTIVATE MACHINE

    @Test
    @DisplayName("Deve reativar uma máquina CNC")
    void shouldReactivateMachineSuccessfully() throws Exception {

        // Act & Assert

        mockMvc.perform(patch("/v1/cncmachine/1/reactivate"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(machineService).reactivateCNCMachine(1);
    }
}
