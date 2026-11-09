package br.com.meucontrolefinanceiro.repository;

import br.com.meucontrolefinanceiro.model.MetaFinanceira;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MetaFinanceiraRepository extends JpaRepository<MetaFinanceira, Long> {
    List<MetaFinanceira> findByUsuario_IdOrderByDataLimiteAscIdAsc(Long usuarioId);
    Optional<MetaFinanceira> findByIdAndUsuario_Id(Long id, Long usuarioId);
}
