# Roteiro de apresentação do Meu Controle Financeiro

**Integrante:** Emerson Saturno.

**Sugestão de duração:** cerca de 5–7 minutos, a adaptar à orientação do professor. As apresentações da disciplina foram informadas para 10 e 17 de novembro de 2026, por ordem de chegada.

1. **Contexto (40 s):** explicar que o objetivo é organizar receitas e despesas em uma aplicação web que salva os dados em banco.
2. **CRUD obrigatório de despesas (2 min):** cadastrar uma despesa de demonstração, consultar e filtrar a listagem, editar o valor e excluir com confirmação. Relacionar essas ações às quatro histórias e às operações INSERT, SELECT, UPDATE e DELETE no H2.
3. **Receitas (45 s):** cadastrar uma receita de demonstração e mostrar que o saldo do período se atualiza com os registros salvos.
4. **Painel (1 min):** selecionar mês e explicar totais, categoria principal, gasto do dia e mês anterior sem inventar números.
5. **Metas e planejamento (1 min):** criar meta, registrar um aporte e mostrar cálculo; definir limite por categoria e conferir situação.
6. **Persistência e mobile (1 min):** se houver tempo, parar e reiniciar servidor; mostrar registro mantido; abrir visualização móvel.
7. **Arquitetura e entrega (40 s):** mostrar o fluxo controller → service → repository → JPA/H2. Explicar o uso de Generics em JpaRepository, informar as tecnologias e entregar o documento impresso. Esclarecer que a aplicação funciona localmente para um usuário, com proteção dos formulários e sem login.

**Antes de entrar em sala:** Java 21 instalado, dependências Maven baixadas, projeto compilado, porta 8080 livre, banco de demonstração preparado sem dados pessoais, impressão conferida e backup do ZIP original separado.

**Plano B:** caso haja falha de rede, usar dependências Maven já baixadas e o aplicativo local. Em caso de falha na aplicação, mostrar o código e capturas/registro de uma execução anterior **claramente identificados** como evidência prévia, sem afirmar que a execução ao vivo aconteceu.
