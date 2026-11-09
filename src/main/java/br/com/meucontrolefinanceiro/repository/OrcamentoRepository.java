package br.com.meucontrolefinanceiro.repository;

import br.com.meucontrolefinanceiro.model.OrcamentoCategoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrcamentoRepository extends JpaRepository<OrcamentoCategoria, Long> {
    List<OrcamentoCategoria> findByMesOrderByCategoriaAsc(String mes);
    Optional<OrcamentoCategoria> findByMesAndCategoria(String mes, String categoria);
}
