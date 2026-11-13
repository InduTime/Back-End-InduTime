package com.indutime.backend.dto;

import com.indutime.backend.enums.CNCLocation;
import com.indutime.backend.enums.CNCManufacturer;
import com.indutime.backend.enums.CNCModel;
import com.indutime.backend.enums.CNCStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CNCMachineResponseDTO {

    // ID interno gerado pelo PostgreSQL.
    private Integer id;

    // ID utilizado pela Raspberry para identificar o torno.
    private Integer raspberryMachineId;

    @NotBlank(message = "O nome fantasia é obrigatório")
    private String displayName;

    private CNCModel model;

    private CNCManufacturer manufacturer;

    @NotBlank(message = "O número de série é obrigatório")
    private String serialNumber;

    private CNCLocation location;

    private CNCStatus status;

    private BigDecimal temperature;
}
