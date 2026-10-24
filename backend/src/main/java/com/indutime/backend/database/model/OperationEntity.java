package com.indutime.backend.database.model;

import com.indutime.backend.enums.OperationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "operacoes")
public class OperationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_operacao")
    private Integer id;

    /**
     * TODO: Mapear o relacionamento com UserEntity quando a entidade de usuário for implementada.
     *  A coluna id_usuario permite valores nulos no esquema atual.
     */

    @ManyToOne
    @JoinColumn(name = "id_torno", nullable = false)
    private CNCMachineEntity machine;

    @Column(name = "data_inicio", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "data_fim")
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private OperationStatus status = OperationStatus.EM_ANDAMENTO;
}