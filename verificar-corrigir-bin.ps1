# Script para Verificar e Corrigir Estrutura do Novo Bin
Write-Host "🔧 VERIFICANDO E CORRIGINDO ESTRUTURA DO BIN" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

$jsonbin_url = "https://api.jsonbin.io/v3/b/690be6da43b1c97be99b8bc7/latest"
$jsonbin_update = "https://api.jsonbin.io/v3/b/690be6da43b1c97be99b8bc7"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'
$headers = @{
    "X-Master-Key" = $apiKey
}

# 1. Buscar estrutura atual
Write-Host "1️⃣ Buscando estrutura atual do bin..." -ForegroundColor Yellow

try {
    $response = Invoke-RestMethod -Uri $jsonbin_url -Method Get -Headers $headers -ErrorAction Stop
    $record = $response.record
    
    Write-Host "✅ Bin acessado!" -ForegroundColor Green
    Write-Host ""
    
    # Verificar campos existentes
    $keys = $record.PSObject.Properties.Name
    Write-Host "📦 Campos encontrados: $($keys -join ', ')" -ForegroundColor White
    Write-Host ""
    
    # Preparar estrutura correta
    $recordFinal = @{}
    
    # Preservar todos os campos existentes (incluindo _login_logs)
    foreach ($key in $keys) {
        $recordFinal[$key] = $record.$key
    }
    
    # Garantir que sessions existe
    if (-not $recordFinal['sessions']) {
        Write-Host "⚠️  Campo 'sessions' não existe - criando..." -ForegroundColor Yellow
        $recordFinal['sessions'] = @{}
    }
    
    # Garantir que users existe e é array
    if (-not $recordFinal['users']) {
        Write-Host "⚠️  Campo 'users' não existe - criando array vazio..." -ForegroundColor Yellow
        $recordFinal['users'] = @()
    } elseif ($recordFinal['users'] -isnot [System.Array]) {
        Write-Host "⚠️  Campo 'users' não é array - convertendo..." -ForegroundColor Yellow
        $recordFinal['users'] = @()
    }
    
    Write-Host ""
    Write-Host "📋 Estrutura preparada:" -ForegroundColor Yellow
    Write-Host "   Sessions: $($recordFinal['sessions'].Count) item(s)" -ForegroundColor White
    Write-Host "   Users: $($recordFinal['users'].Count) usuário(s)" -ForegroundColor White
    Write-Host "   _login_logs: $($recordFinal['_login_logs'].Count) log(s)" -ForegroundColor White
    
    # Contar códigos
    $codes = $keys | Where-Object { $_ -match '^[A-Za-z0-9]{3,10}$' -and $_ -ne 'sessions' -and $_ -ne 'users' }
    $codeCount = ($codes | Measure-Object).Count
    Write-Host "   Códigos: $codeCount" -ForegroundColor White
    
    Write-Host ""
    
    # Verificar se precisa atualizar
    $needsUpdate = $false
    
    if (-not $recordFinal['sessions'] -or $recordFinal['sessions'] -isnot [hashtable]) {
        $needsUpdate = $true
    }
    
    if (-not $recordFinal['users'] -or $recordFinal['users'] -isnot [System.Array]) {
        $needsUpdate = $true
    }
    
    if ($needsUpdate) {
        Write-Host "⚠️  Estrutura precisa ser corrigida!" -ForegroundColor Yellow
        Write-Host ""
        
        $confirm = Read-Host "Deseja corrigir a estrutura do bin? (S/N)"
        
        if ($confirm -eq 'S' -or $confirm -eq 's') {
            Write-Host ""
            Write-Host "💾 Salvando estrutura corrigida..." -ForegroundColor Yellow
            
            $jsonBody = $recordFinal | ConvertTo-Json -Depth 10
            
            $putHeaders = @{
                "X-Master-Key" = $apiKey
                "Content-Type" = "application/json"
            }
            
            $updateResponse = Invoke-RestMethod -Uri $jsonbin_update -Method Put -Headers $putHeaders -Body $jsonBody -ErrorAction Stop
            
            Write-Host "✅ Estrutura corrigida com sucesso!" -ForegroundColor Green
            Write-Host ""
            Write-Host "💡 Agora você pode:" -ForegroundColor Cyan
            Write-Host "   1. Criar códigos no painel admin" -ForegroundColor White
            Write-Host "   2. Ou migrar usuários do bin antigo" -ForegroundColor White
            Write-Host "   3. Os usuários serão sincronizados automaticamente" -ForegroundColor White
        } else {
            Write-Host "❌ Correção cancelada" -ForegroundColor Yellow
        }
    } else {
        Write-Host "✅ Estrutura do bin está correta!" -ForegroundColor Green
        Write-Host ""
        Write-Host "📊 Status:" -ForegroundColor Cyan
        Write-Host "   • Campo 'sessions' existe" -ForegroundColor White
        Write-Host "   • Campo 'users' existe e é array" -ForegroundColor White
        Write-Host "   • $($recordFinal['users'].Count) usuário(s) cadastrado(s)" -ForegroundColor White
        Write-Host "   • $codeCount código(s) cadastrado(s)" -ForegroundColor White
        Write-Host ""
        
        if ($recordFinal['users'].Count -eq 0) {
            Write-Host "⚠️  Nenhum usuário cadastrado ainda!" -ForegroundColor Yellow
            Write-Host "   Você precisa criar usuários no painel admin ou migrar do bin antigo" -ForegroundColor Yellow
        }
    }
    
} catch {
    Write-Host "❌ Erro: $($_.Exception.Message)" -ForegroundColor Red
    $statusCode = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { "N/A" }
    Write-Host "   Status Code: $statusCode" -ForegroundColor Red
}

Write-Host ""
Write-Host "✅ Script concluído!" -ForegroundColor Green




