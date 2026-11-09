# Testa sem usar ou modificar o banco pessoal. Windows PowerShell 5.1 / PowerShell 7.
$ErrorActionPreference = 'Stop'
$raizProjeto = Split-Path $PSScriptRoot -Parent
$diretorioTeste = Join-Path ([IO.Path]::GetTempPath()) ('meucontrole-teste-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $diretorioTeste | Out-Null
$servidor = $null
$script:sessaoHttp = New-Object Microsoft.PowerShell.Commands.WebRequestSession
. (Join-Path $PSScriptRoot 'Java21.ps1')

function Executar-Maven([string[]]$Argumentos, [string]$NomeLog) {
    $caminhoLog = Join-Path $diretorioTeste $NomeLog
    $preferenciaAnterior = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { & .\mvnw.cmd @Argumentos *> $caminhoLog; $codigoSaida = $LASTEXITCODE }
    finally { $ErrorActionPreference = $preferenciaAnterior }
    if ($codigoSaida -ne 0) { throw "Maven falhou (código $codigoSaida). Consulte $caminhoLog" }
}
function Consultar-Pagina([string]$Caminho) {
    $resposta = Invoke-WebRequest -Uri ($urlBase + $Caminho) -WebSession $script:sessaoHttp -UseBasicParsing -TimeoutSec 15
    return $resposta.Content
}
function Enviar-Formulario([string]$Pagina, [string]$Destino, [hashtable]$Dados) {
    $html = Consultar-Pagina $Pagina
    $token = [regex]::Match($html, '<input\b(?=[^>]*\bname="_csrf")(?=[^>]*\bvalue="([^"]+)")[^>]*>')
    if (-not $token.Success) { throw "Formulário $Pagina sem token CSRF" }
    $Dados['_csrf'] = [Net.WebUtility]::HtmlDecode($token.Groups[1].Value)
    $resposta = Invoke-WebRequest -Uri ($urlBase + $Destino) -Method Post -Body $Dados -WebSession $script:sessaoHttp -UseBasicParsing -TimeoutSec 15
    return $resposta.Content
}
function Confirmar-Texto([string]$Html, [string]$Texto) {
    if (-not $Html.Contains($Texto)) { throw "Resultado esperado não encontrado: $Texto" }
}
function Iniciar-Instancia([string]$NomeLog) {
    $argumentosJava = @('-jar', ('"' + $arquivoJar + '"'), ('"--server.port=' + $porta + '"'), '--server.address=127.0.0.1', '--app.demo.enabled=false', ('"--spring.datasource.url=jdbc:h2:file:' + $bancoTeste + ';WRITE_DELAY=0"'))
    $script:servidor = Start-Process -FilePath $java -ArgumentList $argumentosJava -WorkingDirectory $raizProjeto -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $diretorioTeste ($NomeLog + '-saida.log')) -RedirectStandardError (Join-Path $diretorioTeste ($NomeLog + '-erros.log'))
    $limiteEspera = [DateTime]::UtcNow.AddSeconds(55)
    do {
        if ($script:servidor.HasExited) { throw "Servidor encerrou durante a inicialização. Consulte os logs em $diretorioTeste" }
        try { $null = Consultar-Pagina '/login'; return }
        catch { if ([DateTime]::UtcNow -ge $limiteEspera) { throw 'Servidor não respondeu em 55 segundos.' } }
        Start-Sleep -Milliseconds 350
    } while ($true)
}
function Encerrar-Instancia {
    if ($script:servidor -and -not $script:servidor.HasExited) {
        Stop-Process -Id $script:servidor.Id -Force
        $script:servidor.WaitForExit(10000) | Out-Null
    }
    $script:servidor = $null
}

