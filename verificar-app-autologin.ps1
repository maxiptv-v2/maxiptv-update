# Script para verificar se o app está chamando corretamente o auto_login
# Mostra EXATAMENTE onde está o erro e como corrigir

param(
    [Parameter(Mandatory=$true)]
    [string]$Code
)

Write-Host "=== VERIFICACAO DO FLUXO DE AUTO-LOGIN DO APP ===" -ForegroundColor Cyan
Write-Host "Codigo: $Code" -ForegroundColor Yellow
Write-Host ""

$serverUrl = "https://maxiptv-update-1.onrender.com"
$jsonbinUrl = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b"
$jsonbinKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'

Write-Host "PASSO 1: Verificando se o codigo existe no JSONBin..." -ForegroundColor Cyan
try {
    $headers = @{"X-Master-Key" = $jsonbinKey}
    $jsonbin = Invoke-RestMethod -Uri "$jsonbinUrl/latest" -Headers $headers -Method Get
    
    if ($jsonbin.record.$Code) {
        $codeData = $jsonbin.record.$Code
        Write-Host "   ✅ Codigo encontrado no JSONBin" -ForegroundColor Green
        Write-Host "      Username: $($codeData.username)" -ForegroundColor White
        Write-Host "      API URL: $($codeData.apiUrl)" -ForegroundColor White
        Write-Host "      Expiry Date: $($codeData.expiryDate)" -ForegroundColor White
    } else {
        Write-Host "   ❌ ERRO: Codigo $Code NAO encontrado no JSONBin!" -ForegroundColor Red
        Write-Host ""
        Write-Host "   CORRECAO:" -ForegroundColor Yellow
        Write-Host "   1. Abra o app no dispositivo" -ForegroundColor White
        Write-Host "   2. Vá para o painel admin (5 toques no logo)" -ForegroundColor White
        Write-Host "   3. Encontre o usuário e clique em 'Gerar Código'" -ForegroundColor White
        Write-Host "   4. O código será salvo automaticamente no JSONBin" -ForegroundColor White
        exit 1
    }
} catch {
    Write-Host "   ❌ ERRO ao acessar JSONBin: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "PASSO 2: Simulando dl.php primeiro (para criar código pendente)..." -ForegroundColor Cyan
try {
    Write-Host "   Chamando dl.php para criar código pendente..." -ForegroundColor Gray
    $dlUrl = "$serverUrl/dl/$Code"
    try {
        $dlResponse = Invoke-WebRequest -Uri $dlUrl -Method Get -MaximumRedirection 0 -ErrorAction SilentlyContinue
    } catch {
        # Esperado - 302 redirect
        if ($_.Exception.Response.StatusCode.value__ -eq 302) {
            Write-Host "   ✅ dl.php redirecionou (código pendente deve estar salvo)" -ForegroundColor Green
        }
    }
    
    Start-Sleep -Seconds 3
    
    Write-Host ""
    Write-Host "PASSO 2b: Testando get-pending-code.php (o que o app chama)..." -ForegroundColor Cyan
    $pendingUrl = "$serverUrl/get-pending-code.php"
    Write-Host "   URL que o app chama: $pendingUrl" -ForegroundColor Gray
    
    # Agora testar get-pending-code.php
    $pendingResponse = Invoke-RestMethod -Uri $pendingUrl -Method Get -TimeoutSec 10
    Write-Host "   ✅ get-pending-code.php respondeu" -ForegroundColor Green
    Write-Host "   Resposta:" -ForegroundColor Yellow
    $pendingResponse | ConvertTo-Json -Depth 5 | Write-Host
    
    if ($pendingResponse.status -eq "ok") {
        $retrievedCode = $pendingResponse.code
        Write-Host "   ✅ Status: ok, Código: $retrievedCode" -ForegroundColor Green
        
        if ($retrievedCode -eq $Code) {
            Write-Host "   ✅ Código correto retornado!" -ForegroundColor Green
        } else {
            Write-Host "   ❌ ERRO: Código retornado ($retrievedCode) diferente do esperado ($Code)!" -ForegroundColor Red
        }
    } else {
        Write-Host "   ❌ ERRO: Status diferente de 'ok'" -ForegroundColor Red
        Write-Host "   Status retornado: $($pendingResponse.status)" -ForegroundColor Red
        Write-Host ""
        Write-Host "   CORRECAO:" -ForegroundColor Yellow
        Write-Host "   Verifique se get-pending-code.php está:" -ForegroundColor White
        Write-Host "   1. Buscando _pending_logins no JSONBin" -ForegroundColor White
        Write-Host "   2. Retornando status 'ok' quando encontra código" -ForegroundColor White
    }
} catch {
    Write-Host "   ❌ ERRO ao testar get-pending-code.php: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "   Status HTTP: $statusCode" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "PASSO 3: Simulando o que o app faz (auto_login.php)..." -ForegroundColor Cyan
try {
    $autoLoginUrl = "$serverUrl/auto_login.php?code=$Code"
    Write-Host "   URL que o app chama: $autoLoginUrl" -ForegroundColor Gray
    
    $autoLoginResponse = Invoke-RestMethod -Uri $autoLoginUrl -Method Get -TimeoutSec 10
    
    Write-Host "   ✅ auto_login.php respondeu" -ForegroundColor Green
    Write-Host ""
    Write-Host "   RESPOSTA COMPLETA:" -ForegroundColor Yellow
    $jsonComplete = $autoLoginResponse | ConvertTo-Json -Depth 5
    Write-Host $jsonComplete -ForegroundColor White
    Write-Host ""
    
    # Verificar formato esperado pelo app
    Write-Host "   VERIFICANDO FORMATO ESPERADO PELO APP..." -ForegroundColor Cyan
    
    $camposEsperados = @{
        "user" = "username do usuário"
        "password" = "senha do usuário"
        "api" = "URL da API do usuário"
        "expiryDate" = "data de expiração (DD/MM/YYYY)"
    }
    
    $erros = @()
    $camposOk = @()
    
    foreach ($campo in $camposEsperados.Keys) {
        $temCampo = $autoLoginResponse.PSObject.Properties.Name -contains $campo
        if ($temCampo) {
            $valor = $autoLoginResponse.$campo
            if ([string]::IsNullOrWhiteSpace($valor)) {
                $erros += "Campo '$campo' está vazio"
            } else {
                $camposOk += $campo
                Write-Host "   ✅ Campo '$campo': $valor" -ForegroundColor Green
            }
        } else {
            $erros += "Campo '$campo' FALTANDO"
            Write-Host "   ❌ Campo '$campo' FALTANDO!" -ForegroundColor Red
        }
    }
    
    # Verificar campos extras (não devem estar)
    $camposExtras = @()
    foreach ($prop in $autoLoginResponse.PSObject.Properties.Name) {
        if ($prop -notin $camposEsperados.Keys) {
            $camposExtras += $prop
        }
    }
    
    if ($camposExtras.Count -gt 0) {
        Write-Host ""
        Write-Host "   ⚠️ AVISO: Campos EXTRAS encontrados (podem causar problemas):" -ForegroundColor Yellow
        foreach ($extra in $camposExtras) {
            Write-Host "      - $extra = $($autoLoginResponse.$extra)" -ForegroundColor Yellow
        }
    }
    
    if ($erros.Count -eq 0 -and $camposOk.Count -eq 4) {
        Write-Host ""
        Write-Host "   ✅ FORMATO PERFEITO! Todos os campos obrigatórios presentes e preenchidos." -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "   ❌ ERROS ENCONTRADOS:" -ForegroundColor Red
        foreach ($erro in $erros) {
            Write-Host "      - $erro" -ForegroundColor Red
        }
        Write-Host ""
        Write-Host "   CORRECAO NO auto_login.php:" -ForegroundColor Yellow
        Write-Host "   O PHP deve retornar EXATAMENTE:" -ForegroundColor White
        Write-Host "   {" -ForegroundColor Gray
        Write-Host '     "user": "...",' -ForegroundColor Gray
        Write-Host '     "password": "...",' -ForegroundColor Gray
        Write-Host '     "api": "...",' -ForegroundColor Gray
        Write-Host '     "expiryDate": "DD/MM/YYYY"' -ForegroundColor Gray
        Write-Host "   }" -ForegroundColor Gray
        Write-Host ""
        Write-Host "   Exemplo de código PHP correto:" -ForegroundColor White
        Write-Host '   echo json_encode([' -ForegroundColor Cyan
        Write-Host "       'user' => `$userData['username']," -ForegroundColor Cyan
        Write-Host "       'password' => `$userData['password']," -ForegroundColor Cyan
        Write-Host "       'api' => `$userData['apiUrl']," -ForegroundColor Cyan
        Write-Host "       'expiryDate' => `$userData['expiryDate']" -ForegroundColor Cyan
        Write-Host "   ]);" -ForegroundColor Cyan
    }
    
    # Verificar formato de data
    if ($autoLoginResponse.expiryDate) {
        $datePattern = '^\d{2}/\d{2}/\d{4}$'
        if ($autoLoginResponse.expiryDate -match $datePattern) {
            Write-Host ""
            Write-Host "   ✅ Formato de data correto (DD/MM/YYYY)" -ForegroundColor Green
        } else {
            Write-Host ""
            Write-Host "   ❌ ERRO: Formato de data incorreto!" -ForegroundColor Red
            Write-Host "   Data recebida: $($autoLoginResponse.expiryDate)" -ForegroundColor Red
            Write-Host "   Formato esperado: DD/MM/YYYY (ex: 12/05/2026)" -ForegroundColor Yellow
        }
    }
    
} catch {
    Write-Host "   ❌ ERRO ao testar auto_login.php: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "   Status HTTP: $statusCode" -ForegroundColor Red
        
        try {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $errorContent = $reader.ReadToEnd()
            Write-Host "   Resposta de erro:" -ForegroundColor Red
            Write-Host $errorContent -ForegroundColor Red
        } catch {
            Write-Host "   Não foi possível ler a resposta de erro" -ForegroundColor Red
        }
    }
    Write-Host ""
    Write-Host "   CORRECAO:" -ForegroundColor Yellow
    Write-Host "   1. Verifique se auto_login.php está no servidor Render" -ForegroundColor White
    Write-Host "   2. Verifique se o código está correto no JSONBin" -ForegroundColor White
    Write-Host "   3. Verifique os logs do Render para ver erros do PHP" -ForegroundColor White
}

Write-Host ""
Write-Host "PASSO 4: Verificando se o app consegue fazer login com essas credenciais..." -ForegroundColor Cyan
if ($autoLoginResponse -and $autoLoginResponse.user -and $autoLoginResponse.password -and $autoLoginResponse.api) {
    Write-Host "   Credenciais recebidas:" -ForegroundColor White
    Write-Host "      User: $($autoLoginResponse.user)" -ForegroundColor Gray
    Write-Host "      Password: $($autoLoginResponse.password)" -ForegroundColor Gray
    Write-Host "      API: $($autoLoginResponse.api)" -ForegroundColor Gray
    Write-Host ""
    Write-Host "   ✅ O app DEVE conseguir fazer login com essas credenciais" -ForegroundColor Green
    Write-Host ""
    Write-Host "   Se o app não está fazendo login automático, verifique:" -ForegroundColor Yellow
    Write-Host "   1. O app está chamando get-pending-code.php quando abre?" -ForegroundColor White
    Write-Host "   2. O app está chamando auto_login.php com o código recebido?" -ForegroundColor White
    Write-Host "   3. O app está processando a resposta JSON corretamente?" -ForegroundColor White
    Write-Host "   4. O app está chamando UserManager.login() com as credenciais?" -ForegroundColor White
    Write-Host "   5. O app está navegando para 'home' após login bem-sucedido?" -ForegroundColor White
    Write-Host ""
    Write-Host "   Para ver os logs do app, execute:" -ForegroundColor Cyan
    Write-Host "   adb logcat | grep -E 'HomeNav|LoginScreen|auto_login'" -ForegroundColor Gray
} else {
    Write-Host "   ⚠️ Não foi possível verificar - auto_login.php não retornou credenciais válidas" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== RESUMO ===" -ForegroundColor Cyan
Write-Host "Fluxo esperado do app:" -ForegroundColor Yellow
Write-Host "1. App abre → HomeNav verifica se usuário está logado" -ForegroundColor White
Write-Host "2. Se não logado → HomeNav chama get-pending-code.php" -ForegroundColor White
Write-Host "3. Se código encontrado → HomeNav chama auto_login.php?code=CODIGO" -ForegroundColor White
Write-Host "4. Se credenciais válidas → HomeNav chama UserManager.login()" -ForegroundColor White
Write-Host "5. Se login OK → HomeNav navega para 'home'" -ForegroundColor White
Write-Host ""
Write-Host "Se algo não funcionar, os logs do app mostrarão onde falhou." -ForegroundColor Yellow
Write-Host ""

