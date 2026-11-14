
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
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CNCMachineService {

    private static final String CSV_HEADER =
            "id_raspberry,nome_torno,numero_serie,modelo,fabricante,localizacao,data_hora,status,temperatura";

    private static final String STATUS_RUNNING = "LIGADO";
    private static final String STATUS_STOPPED = "DESLIGADO";

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

    public CNCMachineResponseDTO getCNCMachineBySerialNumber(
            String serialNumber) {

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

    public CNCMachineResponseDTO updateMachineTemperature(
            Integer machineId,
            BigDecimal temperature) {

        if (temperature == null) {
            throw new BadRequestException(
                    "A temperatura não pode ser nula."
            );
        }

        CNCMachineEntity machineEntity = findActiveMachineById(machineId);

        machineEntity.setTemperature(temperature);

        CNCMachineEntity machineUpdate =
                machineRepository.save(machineEntity);

        return convertToResponse.convertToResponseDTO(machineUpdate);
    }

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

    private void validateCsvFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new BadRequestException(
                    "O arquivo CSV não pode estar vazio."
            );
        }
    }

    private void validateCsvHeader(String header) {

        if (header == null
                || !header.replace("\uFEFF", "").trim().equals(CSV_HEADER)) {
            throw new BadRequestException("Cabeçalho do CSV inválido.");
        }
    }

    private int processCsvRecords(BufferedReader reader) throws IOException {

        String line;
        int lineNumber = 1;
        int processedReadings = 0;

        while ((line = reader.readLine()) != null) {

            lineNumber++;

            if (line.isBlank()) {
                continue;
            }

            if (processCsvLine(line, lineNumber)) {
                processedReadings++;
            }
        }

        return processedReadings;
    }

    private boolean processCsvLine(String line, int lineNumber) {

        String[] columns = line.split(",", -1);

        if (columns.length != 9) {
            throw new BadRequestException(
                    "Quantidade de colunas inválida na linha " + lineNumber
            );
        }

        Integer raspberryMachineId =
                parseMachineId(columns[0], lineNumber);

        String displayName = parseRequiredValue(
                columns[1], "nome_torno", lineNumber
        );

        String serialNumber = parseRequiredValue(
                columns[2], "numero_serie", lineNumber
        );

        CNCModel model = parseEnum(
                CNCModel.class, columns[3], "modelo", lineNumber
        );

        CNCManufacturer manufacturer = parseEnum(
                CNCManufacturer.class, columns[4], "fabricante", lineNumber
        );

        CNCLocation location = parseEnum(
                CNCLocation.class, columns[5], "localizacao", lineNumber
        );

        OffsetDateTime telemetryDateTime =
                parseDateTime(columns[6], lineNumber);

        String operationStatus = parseRequiredValue(
                columns[7], "status", lineNumber
        ).toUpperCase(Locale.ROOT);

        String temperatureValue = columns[8].trim();

        BigDecimal temperature = null;

        if (!temperatureValue.isEmpty()) {
            temperature = parseTemperature(
                    temperatureValue, lineNumber
            );
        }

        if (!operationStatus.equals(STATUS_RUNNING)
                && !operationStatus.equals(STATUS_STOPPED)) {
            throw new BadRequestException(
                    "Status de operação inválido na linha " + lineNumber
            );
        }

        CNCMachineEntity machineEntity = findOrCreateMachine(
                raspberryMachineId,
                serialNumber
        );

        machineEntity.setRaspberryMachineId(raspberryMachineId);
        machineEntity.setDisplayName(displayName);
        machineEntity.setSerialNumber(serialNumber);
        machineEntity.setModel(model);
        machineEntity.setManufacturer(manufacturer);
        machineEntity.setLocation(location);

        machineEntity.setLastTelemetryAt(telemetryDateTime);

        if (temperature != null) {
            machineEntity.setTemperature(temperature);
        }

        processOperationStatus(
                machineEntity,
                operationStatus,
                telemetryDateTime,
                lineNumber
        );

        machineRepository.save(machineEntity);

        return true;
    }

    private void processOperationStatus(
            CNCMachineEntity machineEntity,
            String operationStatus,
            OffsetDateTime eventDateTime,
            int lineNumber) {

        if (operationStatus.equals(STATUS_RUNNING)) {

            if (!Boolean.TRUE.equals(machineEntity.getRunning())) {

                machineEntity.setRunning(true);
                machineEntity.setOperationStartedAt(eventDateTime);

            } else if (machineEntity.getOperationStartedAt() == null) {

                machineEntity.setOperationStartedAt(eventDateTime);
            }

            return;
        }

        if (operationStatus.equals(STATUS_STOPPED)) {

            if (Boolean.TRUE.equals(machineEntity.getRunning())) {

                OffsetDateTime operationStartedAt =
                        machineEntity.getOperationStartedAt();

                if (operationStartedAt != null) {

                    if (eventDateTime.isBefore(operationStartedAt)) {
                        throw new BadRequestException(
                                "A data de encerramento é anterior ao início "
                                        + "da operação na linha " + lineNumber
                        );
                    }

                    long operatingSeconds = Duration.between(
                            operationStartedAt,
                            eventDateTime
                    ).getSeconds();

                    Long accumulatedSeconds =
                            machineEntity.getTotalOperatingSeconds();

                    if (accumulatedSeconds == null) {
                        accumulatedSeconds = 0L;
                    }

                    machineEntity.setTotalOperatingSeconds(
                            accumulatedSeconds + operatingSeconds
                    );
                }

                machineEntity.setRunning(false);
                machineEntity.setOperationStartedAt(null);
            }
        }
    }

    private CNCMachineEntity findOrCreateMachine(
            Integer raspberryMachineId,
            String serialNumber) {

        CNCMachineEntity machineByRaspberryId =
                machineRepository.findByRaspberryMachineId(raspberryMachineId)
                        .orElse(null);

        CNCMachineEntity machineBySerialNumber =
                machineRepository.findBySerialNumber(serialNumber)
                        .orElse(null);

        if (machineByRaspberryId != null
                && machineBySerialNumber != null
                && !machineByRaspberryId.getId().equals(
                machineBySerialNumber.getId())) {

            throw new ConflictException(
                    "O ID da Raspberry e o número de série informados "
                            + "pertencem a máquinas diferentes."
            );
        }

        CNCMachineEntity machineEntity = machineByRaspberryId != null
                ? machineByRaspberryId
                : machineBySerialNumber;

        if (machineEntity == null) {
            return CNCMachineEntity.builder()
                    .raspberryMachineId(raspberryMachineId)
                    .status(CNCStatus.OFFLINE)
                    .deleted(false)
                    .running(false)
                    .totalOperatingSeconds(0L)
                    .build();
        }

        if (machineEntity.isDeleted()) {
            throw new ConflictException(
                    "A máquina identificada está desativada. "
                            + "Reative-a antes de receber telemetria."
            );
        }

        Integer currentRaspberryId =
                machineEntity.getRaspberryMachineId();

        if (currentRaspberryId != null
                && !currentRaspberryId.equals(raspberryMachineId)) {

            throw new ConflictException(
                    "O número de série já está vinculado "
                            + "a outro ID da Raspberry."
            );
        }

        String currentSerialNumber =
                machineEntity.getSerialNumber();

        if (currentSerialNumber != null
                && !currentSerialNumber.equals(serialNumber)) {

            throw new ConflictException(
                    "O ID da Raspberry já está vinculado "
                            + "a outro número de série."
            );
        }

        return machineEntity;
    }

    private Integer parseMachineId(String value, int lineNumber) {

        try {

            int machineId = Integer.parseInt(value.trim());

            if (machineId <= 0) {
                throw new NumberFormatException("ID deve ser positivo");
            }

            return machineId;

        } catch (NumberFormatException error) {

            throw new BadRequestException(
                    "ID da Raspberry inválido na linha " + lineNumber
            );
        }
    }

    private String parseRequiredValue(
            String value,
            String fieldName,
            int lineNumber) {

        if (value == null || value.isBlank()) {
            throw new BadRequestException(
                    "Campo " + fieldName
                            + " obrigatório na linha " + lineNumber
            );
        }

        return value.trim();
    }

    private <E extends Enum<E>> E parseEnum(
            Class<E> enumClass,
            String value,
            String fieldName,
            int lineNumber) {

        String parsedValue = parseRequiredValue(
                value,
                fieldName,
                lineNumber
        );

        try {
            return Enum.valueOf(enumClass, parsedValue);

        } catch (IllegalArgumentException error) {

            throw new BadRequestException(
                    "Valor inválido para " + fieldName
                            + " na linha " + lineNumber
            );
        }
    }

    private OffsetDateTime parseDateTime(
            String value,
            int lineNumber) {

        try {
            return OffsetDateTime.parse(
                    parseRequiredValue(
                            value,
                            "data_hora",
                            lineNumber
                    )
            );

        } catch (DateTimeParseException error) {

            throw new BadRequestException(
                    "Data/hora inválida na linha " + lineNumber
                            + ". Utilize o formato ISO-8601 com fuso horário."
            );
        }
    }

    private BigDecimal parseTemperature(
            String value,
            int lineNumber) {

        try {
            return new BigDecimal(value.trim());

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
                machineRequest.getSerialNumber(),
                machineId)) {

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
