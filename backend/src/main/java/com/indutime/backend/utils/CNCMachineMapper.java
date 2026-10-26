package com.indutime.backend.utils;

import com.indutime.backend.database.model.CNCMachineEntity;
import com.indutime.backend.dto.CNCMachineRequestDTO;
import com.indutime.backend.dto.CNCMachineResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class CNCMachineMapper {

    public CNCMachineResponseDTO convertToResponseDTO(CNCMachineEntity machineEntity) {
        return CNCMachineResponseDTO.builder()
                .id(machineEntity.getId())
                .displayName(machineEntity.getDisplayName())
                .model(machineEntity.getModel())
                .manufacturer(machineEntity.getManufacturer())
                .serialNumber(machineEntity.getSerialNumber())
                .location(machineEntity.getLocation())
                .status(machineEntity.getStatus())
                .build();
    }

    public void updateEntity(
            CNCMachineEntity machineEntity,
            CNCMachineRequestDTO machineRequest) {

        machineEntity.setDisplayName(machineRequest.getDisplayName());
        machineEntity.setModel(machineRequest.getModel());
        machineEntity.setManufacturer(machineRequest.getManufacturer());
        machineEntity.setSerialNumber(machineRequest.getSerialNumber());
        machineEntity.setLocation(machineRequest.getLocation());
    }



}