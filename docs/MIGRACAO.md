# Migração e recuperação de registros antigos

## Objetivo e escopo

A evolução acrescenta contas individuais ao H2 em arquivo da versão anterior. IDs, datas, valores, categorias e o vínculo entre metas e aportes são preservados. Nenhum registro antigo é atribuído automaticamente ao primeiro cadastro ou à demonstração.

Este procedimento é administrativo e local. A aplicação não oferece uma tela para reivindicar registros antigos. Uma associação manual só deve ocorrer quando houver comprovação de quem é o proprietário.

## Antes de iniciar a nova versão

1. Encerre o servidor normalmente com `Ctrl+C` e aguarde o processo terminar. Feche outras ferramentas conectadas ao H2.
2. Confirme a pasta de execução e o arquivo `data/meucontrole.mv.db`. Caminhos relativos diferentes apontam para bancos diferentes.
3. Preserve o projeto original e uma cópia offline do banco fora do repositório. Registre tamanho, data e SHA-256 da cópia.
4. Teste primeiro uma cópia do banco em uma pasta isolada. Não substitua nem redefina a base original para contornar erros.
5. Depois de validar o funcionamento da cópia e a preservação dos registros, execute a migração da base de uso com backup disponível.

Exemplo de backup manual no PowerShell, com o servidor parado:

```powershell
$bancoOrigem = (Resolve-Path '.\data\meucontrole.mv.db').Path
$pastaBackup = Join-Path $env:LOCALAPPDATA 'MeuControleFinanceiro\Backups'
New-Item -ItemType Directory -Path $pastaBackup -Force | Out-Null
$arquivoBackup = Join-Path $pastaBackup ('meucontrole-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.mv.db')
Copy-Item -LiteralPath $bancoOrigem -Destination $arquivoBackup
Get-FileHash -Algorithm SHA256 -LiteralPath $bancoOrigem,$arquivoBackup
```

Compare os dois hashes. O arquivo copiado deve ter tamanho maior que zero. Não confirme backup com o servidor em execução apenas porque a cópia foi criada.

O iniciador Windows `Iniciar.cmd` utiliza `tools/Iniciar.ps1`: antes de iniciar com banco existente, o procedimento de backup verifica acesso exclusivo e grava cópia com checksum em `%LOCALAPPDATA%\MeuControleFinanceiro\Backups`. Se usar `mvnw.cmd spring-boot:run` ou o JAR diretamente, faça o backup manual antes de migrar uma base antiga. Não mantenha a única cópia dentro da pasta do projeto.

## Esquema versionado

As migrações ficam em `src/main/resources/db/migration`:

- **V1:** esquema financeiro anterior, com despesas, receitas, metas, aportes e orçamentos.
- **V2:** tabela de usuários, e-mail único, hash de senha, indicador demonstrativo e coluna anulável `usuario_id` em despesas, receitas, metas e orçamentos. Acrescenta chaves estrangeiras e índices; substitui a unicidade global de mês/categoria por unicidade de usuário/mês/categoria.

Em banco vazio, Flyway executa V1 e V2. Em banco anterior sem histórico Flyway, o baseline na versão 1 reconhece a estrutura anterior e executa V2. Esse baseline pressupõe o esquema conhecido da versão anterior; ele não converte automaticamente qualquer banco H2 desconhecido. Examine o erro e compare o esquema se houver diferença.

`spring.jpa.hibernate.ddl-auto=validate` faz Hibernate validar o resultado. `spring.flyway.clean-disabled=true` impede limpeza pelo Flyway. Não use `create`, `create-drop`, exclusão de tabelas ou reinicialização do banco para corrigir uma falha. Não edite migrações já aplicadas nem apague seu histórico para forçar nova execução.

## Quarentena dos registros sem proprietário

As novas colunas são anuláveis para não descartar registros antigos. As consultas da aplicação exigem o usuário autenticado, por isso linhas com `usuario_id IS NULL` ficam inacessíveis às novas contas. Elas continuam no arquivo e podem ser contadas por consulta administrativa offline.

A tabela de aportes não duplica `usuario_id`: a propriedade vem da meta referenciada. Uma meta antiga em quarentena mantém todos os aportes vinculados e inacessíveis pelas telas. A recuperação correta associa a meta comprovada; não altera os IDs nem recria os aportes.

