package com.indutime.backend.database.repository;

import com.indutime.backend.database.model.CNCMachineEntity;
import com.indutime.backend.dto.CNCMachineProjection;
import com.indutime.backend.enums.CNCLocation;
import com.indutime.backend.enums.CNCManufacturer;
import com.indutime.backend.enums.CNCModel;
import com.indutime.backend.enums.CNCStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;


public interface ICNCMachineRepository extends JpaRepository<CNCMachineEntity, Integer> , JpaSpecificationExecutor<CNCMachineEntity> {

    boolean existsBySerialNumber(String serialNumber);

    Optional<CNCMachineEntity> findBySerialNumber(String serialNumber);

    boolean existsBySerialNumberAndIdNot(String serialNumber, Integer id);

    List<CNCMachineEntity> findAllByDeletedFalse();

    Optional<CNCMachineEntity> findBySerialNumberAndDeletedFalse(String serialNumber);

    Optional<CNCMachineEntity> findByIdAndDeletedFalse(Integer id);


}
