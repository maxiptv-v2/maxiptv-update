# Script de Diagnóstico - Sincronização de Usuários do Painel Admin
# Verifica por que os usuários não estão sendo sincronizados do JSONBin para o app

param(
    [switch]$Verbose
)

Write-Host "🔍 DIAGNÓSTICO - SINCRONIZAÇÃO DE USUÁRIOS" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

# Configurações
$jsonbin_url_novo = "https://api.jsonbin.io/v3/b/690be6da43b1c97be99b8bc7/latest"
$jsonbin_url_antigo = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'
$headers = @{
    "X-Master-Key" = $apiKey
}

# ==================== ETAPA 1: Verificar novo bin ====================
Write-Host "1️⃣ VERIFICANDO NOVO BIN (690be6da43b1c97be99b8bc7)" -ForegroundColor Cyan
Write-Host "───────────────────────────────────────────────────────────────────────────────" -ForegroundColor Gray

try {
    $response = Invoke-RestMethod -Uri $jsonbin_url_novo -Method Get -Headers $headers -ErrorAction Stop
    
    $record = $response.record
    
    Write-Host "✅ Novo bin acessado com sucesso!" -ForegroundColor Green
    Write-Host ""
    
    # Verificar estrutura
    $keys = $record.PSObject.Properties.Name
    Write-Host "📦 Chaves encontradas: $($keys -join ', ')" -ForegroundColor White
    Write-Host ""
    
    # Verificar usuários
    if ($record.users) {
        $users = $record.users
        $userCount = if ($users -is [System.Array]) { $users.Count } else { ($users | Get-Member -MemberType NoteProperty).Count }
        
        Write-Host "👥 Usuários no novo bin: $userCount" -ForegroundColor Yellow
        
        if ($userCount -gt 0) {
            Write-Host ""
            Write-Host "Lista de usuários:" -ForegroundColor Yellow
            foreach ($user in $users) {
                Write-Host "  • Username: $($user.username)" -ForegroundColor White
                Write-Host "    ID: $($user.id)" -ForegroundColor Gray
                Write-Host "    API URL: $($user.apiUrl)" -ForegroundColor Gray
                Write-Host "    Expiry Date: $($user.expiryDate)" -ForegroundColor Gray
                Write-Host ""
            }
        } else {
            Write-Host "⚠️  Array de usuários está vazio!" -ForegroundColor Yellow
        }
    } else {
        Write-Host "❌ Campo 'users' não existe no novo bin!" -ForegroundColor Red
        Write-Host "💡 Precisa criar: { `"sessions`": {}, `"users`": [] }" -ForegroundColor Yellow
    }
    
    # Verificar sessions
    if ($record.sessions) {
        $sessions = $record.sessions
        $sessionCount = if ($sessions -is [hashtable] -or $sessions -is [PSCustomObject]) { 
            ($sessions | Get-Member -MemberType NoteProperty).Count 
        } else { 
            $sessions.Count 
        }
        Write-Host "🔐 Sessões no novo bin: $sessionCount" -ForegroundColor Yellow
    } else {
        Write-Host "⚠️  Campo 'sessions' não existe no novo bin" -ForegroundColor Yellow
    }
    
    # Verificar códigos
    $codes = $keys | Where-Object { $_ -match '^[A-Za-z0-9]{3,10}$' -and $_ -ne 'sessions' -and $_ -ne 'users' }
    $codeCount = ($codes | Measure-Object).Count
    Write-Host "🔑 Códigos no novo bin: $codeCount" -ForegroundColor Yellow
    
    if ($codeCount -gt 0) {
        Write-Host "Primeiros 5 códigos: $($codes[0..4] -join ', ')" -ForegroundColor Gray
    }
    
} catch {
    Write-Host "❌ Erro ao acessar novo bin: $($_.Exception.Message)" -ForegroundColor Red
    $statusCode = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { "N/A" }
    Write-Host "   Status Code: $statusCode" -ForegroundColor Red
}

Write-Host ""
Write-Host ""

# ==================== ETAPA 2: Verificar bin antigo (para comparação) ====================
Write-Host "2️⃣ VERIFICANDO BIN ANTIGO (68ec647643b1c97be964e96b) - COMPARAÇÃO" -ForegroundColor Cyan
Write-Host "───────────────────────────────────────────────────────────────────────────────" -ForegroundColor Gray

