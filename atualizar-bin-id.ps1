# Script para atualizar Bin ID no código
Write-Host "🔧 Atualizando Bin ID no código..." -ForegroundColor Cyan
Write-Host ""

Write-Host "📋 INSTRUÇÕES:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Acesse: https://jsonbin.io/app/bins" -ForegroundColor Cyan
Write-Host "2. Clique em 'Create New Bin'" -ForegroundColor Cyan
Write-Host "3. Cole este JSON inicial:" -ForegroundColor Cyan
Write-Host ""
Write-Host '   {"sessions": {}, "users": []}' -ForegroundColor Green
Write-Host ""
Write-Host "4. Clique em 'Create'" -ForegroundColor Cyan
Write-Host "5. Copie o BIN ID da URL" -ForegroundColor Cyan
Write-Host "   Exemplo: https://jsonbin.io/app/bins/68ec647643b1c97be964e96b" -ForegroundColor Cyan
Write-Host "          O Bin ID é: 68ec647643b1c97be964e96b" -ForegroundColor Cyan
Write-Host ""

# Solicitar novo Bin ID
$newBinId = Read-Host "Cole o NOVO BIN ID aqui"

if ([string]::IsNullOrWhiteSpace($newBinId)) {
    Write-Host "❌ Bin ID não pode ser vazio!" -ForegroundColor Red
    exit 1
}

# Remover espaços e caracteres especiais
$newBinId = $newBinId.Trim()

Write-Host ""
Write-Host "🔄 Atualizando arquivos..." -ForegroundColor Yellow

# Bin ID antigo
$oldBinId = "68ec647643b1c97be964e96b"

# API Key (mantém a mesma)
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'

Write-Host ""
Write-Host "🔑 API Key (mantida): $apiKey" -ForegroundColor Cyan
Write-Host "📦 Bin ID antigo: $oldBinId" -ForegroundColor Yellow
Write-Host "📦 Bin ID novo: $newBinId" -ForegroundColor Green
Write-Host ""

# Atualizar SessionManager.kt
$sessionManagerFile = "app/src/main/java/com/maxiptv/data/SessionManager.kt"

if (Test-Path $sessionManagerFile) {
    $content = Get-Content $sessionManagerFile -Raw
    
    # Substituir Bin ID
    $content = $content -replace $oldBinId, $newBinId
    
    Set-Content -Path $sessionManagerFile -Value $content -NoNewline
    
    Write-Host "✅ SessionManager.kt atualizado!" -ForegroundColor Green
    Write-Host "   Bin ID antigo: $oldBinId" -ForegroundColor Gray
    Write-Host "   Bin ID novo: $newBinId" -ForegroundColor Green
} else {
    Write-Host "⚠️  SessionManager.kt não encontrado" -ForegroundColor Yellow
}

# Atualizar arquivos PHP no servidor
$phpFiles = @(
    "server/dl.php",
    "server/get-pending-code.php",
    "server/auto_login.php",
    "server/debug-login.php"
)

foreach ($phpFile in $phpFiles) {
    if (Test-Path $phpFile) {
        $content = Get-Content $phpFile -Raw
        
        # Substituir Bin ID nas URLs
        $oldContent = $content
        $content = $content -replace $oldBinId, $newBinId
        
        if ($content -ne $oldContent) {
            Set-Content -Path $phpFile -Value $content -NoNewline
            Write-Host "✅ $phpFile atualizado!" -ForegroundColor Green
        }
    }
}

Write-Host ""
Write-Host "✅ ATUALIZAÇÃO CONCLUÍDA!" -ForegroundColor Green
Write-Host ""
Write-Host "📝 IMPORTANTE:" -ForegroundColor Cyan
Write-Host "   ✅ API Key mantida: $apiKey" -ForegroundColor Green
Write-Host "   ✅ Bin ID atualizado: $newBinId" -ForegroundColor Green
Write-Host ""
Write-Host "📝 PRÓXIMOS PASSOS:" -ForegroundColor Cyan
Write-Host "   1. Compile o app novamente" -ForegroundColor Yellow
Write-Host "   2. Faça deploy dos arquivos PHP atualizados no Render" -ForegroundColor Yellow
Write-Host "   3. Teste o app para garantir que está funcionando" -ForegroundColor Yellow
Write-Host ""
Write-Host "⚠️  LEMBRE-SE:" -ForegroundColor Yellow
Write-Host "   - Cada bin tem seu próprio limite de 10.000 requisições/mês" -ForegroundColor Yellow
Write-Host "   - O novo bin começa do zero (sem dados)" -ForegroundColor Yellow
Write-Host "   - Você precisará sincronizar usuários novamente pelo painel admin" -ForegroundColor Yellow

