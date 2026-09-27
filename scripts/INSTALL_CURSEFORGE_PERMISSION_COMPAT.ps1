$ErrorActionPreference = "Stop"

$repo = "C:\Users\TONES\Documents\AmigoNPC"
$branch = "recovery/amigonpc-2.0.1-curseforge-exact"
$baselineSha = "31B3515FD5897F2D81818030EC9692B485B0850DEE0E452B1804D5630328ED17"

$modsDir = Join-Path $env:APPDATA "Hytale\UserData\Mods"
$baselineInstalled = Join-Path $modsDir "AmigoNPC-2.0.1.jar"
$compatBuilt = Join-Path $repo "build\libs\AmigoNPC-PermissionCompat-1.0.0.jar"
$compatInstalled = Join-Path $modsDir "AmigoNPC-PermissionCompat-1.0.0.jar"

Set-Location $repo

if (@(git status --porcelain).Count -gt 0) {
    git status --short
    throw "Existem alteracoes locais."
}

git fetch origin --prune
git switch $branch
git pull --ff-only origin $branch

$head = (git rev-parse HEAD).Trim()
Write-Host "Branch: $branch"
Write-Host "HEAD:   $head"

if (-not (Test-Path -LiteralPath $baselineInstalled)) {
    throw "AmigoNPC original nao esta instalado: $baselineInstalled"
}

$currentBaselineSha = (Get-FileHash -LiteralPath $baselineInstalled -Algorithm SHA256).Hash.ToUpperInvariant()
if ($currentBaselineSha -ne $baselineSha) {
    throw "O JAR principal nao e a baseline CurseForge. SHA atual: $currentBaselineSha"
}

$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:HYTALE_SERVER_JAR = "$env:APPDATA\Hytale\install\release\package\game\latest\Server\HytaleServer.jar"

& .\gradlew.bat curseForgePermissionCompatJar --console=plain | Out-Host
if ($LASTEXITCODE -ne 0) {
    throw "Falha ao compilar o shim de permissao."
}

if (-not (Test-Path -LiteralPath $compatBuilt)) {
    throw "JAR de compatibilidade nao encontrado: $compatBuilt"
}

New-Item -ItemType Directory -Force -Path $modsDir | Out-Null

# Instala a correcao de permissao na pasta Mods.
Copy-Item -LiteralPath $compatBuilt -Destination $compatInstalled -Force

$afterBaselineSha = (Get-FileHash -LiteralPath $baselineInstalled -Algorithm SHA256).Hash.ToUpperInvariant()
if ($afterBaselineSha -ne $baselineSha) {
    throw "ERRO: o JAR original foi alterado."
}

Write-Host ""
Write-Host "CORRECAO DE PERMISSOES INSTALADA" -ForegroundColor Green
Write-Host "Baseline original:"
Write-Host "  $baselineInstalled"
Write-Host "  SHA256: $afterBaselineSha"
Write-Host ""
Write-Host "Shim:"
Write-Host "  $compatInstalled"
Write-Host "  SHA256: $((Get-FileHash -LiteralPath $compatInstalled -Algorithm SHA256).Hash)"
Write-Host ""
Write-Host "Feche completamente o Hytale antes de executar este script e abra novamente depois." -ForegroundColor Yellow
