package com.indutime.backend.dto;

import com.indutime.backend.enums.CNCLocation;
import com.indutime.backend.enums.CNCManufacturer;
import com.indutime.backend.enums.CNCModel;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CNCMachineRequestDTO {

    private String displayName;
    private CNCModel model;
    private CNCManufacturer manufacturer;
    private String serialNumber;
    private CNCLocation location;
}
