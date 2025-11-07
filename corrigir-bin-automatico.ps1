# Script para Corrigir Automaticamente a Estrutura do Bin
Write-Host "🔧 CORRIGINDO ESTRUTURA DO BIN AUTOMATICAMENTE" -ForegroundColor Cyan
Write-Host ""

$jsonbin_url = "https://api.jsonbin.io/v3/b/690be6da43b1c97be99b8bc7/latest"
$jsonbin_update = "https://api.jsonbin.io/v3/b/690be6da43b1c97be99b8bc7"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'
$headers = @{
    "X-Master-Key" = $apiKey
}

try {
    $response = Invoke-RestMethod -Uri $jsonbin_url -Method Get -Headers $headers -ErrorAction Stop
    $record = $response.record
    
    Write-Host "✅ Bin acessado!" -ForegroundColor Green
    
    # Preparar estrutura corrigida
    $recordFinal = @{}
    
    # Preservar todos os campos existentes
    foreach ($key in $record.PSObject.Properties.Name) {
        $recordFinal[$key] = $record.$key
    }
    
    # Garantir que sessions é objeto
    if (-not $recordFinal['sessions'] -or $recordFinal['sessions'] -isnot [hashtable] -and $recordFinal['sessions'] -isnot [PSCustomObject]) {
        if ($recordFinal['sessions']) {
            # Converter para hashtable se necessário
            $sessions = @{}
            foreach ($key in ($recordFinal['sessions'] | Get-Member -MemberType NoteProperty).Name) {
                $sessions[$key] = $recordFinal['sessions'].$key
            }
            $recordFinal['sessions'] = $sessions
        } else {
            $recordFinal['sessions'] = @{}
        }
    }
    
    # Garantir que users é array
    if (-not $recordFinal['users']) {
        $recordFinal['users'] = @()
    } elseif ($recordFinal['users'] -isnot [System.Array]) {
        # Converter para array se necessário
        $users = @()
        if ($recordFinal['users'] -is [PSCustomObject] -or $recordFinal['users'] -is [hashtable]) {
            foreach ($key in ($recordFinal['users'] | Get-Member -MemberType NoteProperty).Name) {
                $users += $recordFinal['users'].$key
            }
        } else {
            $users = @($recordFinal['users'])
        }
        $recordFinal['users'] = $users
    }
    
    Write-Host "💾 Salvando estrutura corrigida..." -ForegroundColor Yellow
    
    $jsonBody = $recordFinal | ConvertTo-Json -Depth 10
    
    $putHeaders = @{
        "X-Master-Key" = $apiKey
        "Content-Type" = "application/json"
    }
    
    $updateResponse = Invoke-RestMethod -Uri $jsonbin_update -Method Put -Headers $putHeaders -Body $jsonBody -ErrorAction Stop
    
    Write-Host "✅ Estrutura corrigida com sucesso!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📊 Estrutura final:" -ForegroundColor Cyan
    Write-Host "   Sessions: $($recordFinal['sessions'].Count) item(s)" -ForegroundColor White
    Write-Host "   Users: $($recordFinal['users'].Count) usuário(s)" -ForegroundColor White
    Write-Host "   _login_logs: $($recordFinal['_login_logs'].Count) log(s)" -ForegroundColor White
    
    if ($recordFinal['users'].Count -gt 0) {
        Write-Host ""
        Write-Host "👥 Usuários encontrados:" -ForegroundColor Yellow
        foreach ($user in $recordFinal['users']) {
            Write-Host "   • $($user.username)" -ForegroundColor White
        }
    }
    
    Write-Host ""
    Write-Host "💡 Agora você pode:" -ForegroundColor Cyan
    Write-Host "   1. Abrir o painel admin (5 toques no logo)" -ForegroundColor White
    Write-Host "   2. Os usuários devem sincronizar automaticamente" -ForegroundColor White
    Write-Host "   3. Criar novos códigos normalmente" -ForegroundColor White
    
} catch {
    Write-Host "❌ Erro: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "✅ Script concluído!" -ForegroundColor Green




