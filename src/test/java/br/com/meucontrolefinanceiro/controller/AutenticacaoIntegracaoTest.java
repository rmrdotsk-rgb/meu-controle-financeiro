package br.com.meucontrolefinanceiro.controller;

import br.com.meucontrolefinanceiro.CenariosContasTeste;
import br.com.meucontrolefinanceiro.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import br.com.meucontrolefinanceiro.demo.DemonstracaoService;

import java.util.List;
import java.util.stream.Stream;

import static br.com.meucontrolefinanceiro.RequisicoesTeste.post;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AutenticacaoIntegracaoTest extends CenariosContasTeste {
    @Autowired ApplicationContext contexto;

    @Test
    void demonstracaoFicaDesativadaPorPadraoENaoPermiteLoginDaContaDemonstrativa() throws Exception {
        assertTrue(contexto.getBeansOfType(DemonstracaoService.class).isEmpty());
        assertEquals(0, usuarios.count());
        Usuario demo = usuario("Demonstração desativada", "demo@example.test");
        demo.setDemonstracao(true);
        usuarios.save(demo);
        mvc.perform(post("/login").param("email", demo.getEmail()).param("senha", SENHA))
                .andExpect(redirectedUrl("/login?error")).andExpect(unauthenticated());
    }
    @Test
    void cadastroValidoNormalizaEmailEArmazenaSomenteHashSemPrivilegiosDoFormulario() throws Exception {
        mvc.perform(post("/cadastro").param("nome", "  Pessoa de Teste  ")
                .param("email", "  PESSOA@EXAMPLE.TEST  ").param("senha", SENHA).param("confirmacaoSenha", SENHA)
                .param("id", "999999").param("senhaHash", "hash enviado pelo navegador")
                .param("demonstracao", "true").param("admin", "true"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        assertEquals(1, usuarios.count());
        Usuario usuario = usuarios.findByEmail("pessoa@example.test").orElseThrow();
        assertNotEquals(999999L, usuario.getId());
        assertEquals("Pessoa de Teste", usuario.getNome());
        assertNotEquals(SENHA, usuario.getSenhaHash());
        assertNotEquals("hash enviado pelo navegador", usuario.getSenhaHash());
        assertTrue(encoder.matches(SENHA, usuario.getSenhaHash()));
        assertFalse(usuario.isDemonstracao());
        assertEquals(0, despesas.count());
        assertEquals(0, receitas.count());
        assertEquals(0, metas.count());
        assertEquals(0, orcamentos.count());
    }

    static Stream<Arguments> cadastrosInvalidos() {
        return Stream.of(
                Arguments.of("", "pessoa@example.test", SENHA, SENHA, "nome"),
                Arguments.of("x", "pessoa@example.test", SENHA, SENHA, "nome"),
                Arguments.of("Pessoa Teste", "email inválido", SENHA, SENHA, "email"),
                Arguments.of("Pessoa Teste", "", SENHA, SENHA, "email"),
                Arguments.of("Pessoa Teste", "pessoa@example.test", "curta", "curta", "senha"),
                Arguments.of("Pessoa Teste", "pessoa@example.test", "", "", "senha"),
                Arguments.of("Pessoa Teste", "pessoa@example.test", "a".repeat(73), "a".repeat(73), "senha"),
                Arguments.of("Pessoa Teste", "pessoa@example.test", "á".repeat(37), "á".repeat(37), "senha"),
                Arguments.of("Pessoa Teste", "pessoa@example.test", SENHA, "Diferente!2026", "confirmacaoSenha")
        );
    }

    @ParameterizedTest
    @MethodSource("cadastrosInvalidos")
    void cadastroInvalidoNaoCriaConta(String nome, String email, String senha, String confirmacao, String campo) throws Exception {
        mvc.perform(post("/cadastro").param("nome", nome).param("email", email)
                .param("senha", senha).param("confirmacaoSenha", confirmacao))
                .andExpect(status().isOk()).andExpect(view().name("cadastro"))
                .andExpect(model().attributeHasFieldErrors("cadastro", campo));
        assertEquals(0, usuarios.count());
    }

    @Test
    void emailDuplicadoNormalizadoNaoCriaContaNemSubstituiHash() throws Exception {
        Usuario existente = usuario("Nome preservado", "pessoa@example.test");
        String hash = existente.getSenhaHash();
        mvc.perform(post("/cadastro").param("nome", "Tentativa duplicada").param("email", " PESSOA@EXAMPLE.TEST ")
                .param("senha", "OutraSenha!2026").param("confirmacaoSenha", "OutraSenha!2026"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("cadastro", "email"));
        assertEquals(1, usuarios.count());
        Usuario preservado = usuarios.findById(existente.getId()).orElseThrow();
        assertEquals("Nome preservado", preservado.getNome());
        assertEquals(hash, preservado.getSenhaHash());
    }

    @Test
    void loginCorretoCriaSessaoEContaNaoExibeHash() throws Exception {
        Usuario usuario = usuario("Pessoa Autenticada", "pessoa@example.test");
        var sessao = entrar(usuario.getEmail());
        mvc.perform(get("/dashboard").session(sessao)).andExpect(status().isOk());
        mvc.perform(get("/conta").session(sessao)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Pessoa Autenticada")))
                .andExpect(content().string(containsString(usuario.getEmail())))
                .andExpect(content().string(not(containsString(usuario.getSenhaHash()))))
                .andExpect(content().string(not(containsString(SENHA))));
    }

    @Test
    void loginIncorretoNaoAutenticaENaoRevelaSeEmailExiste() throws Exception {
        usuario("Pessoa Teste", "pessoa@example.test");
        for (String email : List.of("pessoa@example.test", "inexistente@example.test")) {
            mvc.perform(post("/login").param("email", email).param("senha", "SenhaErrada!2026"))
                    .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?error"))
                    .andExpect(unauthenticated());
        }
        mvc.perform(get("/login").param("error", ""))
                .andExpect(status().isOk()).andExpect(content().string(not(containsString("UsernameNotFoundException"))));
    }

    @Test
    void senhaExcessivaNoLoginGeraFalhaGenericaSemErroInterno() throws Exception {
        usuario("Pessoa Teste", "pessoa@example.test");
        for (String senha : List.of("x".repeat(1000), "é".repeat(100))) {
            mvc.perform(post("/login").param("email", "pessoa@example.test").param("senha", senha))
                    .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?error"))
                    .andExpect(unauthenticated());
        }
    }

    @Test
    void loginNormalizaEmailETrocaIdentificadorDaSessaoExistente() throws Exception {
        usuario("Pessoa Teste", "pessoa@example.test");
        var sessao = new org.springframework.mock.web.MockHttpSession();
        String anterior = sessao.getId();
        mvc.perform(post("/login").session(sessao).with(requisicao -> {
                    requisicao.setRequestedSessionId(anterior);
                    requisicao.setRequestedSessionIdValid(true);
                    return requisicao;
                }).param("email", " PESSOA@EXAMPLE.TEST ").param("senha", SENHA))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/dashboard"))
                .andExpect(authenticated().withUsername("pessoa@example.test"));
        assertNotEquals(anterior, sessao.getId());
    }

    @Test
    void paginasFinanceirasEContaRedirecionamSemAutenticacao() throws Exception {
        for (String rota : List.of("/", "/dashboard", "/despesas", "/despesas/nova", "/receitas",
                "/metas", "/planejamento", "/conta")) {
            mvc.perform(get(rota)).andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login")).andExpect(unauthenticated());
        }
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/cadastro")).andExpect(status().isOk());
    }

    @Test
    void logoutPostInvalidaSessaoEGetNaoEncerraConta() throws Exception {
        usuario("Pessoa Teste", "pessoa@example.test");
        var sessao = entrar("pessoa@example.test");
        mvc.perform(get("/logout").session(sessao));
        mvc.perform(get("/dashboard").session(sessao)).andExpect(status().isOk());
        mvc.perform(post("/logout").session(sessao)).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout")).andExpect(unauthenticated());
        assertTrue(sessao.isInvalid());
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        assertEquals(1, usuarios.count());
    }

    @Test
    void csrfProtegeCadastroLoginEscritaELogout() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/cadastro").param("nome", "Pessoa Teste")
                .param("email", "pessoa@example.test").param("senha", SENHA).param("confirmacaoSenha", SENHA))
                .andExpect(status().isForbidden());
        mvc.perform(MockMvcRequestBuilders.post("/login").param("email", "pessoa@example.test").param("senha", SENHA))
                .andExpect(status().isForbidden());
        assertEquals(0, usuarios.count());
        usuario("Pessoa Teste", "pessoa@example.test");
        var sessao = entrar("pessoa@example.test");
        mvc.perform(MockMvcRequestBuilders.post("/despesas").session(sessao))
                .andExpect(status().isForbidden());
        mvc.perform(MockMvcRequestBuilders.post("/logout").session(sessao).with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/dashboard").session(sessao)).andExpect(status().isOk());
        assertEquals(0, despesas.count());
    }
}
