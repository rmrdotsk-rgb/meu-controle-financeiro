package br.com.meucontrolefinanceiro.controller;

import br.com.meucontrolefinanceiro.model.Despesa;
import br.com.meucontrolefinanceiro.service.DespesaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import java.time.format.DateTimeParseException;
import java.time.ZoneId;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
public class DespesaController {

    private final DespesaService service;

    public DespesaController(DespesaService service) {
        this.service = service;
    }

    @InitBinder("despesa")
    public void camposPermitidos(WebDataBinder binder) {
        binder.setAllowedFields("descricao", "valor", "categoria", "data");
    }

    @GetMapping("/")
    public String inicio() {
        return "redirect:/dashboard";
    }

    @GetMapping("/despesas")
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
        List<Despesa> despesas = valido ? service.filtrar(dataInicial, dataFinal, categoria, busca) : List.of();
        model.addAttribute("filtrosAtivos", !inicio.isBlank() || !fim.isBlank() || !categoria.isBlank() || !busca.isBlank());
        model.addAttribute("despesas", despesas);
        model.addAttribute("quantidade", despesas.size());
        model.addAttribute("total", service.calcularTotal(despesas));
        return "despesas";
    }

    @GetMapping("/despesas/nova")
    public String nova(Model model) {
        Despesa despesa = new Despesa();
        despesa.setData(LocalDate.now(ZoneId.of("America/Recife")));
        model.addAttribute("despesa", despesa);
        model.addAttribute("modoEdicao", false);
        return "formulario";
    }

    @PostMapping("/despesas")
    public String salvar(@Valid @ModelAttribute("despesa") Despesa despesa,
                         BindingResult resultado,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            model.addAttribute("modoEdicao", false);
            return "formulario";
        }
        // O ID é definido pelo banco; não aceite um ID enviado no formulário de criação.
        despesa.setId(null);
        service.salvar(despesa);
        redirectAttributes.addFlashAttribute("sucesso", "Despesa cadastrada com sucesso");
        return "redirect:/despesas";
    }

    @GetMapping("/despesas/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("despesa", service.buscarPorId(id));
            model.addAttribute("modoEdicao", true);
            return "formulario";
        } catch (DespesaService.DespesaNaoEncontradaException exception) {
            redirectAttributes.addFlashAttribute("erro", "A despesa não foi encontrada");
            return "redirect:/despesas";
        }
    }

    @PostMapping("/despesas/{id}/editar")
    public String atualizar(@PathVariable Long id,
                            @Valid @ModelAttribute("despesa") Despesa despesa,
                            BindingResult resultado,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        despesa.setId(id);
        if (resultado.hasErrors()) {
            model.addAttribute("modoEdicao", true);
            return "formulario";
        }
        try {
            service.atualizar(id, despesa);
            redirectAttributes.addFlashAttribute("sucesso", "Despesa atualizada com sucesso");
        } catch (DespesaService.DespesaNaoEncontradaException exception) {
            redirectAttributes.addFlashAttribute("erro", "A despesa não foi encontrada");
        }
        return "redirect:/despesas";
    }

    @PostMapping("/despesas/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            service.excluir(id);
            redirectAttributes.addFlashAttribute("sucesso", "Despesa excluída com sucesso");
        } catch (DespesaService.DespesaNaoEncontradaException exception) {
            redirectAttributes.addFlashAttribute("erro", "A despesa não foi encontrada");
        }
        return "redirect:/despesas";
    }
}
