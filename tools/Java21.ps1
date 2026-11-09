# Compatível com Windows PowerShell 5.1 e PowerShell 7.
function Inicializar-Java21 {
    $candidatos = @()
    if ($env:JAVA_HOME) { $candidatos += $env:JAVA_HOME }
    $comandoJava = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($comandoJava) { $candidatos += Split-Path (Split-Path $comandoJava.Source -Parent) -Parent }
    foreach ($base in @("$env:ProgramFiles\Eclipse Adoptium", "$env:ProgramFiles\Java", "$env:ProgramFiles\Microsoft", "$env:USERPROFILE\.jdks")) {
        if (Test-Path -LiteralPath $base) {
            $candidatos += Get-ChildItem -LiteralPath $base -Directory -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName
        }
    }
    foreach ($candidato in ($candidatos | Select-Object -Unique)) {
        $java = Join-Path $candidato 'bin\java.exe'
        $javac = Join-Path $candidato 'bin\javac.exe'
        if ((Test-Path -LiteralPath $java) -and (Test-Path -LiteralPath $javac)) {
            # java -version escreve no canal de erro, mesmo quando conclui com sucesso.
            $preferenciaAnterior = $ErrorActionPreference
            $ErrorActionPreference = 'Continue'
            try { $versao = (& $java -version 2>&1 | Out-String) }
            finally { $ErrorActionPreference = $preferenciaAnterior }
            if ($versao -match 'version "21[\."]') {
                $env:JAVA_HOME = $candidato
                $env:PATH = (Join-Path $candidato 'bin') + ';' + $env:PATH
                return $java
            }
        }
    }
    throw 'JDK 21 não encontrado. Instale um JDK 21, ou ajuste JAVA_HOME para a pasta dele, e execute novamente. Nenhuma configuração do Windows foi alterada.'
}
