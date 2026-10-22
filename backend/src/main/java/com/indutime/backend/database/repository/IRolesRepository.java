package com.indutime.backend.database.repository;

import com.indutime.backend.database.model.RolesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface IRolesRepository extends JpaRepository<RolesEntity, Integer> {

    Optional<RolesEntity> findByName(String name);

}
