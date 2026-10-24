package com.indutime.backend.database.model;

import com.indutime.backend.enums.CNCLocation;
import com.indutime.backend.enums.CNCManufacturer;
import com.indutime.backend.enums.CNCModel;
import com.indutime.backend.enums.CNCStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "TornosCNC")
public class CNCMachineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_torno")
    private Integer id;

    @Column(name = "nome_torno_fantasia", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "modelo")
    private CNCModel model;

    @Enumerated(EnumType.STRING)
    @Column(name = "fabricante")
    private CNCManufacturer manufacturer;

    @Column(name = "numero_serie", unique = true)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "localizacao")
    private CNCLocation location;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CNCStatus status = CNCStatus.OFFLINE;

    @OneToMany(mappedBy = "machine")
    private List<OperationEntity> operations;

    @OneToMany(mappedBy = "machine")
    private List<ReportEntity> reports;
}
