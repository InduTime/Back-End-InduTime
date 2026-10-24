package com.indutime.backend.dto;

import com.indutime.backend.enums.CNCLocation;
import com.indutime.backend.enums.CNCManufacturer;
import com.indutime.backend.enums.CNCModel;
import com.indutime.backend.enums.CNCStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CNCMachineResponseDTO {

    private Integer id;
    private String displayName;
    private CNCModel model;
    private CNCManufacturer manufacturer;
    private String serialNumber;
    private CNCLocation location;
    private CNCStatus status;
}