# Script para verificar limite de requisições do JSONBin
Write-Host "🔍 Verificando status do JSONBin..." -ForegroundColor Cyan

$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'
$binId = '68ec647643b1c97be964e96b'

# Tentar fazer uma requisição GET para verificar status
try {
    $url = "https://api.jsonbin.io/v3/b/$binId/latest"
    $headers = @{
        "X-Master-Key" = $apiKey
    }
    
    Write-Host "📡 Fazendo requisição de teste..." -ForegroundColor Yellow
    
    $response = Invoke-RestMethod -Uri $url -Method Get -Headers $headers -ErrorAction Stop
    
    Write-Host "✅ Requisição bem-sucedida!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📊 Status do JSONBin:" -ForegroundColor Cyan
    Write-Host "   Bin ID: $binId"
    Write-Host "   Status: ONLINE"
    
    # Verificar se há dados no record
    if ($response.record) {
        $recordKeys = ($response.record | ConvertTo-Json -Depth 10 | ConvertFrom-Json).PSObject.Properties.Name
        Write-Host ""
        Write-Host "📦 Dados encontrados no JSONBin:" -ForegroundColor Cyan
        Write-Host "   Chaves principais: $($recordKeys -join ', ')"
        
        # Contar usuários
        if ($response.record.users) {
            $userCount = ($response.record.users | Measure-Object).Count
            Write-Host "   Usuários: $userCount"
        }
        
        # Contar sessões
        if ($response.record.sessions) {
            $sessionCount = ($response.record.sessions | Get-Member -MemberType NoteProperty).Count
            Write-Host "   Sessões: $sessionCount"
        }
        
        # Contar códigos (chaves de 3-10 caracteres alfanuméricos)
        $codes = $recordKeys | Where-Object { $_ -match '^[A-Za-z0-9]{3,10}$' }
        if ($codes) {
            Write-Host "   Códigos: $($codes.Count)"
        }
    }
    
    Write-Host ""
    Write-Host "⚠️  IMPORTANTE:" -ForegroundColor Yellow
    Write-Host "   O plano gratuito do JSONBin permite:" -ForegroundColor Yellow
    Write-Host "   - 10.000 requisições/mês" -ForegroundColor Yellow
    Write-Host "   - Verifique seu uso em: https://jsonbin.io/app/dashboard" -ForegroundColor Yellow
    
    Write-Host ""
    Write-Host "💡 DICAS PARA ECONOMIZAR REQUISIÇÕES:" -ForegroundColor Cyan
    Write-Host "   1. Evite abrir o painel admin muitas vezes"
    Write-Host "   2. Use o botão 'Sincronizar' manualmente quando necessário"
    Write-Host "   3. O heartbeat envia requisições a cada 30 segundos (normal)"
    Write-Host "   4. Cada GET e PUT conta como uma requisição"
    
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    $errorMessage = $_.Exception.Message
    
    Write-Host ""
    Write-Host "❌ ERRO ao verificar JSONBin:" -ForegroundColor Red
    Write-Host "   Status Code: $statusCode" -ForegroundColor Red
    
    if ($statusCode -eq 429) {
        Write-Host ""
        Write-Host "🚫 LIMITE DE REQUISIÇÕES ATINGIDO!" -ForegroundColor Red
        Write-Host ""
        Write-Host "Você atingiu o limite de requisições do JSONBin." -ForegroundColor Red
        Write-Host "O plano gratuito permite 10.000 requisições/mês." -ForegroundColor Red
        Write-Host ""
        Write-Host "💡 SOLUÇÕES:" -ForegroundColor Yellow
        Write-Host "   1. Aguarde o próximo mês para resetar"
        Write-Host "   2. Faça upgrade para plano Pro ($10/mês - 100.000 requisições)"
        Write-Host "   3. Crie uma nova conta JSONBin gratuita"
        Write-Host "   4. Verifique uso em: https://jsonbin.io/app/dashboard"
    } elseif ($statusCode -eq 403) {
        Write-Host ""
        Write-Host "🚫 ACESSO NEGADO (403 Forbidden)" -ForegroundColor Red
        Write-Host ""
        Write-Host "Possíveis causas:" -ForegroundColor Yellow
        Write-Host "   1. LIMITE DE REQUISIÇÕES ATINGIDO" -ForegroundColor Yellow
        Write-Host "   2. API Key inválida ou expirada" -ForegroundColor Yellow
        Write-Host "   3. Bin não existe ou foi deletado" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "💡 VERIFICAÇÕES:" -ForegroundColor Cyan
        Write-Host "   1. Acesse: https://jsonbin.io/app/dashboard" -ForegroundColor Cyan
        Write-Host "   2. Verifique se ainda há requisições disponíveis" -ForegroundColor Cyan
        Write-Host "   3. Verifique se a API Key está correta" -ForegroundColor Cyan
        Write-Host "   4. Verifique se o Bin ainda existe" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "⚠️  Se o limite foi atingido:" -ForegroundColor Yellow
        Write-Host "   - O plano gratuito permite 10.000 requisições/mês" -ForegroundColor Yellow
        Write-Host "   - Cada GET e PUT conta como uma requisição" -ForegroundColor Yellow
        Write-Host "   - Heartbeat envia requisições a cada 30 segundos" -ForegroundColor Yellow
    } elseif ($statusCode -eq 401) {
        Write-Host ""
        Write-Host "🔑 ERRO: API Key inválida" -ForegroundColor Red
        Write-Host "Verifique se a API Key está correta." -ForegroundColor Red
    } elseif ($statusCode -eq 404) {
        Write-Host ""
        Write-Host "📦 ERRO: Bin não encontrado" -ForegroundColor Red
        Write-Host "Verifique se o Bin ID está correto." -ForegroundColor Red
    } else {
        Write-Host "   Mensagem: $errorMessage" -ForegroundColor Red
    }
    
    Write-Host ""
    Write-Host "📊 Para verificar seu uso atual:" -ForegroundColor Cyan
    Write-Host "   Acesse: https://jsonbin.io/app/dashboard" -ForegroundColor Cyan
    Write-Host "   E veja quantas requisições foram usadas este mês" -ForegroundColor Cyan
    
    exit 1
}

Write-Host ""
Write-Host "✅ Verificação concluída!" -ForegroundColor Green

