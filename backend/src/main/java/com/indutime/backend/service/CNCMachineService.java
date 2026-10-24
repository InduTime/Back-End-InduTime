package com.indutime.backend.service;


import com.indutime.backend.database.model.CNCMachineEntity;
import com.indutime.backend.database.repository.ICNCMachineRepository;
import com.indutime.backend.dto.CNCMachineRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CNCMachineService {

    private final ICNCMachineRepository machineRepository;

    public void createCNCMachine(CNCMachineRequestDTO machineRequestDTO) {
        CNCMachineEntity machineEntity = CNCMachineEntity.builder()
                .displayName(machineRequestDTO.getDisplayName())
                .model(machineRequestDTO.getModel())
                .manufacturer(machineRequestDTO.getManufacturer())
                .serialNumber(machineRequestDTO.getSerialNumber())
                .location(machineRequestDTO.getLocation())
                .build();

        machineRepository.save(machineEntity);
    }
}
