package br.com.meucontrolefinanceiro.config;

import br.com.meucontrolefinanceiro.model.Usuario;
import br.com.meucontrolefinanceiro.repository.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder codificadorDeSenhas() {
        PasswordEncoder bcrypt = new BCryptPasswordEncoder();
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence senha) { return bcrypt.encode(senha); }

            @Override
            public boolean matches(CharSequence senha, String hash) {
                // O limite do BCrypt vale também no login, que não passa pelo DTO de cadastro.
                if (senha == null || senha.toString().getBytes(StandardCharsets.UTF_8).length > 72) { return false; }
                return bcrypt.matches(senha, hash);
            }

            @Override
            public boolean upgradeEncoding(String hash) { return bcrypt.upgradeEncoding(hash); }
        };
    }

    @Bean
    UserDetailsService detalhesDoUsuario(UsuarioRepository usuarios, Environment ambiente) {
        return email -> {
            Usuario usuario = usuarios.findByEmail(Usuario.normalizarEmail(email))
                    .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
            if (usuario.isDemonstracao() && !ambiente.getProperty("app.demo.enabled", Boolean.class, false)) {
                throw new UsernameNotFoundException("Credenciais inválidas");
            }
            return User.withUsername(usuario.getEmail()).password(usuario.getSenhaHash()).roles("USUARIO").build();
        };
    }

    @Bean
    SecurityFilterChain segurancaLocal(HttpSecurity http) throws Exception {
        var falhaLogin = new SimpleUrlAuthenticationFailureHandler("/login?error");
        return http
                .authorizeHttpRequests(autorizacao -> autorizacao
                        .requestMatchers("/login", "/cadastro", "/css/**", "/favicon.ico", "/error").permitAll()
                        .anyRequest().authenticated())
                .formLogin(login -> login.loginPage("/login").loginProcessingUrl("/login")
                        .usernameParameter("email").passwordParameter("senha")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureHandler((request, response, exception) -> {
                            String email = Usuario.normalizarEmail(request.getParameter("email"));
                            request.getSession().setAttribute("emailLogin", email == null ? "" : email.substring(0, Math.min(email.length(), 254)));
                            falhaLogin.onAuthenticationFailure(request, response, exception);
                        }).permitAll())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(sessao -> sessao.sessionFixation(fixacao -> fixacao.changeSessionId()))
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true).clearAuthentication(true).deleteCookies("JSESSIONID"))
                .build();
    }
}
