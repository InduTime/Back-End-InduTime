package com.indutime.backend.database.repository;

import com.indutime.backend.database.model.CNCMachineEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select machine from CNCMachineEntity machine "
            + "where machine.raspberryMachineId = :raspberryMachineId")
    Optional<CNCMachineEntity> findByRaspberryMachineIdForUpdate(
            @Param("raspberryMachineId") Integer raspberryMachineId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select machine from CNCMachineEntity machine where machine.id = :id")
    Optional<CNCMachineEntity> findByIdForUpdate(@Param("id") Integer id);
}
