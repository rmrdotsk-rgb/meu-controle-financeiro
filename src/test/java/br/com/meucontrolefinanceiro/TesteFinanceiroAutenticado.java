package br.com.meucontrolefinanceiro;

import br.com.meucontrolefinanceiro.model.Despesa;
import br.com.meucontrolefinanceiro.model.Receita;
import br.com.meucontrolefinanceiro.model.Usuario;
import br.com.meucontrolefinanceiro.repository.DespesaRepository;
import br.com.meucontrolefinanceiro.repository.ReceitaRepository;
import br.com.meucontrolefinanceiro.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/** Mantém a cobertura financeira anterior usando um proprietário persistido. */
@WithMockUser(username = TesteFinanceiroAutenticado.EMAIL)
public abstract class TesteFinanceiroAutenticado {
    public static final String EMAIL = "financeiro@teste.local";

    @Autowired private UsuarioRepository usuariosTeste;
    @Autowired private PasswordEncoder encoderTeste;
    @Autowired private DespesaRepository despesasTeste;
    @Autowired private ReceitaRepository receitasTeste;
    @Autowired private WebApplicationContext contextoTeste;

    protected Usuario usuarioTeste;
    protected MockMvc mockMvc;
    protected MockMvc mvc;

    @BeforeEach
    void prepararProprietario() {
        usuarioTeste = usuariosTeste.findByEmail(EMAIL).orElseGet(() -> {
            Usuario usuario = new Usuario();
            usuario.setNome("Usuário de teste financeiro");
            usuario.setEmail(EMAIL);
            usuario.setSenhaHash(encoderTeste.encode("SenhaTeste!2026"));
            return usuariosTeste.save(usuario);
        });
        // O filtro limpa o contexto da requisição. Estes testes também consultam serviços
        // diretamente, então restauram o usuário de teste após cada resposta do MockMvc.
        mockMvc = MockMvcBuilders.webAppContextSetup(contextoTeste).apply(springSecurity())
                .alwaysDo(resultado -> SecurityContextHolder.setContext(TestSecurityContextHolder.getContext()))
                .build();
        mvc = mockMvc;
    }

    protected Despesa salvar(Despesa despesa) {
        despesa.setUsuario(usuarioTeste);
        return despesasTeste.save(despesa);
    }

    protected Receita salvar(Receita receita) {
        receita.setUsuario(usuarioTeste);
        return receitasTeste.save(receita);
    }
}
