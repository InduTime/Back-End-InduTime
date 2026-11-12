
package com.indutime.backend.service;

import com.indutime.backend.database.model.CNCMachineEntity;
import com.indutime.backend.database.repository.ICNCMachineRepository;
import com.indutime.backend.database.repository.CNCMachineSpecification;
import com.indutime.backend.dto.CNCMachineRequestDTO;
import com.indutime.backend.dto.CNCMachineResponseDTO;
import com.indutime.backend.enums.CNCLocation;
import com.indutime.backend.enums.CNCManufacturer;
import com.indutime.backend.enums.CNCModel;
import com.indutime.backend.enums.CNCStatus;
import com.indutime.backend.exception.BadRequestException;
import com.indutime.backend.exception.ConflictException;
import com.indutime.backend.exception.NotFoundException;
import com.indutime.backend.utils.CNCMachineMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CNCMachineService {

    private static final String CSV_HEADER =
            "id_torno,nome_torno,data_hora,status,temperatura";

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

    public CNCMachineResponseDTO getCNCMachineById(Integer machineId) {

        CNCMachineEntity machineEntity = findActiveMachineById(machineId);

        return convertToResponse.convertToResponseDTO(machineEntity);
    }

    public List<CNCMachineResponseDTO> getAllCNCMachine() {

        List<CNCMachineEntity> machineEntityList =
                machineRepository.findAllByDeletedFalse();

        List<CNCMachineResponseDTO> machineResponseList = new ArrayList<>();

        for (CNCMachineEntity machineEntity : machineEntityList) {
            machineResponseList.add(
                    convertToResponse.convertToResponseDTO(machineEntity)
            );
        }

        return machineResponseList;
    }

    public CNCMachineResponseDTO getCNCMachineBySerialNumber(String serialNumber) {

        CNCMachineEntity machineEntity =
                machineRepository.findBySerialNumberAndDeletedFalse(serialNumber)
                        .orElseThrow(() -> new NotFoundException(
                                "Máquina não encontrada para o número de série: "
                                        + serialNumber
                        ));

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
            spec = spec.and(
                    CNCMachineSpecification.hasManufacturer(manufacturer)
            );
        }

        List<CNCMachineEntity> machineList = machineRepository.findAll(spec);

        List<CNCMachineResponseDTO> machineResponseList = new ArrayList<>();

        for (CNCMachineEntity machineEntity : machineList) {
            machineResponseList.add(
                    convertToResponse.convertToResponseDTO(machineEntity)
            );
        }

        return machineResponseList;
    }

    public CNCMachineResponseDTO updateStatusMachine(
            Integer machineId,
            CNCStatus status) {

        CNCMachineEntity machineEntity = findActiveMachineById(machineId);

        if (status == null) {
            throw new BadRequestException("O status não pode ser nulo.");
        }

        machineEntity.setStatus(status);

        machineRepository.save(machineEntity);

        return convertToResponse.convertToResponseDTO(machineEntity);
    }

    // Atualiza a temperatura recebida pela Raspberry Pi
    public CNCMachineResponseDTO updateMachineTemperature(
            Integer machineId,
            BigDecimal temperature) {

        if (temperature == null) {
            throw new BadRequestException("A temperatura não pode ser nula.");
        }

        CNCMachineEntity machineEntity = findActiveMachineById(machineId);

        machineEntity.setTemperature(temperature);

        CNCMachineEntity machineUpdate =
                machineRepository.save(machineEntity);

        return convertToResponse.convertToResponseDTO(machineUpdate);
    }

    // Importa e processa as leituras do arquivo CSV
    @Transactional
    public int importTelemetryCsv(MultipartFile file) {

        validateCsvFile(file);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        file.getInputStream(),
                        StandardCharsets.UTF_8
                ))) {

            validateCsvHeader(reader.readLine());

            return processCsvRecords(reader);

        } catch (IOException error) {
            throw new BadRequestException(
                    "Não foi possível ler o arquivo CSV."
            );
        }
    }

    // Valida se o arquivo foi enviado e não está vazio
    private void validateCsvFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new BadRequestException(
                    "O arquivo CSV não pode estar vazio."
            );
        }
    }

    // Valida o cabeçalho do arquivo CSV
    private void validateCsvHeader(String header) {

        if (header == null ||
                !header.replace("\uFEFF", "").trim().equals(CSV_HEADER)) {

            throw new BadRequestException(
                    "Cabeçalho do CSV inválido."
            );
        }
    }

    // Percorre e processa os registros do CSV
    private int processCsvRecords(BufferedReader reader) throws IOException {

        String line;
        int lineNumber = 1;
        int updatedReadings = 0;

        while ((line = reader.readLine()) != null) {

            lineNumber++;

            if (line.isBlank()) {
                continue;
            }

            if (processCsvLine(line, lineNumber)) {
                updatedReadings++;
            }
        }

        return updatedReadings;
    }

    // Processa uma linha individual do CSV
    private boolean processCsvLine(String line, int lineNumber) {

        String[] columns = line.split(",", -1);

        if (columns.length != 5) {
            throw new BadRequestException(
                    "Quantidade de colunas inválida na linha " + lineNumber
            );
        }

        Integer machineId = parseMachineId(columns[0], lineNumber);

        String temperatureValue = columns[4].trim();

        // Ignora registros sem temperatura
        if (temperatureValue.isEmpty()) {
            return false;
        }

        BigDecimal temperature = parseTemperature(
                temperatureValue,
                lineNumber
        );

        CNCMachineEntity machineEntity = findActiveMachineById(machineId);

        machineEntity.setTemperature(temperature);

        machineRepository.save(machineEntity);

        return true;
    }

    // Converte o identificador do torno para Integer
    private Integer parseMachineId(String value, int lineNumber) {

        try {
            return Integer.parseInt(value.trim());

        } catch (NumberFormatException error) {
            throw new BadRequestException(
                    "ID do torno inválido na linha " + lineNumber
            );
        }
    }

    // Converte a temperatura para BigDecimal
    private BigDecimal parseTemperature(String value, int lineNumber) {

        try {
            return new BigDecimal(value);

        } catch (NumberFormatException error) {
            throw new BadRequestException(
                    "Temperatura inválida na linha " + lineNumber
            );
        }
    }

    public CNCMachineResponseDTO updateCNCMachine(
            Integer machineId,
            CNCMachineRequestDTO machineRequest) {

        CNCMachineEntity machineEntity = findActiveMachineById(machineId);

        if (machineRequest.getSerialNumber() != null
                && machineRequest.getSerialNumber().isBlank()) {
            machineRequest.setSerialNumber(null);
        }

        if (machineRequest.getSerialNumber() != null
                && machineRepository.existsBySerialNumberAndIdNot(
                machineRequest.getSerialNumber(), machineId)) {

            throw new ConflictException(
                    "Número de série já existente: "
                            + machineRequest.getSerialNumber()
            );
        }

        convertToResponse.updateEntity(machineEntity, machineRequest);

        CNCMachineEntity machineUpdate =
                machineRepository.save(machineEntity);

        return convertToResponse.convertToResponseDTO(machineUpdate);
    }

    public void deactivateCNCMachineSerial(String serialNumber) {

        CNCMachineEntity machineEntity =
                machineRepository.findBySerialNumber(serialNumber)
                        .orElseThrow(() -> new NotFoundException(
                                "Maquina não encontrada: " + serialNumber
                        ));

        if (machineEntity.isDeleted()) {
            throw new ConflictException("A máquina já está desativada.");
        }

        machineEntity.setDeleted(true);
        machineRepository.save(machineEntity);
    }

    public void deactivateCNCMachine(Integer machineId) {

        CNCMachineEntity machineEntity =
                machineRepository.findById(machineId)
                        .orElseThrow(() -> new NotFoundException(
                                "Maquina não encontrada: " + machineId
                        ));

        if (machineEntity.isDeleted()) {
            throw new ConflictException("A máquina já está desativada.");
        }

        machineEntity.setDeleted(true);
        machineRepository.save(machineEntity);
    }

    public void deleteCNCMachineSerial(String serialNumber) {

        CNCMachineEntity machineEntity =
                machineRepository.findBySerialNumber(serialNumber)
                        .orElseThrow(() -> new NotFoundException(
                                "Maquina não encontrada: " + serialNumber
                        ));

        machineRepository.delete(machineEntity);
    }

    public void deleteCNCMachinebyId(Integer machineId) {

        CNCMachineEntity machineEntity =
                machineRepository.findById(machineId)
                        .orElseThrow(() -> new NotFoundException(
                                "Maquina não encontrada: " + machineId
                        ));

        machineRepository.delete(machineEntity);
    }

    public void reactivateCNCMachineSerial(String serialNumber) {

        CNCMachineEntity machineEntity =
                machineRepository.findBySerialNumber(serialNumber)
                        .orElseThrow(() -> new NotFoundException(
                                "Maquina não encontrada: " + serialNumber
                        ));

        if (!machineEntity.isDeleted()) {
            throw new ConflictException("A máquina já está ativa.");
        }

        machineEntity.setDeleted(false);
        machineRepository.save(machineEntity);
    }

    public void reactivateCNCMachine(Integer machineId) {

        CNCMachineEntity machineEntity =
                machineRepository.findById(machineId)
                        .orElseThrow(() -> new NotFoundException(
                                "Maquina não encontrada: " + machineId
                        ));

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
