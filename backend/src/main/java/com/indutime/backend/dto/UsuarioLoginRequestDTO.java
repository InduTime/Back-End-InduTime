package com.indutime.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UsuarioLoginRequestDTO(
        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,
        @NotBlank(message = "Senha é obrigatória")
        String senha
) {
    public UsuarioLoginRequestDTO {
        if (email != null) {
            email = email.trim();
        }
    }
}
