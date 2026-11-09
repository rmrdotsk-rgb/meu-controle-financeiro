package br.com.meucontrolefinanceiro.model;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Dados de entrada do cadastro; nunca é persistido nem recebe privilégios. */
public class CadastroForm {
    @NotBlank(message = "Informe seu nome completo")
    @Size(min = 3, max = 80, message = "O nome deve ter entre 3 e 80 caracteres")
    private String nome;

    @NotBlank(message = "Informe seu e-mail")
    @Email(message = "Informe um e-mail válido")
    @Size(max = 254, message = "O e-mail deve ter até 254 caracteres")
    private String email;

    @NotBlank(message = "Informe uma senha")
    @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
    private String senha;

    @NotBlank(message = "Confirme sua senha")
    @Size(max = 72, message = "A confirmação deve ter até 72 caracteres")
    private String confirmacaoSenha;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome == null ? null : nome.strip(); }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = Usuario.normalizarEmail(email); }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public String getConfirmacaoSenha() { return confirmacaoSenha; }
    public void setConfirmacaoSenha(String confirmacaoSenha) { this.confirmacaoSenha = confirmacaoSenha; }

    @AssertTrue(message = "A senha e a confirmação devem ser iguais")
    public boolean isConfirmacaoCorreta() {
        return senha == null || confirmacaoSenha == null || Objects.equals(senha, confirmacaoSenha);
    }

    @AssertTrue(message = "A senha deve ocupar no máximo 72 bytes; caracteres acentuados e símbolos podem ocupar mais de um byte")
    public boolean isTamanhoSenhaValido() {
        return senha == null || senha.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
