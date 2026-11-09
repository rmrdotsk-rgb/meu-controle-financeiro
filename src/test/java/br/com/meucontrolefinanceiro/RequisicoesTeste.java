package br.com.meucontrolefinanceiro;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/** Representa o token CSRF que os formulários Thymeleaf recebem da sessão. */
public final class RequisicoesTeste {
    private RequisicoesTeste() {}

    public static MockHttpServletRequestBuilder post(String caminho, Object... variaveis) {
        return MockMvcRequestBuilders.post(caminho, variaveis).with(csrf());
    }
}
