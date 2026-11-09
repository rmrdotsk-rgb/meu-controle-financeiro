# Inicia a aplicação local. O banco normal fica em data/meucontrole.mv.db.
$ErrorActionPreference = 'Stop'
$raizProjeto = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $raizProjeto
. (Join-Path $PSScriptRoot 'Java21.ps1')
try {
    $java = Inicializar-Java21
    Write-Host 'Java 21 localizado. Preparando a aplicação...'
    $ErrorActionPreference = 'Continue'
    try { & .\mvnw.cmd -B -ntp -DskipTests package; $codigoSaida = $LASTEXITCODE }
    finally { $ErrorActionPreference = 'Stop' }
    if ($codigoSaida -ne 0) { throw 'A preparação falhou. Confira a conexão com a internet e as mensagens acima.' }
    $jar = Join-Path $raizProjeto 'target\meu-controle-financeiro-0.0.1-SNAPSHOT.jar'
    Write-Host ''
    Write-Host 'Após a mensagem Started, abra http://localhost:8080/dashboard'
    Write-Host 'Para encerrar, pressione Ctrl+C. Os dados cadastrados serão preservados.'
    $ErrorActionPreference = 'Continue'
    try { & $java -jar $jar; $codigoSaida = $LASTEXITCODE }
    finally { $ErrorActionPreference = 'Stop' }
    if ($codigoSaida -ne 0) { throw 'A aplicação encerrou com erro. Confira as mensagens acima.' }
}
catch { Write-Host $_.Exception.Message -ForegroundColor Red; exit 1 }
