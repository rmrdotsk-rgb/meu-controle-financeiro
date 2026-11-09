# Inicia a aplicação local. O banco normal fica em data/meucontrole.mv.db.
$ErrorActionPreference = 'Stop'
$raizProjeto = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $raizProjeto
. (Join-Path $PSScriptRoot 'Java21.ps1')
function Calcular-HashArquivo([string]$Caminho) {
    $algoritmo = [Security.Cryptography.SHA256]::Create()
    $arquivo = [IO.File]::OpenRead($Caminho)
    try { return [BitConverter]::ToString($algoritmo.ComputeHash($arquivo)).Replace('-', '') }
    finally { $arquivo.Dispose(); $algoritmo.Dispose() }
}
try {
    $java = Inicializar-Java21
    $banco = Join-Path $raizProjeto 'data\meucontrole.mv.db'
    if (Test-Path -LiteralPath $banco) {
        # Uma cópia de banco aberto não é um backup confiável.
        try {
            $verificacao = [IO.File]::Open($banco, [IO.FileMode]::Open, [IO.FileAccess]::Read, [IO.FileShare]::None)
            $verificacao.Dispose()
        }
        catch { throw 'Encerre a aplicação que usa este banco antes de iniciar a nova versão e criar o backup.' }
        $pastaBackup = Join-Path $env:LOCALAPPDATA 'MeuControleFinanceiro\Backups'
        New-Item -ItemType Directory -Force -Path $pastaBackup | Out-Null
        $copia = Join-Path $pastaBackup ('meucontrole-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N') + '.mv.db')
        Copy-Item -LiteralPath $banco -Destination $copia
        if ((Calcular-HashArquivo $banco) -ne (Calcular-HashArquivo $copia)) { throw 'Não foi possível conferir o backup do banco.' }
        Write-Host "Backup conferido antes da migração: $copia"
    }
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
