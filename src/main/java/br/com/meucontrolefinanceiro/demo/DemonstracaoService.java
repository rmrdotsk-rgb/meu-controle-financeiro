package br.com.meucontrolefinanceiro.demo;

import br.com.meucontrolefinanceiro.model.Usuario;
import br.com.meucontrolefinanceiro.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Dados fictícios restritos a uma conta comum, criada somente por configuração local. */
@Service
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemonstracaoService implements ApplicationRunner {
    private final UsuarioRepository usuarios;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final String email;
    private final String senha;

    public DemonstracaoService(UsuarioRepository usuarios, JdbcTemplate jdbc, PasswordEncoder encoder,
            @Value("${app.demo.email}") String email, @Value("${app.demo.password:}") String senha) {
        this.usuarios = usuarios;
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.email = Usuario.normalizarEmail(email);
        this.senha = senha;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email == null || email.length() > 254 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalStateException("Configure um e-mail válido em DEMO_EMAIL.");
        }
        if (senha == null || senha.length() < 8 || senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Configure DEMO_PASSWORD com ao menos 8 caracteres e até 72 bytes UTF-8.");
        }
        Usuario existente = usuarios.findByEmail(email).orElse(null);
        if (existente != null) {
            if (!existente.isDemonstracao()) {
                throw new IllegalStateException("DEMO_EMAIL já pertence a uma conta real. Escolha outro e-mail.");
            }
            // A senha segue a configuração atual; os dados não são recriados em reinicializações.
            existente.setSenhaHash(encoder.encode(senha));
            usuarios.save(existente);
            return;
        }
        Usuario demo = new Usuario();
        demo.setNome("Conta de demonstração");
        demo.setEmail(email);
        demo.setSenhaHash(encoder.encode(senha));
        demo.setDemonstracao(true);
        usuarios.saveAndFlush(demo);
        inserirExemplos(demo.getId());
    }

    @Transactional
    public void restaurar(Usuario autenticado) {
        if (!autenticado.isDemonstracao() || !email.equals(autenticado.getEmail())) {
            throw new AccessDeniedException("A restauração está disponível somente para a própria conta demonstrativa.");
        }
        Long id = autenticado.getId();
        jdbc.queryForObject("SELECT id FROM usuarios WHERE id = ? FOR UPDATE", Long.class, id);
        jdbc.update("DELETE FROM aportes_metas WHERE meta_id IN (SELECT id FROM metas_financeiras WHERE usuario_id = ?)", id);
        jdbc.update("DELETE FROM metas_financeiras WHERE usuario_id = ?", id);
        jdbc.update("DELETE FROM orcamentos WHERE usuario_id = ?", id);
        jdbc.update("DELETE FROM despesas WHERE usuario_id = ?", id);
        jdbc.update("DELETE FROM receitas WHERE usuario_id = ?", id);
        inserirExemplos(id);
    }

    private void inserirExemplos(Long id) {
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Recife"));
        LocalDate inicio = hoje.withDayOfMonth(1);
        jdbc.update("INSERT INTO receitas(descricao,valor,categoria,data,usuario_id) VALUES (?,?,?,?,?)",
                "Bolsa de estágio — exemplo", new BigDecimal("3000.00"), "Salário", inicio, id);
        jdbc.update("INSERT INTO receitas(descricao,valor,categoria,data,usuario_id) VALUES (?,?,?,?,?)",
                "Trabalho eventual — exemplo", new BigDecimal("450.00"), "Freelance", hoje, id);
        despesa(id, "Moradia — exemplo", "900.00", "Casa", inicio);
        despesa(id, "Mercado — exemplo", "250.00", "Alimentação", hoje);
        despesa(id, "Transporte — exemplo", "150.00", "Transporte", hoje);
        despesa(id, "Lazer — exemplo", "39.90", "Lazer", hoje);
        despesa(id, "Mercado do mês anterior — exemplo", "200.00", "Alimentação", inicio.minusMonths(1));
        meta(id, "Curso — exemplo", "2000.00", "500.00", hoje);
        meta(id, "Reserva — exemplo", "5000.00", "1000.00", hoje);
        String mes = String.format(Locale.ROOT, "%04d-%02d", hoje.getYear(), hoje.getMonthValue());
        jdbc.update("INSERT INTO orcamentos(mes,categoria,limite,usuario_id) VALUES (?,?,?,?)", mes, "Alimentação", new BigDecimal("200.00"), id);
        jdbc.update("INSERT INTO orcamentos(mes,categoria,limite,usuario_id) VALUES (?,?,?,?)", mes, "Transporte", new BigDecimal("200.00"), id);
    }

    private void despesa(Long id, String descricao, String valor, String categoria, LocalDate data) {
        jdbc.update("INSERT INTO despesas(descricao,valor,categoria,data,usuario_id) VALUES (?,?,?,?,?)",
                descricao, new BigDecimal(valor), categoria, data, id);
    }

    private void meta(Long id, String nome, String alvo, String aporte, LocalDate hoje) {
        jdbc.update("INSERT INTO metas_financeiras(nome,valor_alvo,data_limite,usuario_id) VALUES (?,?,?,?)",
                nome, new BigDecimal(alvo), hoje.plusMonths(6), id);
        Long metaId = jdbc.queryForObject("SELECT id FROM metas_financeiras WHERE usuario_id = ? AND nome = ?", Long.class, id, nome);
        jdbc.update("INSERT INTO aportes_metas(meta_id,valor,data) VALUES (?,?,?)", metaId, new BigDecimal(aporte), hoje);
    }
}
