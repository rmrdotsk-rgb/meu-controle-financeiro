package br.com.meucontrolefinanceiro.repository;

import br.com.meucontrolefinanceiro.model.Despesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.time.LocalDate;

import java.util.List;

public interface DespesaRepository extends JpaRepository<Despesa, Long>, JpaSpecificationExecutor<Despesa> {

    List<Despesa> findAllByOrderByDataDescIdDesc();
    List<Despesa> findByDataBetween(LocalDate inicio, LocalDate fim);
}
