package br.com.meucontrolefinanceiro.controller;

import br.com.meucontrolefinanceiro.model.*;
import br.com.meucontrolefinanceiro.repository.*;
import br.com.meucontrolefinanceiro.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static br.com.meucontrolefinanceiro.RequisicoesTeste.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FinancasIntegracaoTest {
    @Autowired MockMvc mvc;
    @Autowired DespesaRepository despesas;
    @Autowired ReceitaRepository receitas;
    @Autowired MetaFinanceiraRepository metas;
    @Autowired AporteMetaRepository aportes;
    @Autowired OrcamentoRepository orcamentos;
    @Autowired PainelService painel;
    @Autowired MetaService metaService;
    @Autowired PlanejamentoService planejamento;

    @BeforeEach void limpar() {
        aportes.deleteAll(); metas.deleteAll(); orcamentos.deleteAll();
        receitas.deleteAll(); despesas.deleteAll();
    }

    @Test void paginasNovasAbriremComBancoVazio() throws Exception {
        for (String rota : List.of("/dashboard", "/receitas", "/metas", "/planejamento", "/ajuda")) {
            mvc.perform(get(rota)).andExpect(status().isOk());
        }
        mvc.perform(get("/dashboard")).andExpect(content().string(containsString("R$ 0,00")));
    }

    @Test void receitaCrudCompletoSemIdForcado() throws Exception {
        Receita antiga = receitas.save(new Receita("Receita antiga", new BigDecimal("20.00"), "Outros", LocalDate.of(2026,11,8)));
        mvc.perform(post("/receitas").param("id", antiga.getId().toString())
                .param("descricao", "Salário mensal").param("valor", "3000.00")
                .param("categoria", "Salário").param("data", "2026-11-09"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/receitas"));
        assertEquals(2, receitas.count());
        assertEquals("Receita antiga", receitas.findById(antiga.getId()).orElseThrow().getDescricao());
        Receita nova = receitas.findAll().stream().filter(r -> !r.getId().equals(antiga.getId())).findFirst().orElseThrow();
        mvc.perform(post("/receitas/{id}/editar", nova.getId()).param("descricao", "Salário corrigido")
                .param("valor", "3100.00").param("categoria", "Salário").param("data", "2026-11-09"))
                .andExpect(status().is3xxRedirection());
        assertEquals(new BigDecimal("3100.00"), receitas.findById(nova.getId()).orElseThrow().getValor());
        mvc.perform(post("/receitas/{id}/excluir", nova.getId())).andExpect(status().is3xxRedirection());
        assertEquals(1, receitas.count());
    }

    @Test void receitaInvalidaNaoPersiste() throws Exception {
        mvc.perform(post("/receitas").param("descricao", "x").param("valor", "0")
                .param("categoria", "Incorreta").param("data", "2026-02-30"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("receita", "valor", "categoria", "data", "descricao"));
        assertEquals(0, receitas.count());
    }

    @Test void filtroUsaBancoERespeitaDatasECategoria() throws Exception {
        despesas.save(new Despesa("Mercado bairro", new BigDecimal("250.00"), "Alimentação", LocalDate.of(2026,11,4)));
        despesas.save(new Despesa("Aluguel casa", new BigDecimal("900.00"), "Casa", LocalDate.of(2026,11,5)));
        mvc.perform(get("/despesas").param("inicio", "2026-11-01").param("fim", "2026-11-30")
                .param("categoria", "Alimentação").param("busca", "mercado"))
                .andExpect(status().isOk()).andExpect(model().attribute("quantidade", 1))
                .andExpect(content().string(containsString("Mercado bairro")));
        mvc.perform(get("/despesas").param("inicio", "2026-12-01").param("fim", "2026-11-01"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Confira o período")));
    }

    @Test void painelSaldoComparacoesEDataDeHoje() {
        receitas.save(new Receita("Salário", new BigDecimal("3000.00"), "Salário", LocalDate.of(2026,11,9)));
        despesas.save(new Despesa("Aluguel", new BigDecimal("900.00"), "Casa", LocalDate.of(2026,11,9)));
        despesas.save(new Despesa("Mercado", new BigDecimal("250.00"), "Alimentação", LocalDate.of(2026,11,9)));
        despesas.save(new Despesa("Transporte", new BigDecimal("150.00"), "Transporte", LocalDate.of(2026,11,9)));
        despesas.save(new Despesa("Mercado anterior", new BigDecimal("200.00"), "Alimentação", LocalDate.of(2026,10,9)));
        var resumo = painel.gerar(YearMonth.of(2026,11), LocalDate.of(2026,11,9));
        assertEquals(0, resumo.saldo().compareTo(new BigDecimal("1700.00")));
        assertEquals(0, resumo.gastoHoje().compareTo(new BigDecimal("1300.00")));
        assertEquals(new BigDecimal("25.0"), resumo.comparacoes().get("Alimentação").percentual());
        var passado = painel.gerar(YearMonth.of(2026,11), LocalDate.of(2026,11,10));
        assertEquals(0, passado.gastoHoje().compareTo(BigDecimal.ZERO));
    }

    @Test void metasEContribuicoesNaoDuplicamDespesas() throws Exception {
        mvc.perform(post("/metas").param("nome", "Reserva pessoal").param("valorAlvo", "2000.00")
                .param("dataLimite", "2027-01-30")).andExpect(status().is3xxRedirection());
        MetaFinanceira meta = metas.findAll().getFirst();
        for (String valor : List.of("400.00", "100.00")) {
            mvc.perform(post("/metas/{id}/aportes", meta.getId()).param("valor", valor).param("data", "2026-11-09"))
                    .andExpect(status().is3xxRedirection());
        }
        var resumo = metaService.listar().getFirst();
        assertEquals(25, resumo.percentual());
        assertEquals(new BigDecimal("1500.00"), resumo.falta());
        assertEquals(0, despesas.count());
        Long aporteId = aportes.findAll().getFirst().getId();
        mvc.perform(post("/metas/{metaId}/aportes/{aporteId}/excluir", meta.getId(), aporteId))
                .andExpect(status().is3xxRedirection());
        assertEquals(1, aportes.count());
        mvc.perform(post("/metas/{id}/excluir", meta.getId())).andExpect(status().is3xxRedirection());
        assertEquals(0, metas.count()); assertEquals(0, aportes.count());
    }

    @Test void planejamentoAtualizaLimiteExistenteEAvisaExcesso() throws Exception {
        despesas.save(new Despesa("Mercado", new BigDecimal("250.00"), "Alimentação", LocalDate.of(2026,11,9)));
        for (String limite : List.of("300.00", "200.00")) {
            mvc.perform(post("/orcamentos").param("mes", "2026-11").param("categoria", "Alimentação")
                    .param("limite", limite)).andExpect(status().is3xxRedirection());
        }
        assertEquals(1, orcamentos.count());
        var resumo = planejamento.listar(YearMonth.of(2026,11)).getFirst();
        assertEquals("Limite ultrapassado", resumo.situacao());
        assertEquals(100, resumo.percentual());
        assertEquals(0, resumo.restante().compareTo(BigDecimal.ZERO));
        mvc.perform(post("/orcamentos/{id}/excluir", resumo.orcamento().getId())).andExpect(status().is3xxRedirection());
        assertEquals(0, orcamentos.count());
    }
}
