package com.indutime.backend.service;

import com.indutime.backend.database.model.TipoUsuario;
import com.indutime.backend.database.model.Usuario;
import com.indutime.backend.database.repository.UsuarioRepository;
import com.indutime.backend.dto.UsuarioRequestDTO;
import com.indutime.backend.dto.UsuarioResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UsuarioService {
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

        Usuario salvo = usuarioRepository.save(usuario);
        return toResponseDTO(salvo);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listar() {
        return usuarioRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new NoSuchElementException("Usuário não encontrado"));
        return toResponseDTO(usuario);
    }

    private UsuarioResponseDTO toResponseDTO(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getIdUsuario(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTipoUsuario()
        );
    }
}
