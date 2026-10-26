package com.indutime.backend.controller;

import com.indutime.backend.dto.CNCMachineRequestDTO;
import com.indutime.backend.dto.CNCMachineResponseDTO;
import com.indutime.backend.enums.CNCLocation;
import com.indutime.backend.enums.CNCManufacturer;
import com.indutime.backend.enums.CNCModel;
import com.indutime.backend.enums.CNCStatus;
import com.indutime.backend.service.CNCMachineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Máquinas CNC",
        description = "Endpoints para gerenciamento de máquinas CNC do InduTime")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/cncmachine")
public class CNCMachineController {

    private final CNCMachineService machineService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Cadastrar máquina CNC",
            description = "Cadastra uma nova máquina CNC no sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Máquina CNC cadastrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados enviados inválidos"),
            @ApiResponse(responseCode = "409", description = "Conflito ao cadastrar a máquina")})
    public void createMachine(
            @Valid @RequestBody CNCMachineRequestDTO requestDTO) {
        machineService.createCNCMachine(requestDTO);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar máquina CNC por ID",
            description = "Busca uma máquina CNC pelo seu identificador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Máquina encontrada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Máquina não encontrada")})
    public CNCMachineResponseDTO getCNCMachineById(
            @Parameter(description = "Identificador da máquina CNC", example = "1")
            @PathVariable Integer id) {
        return machineService.getCNCMachineById(id);
    }

    @GetMapping
    @Operation(
            summary = "Listar máquinas CNC",
            description = "Retorna todas as máquinas CNC ativas cadastradas no sistema.")
    @ApiResponse(
            responseCode = "200",
            description = "Lista de máquinas CNC retornada com sucesso")
    public List<CNCMachineResponseDTO> getAllCNCMachines() {
        return machineService.getAllCNCMachine();
    }

    @GetMapping("/serial/{serialNumber}")
    @Operation(
            summary = "Buscar máquina por número de série",
            description = "Localiza uma máquina CNC pelo seu número de série.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Máquina encontrada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Máquina não encontrada")})
    public CNCMachineResponseDTO getCNCMachineBySerialNumber(
            @Parameter(description = "Número de série da máquina CNC", example = "CNC-001")
            @PathVariable String serialNumber) {
        return machineService.getCNCMachineBySerialNumber(serialNumber);
    }

    @GetMapping("/search")
    @Operation(
            summary = "Pesquisar máquinas CNC",
            description = "Pesquisa máquinas CNC utilizando filtros opcionais de status, localização, modelo e fabricante.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pesquisa realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Um ou mais parâmetros de pesquisa são inválidos")})
    public List<CNCMachineResponseDTO> searchCNCMachines(
            @Parameter(description = "Status da máquina CNC")
            @RequestParam(required = false) CNCStatus status,

            @Parameter(description = "Localização da máquina CNC")
            @RequestParam(required = false) CNCLocation location,

            @Parameter(description = "Modelo da máquina CNC")
            @RequestParam(required = false) CNCModel model,

            @Parameter(description = "Fabricante da máquina CNC")
            @RequestParam(required = false) CNCManufacturer manufacturer) {
        return machineService.searchCNCMachines(
                status, location, model, manufacturer
        );
    }

    @PatchMapping("/{machineId}/status")
    @Operation(
            summary = "Atualizar status da máquina CNC",
            description = "Altera o status de uma máquina CNC identificada pelo seu ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Status informado inválido"),
            @ApiResponse(responseCode = "404", description = "Máquina não encontrada")})
    public CNCMachineResponseDTO updateStatusMachine(
            @Parameter(description = "Identificador da máquina CNC", example = "1")
            @PathVariable Integer machineId,

            @Parameter(description = "Novo status da máquina CNC")
            @RequestParam CNCStatus status) {
        return machineService.updateStatusMachine(machineId, status);
    }

    @PutMapping("/{machineId}")
    @Operation(
            summary = "Atualizar dados da máquina CNC",
            description = "Atualiza os dados de uma máquina CNC existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Máquina atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados enviados inválidos"),
            @ApiResponse(responseCode = "404", description = "Máquina não encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflito ao atualizar a máquina")})
    public CNCMachineResponseDTO updateCNCMachine(
            @Parameter(description = "Identificador da máquina CNC", example = "1")
            @PathVariable Integer machineId,

            @Valid @RequestBody CNCMachineRequestDTO machineRequest) {
        return machineService.updateCNCMachine(machineId, machineRequest);
    }

    @DeleteMapping("/{machineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Desativar máquina CNC",
            description = "Realiza a desativação lógica de uma máquina CNC.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Máquina desativada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Máquina não encontrada")})
    public void deleteCNCMachine(
            @Parameter(description = "Identificador da máquina CNC", example = "1")
            @PathVariable Integer machineId) {
        machineService.deactivateCNCMachine(machineId);
    }

    @PatchMapping("/{machineId}/reactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Reativar máquina CNC",
            description = "Reativa uma máquina CNC previamente desativada.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Máquina reativada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Máquina não encontrada")})
    public void reactivateCNCMachine(
            @Parameter(description = "Identificador da máquina CNC", example = "1")
            @PathVariable Integer machineId) {
        machineService.reactivateCNCMachine(machineId);
    }
}