package com.indutime.backend.database.model;

import com.indutime.backend.enums.ReportPeriod;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "relatorios")
public class ReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_relatorio")
    private Integer id;

    /**
     * TODO: Mapear o relacionamento com UserEntity quando a entidade de usuário for implementada.
     *  A coluna id_usuario é obrigatória no esquema atual (NOT NULL).
     */


    @ManyToOne
    @JoinColumn(name = "id_torno")
    private CNCMachineEntity machine;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_periodo", nullable = false)
    private ReportPeriod period;

    @Column(name = "periodo_inicio", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "periodo_fim", nullable = false)
    private LocalDateTime endDate;
}