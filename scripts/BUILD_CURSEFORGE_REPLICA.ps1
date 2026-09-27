$ErrorActionPreference = "Stop"

$repo = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$branch = "recovery/amigonpc-2.0.1-curseforge-exact"
$expectedSha = "31B3515FD5897F2D81818030EC9692B485B0850DEE0E452B1804D5630328ED17"

$referenceDir = Join-Path $repo "reference"
$baseline = Join-Path $referenceDir "AmigoNPC-2.0.1-CurseForge.jar"
$replica = Join-Path $repo "build\libs\AmigoNPC-2.0.1-curseforge-replica.jar"
$modsDir = Join-Path $env:APPDATA "Hytale\UserData\Mods"
$target = Join-Path $modsDir "AmigoNPC-2.0.1.jar"
$backupDir = Join-Path $env:APPDATA "Hytale\UserData\AmigoNPC_Jar_Backups"

function Get-Sha256([string]$Path) {
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToUpperInvariant()
}

Set-Location $repo

$currentBranch = (git branch --show-current).Trim()
if ($currentBranch -ne $branch) {
    throw "Branch incorreta. Atual: $currentBranch | Esperada: $branch"
}

if (@(git status --porcelain).Count -gt 0) {
    git status --short
    throw "Existem alteracoes locais. Pare antes de preparar a baseline."
}

New-Item -ItemType Directory -Force -Path $referenceDir | Out-Null

if (-not (Test-Path -LiteralPath $baseline)) {
    $candidates = @(
        (Join-Path $env:USERPROFILE "Downloads\AmigoNPC-2.0.1.jar"),
        (Join-Path $env:USERPROFILE "Desktop\AmigoNPC-2.0.1.jar")
    )

    $found = $null
    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate) {
            $candidateSha = Get-Sha256 $candidate
            if ($candidateSha -eq $expectedSha) {
                $found = $candidate
                break
            }
        }
    }

    if ($null -eq $found) {
        throw @"
Baseline original nao encontrada.

Coloque o JAR enviado no caminho:
$baseline

SHA-256 esperado:
$expectedSha
"@
    }

    Copy-Item -LiteralPath $found -Destination $baseline -Force
}

$baselineSha = Get-Sha256 $baseline
if ($baselineSha -ne $expectedSha) {
    throw "Baseline incorreta. SHA atual: $baselineSha | Esperado: $expectedSha"
}

Write-Host ""
Write-Host "BASELINE CURSEFORGE VALIDADA" -ForegroundColor Green
Write-Host "Arquivo: $baseline"
Write-Host "SHA256:  $baselineSha"

& .\gradlew.bat replicaCurseForge --console=plain | Out-Host
if ($LASTEXITCODE -ne 0) {
    throw "Falha ao gerar a replica CurseForge."
}

if (-not (Test-Path -LiteralPath $replica)) {
    throw "Replica nao encontrada: $replica"
}

$replicaSha = Get-Sha256 $replica
if ($replicaSha -ne $expectedSha) {
    throw "Replica nao e byte a byte identica ao original. SHA: $replicaSha"
}

Write-Host ""
Write-Host "REPLICA EXATA GERADA" -ForegroundColor Green
Write-Host "Arquivo: $replica"
Write-Host "SHA256:  $replicaSha"

New-Item -ItemType Directory -Force -Path $modsDir | Out-Null
New-Item -ItemType Directory -Force -Path $backupDir | Out-Null

if (Test-Path -LiteralPath $target) {
    $installedSha = Get-Sha256 $target
    $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $backup = Join-Path $backupDir "AmigoNPC-2.0.1-before-curseforge-replica-$stamp-$($installedSha.Substring(0,12)).jar"
    Copy-Item -LiteralPath $target -Destination $backup -Force
    Write-Host "Backup:  $backup"
}

# Instala a replica exata na pasta Mods.
Copy-Item -LiteralPath $replica -Destination $target -Force

$deployedSha = Get-Sha256 $target
if ($deployedSha -ne $expectedSha) {
    throw "Falha de integridade no deploy. SHA instalado: $deployedSha"
}

Write-Host ""
Write-Host "REPLICA CURSEFORGE INSTALADA" -ForegroundColor Green
Write-Host "Destino: $target"
Write-Host "SHA256:  $deployedSha"
Write-Host ""
Write-Host "Abra o Hytale e use esta versao como baseline de comportamento." -ForegroundColor Yellow
