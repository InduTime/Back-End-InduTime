package com.indutime.backend.controller;

import com.indutime.backend.database.model.TipoUsuario;
import com.indutime.backend.dto.UsuarioRequestDTO;
import com.indutime.backend.dto.UsuarioResponseDTO;
import com.indutime.backend.dto.UsuarioSenhaRequestDTO;
import com.indutime.backend.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @GetMapping("/email/{email}")
    public UsuarioResponseDTO buscarPorEmail(@PathVariable String email) {
        return usuarioService.buscarPorEmail(email);
    }

    @PutMapping("/{idUsuario}")
    public UsuarioResponseDTO atualizarDadosUsuario(
            @PathVariable Long idUsuario,
            @Valid @RequestBody UsuarioRequestDTO usuarioRequestDTO) {
        return usuarioService.atualizarDadosUsuario(idUsuario, usuarioRequestDTO);
    }

    @PatchMapping("/{idUsuario}/senha")
    public UsuarioResponseDTO atualizarSenhaPorId(
            @PathVariable Long idUsuario,
            @Valid @RequestBody UsuarioSenhaRequestDTO usuarioSenhaRequestDTO) {
        return usuarioService.atualizarSenhaPorId(idUsuario, usuarioSenhaRequestDTO);
    }

    @PatchMapping("/{idUsuario}/tipo")
    public UsuarioResponseDTO atualizarTipoUsuario(
            @PathVariable Long idUsuario,
            @RequestBody TipoUsuario tipoUsuario) {
        return usuarioService.atualizarTipoUsuario(idUsuario, tipoUsuario);
    }

    @PatchMapping("/{idUsuario}/desativar")
    public UsuarioResponseDTO desativarPorId(@PathVariable Long idUsuario) {
        return usuarioService.desativarPorId(idUsuario);
    }

    @PatchMapping("/email/{email}/desativar")
    public UsuarioResponseDTO desativarPorEmail(@PathVariable String email) {
        return usuarioService.desativarPorEmail(email);
    }

    @PatchMapping("/{idUsuario}/reativar")
    public UsuarioResponseDTO reativarPorId(@PathVariable Long idUsuario) {
        return usuarioService.reativarPorId(idUsuario);
    }

    @PatchMapping("/email/{email}/reativar")
    public UsuarioResponseDTO reativarPorEmail(@PathVariable String email) {
        return usuarioService.reativarPorEmail(email);
    }

    @DeleteMapping("/{idUsuario}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarPorId(@PathVariable Long idUsuario) {
        usuarioService.deletarPorId(idUsuario);
    }

    @DeleteMapping("/email/{email}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarPorEmail(@PathVariable String email) {
        usuarioService.deletarPorEmail(email);
    }

    @PatchMapping("/email/{email}/senha")
    public UsuarioResponseDTO atualizarSenhaPorEmail(
            @PathVariable String email,
            @Valid @RequestBody UsuarioSenhaRequestDTO usuarioSenhaRequestDTO) {
        return usuarioService.atualizarSenhaPorEmail(email, usuarioSenhaRequestDTO);
    }

}