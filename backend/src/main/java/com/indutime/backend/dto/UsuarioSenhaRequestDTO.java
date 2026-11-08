package com.indutime.backend.dto;

public record UsuarioSenhaRequestDTO(
        String senhaAtual,
        String novaSenha
) {
}