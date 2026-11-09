package br.com.meucontrolefinanceiro.service;

import br.com.meucontrolefinanceiro.model.OrcamentoCategoria;
import br.com.meucontrolefinanceiro.model.Despesa;
import br.com.meucontrolefinanceiro.repository.OrcamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PlanejamentoService {
    private final OrcamentoRepository repositorio;
    private final DespesaService despesas;
    public PlanejamentoService(OrcamentoRepository repositorio, DespesaService despesas) {
        this.repositorio = repositorio; this.despesas = despesas;
    }

    @Transactional(readOnly = true)
    public List<ResumoOrcamento> listar(YearMonth mes) {
        Map<String, BigDecimal> gastos = despesas.consultarPeriodo(mes.atDay(1), mes.atEndOfMonth())
                .stream().collect(Collectors.groupingBy(Despesa::getCategoria,
                Collectors.reducing(BigDecimal.ZERO, Despesa::getValor, BigDecimal::add)));
        return repositorio.findByMesOrderByCategoriaAsc(mes.toString()).stream().map(item -> {
            BigDecimal gasto = gastos.getOrDefault(item.getCategoria(), BigDecimal.ZERO);
            BigDecimal resto = item.getLimite().subtract(gasto);
            int porcentagem = CalculosFinanceiros.percentualLimitado(gasto, item.getLimite());
            String situacao = resto.signum() < 0 ? "Limite ultrapassado"
                    : resto.signum() == 0 ? "Limite atingido"
                    : gasto.multiply(BigDecimal.TEN).compareTo(item.getLimite().multiply(BigDecimal.valueOf(8))) >= 0
                    ? "Próximo do limite" : "Dentro do limite";
            return new ResumoOrcamento(item, gasto, resto.max(BigDecimal.ZERO), porcentagem, situacao);
        }).toList();
    }

    @Transactional
    public void salvar(OrcamentoCategoria dados) {
        PeriodoMensal.interpretar(dados.getMes());
        OrcamentoCategoria orcamento = repositorio.findByMesAndCategoria(dados.getMes(), dados.getCategoria())
                .orElseGet(OrcamentoCategoria::new);
        orcamento.setMes(dados.getMes());
        orcamento.setCategoria(dados.getCategoria());
        orcamento.setLimite(dados.getLimite());
        repositorio.save(orcamento);
    }

    @Transactional
    public void excluir(Long id) {
        OrcamentoCategoria orcamento = repositorio.findById(id)
                .orElseThrow(() -> new OrcamentoNaoEncontradoException());
        repositorio.delete(orcamento);
    }

    public record ResumoOrcamento(OrcamentoCategoria orcamento, BigDecimal gasto,
                                   BigDecimal restante, int percentual, String situacao) {}
    public static class OrcamentoNaoEncontradoException extends RuntimeException {}
}
