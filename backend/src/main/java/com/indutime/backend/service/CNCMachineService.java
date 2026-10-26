package com.indutime.backend.service;


import com.indutime.backend.database.model.CNCMachineEntity;
import com.indutime.backend.database.repository.ICNCMachineRepository;
import com.indutime.backend.dto.CNCMachineRequestDTO;
import com.indutime.backend.dto.CNCMachineResponseDTO;
import com.indutime.backend.exception.BadRequestException;
import com.indutime.backend.exception.ConflictException;
import com.indutime.backend.exception.NotFoundException;
import com.indutime.backend.utils.CNCMachineMapper;
import com.indutime.backend.database.repository.CNCMachineSpecification;
import com.indutime.backend.enums.CNCLocation;
import com.indutime.backend.enums.CNCManufacturer;
import com.indutime.backend.enums.CNCModel;
import com.indutime.backend.enums.CNCStatus;
import org.springframework.data.jpa.domain.Specification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CNCMachineService {

    private final ICNCMachineRepository machineRepository;
    private final CNCMachineMapper convertToResponse;

    public void createCNCMachine(CNCMachineRequestDTO machineRequestDTO) {

        String serialNumber = machineRequestDTO.getSerialNumber();

        if (serialNumber != null && serialNumber.isBlank()) {
            serialNumber = null;
        }

        if (serialNumber != null
                && machineRepository.existsBySerialNumber(serialNumber)) {
            throw new ConflictException(
                    "Número de série já existente: " + serialNumber
            );
        }

        CNCMachineEntity machineEntity = CNCMachineEntity.builder()
                .displayName(machineRequestDTO.getDisplayName())
                .model(machineRequestDTO.getModel())
                .manufacturer(machineRequestDTO.getManufacturer())
                .serialNumber(serialNumber)
                .location(machineRequestDTO.getLocation())
                .build();

        machineRepository.save(machineEntity);
    }

    public CNCMachineResponseDTO getCNCMachineById(Integer machineId)  {
        CNCMachineEntity machineEntity = findActiveMachineById(machineId);
        return convertToResponse.convertToResponseDTO(machineEntity);
    }
    public List<CNCMachineResponseDTO> getAllCNCMachine() {

        List<CNCMachineEntity> machineEntityList = machineRepository.findAllByDeletedFalse();

        List<CNCMachineResponseDTO> machineResponseList = new ArrayList<>();

        for(CNCMachineEntity machineEntity : machineEntityList) {

            machineResponseList.add(convertToResponse.convertToResponseDTO(machineEntity));
        }

        return machineResponseList;

    }

    public CNCMachineResponseDTO getCNCMachineBySerialNumber(String serialNumber) {

        CNCMachineEntity machineEntity = machineRepository.findBySerialNumberAndDeletedFalse(serialNumber)
                .orElseThrow(() -> new NotFoundException("Máquina não encontrada para o número de série: " + serialNumber));

        return convertToResponse.convertToResponseDTO(machineEntity);


    }

    public List<CNCMachineResponseDTO> searchCNCMachines(
            CNCStatus status,
            CNCLocation location,
            CNCModel model,
            CNCManufacturer manufacturer) {

        Specification<CNCMachineEntity> spec =
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.isFalse(root.get("deleted"));

            if (status != null) {
                spec = spec.and(CNCMachineSpecification.hasStatus(status));
            }

            if (location != null) {
                spec = spec.and(CNCMachineSpecification.hasLocation(location));
            }

            if (model != null) {
                spec = spec.and(CNCMachineSpecification.hasModel(model));
            }

            if (manufacturer != null) {
                spec = spec.and(CNCMachineSpecification.hasManufacturer(manufacturer));
            }

            List<CNCMachineEntity> machineList = machineRepository.findAll(spec);

            List<CNCMachineResponseDTO> machineResponseList = new ArrayList<>();

            for(CNCMachineEntity machineEntity : machineList) {
                machineResponseList.add(convertToResponse.convertToResponseDTO(machineEntity));
            }

            return machineResponseList;

        }

    public CNCMachineResponseDTO updateStatusMachine(Integer machineId, CNCStatus status) {

        CNCMachineEntity  machineEntity  =  findActiveMachineById(machineId);

        if (status == null) {
            throw new BadRequestException("O status não pode ser nulo.");
        }

        machineEntity.setStatus(status);
        machineRepository.save(machineEntity);
        return convertToResponse.convertToResponseDTO(machineEntity);

    }

    public CNCMachineResponseDTO updateCNCMachine(Integer machineId, CNCMachineRequestDTO  machineRequest) {

        CNCMachineEntity  machineEntity  = findActiveMachineById(machineId);

        if (machineRequest.getSerialNumber() != null
                && machineRequest.getSerialNumber().isBlank()) {
            machineRequest.setSerialNumber(null);
        }

        if (machineRequest.getSerialNumber() != null
            && machineRepository.existsBySerialNumberAndIdNot(
                    machineRequest.getSerialNumber(),machineId)){
            throw new ConflictException("Número de série ja existente: " + machineRequest.getSerialNumber());
        }

        convertToResponse.updateEntity(machineEntity, machineRequest);
        CNCMachineEntity machineUpdate = machineRepository.save(machineEntity);
        return convertToResponse.convertToResponseDTO(machineUpdate);

    }

    public void deactivateCNCMachine(Integer machineId) {
        CNCMachineEntity machineEntity = machineRepository.findById(machineId)
                .orElseThrow(() -> new NotFoundException("Maquina não encontrada: " +  machineId));

        if (machineEntity.isDeleted()) {
            throw new ConflictException("A máquina já está desativada.");
        }

        machineEntity.setDeleted(true);
        machineRepository.save(machineEntity);

    }

    public void reactivateCNCMachine(Integer machineId) {
        CNCMachineEntity machineEntity = machineRepository.findById(machineId)
                .orElseThrow(() -> new NotFoundException("Maquina não encontrada: " +  machineId));

        if (!machineEntity.isDeleted()) {
            throw new ConflictException("A máquina já está ativa.");
        }

        machineEntity.setDeleted(false);
        machineRepository.save(machineEntity);


    }

    private CNCMachineEntity findActiveMachineById(Integer machineId) {
        return machineRepository.findByIdAndDeletedFalse(machineId)
                .orElseThrow(() -> new NotFoundException(
                        "Máquina não encontrada: " + machineId
                ));
    }


}
