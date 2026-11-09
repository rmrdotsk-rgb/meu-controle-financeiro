package br.com.meucontrolefinanceiro.repository;

import br.com.meucontrolefinanceiro.model.AporteMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AporteMetaRepository extends JpaRepository<AporteMeta, Long> {
    List<AporteMeta> findAllByOrderByDataDescIdDesc();
    Optional<AporteMeta> findByIdAndMeta_Id(Long id, Long metaId);
}
