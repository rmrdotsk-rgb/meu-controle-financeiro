package br.com.meucontrolefinanceiro.service;

import br.com.meucontrolefinanceiro.model.Despesa;
import br.com.meucontrolefinanceiro.repository.DespesaRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static br.com.meucontrolefinanceiro.RequisicoesTeste.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DespesaServiceTest {

    @Autowired
    private DespesaService service;

    @Autowired
    private DespesaRepository repository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Validator validator;

    @BeforeEach
    void limparBanco() {
        repository.deleteAll();
    }

    @Test
    void deveSalvarListarECalcularTotal() {
        service.salvar(new Despesa("Mercado", new BigDecimal("125.50"), "Alimentação", LocalDate.of(2026, 11, 8)));
        service.salvar(new Despesa("Livro", new BigDecimal("80.00"), "Educação", LocalDate.of(2026, 11, 7)));

        assertEquals(2, service.listarTodas().size());
        assertEquals(0, new BigDecimal("205.50").compareTo(service.calcularTotal(service.listarTodas())));
    }

    @Test
    void deveAtualizarSemCriarOutroRegistro() {
        Despesa criada = service.salvar(new Despesa("Conta", new BigDecimal("100.00"), "Casa", LocalDate.now()));
        Despesa atualizada = service.atualizar(criada.getId(), new Despesa("Conta corrigida", new BigDecimal("110.00"), "Casa", LocalDate.now()));

        assertEquals(criada.getId(), atualizada.getId());
        assertEquals(1, service.listarTodas().size());
        assertEquals("Conta corrigida", service.buscarPorId(criada.getId()).getDescricao());
    }

    @Test
    void deveRejeitarDadosInvalidosNaEntidade() {
        Despesa invalida = new Despesa("x", BigDecimal.ZERO, "Categoria inválida", null);
        Set<ConstraintViolation<Despesa>> violations = validator.validate(invalida);

        assertTrue(violations.size() >= 4);
    }

    @Test
    void deveTratarIdInexistente() {
        assertThrows(DespesaService.DespesaNaoEncontradaException.class, () -> service.buscarPorId(999L));
    }

    @Test
    void cadastroIgnoraIdForcadoENaoAlteraRegistroExistente() throws Exception {
        Despesa existente = service.salvar(new Despesa(
                "Despesa original", new BigDecimal("45.00"), "Casa", LocalDate.of(2026, 11, 8)));

        mockMvc.perform(post("/despesas")
                        .param("id", existente.getId().toString())
                        .param("descricao", "Tentativa de sobrescrita")
                        .param("valor", "999.99")
                        .param("categoria", "Lazer")
                        .param("data", "2026-11-09"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/despesas"))
                .andExpect(flash().attribute("sucesso", "Despesa cadastrada com sucesso"));

        assertEquals(2, repository.count());
        Despesa preservada = service.buscarPorId(existente.getId());
        assertEquals("Despesa original", preservada.getDescricao());
        assertEquals(0, new BigDecimal("45.00").compareTo(preservada.getValor()));
    }

    @Test
    void deveOrdenarPorDataEIdDecrescentes() {
        Despesa antiga = service.salvar(new Despesa("Antiga", BigDecimal.ONE, "Casa", LocalDate.of(2026, 11, 7)));
        Despesa primeira = service.salvar(new Despesa("Primeira", BigDecimal.ONE, "Casa", LocalDate.of(2026, 11, 8)));
        Despesa segunda = service.salvar(new Despesa("Segunda", BigDecimal.ONE, "Casa", LocalDate.of(2026, 11, 8)));

        assertEquals(List.of(segunda.getId(), primeira.getId(), antiga.getId()),
                service.listarTodas().stream().map(Despesa::getId).toList());
    }

    @Test
    void devePersistirMaiorValorPermitidoSemArredondar() {
        BigDecimal limite = new BigDecimal("9999999999.99");
        Despesa despesa = new Despesa("Limite do modelo", limite, "Outros", LocalDate.of(2026, 11, 9));
        assertTrue(validator.validate(despesa).isEmpty());

        Despesa salva = service.salvar(despesa);
        assertEquals(limite, service.buscarPorId(salva.getId()).getValor());
    }
}
