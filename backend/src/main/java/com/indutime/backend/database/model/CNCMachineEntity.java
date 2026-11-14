
package com.indutime.backend.database.model;

import com.indutime.backend.enums.CNCLocation;
import com.indutime.backend.enums.CNCManufacturer;
import com.indutime.backend.enums.CNCModel;
import com.indutime.backend.enums.CNCStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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

    @Column(name = "id_raspberry", unique = true)
    private Integer raspberryMachineId;

    @Column(name = "nome_torno_fantasia", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "modelo", nullable = false)
    private CNCModel model;

    @Enumerated(EnumType.STRING)
    @Column(name = "fabricante", nullable = false)
    private CNCManufacturer manufacturer;

    @Column(name = "numero_serie", unique = true)
    private String serialNumber;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "localizacao", nullable = false)
    private CNCLocation location;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CNCStatus status = CNCStatus.OFFLINE;

    @Column(name = "temperatura", precision = 8, scale = 2)
    private BigDecimal temperature;

    @Column(name = "ultima_telemetria_em")
    private OffsetDateTime lastTelemetryAt;

    @Column(name = "em_operacao")
    @Builder.Default
    private Boolean running = false;

    @Column(name = "operacao_iniciada_em")
    private OffsetDateTime operationStartedAt;

    @Column(name = "tempo_operacao_total_segundos")
    @Builder.Default
    private Long totalOperatingSeconds = 0L;

    @OneToMany(mappedBy = "machine")
    private List<OperationEntity> operations;

    @OneToMany(mappedBy = "machine")
    private List<ReportEntity> reports;
}
