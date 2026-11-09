package br.com.meucontrolefinanceiro.controller;

import br.com.meucontrolefinanceiro.model.*;
import br.com.meucontrolefinanceiro.repository.*;
import br.com.meucontrolefinanceiro.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static br.com.meucontrolefinanceiro.RequisicoesTeste.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegressaoFinanceiraTest extends br.com.meucontrolefinanceiro.TesteFinanceiroAutenticado {
    @Autowired DespesaRepository despesas;
    @Autowired ReceitaRepository receitas;
    @Autowired MetaFinanceiraRepository metas;
    @Autowired AporteMetaRepository aportes;
    @Autowired OrcamentoRepository orcamentos;
    @Autowired PlanejamentoService planejamento;
    @Autowired MetaService metaService;
    @Autowired PainelService painel;

    @BeforeEach void limpar() {
        aportes.deleteAll(); metas.deleteAll(); orcamentos.deleteAll();
        receitas.deleteAll(); despesas.deleteAll();
    }

    @Test void receitaFiltraPorPeriodoCategoriaDescricaoESomaSomenteResultados() throws Exception {
        salvar(new Receita("Freelance web", new BigDecimal("125.50"), "Freelance", LocalDate.of(2026,11,1)));
        salvar(new Receita("Freelance design", new BigDecimal("80.25"), "Freelance", LocalDate.of(2026,11,30)));
        salvar(new Receita("Freelance anterior", new BigDecimal("999.00"), "Freelance", LocalDate.of(2026,10,31)));
        salvar(new Receita("Salário", new BigDecimal("1500.00"), "Salário", LocalDate.of(2026,11,9)));
        mvc.perform(get("/receitas").param("inicio", "2026-11-01").param("fim", "2026-11-30")
                .param("categoria", "Freelance").param("busca", "  FREELANCE  "))
                .andExpect(status().isOk()).andExpect(model().attribute("quantidade", 2))
                .andExpect(model().attribute("total", new BigDecimal("205.75")))
                .andExpect(model().attribute("filtrosAtivos", true));
        mvc.perform(get("/receitas").param("busca", "nada cadastrado"))
                .andExpect(status().isOk()).andExpect(model().attribute("quantidade", 0))
                .andExpect(model().attribute("total", BigDecimal.ZERO));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/despesas", "/receitas"})
    void buscaTrataCuringasSqlComoTextoLiteral(String rota) throws Exception {
        salvar(new Despesa("Desconto 10%_oferta", new BigDecimal("10.00"), "Outros", LocalDate.of(2026,11,9)));
        salvar(new Despesa("Desconto 100 oferta", new BigDecimal("20.00"), "Outros", LocalDate.of(2026,11,9)));
        salvar(new Receita("Comissão 10%_oferta", new BigDecimal("10.00"), "Outros", LocalDate.of(2026,11,9)));
        salvar(new Receita("Comissão 100 oferta", new BigDecimal("20.00"), "Outros", LocalDate.of(2026,11,9)));
        mvc.perform(get(rota).param("busca", "%_"))
                .andExpect(status().isOk()).andExpect(model().attribute("quantidade", 1))
                .andExpect(model().attribute("total", new BigDecimal("10.00")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/despesas", "/receitas"})
    void filtrosInvalidosNaoExibemResultadosEnganosos(String rota) throws Exception {
        mvc.perform(get(rota).param("inicio", "2026-02-30"))
                .andExpect(status().isOk()).andExpect(model().attributeExists("filtroErro"))
                .andExpect(model().attribute("quantidade", 0));
        mvc.perform(get(rota).param("inicio", "2026-12-01").param("fim", "2026-11-01"))
                .andExpect(status().isOk()).andExpect(model().attributeExists("filtroErro"));
        mvc.perform(get(rota).param("busca", "x".repeat(121)))
                .andExpect(status().isOk()).andExpect(model().attributeExists("filtroErro"));
    }

    @Test void receitaInexistenteEEdicaoInvalidaNaoProduzemSucesso() throws Exception {
        for (String acao : List.of("editar", "excluir")) {
            mvc.perform(post("/receitas/999999/" + acao).param("descricao", "Receita válida")
                    .param("valor", "20.00").param("categoria", "Outros").param("data", "2026-11-09"))
                    .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("erro"));
        }
        Receita original = salvar(new Receita("Receita original", new BigDecimal("20.00"), "Outros", LocalDate.of(2026,11,9)));
        mvc.perform(post("/receitas/{id}/editar", original.getId()).param("descricao", "Receita inválida")
                .param("valor", "-2").param("categoria", "Outros").param("data", "2026-11-09"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("receita", "valor"));
        assertEquals("Receita original", receitas.findById(original.getId()).orElseThrow().getDescricao());
        assertEquals(1, receitas.count());
    }

    @Test void receitaComErroDeConversaoMostraMensagensCompreensiveis() throws Exception {
        mvc.perform(post("/receitas").param("descricao", "Renda de teste")
                .param("valor", "abc").param("categoria", "Outros").param("data", "2026-02-30"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Informe um valor numérico")))
                .andExpect(content().string(containsString("Informe uma data válida")))
                .andExpect(model().attributeHasFieldErrors("receita", "valor", "data"));
        assertEquals(0, receitas.count());
    }

    @Test void metaEditaSemPerderAportesEBloqueiaRemocaoEmOutraMeta() throws Exception {
        mvc.perform(post("/metas").param("nome", "Curso futuro").param("valorAlvo", "100.00").param("dataLimite", "2027-01-01"))
                .andExpect(status().is3xxRedirection());
        MetaFinanceira meta = metas.findAll().getFirst();
        mvc.perform(post("/metas/{id}/aportes", meta.getId()).param("valor", "150.00").param("data", "2026-11-09"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/metas").param("nome", "Outra meta").param("valorAlvo", "500.00").param("dataLimite", "2027-01-01"))
                .andExpect(status().is3xxRedirection());
        Long outra = metas.findAll().stream().map(MetaFinanceira::getId).filter(id -> !id.equals(meta.getId())).findFirst().orElseThrow();
        Long aporteId = aportes.findAll().getFirst().getId();
        mvc.perform(post("/metas/{metaId}/aportes/{aporteId}/excluir", outra, aporteId))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("erro"));
        assertEquals(1, aportes.count());
        var concluida = metaService.listar().stream().filter(r -> r.meta().getId().equals(meta.getId())).findFirst().orElseThrow();
        assertEquals(100, concluida.percentual());
        assertEquals(0, concluida.falta().signum());
        assertEquals("Concluída", concluida.status());
        mvc.perform(post("/metas/{id}/editar", meta.getId()).param("nome", "Curso ampliado").param("valorAlvo", "300.00")
                .param("dataLimite", "2027-02-01"))
                .andExpect(status().is3xxRedirection());
        var editada = metaService.listar().stream().filter(r -> r.meta().getId().equals(meta.getId())).findFirst().orElseThrow();
        assertEquals(50, editada.percentual());
        assertEquals(new BigDecimal("150.00"), editada.guardado());
        assertEquals(0, despesas.count());
        assertEquals(0, receitas.count());
    }

    @Test void metaEAporteInvalidosNaoGravamNemAlteramResumo() throws Exception {
        mvc.perform(post("/metas").param("nome", "x").param("valorAlvo", "0").param("dataLimite", "2026-02-30"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("meta", "nome", "valorAlvo", "dataLimite"));
        assertEquals(0, metas.count());
        mvc.perform(post("/metas/999999/aportes").param("valor", "20.00").param("data", "2026-11-09"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("erro"));
        mvc.perform(post("/metas/999999/aportes").param("valor", "0.001").param("data", "2026-11-09"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("erro"));
        assertEquals(0, aportes.count());
    }

    @Test void planejamentoReconheceLimiteExatoEIsolaMeses() throws Exception {
        salvar(new Despesa("Gasto novembro", new BigDecimal("100.00"), "Casa", LocalDate.of(2026,11,9)));
        salvar(new Despesa("Gasto outubro", new BigDecimal("500.00"), "Casa", LocalDate.of(2026,10,9)));
        for (String mes : List.of("2026-11", "2026-12")) {
            mvc.perform(post("/orcamentos").param("mes", mes).param("categoria", "Casa").param("limite", "100.00"))
                    .andExpect(status().is3xxRedirection());
        }
        var novembro = planejamento.listar(YearMonth.of(2026,11)).getFirst();
        assertEquals("Limite atingido", novembro.situacao());
        assertEquals(new BigDecimal("100.00"), novembro.gasto());
        assertEquals(0, planejamento.listar(YearMonth.of(2026,12)).getFirst().gasto().signum());
        assertEquals(2, orcamentos.count());
        mvc.perform(post("/orcamentos").param("mes", "2026-13").param("categoria", "Casa").param("limite", "100.00"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("erro"));
        assertEquals(2, orcamentos.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-13", "0000-01", "+999999999-12", "2026-2", "invalido"})
    void mesMalformadoRetornaAvisoSemFalhar(String mes) throws Exception {
        mvc.perform(get("/dashboard").param("mes", mes)).andExpect(status().isOk()).andExpect(model().attributeExists("erro"));
        mvc.perform(get("/planejamento").param("mes", mes)).andExpect(status().isOk()).andExpect(model().attributeExists("erro"));
    }

    @Test void painelRefleteEdicaoEExclusaoPersistidas() throws Exception {
        Receita receita = salvar(new Receita("Renda mensal", new BigDecimal("1000.00"), "Outros", LocalDate.of(2026,11,9)));
        Despesa despesa = salvar(new Despesa("Despesa inicial", new BigDecimal("100.00"), "Casa", LocalDate.of(2026,11,9)));
        assertEquals(new BigDecimal("900.00"), painel.gerar(YearMonth.of(2026,11), LocalDate.of(2026,11,9)).saldo());
        mvc.perform(post("/despesas/{id}/editar", despesa.getId()).param("descricao", "Despesa corrigida")
                .param("valor", "250.00").param("categoria", "Casa").param("data", "2026-11-09"))
                .andExpect(status().is3xxRedirection());
        assertEquals(new BigDecimal("750.00"), painel.gerar(YearMonth.of(2026,11), LocalDate.of(2026,11,9)).saldo());
        mvc.perform(post("/receitas/{id}/excluir", receita.getId())).andExpect(status().is3xxRedirection());
        assertEquals(new BigDecimal("-250.00"), painel.gerar(YearMonth.of(2026,11), LocalDate.of(2026,11,9)).saldo());
    }

    @Test void csrfBloqueiaEscritaSemTokenOuComTokenInvalido() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/despesas").param("descricao", "Tentativa externa")
                .param("valor", "20.00").param("categoria", "Casa").param("data", "2026-11-09"))
                .andExpect(status().isForbidden());
        mvc.perform(MockMvcRequestBuilders.post("/despesas").with(csrf().useInvalidToken())
                .param("descricao", "Token inválido").param("valor", "20.00")
                .param("categoria", "Casa").param("data", "2026-11-09"))
                .andExpect(status().isForbidden());
        assertEquals(0, despesas.count());
        mvc.perform(get("/despesas/nova")).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test void aporteInvalidoPreservaValorEDataDigitadosSemGravar() throws Exception {
        mvc.perform(post("/metas").param("nome", "Reserva de teste").param("valorAlvo", "1000.00")
                .param("dataLimite", "2027-01-30")).andExpect(status().is3xxRedirection());
        Long id = metas.findAll().getFirst().getId();
        var resposta = mvc.perform(post("/metas/{id}/aportes", id).param("valor", "12.345")
                .param("data", "2026-11-09"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/metas"))
                .andExpect(flash().attribute("aporteMetaId", id))
                .andExpect(flash().attribute("aporteValor", "12.345"))
                .andExpect(flash().attribute("aporteData", "2026-11-09")).andReturn();
        mvc.perform(get("/metas").flashAttrs(resposta.getFlashMap()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("12.345")))
                .andExpect(content().string(containsString("2026-11-09")));
        assertEquals(0, aportes.count());
    }

    @Test void planejamentoPreservaMesSelecionadoEFormularioInvalido() throws Exception {
        mvc.perform(post("/orcamentos").param("mes", "2027-03").param("categoria", "Casa").param("limite", "abc"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/planejamento?mes=2027-03"))
                .andExpect(flash().attribute("orcamentoCategoria", "Casa"))
                .andExpect(flash().attribute("orcamentoLimite", "abc"));
        mvc.perform(post("/orcamentos").param("mes", "2027-03").param("categoria", "Casa").param("limite", "100.00"))
                .andExpect(redirectedUrl("/planejamento?mes=2027-03"));
        Long id = orcamentos.findAll().getFirst().getId();
        mvc.perform(post("/orcamentos/{id}/excluir", id))
                .andExpect(redirectedUrl("/planejamento?mes=2027-03"));
        assertEquals(0, orcamentos.count());
    }
}
