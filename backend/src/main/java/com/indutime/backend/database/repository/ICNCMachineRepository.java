package com.indutime.backend.database.repository;

import com.indutime.backend.database.model.CNCMachineEntity;
import com.indutime.backend.enums.CNCManufacturer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ICNCMachineRepository extends JpaRepository<CNCMachineEntity, Integer> {
}