try {
    $response = Invoke-RestMethod -Uri $jsonbin_url_antigo -Method Get -Headers $headers -ErrorAction Stop
    
    $record = $response.record
    
    Write-Host "✅ Bin antigo acessado!" -ForegroundColor Green
    Write-Host ""
    
    if ($record.users) {
        $users = $record.users
        $userCount = if ($users -is [System.Array]) { $users.Count } else { ($users | Get-Member -MemberType NoteProperty).Count }
        
        Write-Host "👥 Usuários no bin antigo: $userCount" -ForegroundColor Yellow
        
        if ($userCount -gt 0) {
            Write-Host ""
            Write-Host "Lista de usuários:" -ForegroundColor Yellow
            foreach ($user in $users) {
                Write-Host "  • Username: $($user.username)" -ForegroundColor White
                Write-Host "    ID: $($user.id)" -ForegroundColor Gray
                Write-Host "    API URL: $($user.apiUrl)" -ForegroundColor Gray
                Write-Host "    Expiry Date: $($user.expiryDate)" -ForegroundColor Gray
                Write-Host ""
            }
        }
    } else {
        Write-Host "⚠️  Campo 'users' não existe no bin antigo" -ForegroundColor Yellow
    }
    
    # Verificar códigos no bin antigo
    $keys = $record.PSObject.Properties.Name
    $codes = $keys | Where-Object { $_ -match '^[A-Za-z0-9]{3,10}$' -and $_ -ne 'sessions' -and $_ -ne 'users' }
    $codeCount = ($codes | Measure-Object).Count
    Write-Host "🔑 Códigos no bin antigo: $codeCount" -ForegroundColor Yellow
    
} catch {
    $statusCode = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { "N/A" }
    
    if ($statusCode -eq 403) {
        Write-Host "❌ Bin antigo bloqueado (limite de requisições atingido)" -ForegroundColor Red
    } else {
        Write-Host "⚠️  Erro ao acessar bin antigo: $($_.Exception.Message)" -ForegroundColor Yellow
        Write-Host "   Status Code: $statusCode" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host ""

# ==================== ETAPA 3: Verificar como getAllUsers() funciona ====================
Write-Host "3️⃣ VERIFICANDO LÓGICA DE SINCRONIZAÇÃO" -ForegroundColor Cyan
Write-Host "───────────────────────────────────────────────────────────────────────────────" -ForegroundColor Gray

Write-Host "📋 Passos que o AdminActivity deveria seguir:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. AdminActivity abre → LaunchedEffect(isAuthenticated)" -ForegroundColor White
Write-Host "2. Chama SessionManager.getAllUsers()" -ForegroundColor White
Write-Host "3. SessionManager.getAllUsers() chama fetchSessions()" -ForegroundColor White
Write-Host "4. fetchSessions() faz GET em: $jsonbin_url_novo" -ForegroundColor White
Write-Host "5. Extrai o array 'users' do record" -ForegroundColor White
Write-Host "6. Retorna lista de GlobalUser" -ForegroundColor White
Write-Host "7. AdminActivity itera sobre os usuários" -ForegroundColor White
Write-Host "8. Para cada usuário:" -ForegroundColor White
Write-Host "   - Se não existe localmente → adiciona" -ForegroundColor White
Write-Host "   - Se existe → atualiza se necessário" -ForegroundColor White
Write-Host ""

Write-Host "🔍 Verificando possíveis problemas:" -ForegroundColor Yellow
Write-Host ""

# Verificar se o novo bin tem estrutura correta
try {
    $response = Invoke-RestMethod -Uri $jsonbin_url_novo -Method Get -Headers $headers -ErrorAction Stop
    $record = $response.record
    
    # Problema 1: users não existe
    if (-not $record.users) {
        Write-Host "❌ PROBLEMA 1: Campo 'users' não existe no novo bin!" -ForegroundColor Red
        Write-Host "   Solução: Inicializar bin com: { `"sessions`": {}, `"users`": [] }" -ForegroundColor Yellow
    } else {
        Write-Host "✅ Campo 'users' existe" -ForegroundColor Green
    }
    
    # Problema 2: users não é array
    if ($record.users) {
        $users = $record.users
        if ($users -isnot [System.Array] -and $users -isnot [System.Collections.ArrayList]) {
            Write-Host "❌ PROBLEMA 2: Campo 'users' não é um array!" -ForegroundColor Red
            Write-Host "   Tipo: $($users.GetType().Name)" -ForegroundColor Yellow
        } else {
            Write-Host "✅ Campo 'users' é um array válido" -ForegroundColor Green
        }
    }
    
    # Problema 3: users está vazio
    if ($record.users) {
        $users = $record.users
        $userCount = if ($users -is [System.Array]) { $users.Count } else { ($users | Get-Member -MemberType NoteProperty).Count }
        
        if ($userCount -eq 0) {
            Write-Host "❌ PROBLEMA 3: Array 'users' está vazio!" -ForegroundColor Red
            Write-Host "   Solução: Adicionar usuários no painel admin ou migrar do bin antigo" -ForegroundColor Yellow
        } else {
            Write-Host "✅ Array 'users' tem $userCount usuário(s)" -ForegroundColor Green
        }
    }
    
    # Problema 4: Estrutura dos usuários
    if ($record.users -and ($record.users | Measure-Object).Count -gt 0) {
        $firstUser = $record.users[0]
        $requiredFields = @('id', 'username', 'password', 'apiUrl', 'expiryDate')
        $missingFields = @()
        
        foreach ($field in $requiredFields) {
            if (-not $firstUser.$field) {
                $missingFields += $field
            }
        }
        
        if ($missingFields.Count -gt 0) {
            Write-Host "❌ PROBLEMA 4: Usuários não têm todos os campos necessários!" -ForegroundColor Red
            Write-Host "   Campos faltando: $($missingFields -join ', ')" -ForegroundColor Yellow
        } else {
            Write-Host "✅ Estrutura dos usuários está correta" -ForegroundColor Green
        }
    }
    
} catch {
    Write-Host "❌ Erro ao verificar estrutura: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host ""

# ==================== ETAPA 4: Verificar se há dados para migrar ====================
Write-Host "4️⃣ VERIFICANDO SE HÁ DADOS PARA MIGRAR" -ForegroundColor Cyan
Write-Host "───────────────────────────────────────────────────────────────────────────────" -ForegroundColor Gray

try {
    # Tentar acessar bin antigo
    $responseAntigo = $null
    try {
        $responseAntigo = Invoke-RestMethod -Uri $jsonbin_url_antigo -Method Get -Headers $headers -ErrorAction Stop
    } catch {
        Write-Host "⚠️  Bin antigo não acessível (limite atingido ou erro)" -ForegroundColor Yellow
    }
    
    # Verificar novo bin
    $responseNovo = Invoke-RestMethod -Uri $jsonbin_url_novo -Method Get -Headers $headers -ErrorAction Stop
    
    if ($responseAntigo -and $responseNovo) {
        $usersAntigo = $responseAntigo.record.users
        $usersNovo = $responseNovo.record.users
        
        $countAntigo = if ($usersAntigo -is [System.Array]) { $usersAntigo.Count } else { 0 }
        $countNovo = if ($usersNovo -is [System.Array]) { $usersNovo.Count } else { 0 }
        
        Write-Host "📊 Comparação:" -ForegroundColor Yellow
        Write-Host "   Bin antigo: $countAntigo usuário(s)" -ForegroundColor White
        Write-Host "   Bin novo: $countNovo usuário(s)" -ForegroundColor White
        Write-Host ""
        
        if ($countAntigo -gt 0 -and $countNovo -eq 0) {
            Write-Host "💡 AÇÃO NECESSÁRIA: Migrar usuários do bin antigo para o novo!" -ForegroundColor Yellow
            Write-Host ""
            Write-Host "Opções:" -ForegroundColor Cyan
            Write-Host "   1. Copiar manualmente os usuários do bin antigo para o novo" -ForegroundColor White
            Write-Host "   2. Criar um script de migração" -ForegroundColor White
            Write-Host "   3. Recriar os usuários no painel admin do novo bin" -ForegroundColor White
        } elseif ($countAntigo -eq 0 -and $countNovo -eq 0) {
            Write-Host "⚠️  Nenhum usuário encontrado em nenhum bin!" -ForegroundColor Yellow
            Write-Host "   Você precisa criar usuários no painel admin primeiro" -ForegroundColor Yellow
        }
    }
    
} catch {
    Write-Host "❌ Erro ao comparar bins: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host ""

# ==================== RESUMO FINAL ====================
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "📊 RESUMO E SOLUÇÕES" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

Write-Host "💡 SOLUÇÕES PARA SINCRONIZAÇÃO:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Se o novo bin está vazio:" -ForegroundColor White
Write-Host "   → Abra o painel admin (5 toques no logo)" -ForegroundColor Gray
Write-Host "   → Adicione usuários manualmente" -ForegroundColor Gray
Write-Host "   → Ou copie os usuários do bin antigo para o novo" -ForegroundColor Gray
Write-Host ""
Write-Host "2. Se o campo 'users' não existe no novo bin:" -ForegroundColor White
Write-Host "   → Inicialize o bin com: { `"sessions`": {}, `"users`": [] }" -ForegroundColor Gray
Write-Host ""
Write-Host "3. Para verificar se a sincronização está funcionando:" -ForegroundColor White
Write-Host "   → Abra o painel admin" -ForegroundColor Gray
Write-Host "   → Veja os logs do Android (adb logcat | grep AdminActivity)" -ForegroundColor Gray
Write-Host "   → Procure por mensagens de sincronização" -ForegroundColor Gray
Write-Host ""
Write-Host "✅ Script concluído!" -ForegroundColor Green
Write-Host ""




