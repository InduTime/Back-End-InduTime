package com.indutime.backend.database.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "operacao_temperaturas")
public class OperationTemperatureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_leitura")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_operacao", nullable = false)
    private OperationEntity operation;

    @Column(name = "temperatura", precision = 8, scale = 2, nullable = false)
    private BigDecimal temperature;

    @Column(name = "medida_em", nullable = false)
    private OffsetDateTime measuredAt;

    @Column(name = "medida_offset_segundos")
    private Integer measuredOffsetSeconds;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "em_operacao", nullable = false)
    private boolean runningAtMeasurement;
}
