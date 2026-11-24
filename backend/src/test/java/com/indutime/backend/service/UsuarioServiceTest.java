
package com.indutime.backend.service;

import com.indutime.backend.database.model.TipoUsuario;
import com.indutime.backend.database.model.Usuario;
import com.indutime.backend.database.repository.UsuarioRepository;
import com.indutime.backend.dto.UsuarioLoginRequestDTO;
import com.indutime.backend.dto.UsuarioRequestDTO;
import com.indutime.backend.dto.UsuarioRecuperacaoSenhaRequestDTO;
import com.indutime.backend.dto.UsuarioSenhaRequestDTO;
import com.indutime.backend.exception.BadRequestException;
import com.indutime.backend.exception.NotFoundException;
import com.indutime.backend.exception.UnauthorizedException;
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
    void deveValidarLoginComCredenciaisCorretasEEmailTrimado() {
        Usuario usuario = criarUsuario();
        UsuarioLoginRequestDTO request = new UsuarioLoginRequestDTO(
                "  marcos@email.com  ", "123456"
        );
        when(usuarioRepository.findByEmailAndDeletedFalse("marcos@email.com"))
                .thenReturn(Optional.of(usuario));

        var response = usuarioService.login(request);

        assertEquals(1L, response.idUsuario());
        assertEquals("Marcos", response.nome());
        assertEquals("marcos@email.com", response.email());
        assertEquals(TipoUsuario.OPERADOR, response.tipoUsuario());
        verify(usuarioRepository).findByEmailAndDeletedFalse("marcos@email.com");
    }

    @Test
    void senhaIncorretaRetorna401ComMensagemGenerica() {
        Usuario usuario = criarUsuario();
        when(usuarioRepository.findByEmailAndDeletedFalse("marcos@email.com"))
                .thenReturn(Optional.of(usuario));

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> usuarioService.login(new UsuarioLoginRequestDTO(
                        "marcos@email.com", "senha-incorreta"
                ))
        );

        assertEquals("E-mail ou senha incorretos", exception.getMessage());
    }

    @Test
    void emailInexistenteRetorna401ComAMesmaMensagem() {
        when(usuarioRepository.findByEmailAndDeletedFalse("inexistente@email.com"))
                .thenReturn(Optional.empty());

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> usuarioService.login(new UsuarioLoginRequestDTO(
                        "inexistente@email.com", "senha-incorreta"
                ))
        );

        assertEquals("E-mail ou senha incorretos", exception.getMessage());
    }

    @Test
    void usuarioDesativadoRetorna401SemConsultarSenha() {
        when(usuarioRepository.findByEmailAndDeletedFalse("marcos@email.com"))
                .thenReturn(Optional.empty());

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> usuarioService.login(new UsuarioLoginRequestDTO(
                        "marcos@email.com", "123456"
                ))
        );

        assertEquals("E-mail ou senha incorretos", exception.getMessage());
        verify(usuarioRepository).findByEmailAndDeletedFalse("marcos@email.com");
    }

    @Test
    void credenciaisVaziasOuNulasNaoCausamNpeNemConsultamRepositorio() {
        assertThrows(BadRequestException.class, () -> usuarioService.login(null));
        assertThrows(BadRequestException.class,
                () -> usuarioService.login(new UsuarioLoginRequestDTO(null, "123456")));
        assertThrows(BadRequestException.class,
                () -> usuarioService.login(new UsuarioLoginRequestDTO(" ", "123456")));
        assertThrows(BadRequestException.class,
                () -> usuarioService.login(new UsuarioLoginRequestDTO("marcos@email.com", null)));
        assertThrows(BadRequestException.class,
                () -> usuarioService.login(new UsuarioLoginRequestDTO("marcos@email.com", "   ")));

        verifyNoInteractions(usuarioRepository);
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
        UsuarioSenhaRequestDTO dto = new UsuarioSenhaRequestDTO(
                "123456", "novaSenha123"
        );

        when(usuarioRepository.findByIdUsuarioAndDeletedFalse(1L))
                .thenReturn(Optional.of(usuario));

        usuarioService.atualizarSenhaPorId(1L, dto);

        assertEquals("novaSenha123", usuario.getSenha());
        assertEquals("Marcos", usuario.getNome());
        assertEquals("marcos@email.com", usuario.getEmail());
        assertEquals(TipoUsuario.OPERADOR, usuario.getTipoUsuario());
        assertFalse(usuario.isDeleted());
    }

    @Test
    void deveRecuperarSenhaPorEmailSemExigirSenhaAtual() {
        Usuario usuario = criarUsuario();
        UsuarioRecuperacaoSenhaRequestDTO dto =
                new UsuarioRecuperacaoSenhaRequestDTO("novaSenha123");

        when(usuarioRepository.findByEmailAndDeletedFalse("marcos@email.com"))
                .thenReturn(Optional.of(usuario));

        usuarioService.atualizarSenhaPorEmail("marcos@email.com", dto);

        assertEquals("novaSenha123", usuario.getSenha());
        assertEquals("Marcos", usuario.getNome());
        assertEquals("marcos@email.com", usuario.getEmail());
        assertEquals(TipoUsuario.OPERADOR, usuario.getTipoUsuario());
        assertFalse(usuario.isDeleted());
    }

    @Test
    void naoDeveConsultarRepositorioParaNovaSenhaInvalida() {
        for (String senha : new String[]{null, "", "   ", "1234567"}) {
            UsuarioRecuperacaoSenhaRequestDTO recoveryRequest =
                    new UsuarioRecuperacaoSenhaRequestDTO(senha);
            UsuarioSenhaRequestDTO idRequest =
                    new UsuarioSenhaRequestDTO("senhaAtual", senha);

            assertThrows(BadRequestException.class,
                    () -> usuarioService.atualizarSenhaPorEmail(
                            "marcos@email.com", recoveryRequest));
            assertThrows(BadRequestException.class,
                    () -> usuarioService.atualizarSenhaPorId(1L, idRequest));
        }

        assertThrows(BadRequestException.class,
                () -> usuarioService.atualizarSenhaPorEmail("marcos@email.com", null));
        assertThrows(BadRequestException.class,
                () -> usuarioService.atualizarSenhaPorId(1L, null));

        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void naoDeveAtualizarSenhaQuandoSenhaAtualNaoConfere() {
        Usuario usuario = criarUsuario();
        UsuarioSenhaRequestDTO dto = new UsuarioSenhaRequestDTO(
                "senhaErrada", "novaSenha123"
        );
        when(usuarioRepository.findByIdUsuarioAndDeletedFalse(1L))
                .thenReturn(Optional.of(usuario));

        assertThrows(BadRequestException.class,
                () -> usuarioService.atualizarSenhaPorId(1L, dto));

        assertEquals("123456", usuario.getSenha());
    }

    @Test
    void deveRetornarNaoEncontradoNaRecuperacaoPorEmail() {
        when(usuarioRepository.findByEmailAndDeletedFalse("inexistente@email.com"))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> usuarioService.atualizarSenhaPorEmail(
                        "inexistente@email.com",
                        new UsuarioRecuperacaoSenhaRequestDTO("novaSenha123")
                ));
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
