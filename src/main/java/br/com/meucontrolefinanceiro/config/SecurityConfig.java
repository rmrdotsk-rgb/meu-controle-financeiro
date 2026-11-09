package br.com.meucontrolefinanceiro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** Protege formulários locais contra CSRF, sem oferecer autenticação ou contas. */
@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain segurancaLocal(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(autorizacao -> autorizacao.anyRequest().permitAll())
                .formLogin(login -> login.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .build();
    }
}
