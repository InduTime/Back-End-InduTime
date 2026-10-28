package com.indutime.backend.dto;

import com.indutime.backend.database.model.TipoUsuario;

public record UsuarioResponseDTO(
        Long idUsuario,
        String nome,
        String email,
        TipoUsuario tipoUsuario
) {
}
