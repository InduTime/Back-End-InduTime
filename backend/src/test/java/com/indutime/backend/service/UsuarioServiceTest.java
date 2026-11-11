
package com.indutime.backend.service;

import com.indutime.backend.database.model.TipoUsuario;
import com.indutime.backend.database.model.Usuario;
import com.indutime.backend.database.repository.UsuarioRepository;
import com.indutime.backend.dto.UsuarioRequestDTO;
import com.indutime.backend.dto.UsuarioSenhaRequestDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario criarUsuario() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1L);
        usuario.setNome("Marcos");
        usuario.setEmail("marcos@email.com");
        usuario.setSenha("123456");
        usuario.setTipoUsuario(TipoUsuario.OPERADOR);
        usuario.setDeleted(false);
        return usuario;
    }

    @Test
    void deveCadastrarUsuarioComSucesso() {
        UsuarioRequestDTO dto = mock(UsuarioRequestDTO.class);

        when(dto.nome()).thenReturn("Marcos");
        when(dto.email()).thenReturn("marcos@email.com");
        when(dto.senha()).thenReturn("123456");
        when(usuarioRepository.existsByEmail("marcos@email.com")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = usuarioService.cadastrar(dto);

        assertEquals("Marcos", response.nome());
        assertEquals("marcos@email.com", response.email());
        assertEquals(TipoUsuario.OPERADOR, response.tipoUsuario());

        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void naoDeveCadastrarUsuarioComEmailExistente() {
        UsuarioRequestDTO dto = mock(UsuarioRequestDTO.class);

        when(dto.email()).thenReturn("marcos@email.com");
        when(usuarioRepository.existsByEmail("marcos@email.com")).thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> usuarioService.cadastrar(dto)
        );

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveListarUsuariosAtivos() {
        Usuario usuario = criarUsuario();

        when(usuarioRepository.findAllByDeletedFalse())
                .thenReturn(List.of(usuario));

        var response = usuarioService.listar();

        assertEquals(1, response.size());
        assertEquals("Marcos", response.get(0).nome());

        verify(usuarioRepository).findAllByDeletedFalse();
    }

    @Test
    void deveBuscarUsuarioAtivoPorId() {
        Usuario usuario = criarUsuario();

        when(usuarioRepository.findByIdUsuarioAndDeletedFalse(1L))
                .thenReturn(Optional.of(usuario));

        var response = usuarioService.buscarPorId(1L);

        assertEquals("Marcos", response.nome());
        assertEquals("marcos@email.com", response.email());
    }

    @Test
    void naoDeveBuscarUsuarioInexistentePorId() {
        when(usuarioRepository.findByIdUsuarioAndDeletedFalse(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> usuarioService.buscarPorId(1L)
        );
    }

    @Test
    void deveBuscarUsuarioAtivoPorEmail() {
        Usuario usuario = criarUsuario();

        when(usuarioRepository.findByEmailAndDeletedFalse("marcos@email.com"))
                .thenReturn(Optional.of(usuario));

        var response = usuarioService.buscarPorEmail("marcos@email.com");

        assertEquals("Marcos", response.nome());
    }

    @Test
    void naoDeveBuscarUsuarioInexistentePorEmail() {
        when(usuarioRepository.findByEmailAndDeletedFalse("inexistente@email.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> usuarioService.buscarPorEmail("inexistente@email.com")
        );
    }

    @Test
    void deveAtualizarDadosDoUsuario() {
        Usuario usuario = criarUsuario();
        UsuarioRequestDTO dto = mock(UsuarioRequestDTO.class);

        when(dto.nome()).thenReturn("Marcos Vinicius");
        when(dto.email()).thenReturn("novo@email.com");
        when(dto.tipousuario()).thenReturn(TipoUsuario.OPERADOR);

        when(usuarioRepository.findByIdUsuarioAndDeletedFalse(1L))
                .thenReturn(Optional.of(usuario));

        when(usuarioRepository.existsByEmailAndIdUsuarioNot("novo@email.com", 1L))
                .thenReturn(false);

        var response = usuarioService.atualizarDadosUsuario(1L, dto);

        assertEquals("Marcos Vinicius", response.nome());
        assertEquals("novo@email.com", response.email());
    }

    @Test
    void naoDeveAtualizarUsuarioComEmailDeOutroUsuario() {
        Usuario usuario = criarUsuario();
        UsuarioRequestDTO dto = mock(UsuarioRequestDTO.class);

        when(dto.email()).thenReturn("outro@email.com");

        when(usuarioRepository.findByIdUsuarioAndDeletedFalse(1L))
                .thenReturn(Optional.of(usuario));

        when(usuarioRepository.existsByEmailAndIdUsuarioNot("outro@email.com", 1L))
                .thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> usuarioService.atualizarDadosUsuario(1L, dto)
        );
    }

    @Test
    void deveAtualizarSenhaDoUsuario() {
        Usuario usuario = criarUsuario();
        UsuarioSenhaRequestDTO dto = mock(UsuarioSenhaRequestDTO.class);

        when(dto.novaSenha()).thenReturn("novaSenha123");

        when(usuarioRepository.findByIdUsuarioAndDeletedFalse(1L))
                .thenReturn(Optional.of(usuario));

        usuarioService.atualizarSenhaPorId(1L, dto);

        assertEquals("novaSenha123", usuario.getSenha());
    }

    @Test
    void deveAtualizarTipoDoUsuario() {
        Usuario usuario = criarUsuario();

        when(usuarioRepository.findByIdUsuarioAndDeletedFalse(1L))
                .thenReturn(Optional.of(usuario));

        usuarioService.atualizarTipoUsuario(1L, TipoUsuario.OPERADOR);

        assertEquals(TipoUsuario.OPERADOR, usuario.getTipoUsuario());
    }

    @Test
    void deveDeletarUsuarioPorId() {
        Usuario usuario = criarUsuario();

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(usuario));

        usuarioService.deletarPorId(1L);

        verify(usuarioRepository).delete(usuario);
    }

    @Test
    void deveDeletarUsuarioPorEmail() {
        Usuario usuario = criarUsuario();

        when(usuarioRepository.findByEmail("marcos@email.com"))
                .thenReturn(Optional.of(usuario));

        usuarioService.deletarPorEmail("marcos@email.com");

        verify(usuarioRepository).delete(usuario);
    }

    @Test
    void deveDesativarUsuarioPorId() {
        Usuario usuario = criarUsuario();

        when(usuarioRepository.findByIdUsuarioAndDeletedFalse(1L))
                .thenReturn(Optional.of(usuario));

        usuarioService.desativarPorId(1L);

        assertTrue(usuario.isDeleted());
    }

    @Test
    void naoDeveDesativarUsuarioJaDesativado() {
        when(usuarioRepository.findByIdUsuarioAndDeletedFalse(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> usuarioService.desativarPorId(1L)
        );
    }

    @Test
    void deveDesativarUsuarioPorEmail() {
        Usuario usuario = criarUsuario();

        when(usuarioRepository.findByEmailAndDeletedFalse("marcos@email.com"))
                .thenReturn(Optional.of(usuario));

        usuarioService.desativarPorEmail("marcos@email.com");

        assertTrue(usuario.isDeleted());
    }

    @Test
    void deveReativarUsuarioPorId() {
        Usuario usuario = criarUsuario();
        usuario.setDeleted(true);

        when(usuarioRepository.findByIdUsuarioAndDeletedTrue(1L))
                .thenReturn(Optional.of(usuario));

        usuarioService.reativarPorId(1L);

        assertFalse(usuario.isDeleted());
    }

    @Test
    void naoDeveReativarUsuarioJaAtivo() {
        when(usuarioRepository.findByIdUsuarioAndDeletedTrue(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> usuarioService.reativarPorId(1L)
        );
    }

    @Test
    void deveReativarUsuarioPorEmail() {
        Usuario usuario = criarUsuario();
        usuario.setDeleted(true);

        when(usuarioRepository.findByEmailAndDeletedTrue("marcos@email.com"))
                .thenReturn(Optional.of(usuario));

        usuarioService.reativarPorEmail("marcos@email.com");

        assertFalse(usuario.isDeleted());
    }
}
