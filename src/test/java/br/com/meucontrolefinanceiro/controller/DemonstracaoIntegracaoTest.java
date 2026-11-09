package br.com.meucontrolefinanceiro.controller;

import br.com.meucontrolefinanceiro.CenariosContasTeste;
import br.com.meucontrolefinanceiro.demo.DemonstracaoService;
import br.com.meucontrolefinanceiro.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static br.com.meucontrolefinanceiro.RequisicoesTeste.post;

@SpringBootTest(properties = {
        "app.demo.enabled=true", "app.demo.email=demo@teste.local", "app.demo.password=SenhaTeste!2026",
        "spring.datasource.url=jdbc:h2:mem:demo-integracao;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemonstracaoIntegracaoTest extends CenariosContasTeste {
    private static final String EMAIL_DEMO = "demo@teste.local";
    @Autowired DemonstracaoService demonstracao;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void prepararDemoIsolada() {
        demonstracao.run(new DefaultApplicationArguments());
    }

    @Test
    void configuracaoExplicitaCriaContaHashEDadosFicticiosCalculadosNoBanco() throws Exception {
        Usuario demo = usuarios.findByEmail(EMAIL_DEMO).orElseThrow();
        assertTrue(demo.isDemonstracao());
        assertTrue(encoder.matches(SENHA, demo.getSenhaHash()));
        assertEquals(1, usuarios.count());
        assertEquals(5, despesas.count());
        assertEquals(2, receitas.count());
        assertEquals(2, metas.count());
        assertEquals(2, aportes.count());
        assertEquals(2, orcamentos.count());
        assertTrue(despesas.findAll().stream().allMatch(d -> d.getUsuario().getId().equals(demo.getId())));
        mvc.perform(get("/login").param("demo", "true")).andExpect(status().isOk())
                .andExpect(content().string(containsString(EMAIL_DEMO)));
        var sessao = entrar(EMAIL_DEMO);
        var resultado = mvc.perform(get("/dashboard").session(sessao)).andExpect(status().isOk()).andReturn();
        var resumo = (br.com.meucontrolefinanceiro.service.PainelService.ResumoPainel)
                resultado.getModelAndView().getModel().get("resumo");
        assertEquals(new BigDecimal("3450.00"), resumo.receitas());
        assertEquals(new BigDecimal("1339.90"), resumo.despesas());
        assertEquals(new BigDecimal("2110.10"), resumo.saldo());
    }

    @Test
    void reinicializacaoNaoDuplicaNemRestauraAutomaticamenteDadosEditados() {
        var editada = despesas.findAll().getFirst();
        editada.setDescricao("Edição mantida após reinício");
        despesas.save(editada);
        demonstracao.run(new DefaultApplicationArguments());
        demonstracao.run(new DefaultApplicationArguments());
        assertEquals(1, usuarios.count());
        assertEquals(5, despesas.count());
        assertEquals(2, receitas.count());
        assertEquals(2, aportes.count());
        assertEquals("Edição mantida após reinício", despesas.findById(editada.getId()).orElseThrow().getDescricao());
    }

    @Test
    void restauracaoExplicitaAlteraSomenteDemoEPreservaContasReais() throws Exception {
        Usuario real = usuario("Pessoa real", "real@example.test");
        var sessao = entrar(real.getEmail());
        despesa(sessao, "Despesa real preservada", "90.00", "Casa", "2026-11-09");
        receita(sessao, "Receita real preservada", "500.00");
        meta(sessao, "Meta real preservada");
        limite(sessao, "300.00");
        Long idDespesa = despesas.findAll().stream().filter(d -> d.getUsuario().getId().equals(real.getId())).findFirst().orElseThrow().getId();
        Usuario demo = usuarios.findByEmail(EMAIL_DEMO).orElseThrow();
        demonstracao.restaurar(demo);
        assertEquals(2, usuarios.count());
        assertEquals(6, despesas.count());
        assertEquals(3, receitas.count());
        assertEquals(3, metas.count());
        assertEquals(2, aportes.count());
        assertEquals(3, orcamentos.count());
        assertEquals("Despesa real preservada", despesas.findById(idDespesa).orElseThrow().getDescricao());
        assertEquals(real.getId(), despesas.findById(idDespesa).orElseThrow().getUsuario().getId());
        assertTrue(metas.findAll().stream().anyMatch(m -> "Meta real preservada".equals(m.getNome())));
        assertThrows(AccessDeniedException.class, () -> demonstracao.restaurar(real));
        assertTrue(despesas.existsById(idDespesa));
    }

    @Test
    void rotaDeRestauracaoExigeSessaoDemoECsrf() throws Exception {
        Usuario real = usuario("Conta real", "real@example.test");
        var sessaoReal = entrar(real.getEmail());
        mvc.perform(post("/demonstracao/restaurar").session(sessaoReal))
                .andExpect(status().isForbidden());
        assertEquals(5, despesas.count());
        var sessaoDemo = entrar(EMAIL_DEMO);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/demonstracao/restaurar").session(sessaoDemo))
                .andExpect(status().isForbidden());
        mvc.perform(post("/demonstracao/restaurar").session(sessaoDemo))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/dashboard"))
                .andExpect(flash().attributeExists("sucesso"));
        assertEquals(5, despesas.count());
        assertEquals(2, usuarios.count());
    }

    @Test
    void emailDeContaRealNaoPodeVirarDemonstracaoNemTerSenhaAlterada() {
        Usuario real = usuario("Conta original", "real@example.test");
        String hash = real.getSenhaHash();
        var outro = new DemonstracaoService(usuarios, jdbc, encoder, real.getEmail(), "NovaSenha!2026");
        assertThrows(IllegalStateException.class, () -> outro.run(new DefaultApplicationArguments()));
        Usuario preservado = usuarios.findById(real.getId()).orElseThrow();
        assertFalse(preservado.isDemonstracao());
        assertEquals(hash, preservado.getSenhaHash());
        assertEquals("Conta original", preservado.getNome());
    }

    @Test
    void habilitacaoSemCredencialValidaFalhaSemCriarContaComSenhaFixa() {
        for (String senha : new String[]{"", "curta", "é".repeat(37)}) {
            var invalida = new DemonstracaoService(usuarios, jdbc, encoder, "nova-demo@example.test", senha);
            assertThrows(IllegalStateException.class, () -> invalida.run(new DefaultApplicationArguments()));
        }
        var emailInvalido = new DemonstracaoService(usuarios, jdbc, encoder, "não-é-email", SENHA);
        assertThrows(IllegalStateException.class, () -> emailInvalido.run(new DefaultApplicationArguments()));
        assertEquals(1, usuarios.count());
    }
}
