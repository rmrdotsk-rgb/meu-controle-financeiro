package br.com.meucontrolefinanceiro.repository;

import br.com.meucontrolefinanceiro.model.Despesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.time.LocalDate;

import java.util.List;
import java.util.Optional;

public interface DespesaRepository extends JpaRepository<Despesa, Long>, JpaSpecificationExecutor<Despesa> {

    List<Despesa> findByUsuario_IdOrderByDataDescIdDesc(Long usuarioId);
    List<Despesa> findByUsuario_IdAndDataBetween(Long usuarioId, LocalDate inicio, LocalDate fim);
    Optional<Despesa> findByIdAndUsuario_Id(Long id, Long usuarioId);
}
