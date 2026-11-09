package br.com.meucontrolefinanceiro.service;

import br.com.meucontrolefinanceiro.model.Receita;
import br.com.meucontrolefinanceiro.repository.ReceitaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;
import jakarta.persistence.criteria.Predicate;
import java.util.List;

@Service
public class ReceitaService {
    private final ReceitaRepository repository;
    public ReceitaService(ReceitaRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<Receita> listarTodas() { return repository.findAllByOrderByDataDescIdDesc(); }

    @Transactional(readOnly = true)
    public List<Receita> filtrar(LocalDate inicio, LocalDate fim, String categoria, String busca) {
        Specification<Receita> filtros = (raiz, query, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            if (inicio != null) condicoes.add(cb.greaterThanOrEqualTo(raiz.get("data"), inicio));
            if (fim != null) condicoes.add(cb.lessThanOrEqualTo(raiz.get("data"), fim));
            if (categoria != null && !categoria.isBlank()) condicoes.add(cb.equal(raiz.get("categoria"), categoria));
            if (busca != null && !busca.isBlank()) {
                String termo = busca.strip().toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                condicoes.add(cb.like(cb.lower(raiz.get("descricao")), "%" + termo + "%", '\\'));
            }
            return cb.and(condicoes.toArray(Predicate[]::new));
        };
        return repository.findAll(filtros, Sort.by(Sort.Order.desc("data"), Sort.Order.desc("id")));
    }
    @Transactional(readOnly = true)
    public List<Receita> consultarPeriodo(LocalDate inicio, LocalDate fim) {
        return repository.findByDataBetween(inicio, fim);
    }
    @Transactional(readOnly = true)
    public Receita buscarPorId(Long id) {
        return repository.findById(id).orElseThrow(() -> new ReceitaNaoEncontradaException());
    }
    @Transactional
    public Receita salvar(Receita dados) { dados.setId(null); return repository.save(dados); }
    @Transactional
    public void atualizar(Long id, Receita dados) {
        Receita atual = buscarPorId(id);
        atual.setDescricao(dados.getDescricao());
        atual.setValor(dados.getValor());
        atual.setCategoria(dados.getCategoria());
        atual.setData(dados.getData());
        repository.save(atual);
    }
    @Transactional
    public void excluir(Long id) { repository.delete(buscarPorId(id)); }

    public BigDecimal calcularTotal(List<Receita> receitas) {
        return receitas.stream().map(Receita::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static class ReceitaNaoEncontradaException extends RuntimeException {}
}
