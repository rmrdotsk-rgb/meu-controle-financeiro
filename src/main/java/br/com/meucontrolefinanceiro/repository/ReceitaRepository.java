package br.com.meucontrolefinanceiro.repository;

import br.com.meucontrolefinanceiro.model.Receita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.time.LocalDate;
import java.util.List;

public interface ReceitaRepository extends JpaRepository<Receita, Long>, JpaSpecificationExecutor<Receita> {
    List<Receita> findAllByOrderByDataDescIdDesc();
    List<Receita> findByDataBetween(LocalDate inicio, LocalDate fim);
}
