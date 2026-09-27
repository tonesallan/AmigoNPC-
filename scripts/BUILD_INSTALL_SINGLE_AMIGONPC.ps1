$ErrorActionPreference = "Stop"

$repo = "C:\Users\TONES\Documents\AmigoNPC"
$branch = "recovery/amigonpc-2.0.1-curseforge-exact"
$baselineSha = "31B3515FD5897F2D81818030EC9692B485B0850DEE0E452B1804D5630328ED17"

$reference = Join-Path $repo "reference\AmigoNPC-2.0.1-CurseForge.jar"
$built = Join-Path $repo "build\libs\AmigoNPC-2.0.1.jar"
$mods = Join-Path $env:APPDATA "Hytale\UserData\Mods"
$target = Join-Path $mods "AmigoNPC-2.0.1.jar"
$oldCompat10 = Join-Path $mods "AmigoNPC-PermissionCompat-1.0.0.jar"
$oldCompat11 = Join-Path $mods "AmigoNPC-PermissionCompat-1.0.1.jar"
$backupDir = Join-Path $env:APPDATA "Hytale\UserData\AmigoNPC_Jar_Backups"

Set-Location $repo

if (@(git status --porcelain).Count -gt 0) {
    git status --short
    throw "Existem alteracoes locais."
}

git fetch origin --prune
git switch $branch
git pull --ff-only origin $branch

if (-not (Test-Path -LiteralPath $reference)) {
    throw "Baseline CurseForge ausente: $reference"
}

$referenceSha = (Get-FileHash -LiteralPath $reference -Algorithm SHA256).Hash.ToUpperInvariant()
if ($referenceSha -ne $baselineSha) {
    throw "Baseline CurseForge incorreta. SHA: $referenceSha"
}

$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:HYTALE_SERVER_JAR = "$env:APPDATA\Hytale\install\release\package\game\latest\Server\HytaleServer.jar"

& .\gradlew.bat curseForgeSingleJar --rerun-tasks --console=plain | Out-Host
if ($LASTEXITCODE -ne 0) {
    throw "Falha ao gerar o AmigoNPC unico corrigido."
}

if (-not (Test-Path -LiteralPath $built)) {
    throw "JAR final nao encontrado: $built"
}

New-Item -ItemType Directory -Force -Path $mods | Out-Null
New-Item -ItemType Directory -Force -Path $backupDir | Out-Null

if (Test-Path -LiteralPath $target) {
    $currentSha = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash.ToUpperInvariant()
    $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $backup = Join-Path $backupDir "AmigoNPC-2.0.1-before-single-$stamp-$($currentSha.Substring(0,12)).jar"
    Copy-Item -LiteralPath $target -Destination $backup -Force
    Write-Host "Backup: $backup"
}

foreach ($oldCompat in @($oldCompat10, $oldCompat11)) {
    if (Test-Path -LiteralPath $oldCompat) {
        Remove-Item -LiteralPath $oldCompat -Force
    }
}

Write-Host ""
Write-Host "JAR UNICO GERADO" -ForegroundColor Green
Write-Host "Origem: $built"
Write-Host "SHA256: $((Get-FileHash -LiteralPath $built -Algorithm SHA256).Hash)"
Write-Host ""

Copy-Item "C:\Users\TONES\Documents\AmigoNPC\build\libs\AmigoNPC-2.0.1.jar" "C:\Users\TONES\AppData\Roaming\Hytale\UserData\Mods\AmigoNPC-2.0.1.jar" -Force

Write-Host "MOD UNICO INSTALADO" -ForegroundColor Green
Write-Host "Destino: $target"
Write-Host "SHA256: $((Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash)"
Write-Host ""
Write-Host "Mods AmigoNPC instalados:"
Get-ChildItem -LiteralPath $mods -Filter "AmigoNPC*.jar" | Select-Object Name, Length
