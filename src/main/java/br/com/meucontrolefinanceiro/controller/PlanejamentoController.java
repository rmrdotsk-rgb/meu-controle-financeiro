package br.com.meucontrolefinanceiro.controller;

import br.com.meucontrolefinanceiro.model.OrcamentoCategoria;
import br.com.meucontrolefinanceiro.service.PlanejamentoService;
import br.com.meucontrolefinanceiro.service.PeriodoMensal;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

@Controller
public class PlanejamentoController {
    private final PlanejamentoService service;
    public PlanejamentoController(PlanejamentoService service) { this.service = service; }

    @InitBinder("orcamento")
    public void camposPermitidos(WebDataBinder binder) { binder.setAllowedFields("mes", "categoria", "limite"); }

    @GetMapping("/planejamento")
    public String listar(@RequestParam(required = false) String mes, Model model) {
        YearMonth periodo = YearMonth.now(ZoneId.of("America/Recife"));
        if (mes != null && !mes.isBlank()) {
            try { periodo = PeriodoMensal.interpretar(mes); }
            catch (DateTimeParseException e) { model.addAttribute("erro", "Escolha um mês válido"); }
        }
        model.addAttribute("periodo", periodo.toString());
        model.addAttribute("orcamentos", service.listar(periodo));
        return "planejamento";
    }

    @PostMapping("/orcamentos")
    public String salvar(@Valid @ModelAttribute("orcamento") OrcamentoCategoria orcamento, BindingResult erros,
                         RedirectAttributes redirect) {
        if (erros.hasErrors()) {
            redirect.addFlashAttribute("erro", "Confira mês, categoria e limite. Use um valor positivo com duas casas decimais.");
            return "redirect:/planejamento";
        }
        try {
            service.salvar(orcamento);
            redirect.addFlashAttribute("sucesso", "Limite mensal salvo");
            return "redirect:/planejamento?mes=" + orcamento.getMes();
        } catch (DateTimeParseException e) {
            redirect.addFlashAttribute("erro", "Informe um mês válido");
            return "redirect:/planejamento";
        }
    }
    @PostMapping("/orcamentos/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.excluir(id);
            redirect.addFlashAttribute("sucesso", "Limite removido");
        } catch (PlanejamentoService.OrcamentoNaoEncontradoException e) {
            redirect.addFlashAttribute("erro", "O limite não foi encontrado");
        }
        return "redirect:/planejamento";
    }
}
