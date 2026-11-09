# Meu Controle Financeiro

**Aplicação web para organização de receitas, despesas e metas**

Instituição: UNINASSAU Recife  
Disciplina: Backend Frameworks — 2026.2  
Professor: Maurício Braga  
Integrante: Emerson Saturno

## 1. Objetivo

O Meu Controle Financeiro é uma aplicação web local para registrar receitas, despesas e metas, acompanhar o resultado financeiro e planejar gastos por categoria. Organiza registros persistidos em banco de dados, sem integração com contas bancárias e sem gerar valores fictícios automaticamente.

## 2. Relação com a disciplina

A implementação demonstra aplicação web funcional, CSS responsivo, operações de leitura e escrita em banco relacional e histórias de usuário que executam ações reais. O material impresso descreve o projeto e as histórias para apresentação acadêmica.

## 3. Tecnologias e arquitetura

Java 21, Spring Boot 4.1.2, Spring MVC, Thymeleaf, Spring Data JPA, Hibernate, H2 persistente em arquivo, Bean Validation, Spring Security para proteção CSRF, Maven Wrapper e testes JUnit/MockMvc.

Fluxo de uma requisição:

```text
Navegador → Controller → Service → Repository → JPA/Hibernate → H2
```

Os controllers recebem requisições e apresentam respostas. A camada de serviços organiza regras, cálculos e transações; repositories fazem o acesso a dados; models representam as entidades. Os templates Thymeleaf produzem HTML no servidor.

## 4. Modelo de dados

| Entidade | Campos principais | Uso |
|---|---|---|
| Despesa | id, descrição, valor, categoria, data | Registrar gastos e analisar histórico |
| Receita | id, descrição, valor, categoria, data | Registrar entradas e calcular saldo |
| MetaFinanceira | id, nome, valor-alvo, prazo | Definir objetivos |
| AporteMeta | id, meta, valor, data | Registrar valores reservados |
| OrcamentoCategoria | id, mês, categoria, limite | Planejar gastos mensais |

Valores monetários são representados por `BigDecimal`. O aporte referencia sua meta por chave estrangeira. O orçamento restringe uma categoria a um limite por mês.

## 5. Histórias de usuário

### HU01 — Cadastrar despesas

Como usuário, quero cadastrar despesas para manter um histórico. Aceite: descrição, valor positivo, categoria e data válidos são gravados; um cadastro inválido não gera registro. **Banco:** INSERT na tabela de despesas; `POST /despesas`.

### HU02 — Consultar e filtrar despesas

Como usuário, quero localizar gastos por descrição, categoria e período. Aceite: consultas feitas ao banco e soma apenas dos resultados; filtros inválidos não devem ser aceitos como período válido. **Banco:** SELECT; `GET /despesas`.

### HU03 — Editar despesas

Como usuário, quero corrigir registros sem criar duplicados. Aceite: alteração preserva o ID original e valida campos. **Banco:** SELECT + UPDATE; `/despesas/{id}/editar`.

### HU04 — Excluir despesas

Como usuário, quero remover gastos incorretos. Aceite: exclusão por POST com confirmação, sem exclusão por GET. **Banco:** SELECT + DELETE; `POST /despesas/{id}/excluir`.

### HU05 — Consultar visão geral

Como usuário, quero consultar receitas, despesas, saldo e categoria de maior gasto no mês. Aceite: números são derivados dos registros em H2 e o banco vazio mostra R$ 0,00. **Banco:** SELECT em despesas e receitas; `GET /dashboard`.

### HU06 — Gerenciar receitas

Como usuário, quero cadastrar, consultar, editar e excluir salário e outras entradas. Aceite: CRUD persistente, valor positivo, data e categoria válidas. **Banco:** INSERT, SELECT, UPDATE e DELETE; `/receitas`.

### HU07 — Comparar períodos

Como usuário, quero comparar gastos do mês atual com o anterior. Aceite: exibir valores absolutos e percentual somente quando há base anterior positiva; caso contrário, mostrar ausência de base. **Banco:** SELECT por datas; `GET /dashboard`.

### HU08 — Consultar gastos por dia

Como usuário, quero visualizar gastos diários e gasto de hoje. Aceite: data atual segue calendário de Recife e as somas correspondem ao período consultado. **Banco:** SELECT em despesas; `GET /dashboard`.

### HU09 — Gerenciar metas

Como usuário, quero criar, consultar, editar e excluir objetivos com valor e prazo. Aceite: valor-alvo positivo, dados persistentes, progresso inicialmente zero até registrar aportes. **Banco:** CRUD em metas; `/metas`.

### HU10 — Registrar e remover aportes

Como usuário, quero informar quanto reservei para cada meta. Aceite: valor maior que zero, vínculo com meta, valor restante correto, progresso visual limitado a 100%. Aportes não são despesas adicionais. **Banco:** INSERT/SELECT/DELETE em aportes; `/metas/{id}/aportes`.

### HU11 — Definir limites mensais

Como usuário, quero definir o limite de uma categoria por mês. Aceite: valor positivo e apenas um limite por categoria/competência, com edição por substituição e exclusão. **Banco:** CRUD em orçamentos; `/planejamento`, `/orcamentos`.

### HU12 — Acompanhar planejamento

Como usuário, quero comparar gastos e limites. Aceite: mostrar gasto da categoria, teto definido, diferença restante e estado textual em caso de excesso. **Banco:** SELECT em despesas e orçamentos; `GET /planejamento`.

A página **Como usar** explica o funcionamento, mas não é contada entre as histórias que acessam o banco, por ser conteúdo educativo estático.

## 6. Regras principais

- Saldo do período = receitas menos despesas do período.
- Variação = (gasto atual − gasto anterior) / gasto anterior × 100, apenas se o gasto anterior for maior que zero.
- Valor reservado em meta = soma dos aportes.
- Restante de meta = máximo(alvo − reservado, 0).
- Limites são informativos; não impedem gastos.

## 7. Testes e limitações

O projeto contém testes JUnit/MockMvc de operações, validações, acesso ao banco e cálculos. A validação da entrega também verifica a aplicação no navegador e a persistência em arquivo após reiniciar o servidor. Os resultados e as evidências de execução acompanham a entrega, separados do código-fonte. Para repetir os testes automatizados, execute `mvnw.cmd clean test`; para gerar o aplicativo, execute `mvnw.cmd clean package`.

O sistema é acadêmico, local, de usuário único e sem autenticação. As alterações por formulário usam proteção CSRF. Não oferece conexão com bancos, PIX, pagamentos ou hospedagem pública.

## 8. Referências

- Material de aula do professor Maurício Braga, Backend Frameworks, UNINASSAU Recife, 2026.2.
- [Spring Boot — documentação](https://docs.spring.io/spring-boot/)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Banco Central — Cidadania Financeira](https://www.bcb.gov.br/cidadaniafinanceira)
