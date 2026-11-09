package br.com.meucontrolefinanceiro.controller;

import br.com.meucontrolefinanceiro.model.CadastroForm;
import br.com.meucontrolefinanceiro.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ContaController {
    private final UsuarioService usuarios;
    private final Environment ambiente;

    public ContaController(UsuarioService usuarios, Environment ambiente) {
        this.usuarios = usuarios;
        this.ambiente = ambiente;
    }

    @InitBinder("cadastro")
    public void camposPermitidos(WebDataBinder binder) {
        binder.setAllowedFields("nome", "email", "senha", "confirmacaoSenha");
    }

    @GetMapping("/login")
    public String login(@RequestParam(defaultValue = "false") boolean demo,
                        Authentication autenticacao, HttpSession sessao, Model model) {
        if (autenticado(autenticacao)) { return "redirect:/dashboard"; }
        String email = (String) sessao.getAttribute("emailLogin");
        if (demo && ambiente.getProperty("app.demo.enabled", Boolean.class, false)) {
            email = ambiente.getProperty("app.demo.email", "");
        }
        model.addAttribute("emailLogin", email == null ? "" : email);
        return "login";
    }

    @GetMapping("/cadastro")
    public String cadastro(Authentication autenticacao, Model model) {
        if (autenticado(autenticacao)) { return "redirect:/dashboard"; }
        model.addAttribute("cadastro", new CadastroForm());
        return "cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastrar(@Valid @ModelAttribute("cadastro") CadastroForm dados, BindingResult erros,
                            Authentication autenticacao, HttpSession sessao, RedirectAttributes redirect) {
        if (autenticado(autenticacao)) { return "redirect:/dashboard"; }
        if (erros.hasFieldErrors("confirmacaoCorreta")) {
            erros.rejectValue("confirmacaoSenha", "senha.confirmacao", "A senha e a confirmação devem ser iguais");
        }
        if (erros.hasFieldErrors("tamanhoSenhaValido")) {
            erros.rejectValue("senha", "senha.tamanho", "A senha deve ocupar no máximo 72 bytes; reduza os caracteres acentuados e símbolos");
        }
        if (erros.hasErrors()) { return "cadastro"; }
        try {
            usuarios.cadastrar(dados);
        } catch (UsuarioService.EmailJaCadastradoException e) {
            erros.rejectValue("email", "email.duplicado", "Já existe uma conta com este e-mail");
            return "cadastro";
        }
        redirect.addFlashAttribute("sucesso", "Conta criada. Entre com seu e-mail e senha.");
        sessao.setAttribute("emailLogin", dados.getEmail());
        return "redirect:/login";
    }

    @GetMapping("/conta")
    public String conta() { return "conta"; }

    private boolean autenticado(Authentication autenticacao) {
        return autenticacao != null && autenticacao.isAuthenticated() && !(autenticacao instanceof AnonymousAuthenticationToken);
    }
}
