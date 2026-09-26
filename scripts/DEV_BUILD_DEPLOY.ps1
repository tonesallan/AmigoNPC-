param(
    [string]$Repo = "$env:USERPROFILE\Documents\AmigoNPC",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$branch = "recovery/amigonpc-2.0.1-source"
$pluginId = "br.tones:AmigoNPC"
$version = "2.0.1"

Set-Location $Repo

$currentBranch = git branch --show-current
if ($currentBranch -ne $branch) {
    throw "Branch incorreta: $currentBranch. Esperado: $branch"
}

if ((git status --porcelain)) {
    git status --short
    throw "Existem alteracoes locais. O deploy de desenvolvimento exige working tree limpo."
}

$jdk25 = Get-ChildItem "C:\Program Files\Microsoft" -Directory -Filter "jdk-25*" -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1

if (-not $jdk25) {
    throw "Java 25 nao encontrado."
}

$env:JAVA_HOME = $jdk25.FullName
$env:Path = "$($jdk25.FullName)\bin;$env:Path"
$env:HYTALE_SERVER_JAR = "$env:APPDATA\Hytale\install\release\package\game\latest\Server\HytaleServer.jar"

if (-not (Test-Path -LiteralPath $env:HYTALE_SERVER_JAR)) {
    throw "HytaleServer.jar nao encontrado: $env:HYTALE_SERVER_JAR"
}

if (-not $SkipBuild) {
    Write-Host ""
    Write-Host "=== BUILD AMIGONPC ===" -ForegroundColor Yellow
    .\gradlew.bat --stop | Out-Host
    .\gradlew.bat clean build --console=plain | Out-Host
    if ($LASTEXITCODE -ne 0) {
        throw "BUILD FALHOU."
    }
}

$sourceJar = Join-Path $Repo "build\libs\AmigoNPC-$version.jar"
if (-not (Test-Path -LiteralPath $sourceJar)) {
    throw "JAR nao encontrado: $sourceJar"
}

$modsDir = "$env:APPDATA\Hytale\UserData\Mods"
$targetJar = Join-Path $modsDir "AmigoNPC-$version.jar"
New-Item -ItemType Directory -Path $modsDir -Force | Out-Null

if (Test-Path -LiteralPath $targetJar) {
    try {
        $stream = [System.IO.File]::Open($targetJar, [System.IO.FileMode]::Open, [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None)
        $stream.Dispose()
    }
    catch {
        Write-Host ""
        Write-Host "O JAR instalado ainda esta em uso pelo Hytale." -ForegroundColor Red
        Write-Host "No jogo/console execute:" -ForegroundColor Yellow
        Write-Host "/plugin unload $pluginId" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "Depois rode este script novamente." -ForegroundColor Yellow
        throw "Plugin precisa ser descarregado antes de substituir o JAR."
    }
}

Copy-Item -LiteralPath $sourceJar -Destination $targetJar -Force

$sourceHash = (Get-FileHash -LiteralPath $sourceJar -Algorithm SHA256).Hash
$targetHash = (Get-FileHash -LiteralPath $targetJar -Algorithm SHA256).Hash

if ($sourceHash -ne $targetHash) {
    throw "Falha de integridade no deploy. SHA256 origem != destino."
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host " AMIGONPC DEV DEPLOY CONCLUIDO" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host "JAR:    $targetJar"
Write-Host "SHA256: $targetHash"
Write-Host ""
Write-Host "No jogo/console execute:" -ForegroundColor Yellow
Write-Host "/plugin load $pluginId" -ForegroundColor Cyan
Write-Host ""
Write-Host "Para apenas recarregar configuracoes do AmigoNPC:" -ForegroundColor Yellow
Write-Host "/amigo reload" -ForegroundColor Cyan
