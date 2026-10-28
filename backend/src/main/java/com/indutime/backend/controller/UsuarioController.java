package com.indutime.backend.controller;

import com.indutime.backend.dto.UsuarioRequestDTO;
import com.indutime.backend.dto.UsuarioResponseDTO;
import com.indutime.backend.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponseDTO cadastrar(@Valid @RequestBody UsuarioRequestDTO usuarioRequestDTO) {
        return usuarioService.cadastrar(usuarioRequestDTO);
    }

    @GetMapping
    public List<UsuarioResponseDTO> listar() {
        return usuarioService.listar();
    }

    @GetMapping("/{idUsuario}")
    public UsuarioResponseDTO buscarPorId(@PathVariable Long idUsuario) {
        return usuarioService.buscarPorId(idUsuario);
    }
}
