# Meu Controle Financeiro

Aplicação web local para registrar receitas e despesas, acompanhar resultados por mês, organizar metas e definir limites por categoria, com contas individuais.

Projeto de Emerson Saturno para a disciplina Back-End Frameworks, UNINASSAU Recife, 2026.2. Professor: Maurício Braga.

## Funcionalidades

- Cadastro de conta com nome, e-mail único normalizado, senha e confirmação; senha armazenada como hash BCrypt.
- Login por e-mail/senha, sessão, informações básicas da conta e logout.
- Despesas e receitas: cadastro, consulta, edição, exclusão e filtros por descrição, categoria e datas, com soma dos resultados.
- Visão geral: receitas, despesas, saldo do período, gastos de hoje, categorias, histórico diário e comparação mensal.
- Metas: criação, edição, exclusão, registro e remoção de aportes, progresso e valor restante.
- Planejamento: um limite por usuário/categoria/mês, atualização sem duplicação e avisos de aproximação, limite atingido e excesso.
- Demonstração opcional, identificada na tela, com conta e registros fictícios separados das contas reais.

As consultas e alterações usam a identidade autenticada. Uma conta real começa vazia; não recebe registros antigos sem proprietário nem exemplos da demonstração. Aportes pertencem ao proprietário da meta e não geram despesas adicionais. O saldo é receitas menos despesas do período, sem representar uma conta bancária.

## Tecnologias e requisitos

Java 21, Spring Boot 4.1.2, Spring MVC, Thymeleaf, Spring Data JPA/Hibernate, Bean Validation, H2, Flyway, Spring Security e Maven Wrapper. Testes com JUnit e MockMvc.

É necessário JDK 21, navegador e internet no primeiro build para obter Maven e dependências. Depois de baixar tudo e compilar, a aplicação funciona localmente. A interface não depende de fontes, scripts ou serviços externos.

## Executar no Windows 11

Na pasta que contém `pom.xml`, `mvnw.cmd` e `src`, abra o PowerShell:

