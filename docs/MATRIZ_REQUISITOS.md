# Relação entre requisitos e implementação

Disciplina Backend Frameworks — professor Maurício Braga — 2026.2.

| Requisito informado no material da disciplina | Implementação | Como demonstrar |
|---|---|---|
| Aplicação web funcional | Spring Boot MVC e formulários HTML em Thymeleaf | Iniciar servidor; abrir páginas e salvar um lançamento |
| Design adaptável | CSS responsivo | Reduzir navegador para 320 e 390 px e conferir formulários/tabelas |
| Todas as histórias acessam banco | Controllers → services → repositories → H2 | Cadastrar, consultar, editar e excluir, depois reiniciar e consultar |
| Persistência em banco | H2 em arquivo, JPA/Hibernate | Reiniciar servidor, preservando o mesmo diretório de execução |
| CSS, cores e fontes organizados | `src/main/resources/static/css/style.css` | Navegar por Dashboard, Despesas, Receitas, Metas e Planejamento |
| Mínimo de quatro histórias | Despesas (CRUD) e histórias adicionais descritas no documento | Demonstrar pelo menos quatro operações com persistência |
| Documento impresso | `DOCUMENTO_ACADEMICO.pdf` | Revisar identificação de Emerson Saturno e imprimir |

Os requisitos de aplicação adaptável, histórias com leitura/escrita no banco, persistência, CSS organizado e documento impresso com pelo menos quatro histórias constam nas páginas 6 e 7 da apresentação da disciplina. A turma pode formar grupos de até seis integrantes.

Os resultados dos testes e as evidências de execução acompanham a entrega, separados do código-fonte. A existência do código, isoladamente, não comprova uma execução bem-sucedida.

## Evolução com contas individuais

A autenticação é uma ampliação do projeto, sem substituir o CRUD exigido. HU01-HU12 continuam descritas no documento; HU13-HU15 acrescentam cadastro, login/logout e isolamento entre contas. Histórias descrevem ações com critérios de aceite, não personas.

| Funcionalidade acrescentada | Implementação | Como demonstrar |
|---|---|---|
| Cadastro de conta | Nome/e-mail validados, unicidade e hash BCrypt | Criar conta; rejeitar e-mail repetido e senha inválida |
| Autenticação por sessão | Spring Security, CSRF, login e logout | Acesso financeiro exige login; sair encerra sessão |
| Dados por proprietário | Vínculos e consultas limitadas ao usuário autenticado | Duas sessões; uma conta não consulta nem altera registros da outra |
| Migração preservando legados | Flyway V1/V2, validação do esquema e quarentena | Abrir cópia antiga e conferir IDs/valores preservados sem atribuição automática |
| Demonstração opcional | Configuração explícita, conta isolada e restauração protegida | Identificar exemplos fictícios e restaurar somente essa conta |