Contas reais novas começam vazias. A ativação demonstrativa cria apenas exemplos ligados à conta demonstrativa e não reivindica registros em quarentena.

## Recuperação manual com proprietário comprovado

Execute somente em cópia de trabalho, com aplicação parada e backup verificado. O administrador deve conhecer o proprietário, o ID da conta e os IDs exatos dos registros a recuperar. Não faça associação em massa por ausência de proprietário, por ordem de cadastro ou por semelhança de descrição.

1. Cadastre a conta real na aplicação e encerre o servidor.
2. Identifique administrativamente a conta por e-mail normalizado. Confirme que `demonstracao = FALSE` e registre seu ID.
3. Consulte e confira os registros específicos em quarentena: IDs, descrição/nome, data, categoria e valores. Nas metas, confira também os IDs e a soma dos aportes.
4. Para cada orçamento, confira antes se a conta já possui limite para o mesmo mês/categoria. Se houver conflito, interrompa e resolva com o proprietário; não sobrescreva ou mescle automaticamente.
5. Use uma conexão JDBC administrativa à cópia offline, `setAutoCommit(false)` e `PreparedStatement` com parâmetros. Associe somente os IDs aprovados e exija exatamente uma linha alterada por registro.
6. Reconsulte os registros na mesma transação e confirme valores originais, chaves estrangeiras, totais dos aportes e proprietário. Faça `commit` somente se tudo corresponder. Diante de qualquer divergência, faça `rollback`.
7. Reinicie a aplicação na cópia e confira os registros com a conta correta; outra conta não deve acessá-los, nem por IDs conhecidos. Só então repita o procedimento aprovado na base de uso, com novo backup offline.

Consultas e alterações parametrizadas de referência; `?` representa um parâmetro vinculado pelo `PreparedStatement`, nunca texto concatenado:

```sql
SELECT id, email, demonstracao FROM usuarios WHERE email = ?;
SELECT id, descricao, valor, categoria, data FROM despesas WHERE id = ? AND usuario_id IS NULL;
SELECT id, descricao, valor, categoria, data FROM receitas WHERE id = ? AND usuario_id IS NULL;
SELECT id, nome, valor_alvo, data_limite FROM metas_financeiras WHERE id = ? AND usuario_id IS NULL;
SELECT id, meta_id, valor, data FROM aportes_metas WHERE meta_id = ? ORDER BY id;
SELECT id, mes, categoria, limite FROM orcamentos WHERE id = ? AND usuario_id IS NULL;
SELECT id FROM orcamentos WHERE usuario_id = ? AND mes = ? AND categoria = ?;

UPDATE despesas SET usuario_id = ? WHERE id = ? AND usuario_id IS NULL;
UPDATE receitas SET usuario_id = ? WHERE id = ? AND usuario_id IS NULL;
UPDATE metas_financeiras SET usuario_id = ? WHERE id = ? AND usuario_id IS NULL;
UPDATE orcamentos SET usuario_id = ? WHERE id = ? AND usuario_id IS NULL;
```

Em cada `UPDATE`, o primeiro parâmetro é o ID do proprietário comprovado e o segundo é um único ID de registro previamente revisado. Não execute um `UPDATE ... WHERE usuario_id IS NULL` sem restrição por ID. Não aceite o ID do proprietário enviado por formulário de um usuário comum. Não associe dados reais à conta de demonstração.

Não é necessário atualizar a tabela `aportes_metas`: seu vínculo existente com a meta determina o acesso. Verifique que nenhum aporte mudou de meta durante o procedimento.

## Verificações e retorno ao backup

Após migrar uma cópia, confira: versões aplicadas no histórico Flyway, inicialização com validação Hibernate, contagem de registros antigos, campos e IDs preservados, vínculos meta/aportes e ausência dos legados nas contas novas. Teste cadastro, login, CRUD e reinicialização com o mesmo arquivo.

Se precisar restaurar, pare a aplicação, preserve a base que falhou para investigação e copie o backup offline verificado para o caminho de uso. Restaure também a versão de código compatível com esse backup. Não substitua o banco enquanto houver processo conectado. Uma base anterior restaurada será migrada novamente se abrir com a nova versão; use primeiro uma cópia isolada para investigar a falha.

Não execute recuperação manual sem comprovação de propriedade. Manter os registros em quarentena é o resultado previsto quando essa comprovação não está disponível.
