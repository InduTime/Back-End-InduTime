package com.indutime.backend.database.repository;

import com.indutime.backend.database.model.CNCMachineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ICNCMachineRepository extends JpaRepository<CNCMachineEntity, Integer>,
        JpaSpecificationExecutor<CNCMachineEntity> {

    boolean existsBySerialNumber(String serialNumber);

    Optional<CNCMachineEntity> findBySerialNumber(String serialNumber);

    boolean existsBySerialNumberAndIdNot(String serialNumber, Integer id);

    List<CNCMachineEntity> findAllByDeletedFalse();

    Optional<CNCMachineEntity> findBySerialNumberAndDeletedFalse(String serialNumber);

    Optional<CNCMachineEntity> findByIdAndDeletedFalse(Integer id);

    // Busca pelo identificador externo enviado pela Raspberry.
    // Não filtra deleted para que o Service consiga detectar máquinas desativadas.
    Optional<CNCMachineEntity> findByRaspberryMachineId(Integer raspberryMachineId);
}
