package br.com.meucontrolefinanceiro;

import br.com.meucontrolefinanceiro.model.Usuario;
import br.com.meucontrolefinanceiro.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static br.com.meucontrolefinanceiro.RequisicoesTeste.post;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercita o filtro de login real, mantendo duas sessões independentes. */
public abstract class CenariosContasTeste {
    protected static final String SENHA = "SenhaTeste!2026";

    @Autowired protected MockMvc mvc;
    @Autowired protected UsuarioRepository usuarios;
    @Autowired protected DespesaRepository despesas;
    @Autowired protected ReceitaRepository receitas;
    @Autowired protected MetaFinanceiraRepository metas;
    @Autowired protected AporteMetaRepository aportes;
    @Autowired protected OrcamentoRepository orcamentos;
    @Autowired protected PasswordEncoder encoder;

    @BeforeEach
    void limparContasEDados() {
        aportes.deleteAll();
        metas.deleteAll();
        orcamentos.deleteAll();
        receitas.deleteAll();
        despesas.deleteAll();
        usuarios.deleteAll();
    }

    protected Usuario usuario(String nome, String email) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenhaHash(encoder.encode(SENHA));
        return usuarios.save(usuario);
    }

    protected MockHttpSession entrar(String email) throws Exception {
        var resultado = mvc.perform(post("/login").param("email", email).param("senha", SENHA))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(authenticated().withUsername(email))
                .andReturn();
        return (MockHttpSession) resultado.getRequest().getSession(false);
    }

    protected void despesa(MockHttpSession sessao, String descricao, String valor, String categoria, String data) throws Exception {
        mvc.perform(post("/despesas").session(sessao).param("descricao", descricao).param("valor", valor)
                .param("categoria", categoria).param("data", data))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("sucesso"));
    }

    protected void receita(MockHttpSession sessao, String descricao, String valor) throws Exception {
        mvc.perform(post("/receitas").session(sessao).param("descricao", descricao).param("valor", valor)
                .param("categoria", "Outros").param("data", "2026-11-09"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("sucesso"));
    }

    protected void meta(MockHttpSession sessao, String nome) throws Exception {
        mvc.perform(post("/metas").session(sessao).param("nome", nome).param("valorAlvo", "1000.00")
                .param("dataLimite", "2027-01-30"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("sucesso"));
    }

    protected void limite(MockHttpSession sessao, String valor) throws Exception {
        mvc.perform(post("/orcamentos").session(sessao).param("mes", "2026-11")
                .param("categoria", "Casa").param("limite", valor))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("sucesso"));
    }
}
