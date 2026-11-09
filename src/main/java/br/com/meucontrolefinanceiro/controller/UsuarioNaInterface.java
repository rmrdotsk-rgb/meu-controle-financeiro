package br.com.meucontrolefinanceiro.controller;

import br.com.meucontrolefinanceiro.model.Usuario;
import br.com.meucontrolefinanceiro.service.UsuarioAtual;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class UsuarioNaInterface {
    private final UsuarioAtual atual;

    public UsuarioNaInterface(UsuarioAtual atual) { this.atual = atual; }

    @ModelAttribute
    public void identificar(Authentication autenticacao, Model model) {
        if (autenticacao != null && autenticacao.isAuthenticated() && !(autenticacao instanceof AnonymousAuthenticationToken)) {
            Usuario usuario = atual.obter();
            model.addAttribute("usuarioNome", usuario.getNome());
            model.addAttribute("usuarioEmail", usuario.getEmail());
            model.addAttribute("usuarioDemo", usuario.isDemonstracao());
        } else {
            model.addAttribute("usuarioDemo", false);
        }
    }
}
