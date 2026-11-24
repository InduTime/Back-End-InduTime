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
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsuarioControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
    }

    private Usuario salvarUsuario() {
        Usuario usuario = new Usuario();
        usuario.setNome("Maria");
        usuario.setEmail("maria@email.com");
        usuario.setSenha("SenhaAtual123");
        usuario.setTipoUsuario(TipoUsuario.OPERADOR);
        return usuarioRepository.save(usuario);
    }

    @Test
    void deveAutenticarUsuarioEDeclararSomenteDadosPublicos() throws Exception {
        Usuario salvo = salvarUsuario();

        mockMvc.perform(post("/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "  maria@email.com  ",
                                  "senha": "SenhaAtual123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(salvo.getIdUsuario()))
                .andExpect(jsonPath("$.nome").value("Maria"))
                .andExpect(jsonPath("$.email").value("maria@email.com"))
                .andExpect(jsonPath("$.tipoUsuario").value("OPERADOR"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andDo(result -> assertThat(result.getResponse().getContentAsString())
                        .doesNotContain("SenhaAtual123"));
    }

    @Test
    void emailInexistenteSenhaIncorretaECaixaDiferenteRetornamMesmo401() throws Exception {
        salvarUsuario();

        String senhaIncorreta = mockMvc.perform(post("/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"maria@email.com\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou senha incorretos"))
                .andExpect(jsonPath("$.status").value(401))
                .andReturn().getResponse().getContentAsString();

        String emailInexistente = mockMvc.perform(post("/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"inexistente@email.com\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou senha incorretos"))
                .andExpect(jsonPath("$.status").value(401))
                .andReturn().getResponse().getContentAsString();

        String caixaDiferente = mockMvc.perform(post("/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"MARIA@email.com\",\"senha\":\"SenhaAtual123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou senha incorretos"))
                .andExpect(jsonPath("$.status").value(401))
                .andReturn().getResponse().getContentAsString();

        assertThat(emailInexistente).isEqualTo(senhaIncorreta);
        assertThat(caixaDiferente).isEqualTo(senhaIncorreta);
    }

    @Test
    void usuarioDesativadoRetornaMesmo401DeCredenciaisInvalidas() throws Exception {
        Usuario usuario = salvarUsuario();
        usuario.setDeleted(true);
        usuarioRepository.save(usuario);

        mockMvc.perform(post("/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"maria@email.com\",\"senha\":\"SenhaAtual123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou senha incorretos"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void camposNulosVaziosOuInvalidosRetornam400() throws Exception {
        String[] payloads = {
                "{}",
                "{\"email\":null,\"senha\":\"SenhaAtual123\"}",
                "{\"email\":\"\",\"senha\":\"SenhaAtual123\"}",
                "{\"email\":\"invalido\",\"senha\":\"SenhaAtual123\"}",
                "{\"email\":\"maria@email.com\",\"senha\":null}",
                "{\"email\":\"maria@email.com\",\"senha\":\"\"}",
                "{\"email\":\"maria@email.com\",\"senha\":\"   \"}"
        };

        for (String payload : payloads) {
            mockMvc.perform(post("/usuarios/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    @Test
    void deveRecuperarSenhaPorEmailSemExigirSenhaAtualEManterOsDemaisDados() throws Exception {
        Usuario salvo = salvarUsuario();

        mockMvc.perform(patch("/usuarios/email/{email}/senha", salvo.getEmail())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"novaSenha":"NovaSenha123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria"))
                .andExpect(jsonPath("$.email").value("maria@email.com"))
                .andExpect(jsonPath("$.tipoUsuario").value("OPERADOR"))
                .andExpect(jsonPath("$.senha").doesNotExist());

        Usuario atualizado = usuarioRepository.findById(salvo.getIdUsuario()).orElseThrow();
        assertThat(atualizado.getSenha()).isEqualTo("NovaSenha123");
        assertThat(atualizado.getNome()).isEqualTo("Maria");
        assertThat(atualizado.getEmail()).isEqualTo("maria@email.com");
        assertThat(atualizado.getTipoUsuario()).isEqualTo(TipoUsuario.OPERADOR);
        assertThat(atualizado.isDeleted()).isFalse();
    }

    @Test
    void deveAtualizarSenhaPorIdValidandoSenhaAtual() throws Exception {
        Usuario salvo = salvarUsuario();

        mockMvc.perform(patch("/usuarios/{idUsuario}/senha", salvo.getIdUsuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senhaAtual": "SenhaAtual123",
                                  "novaSenha": "NovaSenha123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senha").doesNotExist());

        Usuario atualizado = usuarioRepository.findById(salvo.getIdUsuario()).orElseThrow();
        assertThat(atualizado.getSenha()).isEqualTo("NovaSenha123");
        assertThat(atualizado.getNome()).isEqualTo("Maria");
        assertThat(atualizado.getEmail()).isEqualTo("maria@email.com");
        assertThat(atualizado.isDeleted()).isFalse();
    }

    @Test
    void deveRetornarBadRequestEManterSenhaQuandoNovaSenhaInvalida() throws Exception {
        Usuario salvo = salvarUsuario();
        String[] payloads = {
                "{}",
                "{\"novaSenha\":null}",
                "{\"novaSenha\":\"\"}",
                "{\"novaSenha\":\"        \"}",
                "{\"novaSenha\":\"1234567\"}"
        };

        for (String payload : payloads) {
            mockMvc.perform(patch("/usuarios/email/{email}/senha", salvo.getEmail())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.senha").doesNotExist());
        }

        Usuario inalterado = usuarioRepository.findById(salvo.getIdUsuario()).orElseThrow();
        assertThat(inalterado.getSenha()).isEqualTo("SenhaAtual123");
    }

    @Test
    void deveRetornarBadRequestQuandoSenhaAtualEstiverAusenteOuIncorreta() throws Exception {
        Usuario salvo = salvarUsuario();

        mockMvc.perform(patch("/usuarios/{idUsuario}/senha", salvo.getIdUsuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novaSenha\":\"NovaSenha123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(patch("/usuarios/{idUsuario}/senha", salvo.getIdUsuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senhaAtual": "senha-incorreta",
                                  "novaSenha": "NovaSenha123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        Usuario inalterado = usuarioRepository.findById(salvo.getIdUsuario()).orElseThrow();
        assertThat(inalterado.getSenha()).isEqualTo("SenhaAtual123");
    }

    @Test
    void deveRetornarNaoEncontradoAoRecuperarSenhaDeUsuarioInexistente() throws Exception {
        mockMvc.perform(patch("/usuarios/email/{email}/senha", "inexistente@email.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novaSenha\":\"NovaSenha123\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
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
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.status").value(400));
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
