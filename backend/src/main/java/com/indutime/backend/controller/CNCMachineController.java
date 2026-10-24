package com.indutime.backend.controller;


import com.indutime.backend.dto.CNCMachineRequestDTO;
import com.indutime.backend.service.CNCMachineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Máquinas CNC",
        description = "Endpoints para gerenciamento de máquinas CNC do InduTime"
)
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/v1/cncmachine")
public class CNCMachineController {

    private final CNCMachineService machineService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Cadastrar máquina CNC",
            description = "Cadastra uma nova máquina CNC no sistema."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Máquina CNC cadastrada com sucesso"
    )
    public void createMachine(@Validated @RequestBody CNCMachineRequestDTO requestDTO) {
        machineService.createCNCMachine(requestDTO);
    }
}
