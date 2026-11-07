package com.indutime.backend.service;

import com.indutime.backend.database.model.TipoUsuario;
import com.indutime.backend.database.model.Usuario;
import com.indutime.backend.database.repository.UsuarioRepository;
import com.indutime.backend.dto.UsuarioRequestDTO;
import com.indutime.backend.dto.UsuarioResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void deveCadastrarUsuarioComPerfilOperador() {
        UsuarioRequestDTO request = new UsuarioRequestDTO("João", "joao@email.com", "senha1234");
        when(usuarioRepository.existsByEmail(request.email())).thenReturn(false);
        when(usuarioRepository.save(org.mockito.ArgumentMatchers.any(Usuario.class)))
                .thenAnswer(invocation -> {
                    Usuario usuario = invocation.getArgument(0);
                    usuario.setIdUsuario(10L);
                    return usuario;
                });

        UsuarioResponseDTO response = usuarioService.cadastrar(request);
        assertThat(response.idUsuario()).isEqualTo(10L);
        assertThat(response.tipoUsuario()).isEqualTo(TipoUsuario.OPERADOR);
    }

    @Test
    void deveLancarConflitoQuandoEmailJaExistir() {
        UsuarioRequestDTO request = new UsuarioRequestDTO("João", "joao@email.com", "senha1234");
        when(usuarioRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.cadastrar(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("E-mail já cadastrado");
    }

    @Test
    void deveBuscarUsuarioPorId() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1L);
        usuario.setNome("Ana");
        usuario.setEmail("ana@email.com");
        usuario.setSenha("hash");
        usuario.setTipoUsuario(TipoUsuario.OPERADOR);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        UsuarioResponseDTO response = usuarioService.buscarPorId(1L);

        assertThat(response.nome()).isEqualTo("Ana");
        assertThat(response.email()).isEqualTo("ana@email.com");
    }

    @Test
    void deveLancarExcecaoQuandoNaoEncontrarUsuarioPorId() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.buscarPorId(1L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Usuário não encontrado");
    }
}
