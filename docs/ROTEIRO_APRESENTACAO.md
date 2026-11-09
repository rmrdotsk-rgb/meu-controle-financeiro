# Roteiro de apresentação do Meu Controle Financeiro

**Integrante:** Emerson Saturno.

**Duração sugerida:** cerca de 7 minutos, conforme orientação do professor. As apresentações foram informadas para 10 e 17 de novembro de 2026.

1. **Contexto e acesso (40 s):** apresentar a organização de receitas e despesas em aplicação Java/Spring Boot com banco persistente. Entrar na conta demonstrativa previamente configurada, indicando que todos os seus dados são fictícios.
2. **CRUD obrigatório de despesas (2 min):** cadastrar uma despesa de exemplo, consultá-la por descrição/categoria/período, editar o valor e excluir com confirmação. Relacionar HU01-HU04 a INSERT, SELECT, UPDATE e DELETE no H2. Não excluir exemplos necessários para outras etapas.
3. **Receitas e painel (1 min):** registrar receita e mostrar atualização dos totais do mês. Explicar saldo calculado, categorias, gastos por dia e comparação com o mês anterior, sem inventar resultados.
4. **Metas e planejamento (1 min):** mostrar meta e aporte, progresso e valor restante. Definir ou atualizar limite e explicar o aviso de excesso. Aporte não gera despesa adicional; limite não impede gasto.
5. **Contas e isolamento (1 min):** abrir outro perfil/janela anônima, cadastrar uma conta real de teste e mostrar o estado vazio. Voltar à sessão demonstrativa e indicar que os exemplos continuam nela. Explicar que IDs de outro proprietário não autorizam alteração; usar somente tentativas previamente preparadas e verificadas.
6. **Persistência e celular (1 min):** mostrar a interface em largura de 390 ou 320 px. Se houver tempo, sair, encerrar e reiniciar o servidor com o mesmo banco; entrar e consultar um registro mantido.
7. **Arquitetura e conclusão (30 s):** explicar Spring Security -> controller -> service -> repository -> JPA/H2, migrações Flyway e BCrypt. Entregar o documento impresso. Ressaltar uso acadêmico local e ausência de integração bancária.

**Antes da aula:** JDK 21 instalado, dependências Maven baixadas, testes executados, porta livre, backup offline e original estável preservados, demonstração habilitada com senha local e impressão conferida. A restauração dos exemplos fica em Minha conta da demonstração; use-a antes da apresentação, pois remove alterações daquela conta.

**Plano B:** sem rede, usar dependências já baixadas e o aplicativo local. Se a nova evolução apresentar falha importante, utilizar o original estável preservado e explicar suas limitações. Capturas e registros anteriores devem ser identificados como evidência prévia, sem afirmar execução ao vivo.
