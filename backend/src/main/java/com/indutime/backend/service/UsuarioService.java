package com.indutime.backend.service;

import com.indutime.backend.database.model.TipoUsuario;
import com.indutime.backend.database.model.Usuario;
import com.indutime.backend.database.repository.UsuarioRepository;
import com.indutime.backend.dto.UsuarioLoginRequestDTO;
import com.indutime.backend.dto.UsuarioRequestDTO;
import com.indutime.backend.dto.UsuarioRecuperacaoSenhaRequestDTO;
import com.indutime.backend.dto.UsuarioResponseDTO;
import com.indutime.backend.dto.UsuarioSenhaRequestDTO;
import com.indutime.backend.exception.BadRequestException;
import com.indutime.backend.exception.NotFoundException;
import com.indutime.backend.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final String INVALID_LOGIN_MESSAGE = "E-mail ou senha incorretos";

    private final UsuarioRepository usuarioRepository;

    @Transactional
    public UsuarioResponseDTO cadastrar(UsuarioRequestDTO usuarioRequestDTO) {

        if (usuarioRepository.existsByEmail(usuarioRequestDTO.email())) {
            throw new IllegalStateException("E-mail já cadastrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(usuarioRequestDTO.nome());
        usuario.setEmail(usuarioRequestDTO.email());
        usuario.setSenha(usuarioRequestDTO.senha());
        usuario.setTipoUsuario(TipoUsuario.OPERADOR);

        Usuario usuarioSalvo = usuarioRepository.save(usuario);

        return toResponseDTO(usuarioSalvo);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listar() {

        return usuarioRepository.findAllByDeletedFalse()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(Long idUsuario) {

        Usuario usuario = usuarioRepository.findByIdUsuarioAndDeletedFalse(idUsuario)
                .orElseThrow(() ->
                        new NoSuchElementException("Usuário não encontrado"));

        return toResponseDTO(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorEmail(String email) {

        Usuario usuario = usuarioRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() ->
                        new NoSuchElementException("Usuário não encontrado"));

        return toResponseDTO(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO login(UsuarioLoginRequestDTO usuarioLoginRequestDTO) {

        if (usuarioLoginRequestDTO == null
                || usuarioLoginRequestDTO.email() == null
                || usuarioLoginRequestDTO.email().isBlank()
                || usuarioLoginRequestDTO.senha() == null
                || usuarioLoginRequestDTO.senha().isBlank()) {
            throw new BadRequestException("E-mail e senha são obrigatórios");
        }

        String email = usuarioLoginRequestDTO.email().trim();
        Usuario usuario = usuarioRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new UnauthorizedException(INVALID_LOGIN_MESSAGE));

        if (!senhaConfere(usuario, usuarioLoginRequestDTO.senha())) {
            throw new UnauthorizedException(INVALID_LOGIN_MESSAGE);
        }

        return toResponseDTO(usuario);
    }

    @Transactional
    public UsuarioResponseDTO atualizarDadosUsuario(
            Long idUsuario,
            UsuarioRequestDTO usuarioRequestDTO) {

        Usuario usuario = usuarioRepository.findByIdUsuarioAndDeletedFalse(idUsuario)
                .orElseThrow(() ->
                        new NoSuchElementException("Usuário não encontrado"));

        if (usuarioRepository.existsByEmailAndIdUsuarioNot(
                usuarioRequestDTO.email(), idUsuario)) {
            throw new IllegalStateException("E-mail já cadastrado");
        }

        usuario.setNome(usuarioRequestDTO.nome());
        usuario.setEmail(usuarioRequestDTO.email());
        usuario.setTipoUsuario(usuarioRequestDTO.tipousuario());

        return toResponseDTO(usuario);
    }

    @Transactional
    public UsuarioResponseDTO atualizarSenhaPorId(
            Long idUsuario,
            UsuarioSenhaRequestDTO usuarioSenhaRequestDTO) {

        validarSenhaAtual(usuarioSenhaRequestDTO == null
                ? null
                : usuarioSenhaRequestDTO.senhaAtual());
        validarNovaSenha(usuarioSenhaRequestDTO == null
                ? null
                : usuarioSenhaRequestDTO.novaSenha());

        Usuario usuario = usuarioRepository.findByIdUsuarioAndDeletedFalse(idUsuario)
                .orElseThrow(() ->
                        new NotFoundException("Usuário não encontrado"));

        if (!Objects.equals(usuario.getSenha(), usuarioSenhaRequestDTO.senhaAtual())) {
            throw new BadRequestException("A senha atual informada é inválida");
        }

        usuario.setSenha(usuarioSenhaRequestDTO.novaSenha());

        return toResponseDTO(usuario);
    }

    @Transactional
    public UsuarioResponseDTO atualizarTipoUsuario(
            Long idUsuario,
            TipoUsuario tipoUsuario) {

        Usuario usuario = usuarioRepository.findByIdUsuarioAndDeletedFalse(idUsuario)
                .orElseThrow(() ->
                        new NoSuchElementException("Usuário não encontrado"));

        usuario.setTipoUsuario(tipoUsuario);

        return toResponseDTO(usuario);
    }

    @Transactional
    public void deletarPorId(Long idUsuario) {

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() ->
                        new NoSuchElementException("Usuário não encontrado"));

        usuarioRepository.delete(usuario);
    }

    @Transactional
    public void deletarPorEmail(String email) {

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() ->
                        new NoSuchElementException("Usuário não encontrado"));

        usuarioRepository.delete(usuario);
    }

    @Transactional
    public UsuarioResponseDTO desativarPorId(Long idUsuario) {

        Usuario usuario = usuarioRepository.findByIdUsuarioAndDeletedFalse(idUsuario)
                .orElseThrow(() ->
                        new NoSuchElementException("Usuário não encontrado ou já desativado"));

        usuario.setDeleted(true);

        return toResponseDTO(usuario);
    }

    @Transactional
    public UsuarioResponseDTO desativarPorEmail(String email) {

        Usuario usuario = usuarioRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() ->
                        new NoSuchElementException("Usuário não encontrado ou já desativado"));

        usuario.setDeleted(true);

        return toResponseDTO(usuario);
    }

    @Transactional
    public UsuarioResponseDTO reativarPorId(Long idUsuario) {

        Usuario usuario = usuarioRepository.findByIdUsuarioAndDeletedTrue(idUsuario)
                .orElseThrow(() ->
                        new NoSuchElementException("Usuário não encontrado ou já ativo"));

        usuario.setDeleted(false);

        return toResponseDTO(usuario);
    }

    @Transactional
    public UsuarioResponseDTO reativarPorEmail(String email) {

        Usuario usuario = usuarioRepository.findByEmailAndDeletedTrue(email)
                .orElseThrow(() ->
                        new NoSuchElementException("Usuário não encontrado ou já ativo"));

        usuario.setDeleted(false);

        return toResponseDTO(usuario);
    }

    @Transactional
    public UsuarioResponseDTO atualizarSenhaPorEmail(
            String email,
            UsuarioRecuperacaoSenhaRequestDTO usuarioSenhaRequestDTO) {

        validarNovaSenha(usuarioSenhaRequestDTO == null
                ? null
                : usuarioSenhaRequestDTO.novaSenha());

        Usuario usuario = usuarioRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() ->
                        new NotFoundException("Usuário não encontrado"));

        usuario.setSenha(usuarioSenhaRequestDTO.novaSenha());

        return toResponseDTO(usuario);
    }

    private void validarSenhaAtual(String senhaAtual) {
        if (senhaAtual == null || senhaAtual.isBlank()) {
            throw new BadRequestException("A senha atual é obrigatória");
        }
    }

    private void validarNovaSenha(String novaSenha) {
        if (novaSenha == null || novaSenha.isBlank()) {
            throw new BadRequestException("A nova senha é obrigatória");
        }

        if (novaSenha.length() < 8) {
            throw new BadRequestException(
                    "A nova senha deve ter no mínimo 8 caracteres"
            );
        }
    }

    private UsuarioResponseDTO toResponseDTO(Usuario usuario) {

        return new UsuarioResponseDTO(
                usuario.getIdUsuario(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTipoUsuario()
        );
    }

    private boolean senhaConfere(Usuario usuario, String senha) {
        return usuario != null
                && usuario.getSenha() != null
                && senha != null
                && usuario.getSenha().equals(senha);
    }
}
