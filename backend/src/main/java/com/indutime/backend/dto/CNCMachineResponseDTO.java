
package com.indutime.backend.dto;

import com.indutime.backend.enums.CNCLocation;
import com.indutime.backend.enums.CNCManufacturer;
import com.indutime.backend.enums.CNCModel;
import com.indutime.backend.enums.CNCStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CNCMachineResponseDTO {

    private Integer id;

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

    private OffsetDateTime lastTelemetryAt;

    private Boolean running;

    private OffsetDateTime operationStartedAt;

    private Long totalOperatingSeconds;

    private Long operatingTimeSeconds;
}
