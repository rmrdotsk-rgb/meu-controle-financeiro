package br.com.meucontrolefinanceiro.service;

import br.com.meucontrolefinanceiro.model.Despesa;
import br.com.meucontrolefinanceiro.repository.DespesaRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Locale;
import jakarta.persistence.criteria.Predicate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DespesaService {

    private final DespesaRepository repository;
    private final UsuarioAtual usuarioAtual;

    public DespesaService(DespesaRepository repository, UsuarioAtual usuarioAtual) {
        this.repository = repository;
        this.usuarioAtual = usuarioAtual;
    }

    @Transactional(readOnly = true)
    public List<Despesa> listarTodas() {
        return repository.findByUsuario_IdOrderByDataDescIdDesc(usuarioAtual.id());
    }

    @Transactional(readOnly = true)
    public List<Despesa> filtrar(LocalDate inicio, LocalDate fim, String categoria, String busca) {
        Long usuarioId = usuarioAtual.id();
        Specification<Despesa> filtros = (raiz, query, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            condicoes.add(cb.equal(raiz.get("usuario").get("id"), usuarioId));
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
    public List<Despesa> consultarPeriodo(LocalDate inicio, LocalDate fim) {
        return repository.findByUsuario_IdAndDataBetween(usuarioAtual.id(), inicio, fim);
    }

    @Transactional(readOnly = true)
    public Despesa buscarPorId(Long id) {
        return repository.findByIdAndUsuario_Id(id, usuarioAtual.id()).orElseThrow(() -> new DespesaNaoEncontradaException(id));
    }

    @Transactional
    public Despesa salvar(Despesa despesa) {
        despesa.setId(null);
        despesa.setUsuario(usuarioAtual.obter());
        return repository.save(despesa);
    }

    @Transactional
    public Despesa atualizar(Long id, Despesa dados) {
        Despesa existente = buscarPorId(id);
        existente.setDescricao(dados.getDescricao());
        existente.setValor(dados.getValor());
        existente.setCategoria(dados.getCategoria());
        existente.setData(dados.getData());
        return repository.save(existente);
    }

    @Transactional
    public void excluir(Long id) {
        Despesa existente = buscarPorId(id);
        repository.delete(existente);
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularTotal(List<Despesa> despesas) {
        return despesas.stream()
                .map(Despesa::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static class DespesaNaoEncontradaException extends RuntimeException {
        public DespesaNaoEncontradaException(Long id) {
            super("Despesa não encontrada: " + id);
        }
    }
}
