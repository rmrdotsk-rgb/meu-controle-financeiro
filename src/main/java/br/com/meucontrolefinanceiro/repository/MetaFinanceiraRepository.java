package br.com.meucontrolefinanceiro.repository;

import br.com.meucontrolefinanceiro.model.MetaFinanceira;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MetaFinanceiraRepository extends JpaRepository<MetaFinanceira, Long> {
    List<MetaFinanceira> findAllByOrderByDataLimiteAscIdAsc();
}
