# Meu Controle Financeiro

Aplicação web local para registrar receitas e despesas, acompanhar resultados por mês, organizar metas e definir limites por categoria.

Projeto de Emerson Saturno para a disciplina Back-End Frameworks, UNINASSAU Recife, 2026.2. Professor: Maurício Braga.

## Funcionalidades

- Despesas e receitas: cadastro, consulta, edição, exclusão e filtros por descrição, categoria e datas, com soma dos resultados.
- Visão geral: receitas, despesas, saldo do período, gastos de hoje, categorias, histórico diário e comparação mensal.
- Metas: criação, edição, exclusão, registro e remoção de aportes, progresso e valor restante.
- Planejamento: um limite por categoria/mês, atualização sem duplicação e avisos de aproximação, limite atingido e excesso.

Todos os valores vêm do banco. A aplicação começa vazia. Saldo é a diferença entre receitas e despesas do período, sem representar conta bancária. Aportes registram reservas declaradas e não criam despesas adicionais.

## Tecnologias e requisitos

Java 21, Spring Boot 4.1.2, Spring MVC, Thymeleaf, Spring Data JPA/Hibernate, Bean Validation, H2, Spring Security para CSRF e Maven Wrapper. Testes com JUnit e MockMvc.

É necessário JDK 21, navegador e internet no primeiro build para obter Maven e dependências. Depois de baixar tudo e compilar, a aplicação funciona localmente. Não depende de fontes, scripts ou serviços externos na interface.

## Executar no Windows

Extraia o ZIP em uma pasta e abra nela o PowerShell. A raiz deve conter `pom.xml`, `mvnw.cmd` e `src`.

```powershell
java -version
.\mvnw.cmd clean test
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```

Abra **http://localhost:8080/dashboard**. Para encerrar, use `Ctrl+C` no terminal.

Se o Java estiver instalado, mas não for reconhecido no terminal, configure apenas esta sessão, ajustando o caminho à instalação:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Também é possível iniciar por `Iniciar.cmd`, que procura o JDK instalado e mantém o terminal aberto. Para repetir os testes e verificar persistência em banco temporário:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\Testar-No-Windows.ps1
```

O script informa onde salvou os logs e não usa o banco pessoal. No Linux/macOS, use `./mvnw` no lugar de `.\mvnw.cmd`; se necessário, execute `chmod +x mvnw`.

## Persistência e uso local

O banco normal fica em `data/meucontrole.mv.db`, criado no primeiro uso. Execute sempre a partir da mesma pasta para reutilizá-lo. As gravações usam `WRITE_DELAY=0` para sincronizar o registro de transações sem a espera padrão do H2. Para encerrar normalmente, use `Ctrl+C`; para backup, encerre o servidor antes de copiar esse arquivo. Os testes JUnit usam H2 em memória, separado do banco normal.

A aplicação atende apenas em `127.0.0.1`; o console H2 está desativado. Formulários usam sessão e tokens CSRF. Não há login, cadastro de contas, segregação por usuário ou integração bancária. Qualquer pessoa com acesso ao computador pode usar os dados. Esse é um projeto local de usuário único; publicar na internet exigiria outro modelo de acesso e configuração.

Não inclua `target/`, bancos `.mv.db`, logs, credenciais ou caches em entregas públicas. O `.gitignore` já os exclui.

## Organização

- `model`: entidades e validações.
- `repository`: consultas e persistência.
- `service`: regras, cálculos e transações.
- `controller`: requisições, formulários e mensagens.
- `config`: proteção dos formulários.
- `templates` e `static/css`: interface responsiva.
- `src/test`: testes automatizados.
- `docs`: documento acadêmico em PDF e Markdown, histórias e roteiro de apresentação.

As quatro histórias obrigatórias são cadastrar, consultar, editar e excluir despesas. As demais funcionalidades ampliam esse escopo. O documento está identificado com Emerson Saturno; revise e imprima `docs/DOCUMENTO_ACADEMICO.pdf` para a apresentação. O roteiro demonstra primeiro o CRUD obrigatório.
