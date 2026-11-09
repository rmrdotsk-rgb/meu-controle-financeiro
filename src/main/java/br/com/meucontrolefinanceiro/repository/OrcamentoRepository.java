package br.com.meucontrolefinanceiro.repository;

import br.com.meucontrolefinanceiro.model.OrcamentoCategoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrcamentoRepository extends JpaRepository<OrcamentoCategoria, Long> {
    List<OrcamentoCategoria> findByUsuario_IdAndMesOrderByCategoriaAsc(Long usuarioId, String mes);
    Optional<OrcamentoCategoria> findByUsuario_IdAndMesAndCategoria(Long usuarioId, String mes, String categoria);
    Optional<OrcamentoCategoria> findByIdAndUsuario_Id(Long id, Long usuarioId);
}
