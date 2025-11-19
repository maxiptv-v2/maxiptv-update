# Script para limpar arquivos desnecessários do app
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "LIMPEZA DE ARQUIVOS DESNECESSARIOS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$removed = 0
$kept = 0

# 1. Remover arquivos .md de documentação temporária (manter apenas README.md)
Write-Host "1. Removendo arquivos .md de documentacao temporaria..." -ForegroundColor Yellow
$mdFiles = Get-ChildItem -Path . -Filter "*.md" -Recurse | Where-Object { 
    $_.FullName -notmatch "README" -and 
    $_.FullName -notmatch "server\\" 
}

foreach ($file in $mdFiles) {
    Write-Host "   Removendo: $($file.Name)" -ForegroundColor Gray
    Remove-Item $file.FullName -Force
    $removed++
}
Write-Host "   Removidos: $removed arquivos .md" -ForegroundColor Green
Write-Host ""

# 2. Remover scripts PowerShell temporários (manter apenas build-release.ps1)
Write-Host "2. Removendo scripts PowerShell temporarios..." -ForegroundColor Yellow
$ps1Files = Get-ChildItem -Path . -Filter "*.ps1" -Recurse | Where-Object { 
    $_.Name -ne "build-release.ps1" -and
    $_.Name -ne "limpar-app-desnecessario.ps1" -and
    $_.FullName -notmatch "server\\"
}

$removed = 0
foreach ($file in $ps1Files) {
    Write-Host "   Removendo: $($file.Name)" -ForegroundColor Gray
    Remove-Item $file.FullName -Force
    $removed++
}
Write-Host "   Removidos: $removed scripts .ps1" -ForegroundColor Green
Write-Host ""

# 3. Remover arquivos temporários e de teste
Write-Host "3. Removendo arquivos temporarios e de teste..." -ForegroundColor Yellow
$tempFiles = @(
    "*.txt",
    "*.png",
    "*.jpg",
    "*.hprof",
    "*.json",
    "*.php"
) | Where-Object { 
    $_ -notmatch "version.json|update.json|keystore.properties|local.properties|gradle.properties"
}

$removed = 0
foreach ($pattern in $tempFiles) {
    $files = Get-ChildItem -Path . -Filter $pattern -Recurse | Where-Object {
        $_.FullName -notmatch "app\\src\\" -and
        $_.FullName -notmatch "server\\" -and
        $_.FullName -notmatch "gradle\\" -and
        $_.FullName -notmatch "build\\"
    }
    
    foreach ($file in $files) {
        Write-Host "   Removendo: $($file.Name)" -ForegroundColor Gray
        Remove-Item $file.FullName -Force
        $removed++
    }
}
Write-Host "   Removidos: $removed arquivos temporarios" -ForegroundColor Green
Write-Host ""

# 4. Verificar FingerprintApi.kt (não parece ser usado)
Write-Host "4. Verificando FingerprintApi.kt..." -ForegroundColor Yellow
$fingerprintApiFile = "app/src/main/java/com/maxiptv/data/FingerprintApi.kt"
if (Test-Path $fingerprintApiFile) {
    $content = Get-Content $fingerprintApiFile -Raw
    $references = Select-String -Path "app/src/main/java/com/maxiptv/**/*.kt" -Pattern "FingerprintApi\." -SimpleMatch
    if ($references.Count -eq 0) {
        Write-Host "   FingerprintApi.kt nao parece ser usado - MANTER por enquanto (verificar manualmente)" -ForegroundColor Yellow
        $kept++
    } else {
        Write-Host "   FingerprintApi.kt e usado em $($references.Count) arquivo(s)" -ForegroundColor Green
    }
}
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "LIMPEZA CONCLUIDA" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Arquivos removidos: $removed" -ForegroundColor Green
Write-Host "Arquivos mantidos (verificacao manual): $kept" -ForegroundColor Yellow
Write-Host ""

