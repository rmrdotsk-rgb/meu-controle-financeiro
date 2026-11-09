package br.com.meucontrolefinanceiro.service;

import br.com.meucontrolefinanceiro.model.Usuario;
import br.com.meucontrolefinanceiro.repository.UsuarioRepository;
import org.springframework.core.env.Environment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioAtual {
    private final UsuarioRepository usuarios;
    private final Environment ambiente;

    public UsuarioAtual(UsuarioRepository usuarios, Environment ambiente) {
        this.usuarios = usuarios;
        this.ambiente = ambiente;
    }

    @Transactional(readOnly = true)
    public Usuario obter() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated() || autenticacao instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Autenticação necessária");
        }
        Usuario usuario = usuarios.findByEmail(Usuario.normalizarEmail(autenticacao.getName()))
                .orElseThrow(() -> new AccessDeniedException("Conta indisponível"));
        if (usuario.isDemonstracao() && !ambiente.getProperty("app.demo.enabled", Boolean.class, false)) {
            throw new AccessDeniedException("Demonstração indisponível");
        }
        return usuario;
    }

    public Long id() { return obter().getId(); }
}
