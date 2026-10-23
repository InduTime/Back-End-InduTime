package com.indutime.backend.database.model;

import com.indutime.backend.enums.RoleTypeEnum;
import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;


@Entity
@Table(name = "roles")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class RolesEntity implements GrantedAuthority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private RoleTypeEnum name;

    @Override
    public @Nullable String getAuthority() {
        return name.name();
    }
}
