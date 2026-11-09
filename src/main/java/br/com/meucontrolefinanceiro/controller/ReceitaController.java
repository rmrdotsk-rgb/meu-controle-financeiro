package br.com.meucontrolefinanceiro.controller;

import br.com.meucontrolefinanceiro.model.Receita;
import br.com.meucontrolefinanceiro.service.ReceitaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.util.List;
import java.time.format.DateTimeParseException;
import java.time.ZoneId;

@Controller
public class ReceitaController {
    private final ReceitaService service;
    public ReceitaController(ReceitaService service) { this.service = service; }

    @InitBinder("receita")
    public void camposPermitidos(WebDataBinder binder) { binder.setAllowedFields("descricao", "valor", "categoria", "data"); }

    @GetMapping("/receitas")
    public String listar(@RequestParam(defaultValue = "") String inicio,
                         @RequestParam(defaultValue = "") String fim,
                         @RequestParam(defaultValue = "") String categoria,
                         @RequestParam(defaultValue = "") String busca, Model model) {
        model.addAttribute("inicio", inicio);
        model.addAttribute("fim", fim);
        model.addAttribute("categoriaSelecionada", categoria);
        model.addAttribute("busca", busca);
        LocalDate dataInicial = null;
        LocalDate dataFinal = null;
        boolean valido = true;
        try {
            if (!inicio.isBlank()) dataInicial = LocalDate.parse(inicio);
            if (!fim.isBlank()) dataFinal = LocalDate.parse(fim);
            if (dataInicial != null && dataFinal != null && dataInicial.isAfter(dataFinal)) {
                throw new IllegalArgumentException("Período invertido");
            }
        } catch (DateTimeParseException | IllegalArgumentException e) {
            model.addAttribute("filtroErro", "Confira o período: a data inicial não pode ser posterior à final");
            valido = false;
        }
        if (busca.length() > 120 || categoria.length() > 30) {
            model.addAttribute("filtroErro", "O filtro informado é muito longo");
            valido = false;
        }
        List<Receita> receitas = valido ? service.filtrar(dataInicial, dataFinal, categoria, busca) : List.of();
        model.addAttribute("filtrosAtivos", !inicio.isBlank() || !fim.isBlank() || !categoria.isBlank() || !busca.isBlank());
        model.addAttribute("receitas", receitas);
        model.addAttribute("quantidade", receitas.size());
        model.addAttribute("total", service.calcularTotal(receitas));
        return "receitas";
    }

    @GetMapping("/receitas/nova")
    public String nova(Model model) {
        Receita receita = new Receita();
        receita.setData(LocalDate.now(ZoneId.of("America/Recife")));
        model.addAttribute("receita", receita);
        model.addAttribute("modoEdicao", false);
        return "receita-formulario";
    }
    @PostMapping("/receitas")
    public String salvar(@Valid @ModelAttribute("receita") Receita receita, BindingResult erros,
                         Model model, RedirectAttributes redirect) {
        if (erros.hasErrors()) { model.addAttribute("modoEdicao", false); return "receita-formulario"; }
        service.salvar(receita);
        redirect.addFlashAttribute("sucesso", "Receita cadastrada com sucesso");
        return "redirect:/receitas";
    }
    @GetMapping("/receitas/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("receita", service.buscarPorId(id));
            model.addAttribute("modoEdicao", true);
            return "receita-formulario";
        } catch (ReceitaService.ReceitaNaoEncontradaException e) {
            redirect.addFlashAttribute("erro", "A receita não foi encontrada");
            return "redirect:/receitas";
        }
    }
    @PostMapping("/receitas/{id}/editar")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("receita") Receita receita,
                            BindingResult erros, Model model, RedirectAttributes redirect) {
        receita.setId(id);
        try {
            service.buscarPorId(id);
            if (erros.hasErrors()) { model.addAttribute("modoEdicao", true); return "receita-formulario"; }
            service.atualizar(id, receita);
            redirect.addFlashAttribute("sucesso", "Receita atualizada com sucesso");
        } catch (ReceitaService.ReceitaNaoEncontradaException e) {
            redirect.addFlashAttribute("erro", "A receita não foi encontrada");
        }
        return "redirect:/receitas";
    }
    @PostMapping("/receitas/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.excluir(id);
            redirect.addFlashAttribute("sucesso", "Receita excluída com sucesso");
        } catch (ReceitaService.ReceitaNaoEncontradaException e) {
            redirect.addFlashAttribute("erro", "A receita não foi encontrada");
        }
        return "redirect:/receitas";
    }
}
