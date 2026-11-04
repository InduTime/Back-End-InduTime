
package com.indutime.backend.specification;

import com.indutime.backend.database.model.CNCMachineEntity;
import com.indutime.backend.database.repository.CNCMachineSpecification;
import com.indutime.backend.enums.CNCLocation;
import com.indutime.backend.enums.CNCManufacturer;
import com.indutime.backend.enums.CNCModel;
import com.indutime.backend.enums.CNCStatus;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Testes do CNCMachineSpecification")
class CNCMachineSpecificationTest {

    @Test
    @DisplayName("Deve criar um filtro por status")
    void shouldCreateStatusFilter() {

        // Arrange

        Root<CNCMachineEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);

        Path<CNCStatus> path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        CNCStatus status = CNCStatus.ONLINE;

        when(root.<CNCStatus>get("status")).thenReturn(path);
        when(criteriaBuilder.equal(path, status)).thenReturn(predicate);

        Specification<CNCMachineEntity> specification =
                CNCMachineSpecification.hasStatus(status);

        // Act

        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Assert

        assertSame(predicate, result);

        verify(root).get("status");
        verify(criteriaBuilder).equal(path, status);
    }

    @Test
    @DisplayName("Deve criar um filtro por localização")
    void shouldCreateLocationFilter() {

        // Arrange

        Root<CNCMachineEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);

        Path<CNCLocation> path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        CNCLocation location = CNCLocation.SETOR_USINAGEM;

        when(root.<CNCLocation>get("location")).thenReturn(path);
        when(criteriaBuilder.equal(path, location)).thenReturn(predicate);

        Specification<CNCMachineEntity> specification =
                CNCMachineSpecification.hasLocation(location);

        // Act

        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Assert

        assertSame(predicate, result);

        verify(root).get("location");
        verify(criteriaBuilder).equal(path, location);
    }

    @Test
    @DisplayName("Deve criar um filtro por modelo")
    void shouldCreateModelFilter() {

        // Arrange

        Root<CNCMachineEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);

        Path<CNCModel> path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        CNCModel model = CNCModel.HAAS_ST_10;

        when(root.<CNCModel>get("model")).thenReturn(path);
        when(criteriaBuilder.equal(path, model)).thenReturn(predicate);

        Specification<CNCMachineEntity> specification =
                CNCMachineSpecification.hasModel(model);

        // Act

        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Assert

        assertSame(predicate, result);

        verify(root).get("model");
        verify(criteriaBuilder).equal(path, model);
    }

    @Test
    @DisplayName("Deve criar um filtro por fabricante")
    void shouldCreateManufacturerFilter() {

        // Arrange

        Root<CNCMachineEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);

        Path<CNCManufacturer> path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        CNCManufacturer manufacturer = CNCManufacturer.HAAS;

        when(root.<CNCManufacturer>get("manufacturer")).thenReturn(path);
        when(criteriaBuilder.equal(path, manufacturer)).thenReturn(predicate);

        Specification<CNCMachineEntity> specification =
                CNCMachineSpecification.hasManufacturer(manufacturer);

        // Act

        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        // Assert

        assertSame(predicate, result);

        verify(root).get("manufacturer");
        verify(criteriaBuilder).equal(path, manufacturer);
    }
}
