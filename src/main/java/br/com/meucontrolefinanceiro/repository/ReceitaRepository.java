package br.com.meucontrolefinanceiro.repository;

import br.com.meucontrolefinanceiro.model.Receita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReceitaRepository extends JpaRepository<Receita, Long>, JpaSpecificationExecutor<Receita> {
    List<Receita> findByUsuario_IdOrderByDataDescIdDesc(Long usuarioId);
    List<Receita> findByUsuario_IdAndDataBetween(Long usuarioId, LocalDate inicio, LocalDate fim);
    Optional<Receita> findByIdAndUsuario_Id(Long id, Long usuarioId);
}
