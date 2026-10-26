package com.indutime.backend.database.repository;

import com.indutime.backend.database.model.CNCMachineEntity;
import com.indutime.backend.enums.CNCLocation;
import com.indutime.backend.enums.CNCManufacturer;
import com.indutime.backend.enums.CNCModel;
import com.indutime.backend.enums.CNCStatus;
import org.springframework.data.jpa.domain.Specification;

public class CNCMachineSpecification {

    public static Specification<CNCMachineEntity> hasStatus(CNCStatus status) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), status);
    }
    public static Specification<CNCMachineEntity> hasLocation(CNCLocation location) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("location"), location);
    }
    public static Specification<CNCMachineEntity> hasModel(CNCModel model) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("model"), model);
    }

    public static Specification<CNCMachineEntity> hasManufacturer(CNCManufacturer manufacturer) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("manufacturer"), manufacturer);
    }
}