package br.com.meucontrolefinanceiro.service;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MigracaoBancoTest {
    @TempDir Path pasta;

    @Test
    void bancoVazioRecebeEstruturaVersionadaSemDadosFinanceirosOuUsuariosInventados() throws Exception {
        String url = "jdbc:h2:mem:migracao-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        migrar(url);
        try (Connection conexao = DriverManager.getConnection(url, "sa", "")) {
            for (String tabela : new String[]{"despesas", "receitas", "metas_financeiras", "aportes_metas", "orcamentos", "usuarios"}) {
                assertEquals(0, numero(conexao, "SELECT COUNT(*) FROM " + tabela));
            }
            assertEquals(2, numero(conexao, "SELECT COUNT(*) FROM \"flyway_schema_history\" WHERE \"success\" = TRUE AND \"type\" = 'SQL'"));
        }
    }

    @Test
    void bancoAntigoSemHistoricoMigraPreservandoTodosOsCamposRelacionamentosEQuarentenaAposReabrir() throws Exception {
        String url = "jdbc:h2:file:" + pasta.resolve("financeiro-antigo").toAbsolutePath().toString().replace('\\', '/');
        try (Connection conexao = DriverManager.getConnection(url, "sa", "")) {
            ScriptUtils.executeSqlScript(conexao, new ClassPathResource("db/migration/V1__estrutura_financeira_original.sql"));
            sql(conexao, "INSERT INTO despesas(id,categoria,data,descricao,valor) VALUES(101,'Casa','2026-10-09','Despesa original',123.45)");
            sql(conexao, "INSERT INTO receitas(id,categoria,data,descricao,valor) VALUES(202,'Outros','2026-10-08','Receita original',900.25)");
            sql(conexao, "INSERT INTO metas_financeiras(id,data_limite,nome,valor_alvo) VALUES(303,'2027-01-01','Meta original',5000.50)");
            sql(conexao, "INSERT INTO aportes_metas(id,data,valor,meta_id) VALUES(404,'2026-10-07',250.75,303)");
            sql(conexao, "INSERT INTO orcamentos(id,categoria,limite,mes) VALUES(505,'Casa',300.00,'2026-10')");
        }
        migrar(url);
        // Nova conexão após o encerramento anterior: verifica o próprio arquivo persistente.
        try (Connection conexao = DriverManager.getConnection(url, "sa", "")) {
            assertEquals(1, numero(conexao, "SELECT COUNT(*) FROM despesas WHERE id=101 AND categoria='Casa' AND data='2026-10-09' AND descricao='Despesa original' AND valor=123.45 AND usuario_id IS NULL"));
            assertEquals(1, numero(conexao, "SELECT COUNT(*) FROM receitas WHERE id=202 AND categoria='Outros' AND data='2026-10-08' AND descricao='Receita original' AND valor=900.25 AND usuario_id IS NULL"));
            assertEquals(1, numero(conexao, "SELECT COUNT(*) FROM metas_financeiras WHERE id=303 AND data_limite='2027-01-01' AND nome='Meta original' AND valor_alvo=5000.50 AND usuario_id IS NULL"));
            assertEquals(1, numero(conexao, "SELECT COUNT(*) FROM aportes_metas WHERE id=404 AND data='2026-10-07' AND valor=250.75 AND meta_id=303"));
            assertEquals(1, numero(conexao, "SELECT COUNT(*) FROM orcamentos WHERE id=505 AND categoria='Casa' AND limite=300.00 AND mes='2026-10' AND usuario_id IS NULL"));
            assertEquals(0, numero(conexao, "SELECT COUNT(*) FROM usuarios"));
            assertEquals(1, numero(conexao, "SELECT COUNT(*) FROM \"flyway_schema_history\" WHERE \"type\"='BASELINE' AND \"version\"='1'"));
            sql(conexao, "INSERT INTO usuarios(id,nome,email,senha_hash,demonstracao) VALUES(10,'Conta A','a@example.test','hash-apenas-fixture',false)");
            sql(conexao, "INSERT INTO usuarios(id,nome,email,senha_hash,demonstracao) VALUES(20,'Conta B','b@example.test','hash-apenas-fixture',false)");
            sql(conexao, "INSERT INTO orcamentos(categoria,limite,mes,usuario_id) VALUES('Casa',100.00,'2026-10',10)");
            sql(conexao, "INSERT INTO orcamentos(categoria,limite,mes,usuario_id) VALUES('Casa',200.00,'2026-10',20)");
            assertThrows(java.sql.SQLException.class, () -> sql(conexao, "INSERT INTO orcamentos(categoria,limite,mes,usuario_id) VALUES('Casa',999.00,'2026-10',10)"));
            assertEquals(1, numero(conexao, "SELECT COUNT(*) FROM orcamentos WHERE usuario_id IS NULL"));
        }
        migrar(url);
        try (Connection conexao = DriverManager.getConnection(url, "sa", "")) {
            assertEquals(1, numero(conexao, "SELECT COUNT(*) FROM despesas WHERE id=101 AND usuario_id IS NULL"));
            assertEquals(1, numero(conexao, "SELECT COUNT(*) FROM aportes_metas WHERE id=404 AND meta_id=303"));
            assertEquals(3, numero(conexao, "SELECT COUNT(*) FROM orcamentos"));
        }
    }

    private static void migrar(String url) {
        Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration")
                .baselineOnMigrate(true).baselineVersion("1").load().migrate();
    }

    private static void sql(Connection conexao, String sql) throws Exception {
        try (var comando = conexao.createStatement()) { comando.execute(sql); }
    }

    private static int numero(Connection conexao, String sql) throws Exception {
        try (var comando = conexao.createStatement(); ResultSet resultado = comando.executeQuery(sql)) {
            assertTrue(resultado.next());
            return resultado.getInt(1);
        }
    }
}
