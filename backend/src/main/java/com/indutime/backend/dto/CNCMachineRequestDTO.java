package com.indutime.backend.dto;

import com.indutime.backend.enums.CNCLocation;
import com.indutime.backend.enums.CNCManufacturer;
import com.indutime.backend.enums.CNCModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dados necessários para cadastrar uma máquina CNC no InduTime")
public class CNCMachineRequestDTO {

    @Schema(description = "Nome de identificação da máquina", example = "Torno CNC Haas ST-10")
    @NotBlank(message = "O nome fantasia é obrigatório")
    private String displayName;

    @Schema(description = "Modelo da máquina", example = "HAAS_ST_10")
    private CNCModel model;

    @Schema(description = "Fabricante da máquina", example = "HAAS")
    private CNCManufacturer manufacturer;

    @Schema(description = "Número de série único da máquina", example = "HAAS-TEST-001")
    private String serialNumber;

    @Schema(description = "Setor onde a máquina está localizada", example = "SETOR_USINAGEM")
    private CNCLocation location;
}
