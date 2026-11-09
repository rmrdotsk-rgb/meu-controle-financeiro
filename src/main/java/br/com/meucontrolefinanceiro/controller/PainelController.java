package br.com.meucontrolefinanceiro.controller;

import br.com.meucontrolefinanceiro.service.MetaService;
import br.com.meucontrolefinanceiro.service.PainelService;
import br.com.meucontrolefinanceiro.service.PeriodoMensal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

@Controller
public class PainelController {
    private final PainelService painel;
    private final MetaService metas;
    public PainelController(PainelService painel, MetaService metas) {
        this.painel = painel; this.metas = metas;
    }
    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String mes, Model model) {
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Recife"));
        YearMonth periodo = YearMonth.from(hoje);
        if (mes != null && !mes.isBlank()) {
            try { periodo = PeriodoMensal.interpretar(mes); }
            catch (DateTimeParseException e) { model.addAttribute("erro", "Escolha um mês válido"); }
        }
        model.addAttribute("resumo", painel.gerar(periodo, hoje));
        model.addAttribute("periodo", periodo.toString());
        model.addAttribute("metas", metas.listar());
        return "dashboard";
    }
    @GetMapping("/ajuda")
    public String ajuda() { return "ajuda"; }
}
