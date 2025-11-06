# Script para criar novo Bin no JSONBin e atualizar o código
Write-Host "🔧 Criando novo Bin no JSONBin..." -ForegroundColor Cyan
Write-Host ""

# API Key atual (mantém a mesma)
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'

Write-Host "📋 INSTRUÇÕES:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Acesse: https://jsonbin.io/app/bins" -ForegroundColor Cyan
Write-Host "2. Clique em 'Create New Bin'" -ForegroundColor Cyan
Write-Host "3. Cole este JSON inicial:" -ForegroundColor Cyan
Write-Host ""
Write-Host "   {`"sessions`": {}, `"users`": []}" -ForegroundColor Green
Write-Host ""
Write-Host "4. Clique em 'Create'" -ForegroundColor Cyan
Write-Host "5. Copie o BIN ID da URL (ex: 68ec647643b1c97be964e96b)" -ForegroundColor Cyan
Write-Host "6. Cole o BIN ID abaixo quando solicitado" -ForegroundColor Cyan
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
Write-Host "🧪 Testando novo Bin..." -ForegroundColor Yellow

try {
    $url = "https://api.jsonbin.io/v3/b/$newBinId/latest"
    $headers = @{
        "X-Master-Key" = $apiKey
    }
    
    $response = Invoke-RestMethod -Uri $url -Method Get -Headers $headers -ErrorAction Stop
    
    Write-Host "✅ Novo Bin está funcionando!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📊 Dados do novo Bin:" -ForegroundColor Cyan
    Write-Host "   Bin ID: $newBinId"
    Write-Host "   Status: Ativo"
    
    # Verificar estrutura
    if ($response.record) {
        Write-Host "   Estrutura: OK"
    }
    
    Write-Host ""
    Write-Host "🔄 Atualizando código..." -ForegroundColor Yellow
    
    # Atualizar SessionManager.kt
    $sessionManagerFile = "app/src/main/java/com/maxiptv/data/SessionManager.kt"
    
    if (Test-Path $sessionManagerFile) {
        $content = Get-Content $sessionManagerFile -Raw
        
        # Substituir Bin ID
        $oldPattern = 'private const val JSONBIN_BIN_ID = "[^"]*"'
        $newContent = $content -replace $oldPattern, "private const val JSONBIN_BIN_ID = `"$newBinId`""
        
        Set-Content -Path $sessionManagerFile -Value $newContent -NoNewline
        
        Write-Host "✅ SessionManager.kt atualizado!" -ForegroundColor Green
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
            $oldPattern = '68ec647643b1c97be964e96b'
            $newContent = $content -replace $oldPattern, $newBinId
            
            if ($newContent -ne $content) {
                Set-Content -Path $phpFile -Value $newContent -NoNewline
                Write-Host "✅ $phpFile atualizado!" -ForegroundColor Green
            }
        }
    }
    
    Write-Host ""
    Write-Host "✅ ATUALIZAÇÃO CONCLUÍDA!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📝 PRÓXIMOS PASSOS:" -ForegroundColor Cyan
    Write-Host "   1. Compile o app novamente" -ForegroundColor Yellow
    Write-Host "   2. Faça deploy dos arquivos PHP atualizados no Render" -ForegroundColor Yellow
    Write-Host "   3. Teste o app para garantir que está funcionando" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "⚠️  IMPORTANTE:" -ForegroundColor Yellow
    Write-Host "   - O novo Bin começa com 10.000 requisições disponíveis" -ForegroundColor Yellow
    Write-Host "   - Você precisará migrar os dados do Bin antigo se necessário" -ForegroundColor Yellow
    Write-Host "   - Ou pode começar do zero com o novo Bin" -ForegroundColor Yellow
    
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host ""
    Write-Host "❌ ERRO ao testar novo Bin:" -ForegroundColor Red
    Write-Host "   Status Code: $statusCode" -ForegroundColor Red
    Write-Host "   Mensagem: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "💡 Verifique:" -ForegroundColor Yellow
    Write-Host "   1. Se o Bin ID está correto" -ForegroundColor Yellow
    Write-Host "   2. Se a API Key está correta" -ForegroundColor Yellow
    Write-Host "   3. Se o Bin foi criado corretamente" -ForegroundColor Yellow
    exit 1
}

