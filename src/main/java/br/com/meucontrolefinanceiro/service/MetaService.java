package br.com.meucontrolefinanceiro.service;

import br.com.meucontrolefinanceiro.model.AporteMeta;
import br.com.meucontrolefinanceiro.model.MetaFinanceira;
import br.com.meucontrolefinanceiro.repository.AporteMetaRepository;
import br.com.meucontrolefinanceiro.repository.MetaFinanceiraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MetaService {
    private final MetaFinanceiraRepository metas;
    private final AporteMetaRepository aportes;
    private final UsuarioAtual usuarioAtual;
    public MetaService(MetaFinanceiraRepository metas, AporteMetaRepository aportes, UsuarioAtual usuarioAtual) {
        this.metas = metas;
        this.aportes = aportes;
        this.usuarioAtual = usuarioAtual;
    }

    @Transactional(readOnly = true)
    public MetaFinanceira buscar(Long id) {
        return metas.findByIdAndUsuario_Id(id, usuarioAtual.id()).orElseThrow(() -> new MetaNaoEncontradaException());
    }

    @Transactional(readOnly = true)
    public List<ResumoMeta> listar() {
        Long usuarioId = usuarioAtual.id();
        Map<Long, List<AporteMeta>> grupos = aportes.findByMeta_Usuario_IdOrderByDataDescIdDesc(usuarioId).stream()
                .collect(Collectors.groupingBy(a -> a.getMeta().getId()));
        List<ResumoMeta> resultado = new ArrayList<>();
        for (MetaFinanceira meta : metas.findByUsuario_IdOrderByDataLimiteAscIdAsc(usuarioId)) {
            resultado.add(resumir(meta, grupos.getOrDefault(meta.getId(), List.of())));
        }
        return resultado;
    }

    @Transactional
    public void salvar(MetaFinanceira meta) {
        meta.setId(null);
        meta.setUsuario(usuarioAtual.obter());
        metas.save(meta);
    }
    @Transactional
    public void atualizar(Long id, MetaFinanceira dados) {
        MetaFinanceira meta = buscar(id);
        meta.setNome(dados.getNome());
        meta.setValorAlvo(dados.getValorAlvo());
        meta.setDataLimite(dados.getDataLimite());
        metas.save(meta);
    }
    @Transactional
    public void excluir(Long id) { metas.delete(buscar(id)); }

    @Transactional
    public void registrarAporte(Long id, AporteMeta dados) {
        MetaFinanceira meta = buscar(id);
        AporteMeta aporte = new AporteMeta();
        aporte.setValor(dados.getValor());
        aporte.setData(dados.getData());
        aporte.setMeta(meta);
        aportes.save(aporte);
    }
    @Transactional
    public void excluirAporte(Long metaId, Long aporteId) {
        buscar(metaId);
        AporteMeta aporte = aportes.findByIdAndMeta_IdAndMeta_Usuario_Id(aporteId, metaId, usuarioAtual.id())
                .orElseThrow(() -> new MetaNaoEncontradaException());
        aportes.delete(aporte);
    }

    public ResumoMeta resumir(MetaFinanceira meta, List<AporteMeta> lancamentos) {
        BigDecimal total = lancamentos.stream().map(AporteMeta::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal falta = meta.getValorAlvo().subtract(total).max(BigDecimal.ZERO);
        int percentual = CalculosFinanceiros.percentualLimitado(total, meta.getValorAlvo());
        String status = total.compareTo(meta.getValorAlvo()) >= 0 ? "Concluída"
                : meta.getDataLimite().isBefore(LocalDate.now(java.time.ZoneId.of("America/Recife")))
                ? "Prazo encerrado" : "Em andamento";
        return new ResumoMeta(meta, total, falta, percentual, status, List.copyOf(lancamentos));
    }

    public record ResumoMeta(MetaFinanceira meta, BigDecimal guardado, BigDecimal falta,
                             int percentual, String status, List<AporteMeta> aportes) {}
    public static class MetaNaoEncontradaException extends RuntimeException {}
}
