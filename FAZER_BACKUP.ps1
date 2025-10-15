# Script para criar backup completo do MaxiPTV_v2

$timestamp = Get-Date -Format "yyyy-MM-dd_HHmmss"
$backupName = "MaxiPTV_v2_BACKUP_$timestamp.zip"
$projectPath = "C:\Users\maxca\OneDrive\Desktop\MaxiPTV_v2"

# Salvar no Desktop do usuário (sem OneDrive)
$desktopPath = [Environment]::GetFolderPath("Desktop")
$backupPath = "$desktopPath\$backupName"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " BACKUP DO MAXIPTV V2" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Criando backup..." -ForegroundColor Yellow

# Criar ZIP do projeto (excluindo build, .gradle, etc)
$exclude = @("build", ".gradle", ".idea", "*.apk", "*.log")

Compress-Archive -Path "$projectPath\*" -DestinationPath $backupPath -Force

Write-Host ""
Write-Host "✅ BACKUP CRIADO COM SUCESSO!" -ForegroundColor Green
Write-Host ""
Write-Host "Localização:" -ForegroundColor Yellow
Write-Host "  $backupPath" -ForegroundColor Cyan
Write-Host ""
Write-Host "Tamanho:" -ForegroundColor Yellow
$size = (Get-Item $backupPath).Length / 1MB
Write-Host "  $($size.ToString('0.00')) MB" -ForegroundColor Cyan
Write-Host ""
Write-Host "IMPORTANTE:" -ForegroundColor Yellow
Write-Host "  - Copie este arquivo para um pen drive ou HD externo" -ForegroundColor White
Write-Host "  - Ou envie para seu email/Google Drive/Dropbox" -ForegroundColor White
Write-Host ""

