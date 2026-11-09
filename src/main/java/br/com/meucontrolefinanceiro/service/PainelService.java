package br.com.meucontrolefinanceiro.service;

import br.com.meucontrolefinanceiro.model.Despesa;
import br.com.meucontrolefinanceiro.model.Receita;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
public class PainelService {
    private final DespesaService despesas;
    private final ReceitaService receitas;
    public PainelService(DespesaService despesas, ReceitaService receitas) {
        this.despesas = despesas; this.receitas = receitas;
    }
    @Transactional(readOnly = true)
    public ResumoPainel gerar(YearMonth mes, LocalDate hoje) {
        List<Despesa> gastos = despesas.consultarPeriodo(mes.atDay(1), mes.atEndOfMonth());
        List<Receita> entradas = receitas.consultarPeriodo(mes.atDay(1), mes.atEndOfMonth());
        List<Despesa> anteriores = despesas.consultarPeriodo(mes.minusMonths(1).atDay(1), mes.minusMonths(1).atEndOfMonth());
        BigDecimal gasto = somarDespesas(gastos);
        BigDecimal receita = entradas.stream().map(Receita::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal anterior = somarDespesas(anteriores);
        BigDecimal hojeGasto = mes.equals(YearMonth.from(hoje))
                ? gastos.stream().filter(d -> hoje.equals(d.getData())).map(Despesa::getValor)
                        .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;

        Map<String, BigDecimal> porCategoria = new TreeMap<>();
        Map<LocalDate, BigDecimal> porDia = new TreeMap<>();
        gastos.forEach(d -> {
            porCategoria.merge(d.getCategoria(), d.getValor(), BigDecimal::add);
            porDia.merge(d.getData(), d.getValor(), BigDecimal::add);
        });
        Map<String, BigDecimal> ranking = porCategoria.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (primeiro, segundo) -> primeiro, LinkedHashMap::new));
        Map<String, ComparacaoCategoria> comparacao = new LinkedHashMap<>();
        Map<String, BigDecimal> antigas = new TreeMap<>();
        anteriores.forEach(d -> antigas.merge(d.getCategoria(), d.getValor(), BigDecimal::add));
        java.util.Set<String> categoriasComparadas = new java.util.LinkedHashSet<>(ranking.keySet());
        categoriasComparadas.addAll(antigas.keySet());
        categoriasComparadas.forEach(categoria -> {
            BigDecimal atual = ranking.getOrDefault(categoria, BigDecimal.ZERO);
            BigDecimal antes = antigas.getOrDefault(categoria, BigDecimal.ZERO);
            BigDecimal diferenca = atual.subtract(antes);
            BigDecimal percentual = CalculosFinanceiros.variacaoPercentual(atual, antes);
            comparacao.put(categoria, new ComparacaoCategoria(categoria, atual, antes, diferenca, percentual));
        });
        BigDecimal variacao = CalculosFinanceiros.variacaoPercentual(gasto, anterior);
        return new ResumoPainel(mes, receita, gasto, CalculosFinanceiros.saldo(receita, gasto), hojeGasto,
                gastos.size() + entradas.size(), anterior, variacao, ranking,
                porDia, comparacao, ranking.isEmpty() ? "Nenhuma" : ranking.keySet().iterator().next());
    }

    private BigDecimal somarDespesas(List<Despesa> despesas) {
        return despesas.stream().map(Despesa::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public record ComparacaoCategoria(String categoria, BigDecimal atual, BigDecimal anterior,
                                      BigDecimal diferenca, BigDecimal percentual) {}
    public record ResumoPainel(YearMonth mes, BigDecimal receitas, BigDecimal despesas, BigDecimal saldo,
                               BigDecimal gastoHoje, int lancamentos, BigDecimal despesasAnterior,
                               BigDecimal variacao, Map<String, BigDecimal> categorias,
                               Map<LocalDate, BigDecimal> porDia,
                               Map<String, ComparacaoCategoria> comparacoes, String maiorCategoria) {}
}