```powershell
java -version
.\mvnw.cmd clean test
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

Abra [http://127.0.0.1:8080/login](http://127.0.0.1:8080/login), escolha **Criar conta**, preencha os dados e entre com o e-mail cadastrado. A senha deve ter pelo menos 8 caracteres e no máximo 72 bytes em UTF-8; acentos e símbolos podem ocupar mais de um byte. E-mail repetido é rejeitado. Em caso de erro, nome/e-mail são preservados, mas as senhas precisam ser digitadas novamente.

Para encerrar o servidor, use `Ctrl+C` no terminal. `clean verify` também gera o JAR em `target/`. A execução sem Maven, após essa compilação, pode ser feita com `java -jar target/meu-controle-financeiro-0.0.1-SNAPSHOT.jar`.

Se o Java estiver instalado, mas não for reconhecido, ajuste apenas esta sessão, usando o caminho real do seu JDK 21:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Também é possível iniciar por `Iniciar.cmd`, que procura o JDK instalado e executa `tools/Iniciar.ps1`. Quando há banco existente, o iniciador verifica acesso exclusivo e faz backup com checksum fora do repositório, em `%LOCALAPPDATA%\MeuControleFinanceiro\Backups`, antes da inicialização. Se usar Maven/JAR diretamente, faça o backup offline antes de migrar. No Linux/macOS, use `./mvnw`; se necessário, execute `chmod +x mvnw`.

## Demonstração acadêmica opcional

A demonstração fica **desativada por padrão**. Para ativá-la localmente, configure uma senha no próprio terminal, sem gravá-la no repositório ou em um arquivo de instruções:

```powershell
$env:DEMO_ENABLED = 'true'
$env:DEMO_EMAIL = 'demo@meucontrole.local'
$senhaDemonstracao = Read-Host 'Senha local da demonstracao (minimo 8 caracteres)' -AsSecureString
$env:DEMO_PASSWORD = [System.Net.NetworkCredential]::new('', $senhaDemonstracao).Password
try {
    .\mvnw.cmd spring-boot:run
} finally {
    Remove-Item Env:DEMO_PASSWORD -ErrorAction SilentlyContinue
    $senhaDemonstracao.Dispose()
}
```

Na tela de login, clique em **Usar e-mail de demonstração** e digite a senha que acabou de configurar. A senha não aparece no HTML nem é preenchida automaticamente. Essa conta possui receitas, despesas, metas, aportes e limites fictícios; o dashboard calcula seus valores a partir desses registros.

Com a demonstração habilitada, a inicialização exige e-mail válido e senha de pelo menos 8 caracteres e até 72 bytes UTF-8. A ausência de senha impede a inicialização dessa configuração. Se o e-mail já pertencer a uma conta real, a inicialização falha sem convertê-la em demonstração. Escolha outro e-mail.

Reinicializar não duplica nem restaura os registros da demonstração. A senha da conta demonstrativa acompanha a configuração local informada na inicialização. Para retornar aos exemplos iniciais, entre nessa conta, abra **Minha conta** e use **Restaurar dados de demonstração**. A ação exige confirmação, sessão demonstrativa e CSRF; afeta apenas os registros da própria demonstração. Ela não concede permissões administrativas.

Para desativar no próximo início:

```powershell
$env:DEMO_ENABLED = 'false'
Remove-Item Env:DEMO_PASSWORD -ErrorAction SilentlyContinue
.\mvnw.cmd spring-boot:run
```

A conta demonstrativa deixa de aceitar login quando o recurso está desativado. Os dados continuam no banco, separados das contas reais. Não use informação pessoal na demonstração.

## Persistência e migração

O banco normal fica em `data/meucontrole.mv.db`. Execute sempre a partir da mesma pasta para reutilizá-lo. As gravações usam `WRITE_DELAY=0`. Encerre o servidor antes de fazer backup ou substituir o arquivo; não copie um banco aberto.

Flyway mantém migrações versionadas: V1 representa o esquema anterior; V2 acrescenta usuários, vínculos e unicidade do orçamento por proprietário. Em banco antigo reconhecido, o baseline é a versão 1. Em banco vazio, V1 e V2 são aplicadas. Hibernate usa `ddl-auto=validate`, sem recriar o esquema. O comando de limpeza do Flyway está desabilitado.

Registros antigos com `usuario_id` nulo são preservados em quarentena: não aparecem em contas novas, no dashboard nem na demonstração. A recuperação exige comprovação do proprietário, backup offline e associação explícita de IDs específicos em transação. Não atribua tudo ao primeiro usuário. Leia [o procedimento de migração e recuperação](docs/MIGRACAO.md) antes de abrir uma base antiga com a nova versão.

## Sessão e uso local

O servidor atende apenas em `127.0.0.1`; o console H2 está desativado. Páginas financeiras exigem autenticação. Formulários POST, inclusive login, cadastro, logout e restauração demonstrativa, usam CSRF. O ID da sessão é renovado após login; logout invalida a sessão. Cookies usam `HttpOnly` e `SameSite=Strict`, com expiração por 30 minutos de inatividade.

O endereço local utiliza HTTP. Não há configuração de publicação, TLS, JWT, OAuth, recuperação de senha ou integração bancária. Quem administra o computador ou copia o arquivo H2 pode acessar o banco; ele não é um cofre criptografado. Esta é uma aplicação acadêmica local, sem compromisso de operação pública em produção.

## Testes

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean verify
powershell -ExecutionPolicy Bypass -File .\tools\Testar-No-Windows.ps1
```

Os testes usam bancos temporários separados da base pessoal. Há cenários para autenticação, CSRF, CRUD, filtros, cálculos, isolamento entre contas, demonstração e migração. A quantidade e os resultados observados pertencem ao registro de validação da entrega; a presença de testes não substitui sua execução. Confirme também as páginas no navegador em 320, 390 e desktop, e a permanência dos dados após reiniciar com o mesmo banco.

## Organização e entrega

- `model`: entidades e validações; `repository`: consultas limitadas por proprietário.
- `service`: regras, identidade atual, cálculos e transações.
- `controller`: requisições, formulários, mensagens e informações da conta.
- `config`: autenticação, sessão e proteção dos formulários.
- `demo`: ativação explícita e restauração dos exemplos fictícios.
- `src/main/resources/db/migration`: migrações SQL versionadas.
- `templates` e `static/css`: interface responsiva; `src/test`: testes.
- `docs`: documento acadêmico em PDF/Markdown, requisitos, roteiro e migração.

Não inclua `target/`, bancos `.mv.db`, backups, logs, credenciais ou caches nos commits. As quatro histórias obrigatórias continuam sendo cadastrar, consultar, editar e excluir despesas. O documento identifica Emerson Saturno e acrescenta cadastro de conta, autenticação e isolamento às histórias anteriores. Revise e imprima `docs/DOCUMENTO_ACADEMICO.pdf` para a apresentação.
