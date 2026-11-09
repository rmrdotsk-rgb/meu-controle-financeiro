package br.com.meucontrolefinanceiro.service;

import br.com.meucontrolefinanceiro.model.CadastroForm;
import br.com.meucontrolefinanceiro.model.Usuario;
import br.com.meucontrolefinanceiro.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder senhas) {
        this.usuarios = usuarios;
        this.senhas = senhas;
    }

    @Transactional
    public Usuario cadastrar(@Valid CadastroForm dados) {
        String email = Usuario.normalizarEmail(dados.getEmail());
        if (usuarios.existsByEmail(email)) { throw new EmailJaCadastradoException(); }
        Usuario usuario = new Usuario();
        usuario.setNome(dados.getNome());
        usuario.setEmail(email);
        usuario.setSenhaHash(senhas.encode(dados.getSenha()));
        usuario.setDemonstracao(false);
        try {
            // A restrição única também protege cadastros concorrentes.
            return usuarios.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            throw new EmailJaCadastradoException();
        }
    }

    public static class EmailJaCadastradoException extends RuntimeException {}
}
