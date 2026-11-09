package br.com.meucontrolefinanceiro.demo;

import br.com.meucontrolefinanceiro.service.UsuarioAtual;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemonstracaoController {
    private final DemonstracaoService demonstracao;
    private final UsuarioAtual atual;

    public DemonstracaoController(DemonstracaoService demonstracao, UsuarioAtual atual) {
        this.demonstracao = demonstracao;
        this.atual = atual;
    }

    @PostMapping("/demonstracao/restaurar")
    public String restaurar(RedirectAttributes redirect) {
        demonstracao.restaurar(atual.obter());
        redirect.addFlashAttribute("sucesso", "Demonstração restaurada com dados fictícios de exemplo.");
        return "redirect:/dashboard";
    }
}
