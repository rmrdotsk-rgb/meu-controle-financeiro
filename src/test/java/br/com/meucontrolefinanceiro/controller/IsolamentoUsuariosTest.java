package br.com.meucontrolefinanceiro.controller;

import br.com.meucontrolefinanceiro.CenariosContasTeste;
import br.com.meucontrolefinanceiro.model.*;
import br.com.meucontrolefinanceiro.service.PainelService.ResumoPainel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static br.com.meucontrolefinanceiro.RequisicoesTeste.post;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IsolamentoUsuariosTest extends CenariosContasTeste {
    @Test
    void outraSessaoNaoVeListasFiltrosMetasOuTotaisDoPrimeiroUsuario() throws Exception {
        usuario("Pessoa A", "a@example.test");
        usuario("Pessoa B", "b@example.test");
        var a = entrar("a@example.test");
        var b = entrar("b@example.test");
        despesa(a, "Despesa exclusiva A", "90.00", "Casa", "2026-11-09");
        receita(a, "Receita exclusiva A", "1000.00");
        meta(a, "Meta exclusiva A");
        limite(a, "300.00");
        for (String rota : List.of("/despesas", "/receitas", "/metas", "/planejamento", "/dashboard")) {
            mvc.perform(get(rota).session(b).param("mes", "2026-11").param("busca", "exclusiva"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("Despesa exclusiva A"))))
                    .andExpect(content().string(not(containsString("Receita exclusiva A"))))
                    .andExpect(content().string(not(containsString("Meta exclusiva A"))));
        }
        mvc.perform(get("/despesas").session(b)).andExpect(model().attribute("quantidade", 0))
                .andExpect(model().attribute("total", BigDecimal.ZERO));
        mvc.perform(get("/receitas").session(b)).andExpect(model().attribute("quantidade", 0));
        ResumoPainel resumo = painel(b);
        assertEquals(0, resumo.receitas().signum());
        assertEquals(0, resumo.despesas().signum());
        assertEquals(0, resumo.saldo().signum());
        assertEquals(0, resumo.lancamentos());
        assertTrue(resumo.categorias().isEmpty());
        assertTrue(resumo.porDia().isEmpty());
        assertTrue(resumo.comparacoes().isEmpty());
        mvc.perform(get("/despesas").session(a)).andExpect(model().attribute("quantidade", 1));
    }

    @Test
    void idsConhecidosNaoPermitemLerEditarOuExcluirDespesasEReceitasAlheias() throws Exception {
        usuario("Pessoa A", "a@example.test");
        usuario("Pessoa B", "b@example.test");
        var a = entrar("a@example.test");
        var b = entrar("b@example.test");
        despesa(a, "Despesa preservada A", "90.00", "Casa", "2026-11-09");
        receita(a, "Receita preservada A", "1000.00");
        Long despesaId = despesas.findAll().getFirst().getId();
        Long receitaId = receitas.findAll().getFirst().getId();
        for (String rota : List.of("/despesas/" + despesaId, "/receitas/" + receitaId)) {
            mvc.perform(get(rota + "/editar").session(b)).andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("erro"));
            mvc.perform(post(rota + "/editar").session(b).param("descricao", "Tentativa B")
                    .param("valor", "1.00").param("categoria", "Outros").param("data", "2026-11-09"))
                    .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("erro"));
            mvc.perform(post(rota + "/excluir").session(b)).andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("erro"));
        }
        assertEquals("Despesa preservada A", despesas.findById(despesaId).orElseThrow().getDescricao());
        assertEquals(new BigDecimal("90.00"), despesas.findById(despesaId).orElseThrow().getValor());
        assertEquals("Receita preservada A", receitas.findById(receitaId).orElseThrow().getDescricao());
        assertEquals(1, despesas.count());
        assertEquals(1, receitas.count());
    }

    @Test
    void idsDeMetasEAportesAlheiosNaoPermitemNenhumaAlteracao() throws Exception {
        usuario("Pessoa A", "a@example.test");
        usuario("Pessoa B", "b@example.test");
        var a = entrar("a@example.test");
        var b = entrar("b@example.test");
        meta(a, "Meta privada A");
        Long metaA = metas.findAll().getFirst().getId();
        mvc.perform(post("/metas/{id}/aportes", metaA).session(a).param("valor", "150.00").param("data", "2026-11-09"))
                .andExpect(flash().attributeExists("sucesso"));
        Long aporteA = aportes.findAll().getFirst().getId();
        meta(b, "Meta própria B");
        Long metaB = metas.findAll().stream().filter(m -> !m.getId().equals(metaA)).findFirst().orElseThrow().getId();
        mvc.perform(get("/metas/{id}/editar", metaA).session(b)).andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erro"));
        mvc.perform(post("/metas/{id}/editar", metaA).session(b).param("nome", "Meta invadida")
                .param("valorAlvo", "10.00").param("dataLimite", "2027-01-30"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("erro"));
        mvc.perform(post("/metas/{id}/aportes", metaA).session(b).param("valor", "999.00").param("data", "2026-11-09"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("erro"));
        for (Long metaId : List.of(metaA, metaB)) {
            mvc.perform(post("/metas/{meta}/aportes/{aporte}/excluir", metaId, aporteA).session(b))
                    .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("erro"));
        }
        mvc.perform(post("/metas/{id}/excluir", metaA).session(b)).andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erro"));
        assertEquals("Meta privada A", metas.findById(metaA).orElseThrow().getNome());
        assertEquals(new BigDecimal("1000.00"), metas.findById(metaA).orElseThrow().getValorAlvo());
        assertEquals(new BigDecimal("150.00"), aportes.findById(aporteA).orElseThrow().getValor());
        assertEquals(2, metas.count());
        assertEquals(1, aportes.count());
    }

    @Test
    void limitesSaoUnicosPorUsuarioEMesENaoPodemSerExcluidosPorOutraConta() throws Exception {
        Usuario usuarioA = usuario("Pessoa A", "a@example.test");
        Usuario usuarioB = usuario("Pessoa B", "b@example.test");
        var a = entrar(usuarioA.getEmail());
        var b = entrar(usuarioB.getEmail());
        limite(a, "100.00");
        Long idA = orcamentos.findAll().getFirst().getId();
        limite(b, "200.00");
        limite(b, "250.00");
        assertEquals(2, orcamentos.count());
        assertEquals(new BigDecimal("100.00"), orcamentos.findById(idA).orElseThrow().getLimite());
        var bSalvo = orcamentos.findAll().stream().filter(o -> o.getUsuario().getId().equals(usuarioB.getId())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("250.00"), bSalvo.getLimite());
        mvc.perform(post("/orcamentos/{id}/excluir", idA).session(b).param("mes", "2026-11"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("erro"));
        assertTrue(orcamentos.existsById(idA));
        mvc.perform(post("/orcamentos/{id}/excluir", bSalvo.getId()).session(b).param("mes", "2026-11"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/planejamento?mes=2026-11"));
        assertEquals(1, orcamentos.count());
        assertTrue(orcamentos.existsById(idA));
    }

    @Test
    void dashboardSomaSomenteProprietarioInclusiveComparacoesCategoriasEDias() throws Exception {
        usuario("Pessoa A", "a@example.test");
        usuario("Pessoa B", "b@example.test");
        var a = entrar("a@example.test");
        var b = entrar("b@example.test");
        receita(a, "Receita grande A", "8000.00");
        despesa(a, "Despesa grande A", "4000.00", "Casa", "2026-11-09");
        despesa(a, "Despesa antiga A", "3000.00", "Casa", "2026-10-09");
        receita(b, "Receita B", "1000.00");
        despesa(b, "Mercado B", "150.00", "Alimentação", "2026-11-09");
        despesa(b, "Mercado anterior B", "100.00", "Alimentação", "2026-10-09");
        ResumoPainel resumo = painel(b);
        assertEquals(new BigDecimal("1000.00"), resumo.receitas());
        assertEquals(new BigDecimal("150.00"), resumo.despesas());
        assertEquals(new BigDecimal("850.00"), resumo.saldo());
        assertEquals(new BigDecimal("100.00"), resumo.despesasAnterior());
        assertEquals(new BigDecimal("50.0"), resumo.variacao());
        assertEquals(2, resumo.lancamentos());
        assertEquals(1, resumo.categorias().size());
        assertFalse(resumo.categorias().containsKey("Casa"));
        assertEquals(new BigDecimal("150.00"), resumo.porDia().get(LocalDate.of(2026, 11, 9)));
        assertEquals(new BigDecimal("50.0"), resumo.comparacoes().get("Alimentação").percentual());
    }

    @Test
    void formularioNaoDefineProprietarioNemTransfereRegistroNaEdicao() throws Exception {
        Usuario usuarioA = usuario("Pessoa A", "a@example.test");
        Usuario usuarioB = usuario("Pessoa B", "b@example.test");
        var a = entrar(usuarioA.getEmail());
        for (String rota : List.of("/despesas", "/receitas")) {
            mvc.perform(post(rota).session(a).param("descricao", "Dono vem da sessão")
                    .param("valor", "25.00").param("categoria", "Outros").param("data", "2026-11-09")
                    .param("usuario.id", usuarioB.getId().toString()).param("usuarioId", usuarioB.getId().toString()))
                    .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("sucesso"));
        }
        mvc.perform(post("/metas").session(a).param("nome", "Meta da sessão").param("valorAlvo", "100.00")
                .param("dataLimite", "2027-01-30").param("usuario.id", usuarioB.getId().toString()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("sucesso"));
        mvc.perform(post("/orcamentos").session(a).param("mes", "2026-11").param("categoria", "Casa")
                .param("limite", "100.00").param("usuario.id", usuarioB.getId().toString()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("sucesso"));
        Long id = despesas.findAll().getFirst().getId();
        mvc.perform(post("/despesas/{id}/editar", id).session(a).param("descricao", "Editada com dono preservado")
                .param("valor", "30.00").param("categoria", "Casa").param("data", "2026-11-09")
                .param("usuario.id", usuarioB.getId().toString()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("sucesso"));
        assertEquals(usuarioA.getId(), despesas.findById(id).orElseThrow().getUsuario().getId());
        assertEquals(usuarioA.getId(), receitas.findAll().getFirst().getUsuario().getId());
        assertEquals(usuarioA.getId(), metas.findAll().getFirst().getUsuario().getId());
        assertEquals(usuarioA.getId(), orcamentos.findAll().getFirst().getUsuario().getId());
    }

    @Test
    void registrosLegadosSemDonoPermanecemPreservadosEInacessiveisAContaNova() throws Exception {
        Despesa legado = despesas.save(new Despesa("Legado sem proprietário", new BigDecimal("400.00"), "Casa", LocalDate.of(2026, 11, 9)));
        Receita entrada = receitas.save(new Receita("Receita legada", new BigDecimal("900.00"), "Outros", LocalDate.of(2026, 11, 9)));
        Usuario novo = usuario("Conta nova", "nova@example.test");
        var sessao = entrar(novo.getEmail());
        mvc.perform(get("/despesas").session(sessao)).andExpect(model().attribute("quantidade", 0));
        mvc.perform(get("/receitas").session(sessao)).andExpect(model().attribute("quantidade", 0));
        mvc.perform(get("/despesas/{id}/editar", legado.getId()).session(sessao))
                .andExpect(flash().attributeExists("erro"));
        mvc.perform(post("/receitas/{id}/excluir", entrada.getId()).session(sessao))
                .andExpect(flash().attributeExists("erro"));
        assertEquals(0, painel(sessao).lancamentos());
        assertNull(despesas.findById(legado.getId()).orElseThrow().getUsuario());
        assertNull(receitas.findById(entrada.getId()).orElseThrow().getUsuario());
        assertEquals(new BigDecimal("400.00"), despesas.findById(legado.getId()).orElseThrow().getValor());
    }

    private ResumoPainel painel(MockHttpSession sessao) throws Exception {
        return (ResumoPainel) mvc.perform(get("/dashboard").session(sessao).param("mes", "2026-11"))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel().get("resumo");
    }
}
