package com.indutime.backend.dto;

import com.indutime.backend.enums.CNCLocation;
import com.indutime.backend.enums.CNCManufacturer;
import com.indutime.backend.enums.CNCModel;
import com.indutime.backend.enums.CNCStatus;

public interface CNCMachineProjection {

    Integer getId();
    String getDisplayName();
    CNCModel getModel();
    CNCManufacturer getManufacturer();
    String getSerialNumber();
    CNCLocation getLocation();
    CNCStatus getStatus();

}