package br.com.meucontrolefinanceiro.controller;

import br.com.meucontrolefinanceiro.model.AporteMeta;
import br.com.meucontrolefinanceiro.model.MetaFinanceira;
import br.com.meucontrolefinanceiro.service.MetaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.time.ZoneId;

@Controller
public class MetaController {
    private final MetaService service;
    public MetaController(MetaService service) { this.service = service; }

    @InitBinder("meta")
    public void camposDaMeta(WebDataBinder binder) { binder.setAllowedFields("nome", "valorAlvo", "dataLimite"); }
    @InitBinder("aporte")
    public void camposDoAporte(WebDataBinder binder) { binder.setAllowedFields("valor", "data"); }

    @GetMapping("/metas")
    public String listar(Model model) {
        model.addAttribute("metas", service.listar());
        model.addAttribute("dataHoje", LocalDate.now(ZoneId.of("America/Recife")));
        return "metas";
    }
    @GetMapping("/metas/nova")
    public String nova(Model model) {
        MetaFinanceira meta = new MetaFinanceira();
        meta.setDataLimite(LocalDate.now(ZoneId.of("America/Recife")).plusMonths(3));
        model.addAttribute("meta", meta);
        model.addAttribute("modoEdicao", false);
        return "meta-formulario";
    }
    @PostMapping("/metas")
    public String salvar(@Valid @ModelAttribute("meta") MetaFinanceira meta, BindingResult erros,
                         Model model, RedirectAttributes redirect) {
        if (erros.hasErrors()) { model.addAttribute("modoEdicao", false); return "meta-formulario"; }
        service.salvar(meta);
        redirect.addFlashAttribute("sucesso", "Meta criada com sucesso");
        return "redirect:/metas";
    }
    @GetMapping("/metas/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("meta", service.buscar(id));
            model.addAttribute("modoEdicao", true);
            return "meta-formulario";
        } catch (MetaService.MetaNaoEncontradaException e) {
            redirect.addFlashAttribute("erro", "A meta não foi encontrada");
            return "redirect:/metas";
        }
    }
    @PostMapping("/metas/{id}/editar")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("meta") MetaFinanceira meta,
                            BindingResult erros, Model model, RedirectAttributes redirect) {
        meta.setId(id);
        if (erros.hasErrors()) { model.addAttribute("modoEdicao", true); return "meta-formulario"; }
        try {
            service.atualizar(id, meta);
            redirect.addFlashAttribute("sucesso", "Meta atualizada com sucesso");
        } catch (MetaService.MetaNaoEncontradaException e) {
            redirect.addFlashAttribute("erro", "A meta não foi encontrada");
        }
        return "redirect:/metas";
    }
    @PostMapping("/metas/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.excluir(id);
            redirect.addFlashAttribute("sucesso", "Meta excluída com seus aportes");
        } catch (MetaService.MetaNaoEncontradaException e) {
            redirect.addFlashAttribute("erro", "A meta não foi encontrada");
        }
        return "redirect:/metas";
    }
    @PostMapping("/metas/{id}/aportes")
    public String aportar(@PathVariable Long id, @Valid @ModelAttribute("aporte") AporteMeta aporte,
                          BindingResult erros, RedirectAttributes redirect) {
        if (erros.hasErrors()) {
            redirect.addFlashAttribute("erro", "Informe um valor positivo, com até duas casas decimais, e uma data válida para o aporte");
            return "redirect:/metas";
        }
        try {
            service.registrarAporte(id, aporte);
            redirect.addFlashAttribute("sucesso", "Valor reservado registrado na meta");
        } catch (MetaService.MetaNaoEncontradaException e) {
            redirect.addFlashAttribute("erro", "A meta não foi encontrada");
        }
        return "redirect:/metas";
    }
    @PostMapping("/metas/{metaId}/aportes/{aporteId}/excluir")
    public String excluirAporte(@PathVariable Long metaId, @PathVariable Long aporteId,
                                RedirectAttributes redirect) {
        try {
            service.excluirAporte(metaId, aporteId);
            redirect.addFlashAttribute("sucesso", "Aporte removido da meta");
        } catch (MetaService.MetaNaoEncontradaException e) {
            redirect.addFlashAttribute("erro", "O aporte não foi encontrado nesta meta");
        }
        return "redirect:/metas";
    }
}
