# Meu Controle Financeiro

**Aplicação web para organização de receitas, despesas e metas**

Instituição: UNINASSAU Recife
Disciplina: Back-End Frameworks - 2026.2
Professor: Maurício Braga
Integrante: Emerson Saturno

## 1. Objetivo

O Meu Controle Financeiro é uma aplicação web local para registrar receitas, despesas e metas, acompanhar resultados mensais e planejar gastos por categoria. A evolução acrescenta contas individuais, autenticação por sessão e acesso limitado ao proprietário de cada registro. Contas reais começam vazias; a demonstração opcional utiliza uma conta separada, identificada com dados fictícios.

## 2. Relação com a disciplina

A implementação demonstra aplicação web funcional, interface responsiva, operações reais de leitura e escrita em banco relacional e histórias de usuário. As quatro histórias principais continuam sendo cadastrar, consultar, editar e excluir despesas. Os recursos de receitas, metas, planejamento e autenticação ampliam esse escopo. Este documento acompanha a apresentação e demonstração do sistema.

## 3. Tecnologias e arquitetura

Java 21, Spring Boot 4.1.2, Spring MVC, Thymeleaf, Spring Data JPA, Hibernate, H2 persistente em arquivo, Bean Validation, Spring Security, Flyway, Maven Wrapper e testes JUnit/MockMvc.

Fluxo de uma requisição autenticada:

```text
Navegador -> Spring Security -> Controller -> Service -> Repository -> JPA/Hibernate -> H2
```

Spring Security verifica a sessão e protege os formulários. Controllers recebem requisições; serviços aplicam regras, identificam o usuário atual e organizam transações; repositories consultam os registros desse proprietário. Models representam entidades e validações; Thymeleaf produz o HTML no servidor. Nenhum formulário pode escolher o proprietário ou conceder privilégios.

## 4. Modelo de dados

| Entidade | Campos principais | Uso |
|---|---|---|
| Usuario | id, nome, e-mail único, hash de senha, demonstração | Autenticar e identificar proprietário |
| Despesa | id, descrição, valor, categoria, data, usuário | Registrar gastos próprios |
| Receita | id, descrição, valor, categoria, data, usuário | Registrar entradas próprias |
| MetaFinanceira | id, nome, valor-alvo, prazo, usuário | Definir objetivos próprios |
| AporteMeta | id, meta, valor, data | Reservar valores na meta do proprietário |
| OrcamentoCategoria | id, mês, categoria, limite, usuário | Planejar gastos mensais próprios |

Valores usam BigDecimal. Aportes herdam a propriedade da meta. Orçamento é único por usuário/mês/categoria. Senhas usam BCrypt; a confirmação não é persistida.

## 5. Histórias de usuário

As histórias abaixo descrevem ações e critérios de aceite, não personas. Em todas as histórias financeiras, o usuário precisa estar autenticado e a consulta ou alteração é restrita aos próprios registros.

### HU01 - Cadastrar despesas

Como usuário autenticado, quero cadastrar uma despesa para manter meu histórico. Aceite: descrição, valor positivo, categoria e data válidos são gravados com o proprietário da sessão; cadastro inválido não gera registro. **Banco:** INSERT em despesas; POST /despesas.

### HU02 - Consultar e filtrar despesas

Como usuário autenticado, quero localizar meus gastos por descrição, categoria e período. Aceite: consulta e soma apenas dos meus resultados; filtros de período inválidos geram mensagem. **Banco:** SELECT por proprietário; GET /despesas.

### HU03 - Editar despesas

Como usuário autenticado, quero corrigir uma despesa minha sem criar duplicação. Aceite: alteração valida campos e preserva o ID; ID de outra conta não pode ser editado. **Banco:** SELECT e UPDATE por proprietário; /despesas/{id}/editar.

### HU04 - Excluir despesas

Como usuário autenticado, quero remover uma despesa minha incorreta. Aceite: exclusão por POST protegido por CSRF, com confirmação; não há exclusão por GET e outra conta não pode excluir o registro. **Banco:** SELECT e DELETE; POST /despesas/{id}/excluir.

### HU05 - Consultar visão geral

Como usuário autenticado, quero consultar minhas receitas, despesas, saldo e categoria de maior gasto no mês. Aceite: valores vêm dos meus registros no H2; conta vazia mostra zero e estados vazios legítimos. **Banco:** SELECT em despesas e receitas; GET /dashboard.

### HU06 - Gerenciar receitas

Como usuário autenticado, quero cadastrar, consultar, filtrar, editar e excluir minhas entradas. Aceite: CRUD persistente, valor positivo, data e categoria válidas, com isolamento entre contas. **Banco:** INSERT, SELECT, UPDATE e DELETE em receitas; /receitas.

### HU07 - Comparar períodos

Como usuário autenticado, quero comparar meus gastos de um mês com o anterior. Aceite: valores absolutos e percentual somente quando há base anterior positiva; caso contrário, mensagem de ausência de base. **Banco:** SELECT por datas e proprietário; GET /dashboard.

### HU08 - Consultar gastos por dia

Como usuário autenticado, quero visualizar meus gastos diários e gasto de hoje. Aceite: calendário de Recife, somas do período e gasto de hoje somente quando a data atual pertence ao mês consultado. **Banco:** SELECT em despesas; GET /dashboard.

### HU09 - Gerenciar metas

Como usuário autenticado, quero criar, consultar, editar e excluir meus objetivos. Aceite: valor-alvo positivo, prazo e dados persistentes; progresso inicialmente zero até registrar aportes. Outra conta não acessa a meta. **Banco:** CRUD em metas; /metas.

