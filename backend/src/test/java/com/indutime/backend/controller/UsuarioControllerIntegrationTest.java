package com.indutime.backend.controller;

import com.indutime.backend.database.model.TipoUsuario;
import com.indutime.backend.database.model.Usuario;
import com.indutime.backend.database.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UsuarioControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
    }

    @Test
    void deveCadastrarUsuarioComPerfilOperadorESemRetornarSenha() throws Exception {
        String body = """
                {
                  "nome": "Maria",
                  "email": "maria@email.com",
                  "senha": "senhaSegura123",
                  "tipoUsuario": "ADMIN"
                }
                """;

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Maria"))
                .andExpect(jsonPath("$.email").value("maria@email.com"))
                .andExpect(jsonPath("$.tipoUsuario").value("OPERADOR"))
                .andExpect(jsonPath("$.senha").doesNotExist());

        Usuario usuario = usuarioRepository.findByEmail("maria@email.com").orElseThrow();
        assertThat(usuario.getSenha()).isNotEqualTo("senhaSegura123");
        assertThat(usuario.getTipoUsuario()).isEqualTo(TipoUsuario.OPERADOR);
    }

    @Test
    void deveRetornarConflitoQuandoEmailDuplicado() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setNome("Maria");
        usuario.setEmail("maria@email.com");
        usuario.setSenha("hash");
        usuario.setTipoUsuario(TipoUsuario.OPERADOR);
        usuarioRepository.save(usuario);

        String body = """
                {
                  "nome": "Maria 2",
                  "email": "maria@email.com",
                  "senha": "senhaSegura123"
                }
                """;

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("E-mail já cadastrado"));
    }

    @Test
    void deveRetornarBadRequestParaDadosInvalidos() throws Exception {
        String body = """
                {
                  "nome": "",
                  "email": "email-invalido",
                  "senha": "123"
                }
                """;

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.nome").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.senha").exists());
    }

    @Test
    void deveBuscarUsuarioPorId() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setNome("Carlos");
        usuario.setEmail("carlos@email.com");
        usuario.setSenha("hash");
        usuario.setTipoUsuario(TipoUsuario.OPERADOR);
        Usuario salvo = usuarioRepository.save(usuario);

        mockMvc.perform(get("/usuarios/{idUsuario}", salvo.getIdUsuario()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(salvo.getIdUsuario()))
                .andExpect(jsonPath("$.email").value("carlos@email.com"))
                .andExpect(jsonPath("$.senha").doesNotExist());
    }
}
