package br.com.meucontrolefinanceiro.controller;

import br.com.meucontrolefinanceiro.model.Despesa;
import br.com.meucontrolefinanceiro.repository.DespesaRepository;
import br.com.meucontrolefinanceiro.service.DespesaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static br.com.meucontrolefinanceiro.RequisicoesTeste.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DespesaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DespesaService service;

    @Autowired
    private DespesaRepository repository;

    @BeforeEach
    void limparBanco() {
        repository.deleteAll();
    }

    @Test
    void inicioRedirecionaEListaVaziaNaoInventaRegistros() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
        mockMvc.perform(get("/despesas"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("quantidade", 0))
                .andExpect(model().attribute("total", BigDecimal.ZERO))
                .andExpect(content().string(containsString("Ainda não há despesas cadastradas.")))
                .andExpect(content().string(containsString("0,00")));
    }

    @Test
    void formularioNovoNaoGravaEPreencheDataAtual() throws Exception {
        mockMvc.perform(get("/despesas/nova"))
                .andExpect(status().isOk())
                .andExpect(view().name("formulario"))
                .andExpect(model().attribute("despesa", hasProperty("data", is(LocalDate.now(java.time.ZoneId.of("America/Recife"))))))
                .andExpect(model().attribute("modoEdicao", false));
        assertEquals(0, repository.count());
    }

    @Test
    void cadastroValidoGeraIdEListaTotalExato() throws Exception {
        mockMvc.perform(formulario(post("/despesas"), "  Compra de teste  ", "0.01", "Outros", "2026-11-09"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/despesas"))
                .andExpect(flash().attribute("sucesso", "Despesa cadastrada com sucesso"));
        Despesa salva = service.listarTodas().getFirst();
        assertTrue(salva.getId() > 0);
        assertEquals("Compra de teste", salva.getDescricao());

        service.salvar(new Despesa("Outro teste", new BigDecimal("1250.90"), "Casa", LocalDate.of(2026, 11, 8)));
        service.salvar(new Despesa("Terceiro teste", new BigDecimal("12345.67"), "Casa", LocalDate.of(2026, 11, 7)));
        mockMvc.perform(get("/despesas"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("quantidade", 3))
                .andExpect(model().attribute("total", new BigDecimal("13596.58")))
                .andExpect(content().string(containsString("1.250,90")))
                .andExpect(content().string(containsString("12.345,67")))
                .andExpect(content().string(containsString("13.596,58")));
    }

    static Stream<Arguments> cadastrosInvalidos() {
        return Stream.of(
                Arguments.of("", "10.00", "Casa", "2026-11-09", "descricao"),
                Arguments.of("x", "10.00", "Casa", "2026-11-09", "descricao"),
                Arguments.of(" x ", "10.00", "Casa", "2026-11-09", "descricao"),
                Arguments.of("x".repeat(121), "10.00", "Casa", "2026-11-09", "descricao"),
                Arguments.of("Teste", "", "Casa", "2026-11-09", "valor"),
                Arguments.of("Teste", "0", "Casa", "2026-11-09", "valor"),
                Arguments.of("Teste", "-1", "Casa", "2026-11-09", "valor"),
                Arguments.of("Teste", "1.001", "Casa", "2026-11-09", "valor"),
                Arguments.of("Teste", "10000000000.00", "Casa", "2026-11-09", "valor"),
                Arguments.of("Teste", "abc", "Casa", "2026-11-09", "valor"),
                Arguments.of("Teste", "10.00", "", "2026-11-09", "categoria"),
                Arguments.of("Teste", "10.00", "Inexistente", "2026-11-09", "categoria"),
                Arguments.of("Teste", "10.00", "Casa", "", "data"),
                Arguments.of("Teste", "10.00", "Casa", "2026-02-30", "data")
        );
    }

    @ParameterizedTest
    @MethodSource("cadastrosInvalidos")
    void cadastroInvalidoExibeErroSemGravar(String descricao, String valor, String categoria,
                                           String data, String campo) throws Exception {
        mockMvc.perform(formulario(post("/despesas"), descricao, valor, categoria, data))
                .andExpect(status().isOk())
                .andExpect(view().name("formulario"))
                .andExpect(model().attributeHasFieldErrors("despesa", campo))
                .andExpect(model().attribute("modoEdicao", false))
                .andExpect(flash().attributeCount(0));
        assertEquals(0, repository.count());
    }

    @Test
    void erroDeConversaoTemMensagemCompreensivel() throws Exception {
        mockMvc.perform(formulario(post("/despesas"), "Teste", "abc", "Casa", "2026-02-30"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Informe um valor numérico válido")))
                .andExpect(content().string(containsString("Informe uma data válida.")))
                .andExpect(content().string(not(containsString("Failed to convert"))));
        assertEquals(0, repository.count());
    }

    @Test
    void edicaoCarregaRegistroEUsaIdDaUrlSemDuplicar() throws Exception {
        Despesa original = criarDespesa("Despesa original");
        Despesa outra = criarDespesa("Outra despesa");
        mockMvc.perform(get("/despesas/{id}/editar", original.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("modoEdicao", true))
                .andExpect(content().string(containsString("Despesa original")));

        mockMvc.perform(formulario(post("/despesas/{id}/editar", original.getId()),
                        "Despesa editada", "20.25", "Lazer", "2026-11-08")
                        .param("id", outra.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/despesas"))
                .andExpect(flash().attribute("sucesso", "Despesa atualizada com sucesso"));

        Despesa editada = service.buscarPorId(original.getId());
        assertEquals(original.getId(), editada.getId());
        assertEquals("Despesa editada", editada.getDescricao());
        assertEquals(new BigDecimal("20.25"), editada.getValor());
        assertEquals("Lazer", editada.getCategoria());
        assertEquals(LocalDate.of(2026, 11, 8), editada.getData());
        assertEquals("Outra despesa", service.buscarPorId(outra.getId()).getDescricao());
        assertEquals(2, repository.count());
    }

    @Test
    void edicaoInvalidaPreservaRegistroEFormularioPreenchido() throws Exception {
        Despesa original = criarDespesa("Despesa original");
        mockMvc.perform(formulario(post("/despesas/{id}/editar", original.getId()),
                        "Texto mantido", "0", "Lazer", "2026-11-08"))
                .andExpect(status().isOk())
                .andExpect(view().name("formulario"))
                .andExpect(model().attributeHasFieldErrors("despesa", "valor"))
                .andExpect(model().attribute("modoEdicao", true))
                .andExpect(content().string(containsString("Texto mantido")))
                .andExpect(content().string(containsString("/despesas/" + original.getId() + "/editar")));
        assertEquals("Despesa original", service.buscarPorId(original.getId()).getDescricao());
        assertEquals(new BigDecimal("10.00"), service.buscarPorId(original.getId()).getValor());
        assertEquals(1, repository.count());
    }

    @Test
    void idsInexistentesRetornamErroSemSucessoFalso() throws Exception {
        mockMvc.perform(get("/despesas/999999/editar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/despesas"))
                .andExpect(flash().attribute("erro", "A despesa não foi encontrada"))
                .andExpect(flash().attributeCount(1));
        mockMvc.perform(formulario(post("/despesas/999999/editar"), "Teste", "10.00", "Casa", "2026-11-09"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("erro", "A despesa não foi encontrada"))
                .andExpect(flash().attributeCount(1));
        mockMvc.perform(post("/despesas/999999/excluir"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("erro", "A despesa não foi encontrada"))
                .andExpect(flash().attributeCount(1));
        assertEquals(0, repository.count());
    }

    @Test
    void exclusaoSoAceitaPostEAtualizaResumo() throws Exception {
        Despesa despesa = criarDespesa("Despesa para excluir");
        mockMvc.perform(get("/despesas/{id}/excluir", despesa.getId()))
                .andExpect(status().isMethodNotAllowed());
        assertTrue(repository.existsById(despesa.getId()));

        mockMvc.perform(post("/despesas/{id}/excluir", despesa.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/despesas"))
                .andExpect(flash().attribute("sucesso", "Despesa excluída com sucesso"));
        assertFalse(repository.existsById(despesa.getId()));
        mockMvc.perform(get("/despesas"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("quantidade", 0))
                .andExpect(model().attribute("total", BigDecimal.ZERO));
    }

    @Test
    void descricaoHtmlEExibidaComoTextoSemExecutarScript() throws Exception {
        criarDespesa("<script>alert(1)</script>");
        mockMvc.perform(get("/despesas"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
    }

    private Despesa criarDespesa(String descricao) {
        return service.salvar(new Despesa(descricao, new BigDecimal("10.00"), "Casa", LocalDate.of(2026, 11, 9)));
    }

    private MockHttpServletRequestBuilder formulario(MockHttpServletRequestBuilder request,
                                                      String descricao, String valor, String categoria, String data) {
        return request.param("descricao", descricao).param("valor", valor)
                .param("categoria", categoria).param("data", data);
    }
}
