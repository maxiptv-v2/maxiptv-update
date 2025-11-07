# Script para Migrar Usuários do Bin Antigo para o Novo Bin
Write-Host "🔄 MIGRANDO USUÁRIOS DO BIN ANTIGO PARA O NOVO" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

# Configurações
$jsonbin_url_antigo = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest"
$jsonbin_url_novo = "https://api.jsonbin.io/v3/b/690be6da43b1c97be99b8bc7/latest"
$jsonbin_update_novo = "https://api.jsonbin.io/v3/b/690be6da43b1c97be99b8bc7"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'
$headers = @{
    "X-Master-Key" = $apiKey
    "Content-Type" = "application/json"
}

# 1. Buscar dados do bin antigo
Write-Host "1️⃣ Buscando usuários do bin antigo..." -ForegroundColor Yellow

try {
    $responseAntigo = Invoke-RestMethod -Uri $jsonbin_url_antigo -Method Get -Headers $headers -ErrorAction Stop
    $usersAntigo = $responseAntigo.record.users
    
    if (-not $usersAntigo -or ($usersAntigo | Measure-Object).Count -eq 0) {
        Write-Host "❌ Nenhum usuário encontrado no bin antigo!" -ForegroundColor Red
        exit 1
    }
    
    $userCount = if ($usersAntigo -is [System.Array]) { $usersAntigo.Count } else { ($usersAntigo | Get-Member -MemberType NoteProperty).Count }
    
    Write-Host "✅ Encontrados $userCount usuário(s) no bin antigo" -ForegroundColor Green
    Write-Host ""
    
    # Listar usuários
    foreach ($user in $usersAntigo) {
        Write-Host "  • $($user.username) (ID: $($user.id))" -ForegroundColor White
    }
    
} catch {
    $statusCode = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { "N/A" }
    
    if ($statusCode -eq 403) {
        Write-Host "❌ Bin antigo bloqueado (limite de requisições atingido)" -ForegroundColor Red
        Write-Host "💡 Você precisa copiar manualmente os usuários ou criar novamente no painel admin" -ForegroundColor Yellow
        exit 1
    } else {
        Write-Host "❌ Erro ao buscar bin antigo: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host ""

# 2. Buscar dados do novo bin
Write-Host "2️⃣ Buscando estrutura do novo bin..." -ForegroundColor Yellow

try {
    $responseNovo = Invoke-RestMethod -Uri $jsonbin_url_novo -Method Get -Headers $headers -ErrorAction Stop
    $recordNovo = $responseNovo.record
    
    Write-Host "✅ Novo bin acessado!" -ForegroundColor Green
    Write-Host ""
    
    # Preparar estrutura
    $recordFinal = @{}
    
    # Preservar campos existentes
    foreach ($key in $recordNovo.PSObject.Properties.Name) {
        if ($key -ne 'users' -and $key -ne 'sessions') {
            $recordFinal[$key] = $recordNovo.$key
        }
    }
    
    # Adicionar sessions (se não existir, criar vazio)
    if ($recordNovo.sessions) {
        $recordFinal['sessions'] = $recordNovo.sessions
    } else {
        $recordFinal['sessions'] = @{}
    }
    
    # Adicionar usuarios (migrar do antigo)
    $recordFinal['users'] = @()
    
    foreach ($user in $usersAntigo) {
        $recordFinal['users'] += @{
            id = $user.id
            username = $user.username
            password = $user.password
            apiUrl = $user.apiUrl
            expiryDate = $user.expiryDate
        }
    }
    
    Write-Host "📋 Estrutura preparada:" -ForegroundColor Yellow
    Write-Host "   Sessions: $($recordFinal['sessions'].Count) item(s)" -ForegroundColor White
    Write-Host "   Users: $($recordFinal['users'].Count) usuário(s)" -ForegroundColor White
    Write-Host "   Outros campos: $($recordFinal.Keys.Count - 2) campo(s)" -ForegroundColor White
    
} catch {
    Write-Host "❌ Erro ao preparar novo bin: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host ""

# 3. Confirmar migração
Write-Host "3️⃣ Confirmando migração..." -ForegroundColor Yellow

$confirm = Read-Host "Deseja migrar $userCount usuário(s) para o novo bin? (S/N)"

if ($confirm -ne 'S' -and $confirm -ne 's') {
    Write-Host "❌ Migração cancelada pelo usuário" -ForegroundColor Yellow
    exit 0
}

Write-Host ""
Write-Host ""

# 4. Salvar no novo bin
Write-Host "4️⃣ Salvando no novo bin..." -ForegroundColor Yellow

try {
    $jsonBody = $recordFinal | ConvertTo-Json -Depth 10
    
    $putHeaders = @{
        "X-Master-Key" = $apiKey
        "Content-Type" = "application/json"
    }
    
    $response = Invoke-RestMethod -Uri $jsonbin_update_novo -Method Put -Headers $putHeaders -Body $jsonBody -ErrorAction Stop
    
    Write-Host "✅ Migração concluída com sucesso!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📊 Resultado:" -ForegroundColor Yellow
    Write-Host "   $userCount usuário(s) migrado(s)" -ForegroundColor White
    Write-Host ""
    Write-Host "💡 Próximos passos:" -ForegroundColor Cyan
    Write-Host "   1. Abra o painel admin no app (5 toques no logo)" -ForegroundColor White
    Write-Host "   2. Os usuários devem aparecer automaticamente" -ForegroundColor White
    Write-Host "   3. Use o botão 'Sincronizar' se necessário" -ForegroundColor White
    
} catch {
    Write-Host "❌ Erro ao salvar no novo bin: $($_.Exception.Message)" -ForegroundColor Red
    $statusCode = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { "N/A" }
    Write-Host "   Status Code: $statusCode" -ForegroundColor Red
    
    if ($Verbose) {
        Write-Host ""
        Write-Host "JSON que tentou salvar:" -ForegroundColor Yellow
        Write-Host $jsonBody -ForegroundColor Gray
    }
    
    exit 1
}

Write-Host ""
Write-Host "✅ Script concluído!" -ForegroundColor Green




