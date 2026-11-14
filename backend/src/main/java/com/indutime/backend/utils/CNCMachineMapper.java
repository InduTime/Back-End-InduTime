
package com.indutime.backend.utils;

import com.indutime.backend.database.model.CNCMachineEntity;
import com.indutime.backend.dto.CNCMachineRequestDTO;
import com.indutime.backend.dto.CNCMachineResponseDTO;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;

@Component
public class CNCMachineMapper {

    public CNCMachineResponseDTO convertToResponseDTO(
            CNCMachineEntity machineEntity) {

        return CNCMachineResponseDTO.builder()
                .id(machineEntity.getId())
                .raspberryMachineId(machineEntity.getRaspberryMachineId())
                .displayName(machineEntity.getDisplayName())
                .model(machineEntity.getModel())
                .manufacturer(machineEntity.getManufacturer())
                .serialNumber(machineEntity.getSerialNumber())
                .location(machineEntity.getLocation())
                .temperature(machineEntity.getTemperature())
                .status(machineEntity.getStatus())
                .lastTelemetryAt(machineEntity.getLastTelemetryAt())
                .running(machineEntity.getRunning())
                .operationStartedAt(machineEntity.getOperationStartedAt())
                .totalOperatingSeconds(
                        getTotalOperatingSeconds(machineEntity)
                )
                .operatingTimeSeconds(
                        calculateOperatingTimeSeconds(machineEntity)
                )
                .build();
    }

    private Long getTotalOperatingSeconds(CNCMachineEntity machineEntity) {

        return machineEntity.getTotalOperatingSeconds() == null
                ? 0L
                : machineEntity.getTotalOperatingSeconds();
    }

    private Long calculateOperatingTimeSeconds(
            CNCMachineEntity machineEntity) {

        long totalSeconds = getTotalOperatingSeconds(machineEntity);

        if (!Boolean.TRUE.equals(machineEntity.getRunning())) {
            return totalSeconds;
        }

        OffsetDateTime operationStartedAt =
                machineEntity.getOperationStartedAt();

        if (operationStartedAt == null) {
            return totalSeconds;
        }

        long currentSessionSeconds = Duration.between(
                operationStartedAt,
                OffsetDateTime.now()
        ).getSeconds();

        return totalSeconds + Math.max(currentSessionSeconds, 0L);
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