Push-Location -LiteralPath $raizProjeto
try {
    Write-Host "Logs e banco temporário: $diretorioTeste"
    Write-Host '[1/8] Localizando Java 21...'
    $java = Inicializar-Java21
    $preferenciaAnterior = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { & $java -version 2>&1 | Out-File -LiteralPath (Join-Path $diretorioTeste 'java-version.log') }
    finally { $ErrorActionPreference = $preferenciaAnterior }
    Write-Host '[2/8] Executando clean test...'
    Executar-Maven @('-B', '-ntp', 'clean', 'test') 'maven-test.log'
    Write-Host '[3/8] Executando clean verify (inclui os testes novamente)...'
    Executar-Maven @('-B', '-ntp', 'clean', 'verify') 'maven-verify.log'
    $arquivoJar = Join-Path $raizProjeto 'target\meu-controle-financeiro-0.0.1-SNAPSHOT.jar'
    if (-not (Test-Path -LiteralPath $arquivoJar)) { throw 'JAR não foi gerado.' }
    $bancoTeste = (Join-Path $diretorioTeste 'banco-isolado').Replace('\', '/')
    $ouvinte = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback, 0)
    $ouvinte.Start()
    $porta = $ouvinte.LocalEndpoint.Port
    $ouvinte.Stop()
    $urlBase = 'http://127.0.0.1:' + $porta
    Write-Host '[4/8] Iniciando aplicação com H2 isolado...'
    Iniciar-Instancia 'primeiro-servidor'
    $emailTeste = 'teste-' + [guid]::NewGuid().ToString('N') + '@example.test'
    $senhaTeste = [guid]::NewGuid().ToString('N') + 'aA9!'
    $html = Enviar-Formulario '/cadastro' '/cadastro' @{ nome='Conta de teste isolada'; email=$emailTeste; senha=$senhaTeste; confirmacaoSenha=$senhaTeste }
    Confirmar-Texto $html 'Conta criada'
    $html = Enviar-Formulario '/login' '/login' @{ email=$emailTeste; senha=$senhaTeste }
    Confirmar-Texto $html 'Visão geral'
    foreach ($rota in @('/dashboard', '/despesas', '/receitas', '/metas', '/planejamento', '/ajuda')) { $null = Consultar-Pagina $rota }
    Write-Host '[5/8] Cadastrando, consultando e editando uma despesa com CSRF real...'
    $html = Enviar-Formulario '/despesas/nova' '/despesas' @{ descricao='Despesa isolada de teste'; valor='123.45'; categoria='Casa'; data='2026-11-09' }
    Confirmar-Texto $html 'Despesa isolada de teste'
    Confirmar-Texto $html '123,45'
    $registro = [regex]::Match($html, 'href="/despesas/(\d+)/editar"')
    if (-not $registro.Success) { throw 'Despesa criada sem identificação na listagem.' }
    $idDespesa = $registro.Groups[1].Value
    $html = Enviar-Formulario ("/despesas/$idDespesa/editar") ("/despesas/$idDespesa/editar") @{ descricao='Despesa editada e persistida'; valor='222.22'; categoria='Educação'; data='2026-11-09' }
    Confirmar-Texto $html 'Despesa editada e persistida'
    Confirmar-Texto $html '222,22'
    Write-Host '[6/8] Confirmando proteção contra POST sem token...'
    $bloqueou = $false
    try { $null = Invoke-WebRequest -Uri ($urlBase + "/despesas/$idDespesa/excluir") -Method Post -WebSession $script:sessaoHttp -UseBasicParsing -TimeoutSec 15 }
    catch { if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 403) { $bloqueou = $true } else { throw } }
    if (-not $bloqueou) { throw 'POST sem token não foi bloqueado com HTTP 403.' }
    Write-Host '[7/8] Reiniciando o servidor e consultando o mesmo banco...'
    Encerrar-Instancia
    $script:sessaoHttp = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    Iniciar-Instancia 'segundo-servidor'
    $null = Enviar-Formulario '/login' '/login' @{ email=$emailTeste; senha=$senhaTeste }
    $html = Consultar-Pagina '/despesas'
    Confirmar-Texto $html 'Despesa editada e persistida'
    Confirmar-Texto $html '222,22'
    if (-not (Test-Path -LiteralPath ($bancoTeste + '.mv.db'))) { throw 'Arquivo persistente H2 não encontrado.' }
    Write-Host '[8/8] Excluindo a despesa e confirmando a listagem vazia...'
    $html = Enviar-Formulario '/despesas' ("/despesas/$idDespesa/excluir") @{}
    if ($html.Contains('Despesa editada e persistida')) { throw 'A despesa continuou na listagem após a exclusão.' }
    Confirmar-Texto $html 'Ainda não há despesas cadastradas.'
    $html = Enviar-Formulario '/conta' '/logout' @{}
    Confirmar-Texto $html 'Você saiu da sua conta'
    @('APROVADO', 'Java 21; clean test; clean verify; cadastro e login; seis páginas autenticadas; CRUD despesas; CSRF 403; persistência H2 após reiniciar; exclusão e logout confirmados.', ('Banco isolado: ' + $bancoTeste), ('Porta de teste: ' + $porta)) | Set-Content -LiteralPath (Join-Path $diretorioTeste 'resultado.txt') -Encoding UTF8
    Write-Host 'APROVADO: compilação, testes, HTTP, CRUD, CSRF e persistência após reinício.' -ForegroundColor Green
    Write-Host "Evidências preservadas em: $diretorioTeste"
}
catch {
    $_ | Out-String | Set-Content -LiteralPath (Join-Path $diretorioTeste 'falha.txt') -Encoding UTF8
    Write-Host ('FALHOU: ' + $_.Exception.Message) -ForegroundColor Red
    Write-Host "Logs preservados em: $diretorioTeste"
    exit 1
}
finally {
    Encerrar-Instancia
    Pop-Location
}