### HU10 - Registrar e remover aportes

Como usuário autenticado, quero registrar quanto reservei para minhas metas. Aceite: valor positivo, vínculo com meta própria, restante correto e progresso visual limitado a 100%. Erro preserva os campos preenchidos; aportes não geram novas despesas. **Banco:** INSERT, SELECT e DELETE em aportes por meta própria; /metas/{id}/aportes.

### HU11 - Definir limites mensais

Como usuário autenticado, quero definir um limite por categoria e mês. Aceite: valor positivo, unicidade por usuário/categoria/competência, substituição sem duplicação e exclusão mantendo o mês selecionado. **Banco:** CRUD em orçamentos; /planejamento e /orcamentos.

### HU12 - Acompanhar planejamento

Como usuário autenticado, quero comparar meus gastos com meus limites. Aceite: gasto da categoria, teto definido, restante e situação textual; excesso mostra o valor ultrapassado. **Banco:** SELECT em despesas e orçamentos do proprietário; GET /planejamento.

### HU13 - Cadastrar conta

Como pessoa que deseja usar o sistema, quero criar uma conta com nome, e-mail e senha confirmada. Aceite: validação em português, e-mail normalizado único, senha com pelo menos 8 caracteres e até 72 bytes UTF-8, confirmação igual e armazenamento apenas do hash. A nova conta não recebe registros financeiros automaticamente. **Banco:** SELECT e INSERT em usuários; POST /cadastro.

### HU14 - Entrar, consultar conta e sair

Como usuário cadastrado, quero entrar com e-mail e senha e encerrar minha sessão. Aceite: login correto leva ao dashboard, erro usa mensagem genérica, área financeira exige autenticação e logout por POST invalida a sessão. Minha conta mostra nome e e-mail, sem mostrar hash ou senha. **Banco:** SELECT em usuários; /login, /conta e /logout.

### HU15 - Manter informações isoladas

Como usuário autenticado, quero que minhas informações sejam acessíveis apenas à minha conta. Aceite: outra sessão não lista, soma, edita ou exclui meus registros, nem adiciona ou remove aportes em minhas metas, mesmo conhecendo os IDs. Limites iguais de categoria/mês podem existir para usuários diferentes. **Banco:** consultas e alterações filtradas por proprietário em todas as entidades financeiras.

A página Como usar contém orientação estática e não é contada como história com acesso ao banco. A demonstração utiliza as mesmas funcionalidades, sem perfil administrativo.

## 6. Regras, segurança e migração

- Saldo do período = receitas menos despesas do período.
- Variação = (gasto atual - gasto anterior) / gasto anterior x 100, somente com gasto anterior positivo.
- Valor reservado = soma dos aportes; restante da meta = máximo(alvo - reservado, 0).
- Limites são informativos, sem impedir gastos ou movimentar dinheiro.
- Autenticação utiliza sessão do Spring Security, BCrypt, proteção CSRF e renovação do ID da sessão após login. Logout invalida a sessão.
- Cookies usam HttpOnly e SameSite=Strict; sessão expira após 30 minutos de inatividade. A aplicação atende apenas em 127.0.0.1, por HTTP local.

Flyway versiona o esquema: V1 representa a base anterior e V2 acrescenta contas e vínculos. Hibernate valida o esquema, sem recriar tabelas. Registros antigos sem proprietário são preservados com vínculo nulo e ficam inacessíveis às contas novas. Não são transferidos para o primeiro cadastro ou para a demonstração. Recuperação manual exige comprovação de propriedade, backup offline e associação explícita de IDs em transação; o procedimento está em docs/MIGRACAO.md.

A demonstração fica desativada por padrão. Quando ativada explicitamente no ambiente local, cria conta separada com receitas, despesas, metas, aportes e limites fictícios. A senha é configurada no terminal e não integra o código. Reinicializações não duplicam os exemplos. A própria conta demonstrativa pode restaurá-los com confirmação e CSRF, sem alterar dados de outras contas.

## 7. Execução, testes e limitações

No PowerShell, na pasta do projeto, execute java -version, depois .\mvnw.cmd clean test e .\mvnw.cmd clean verify. Inicie com .\mvnw.cmd spring-boot:run e abra http://127.0.0.1:8080/login. Cadastre uma conta ou utilize a demonstração explicitamente configurada conforme o README.

O projeto contém testes JUnit/MockMvc para operações, validações, autenticação, CSRF, cálculos e isolamento entre contas, além de cenários de migração e demonstração. Os resultados observados de execução, navegador e persistência acompanham a validação da entrega. A existência de código ou testes não comprova, isoladamente, sua aprovação.

O sistema é acadêmico e local. Não oferece integração bancária, PIX, pagamentos, recuperação de senha ou hospedagem pública. O arquivo H2 não é criptografado; sua proteção também depende do acesso ao computador. Não há JWT ou OAuth. O documento não caracteriza a aplicação como serviço financeiro pronto para produção.

## 8. Referências

- Material de aula do professor Maurício Braga, Back-End Frameworks, UNINASSAU Recife, 2026.2.
- [Spring Boot - documentação](https://docs.spring.io/spring-boot/)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Spring Security](https://docs.spring.io/spring-security/reference/)
- [Flyway - documentação](https://documentation.red-gate.com/fd)
- [Banco Central - Cidadania Financeira](https://www.bcb.gov.br/cidadaniafinanceira)
