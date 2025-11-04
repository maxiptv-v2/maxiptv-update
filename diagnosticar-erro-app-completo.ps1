# Script para diagnosticar erros no app Android analisando o código fonte
# Compara o código do app com o que o servidor espera e mostra EXATAMENTE onde está o erro

param(
    [Parameter(Mandatory=$true)]
    [string]$Code
)

Write-Host "=== DIAGNOSTICO COMPLETO DO APP ANDROID ===" -ForegroundColor Cyan
Write-Host "Codigo: $Code" -ForegroundColor Yellow
Write-Host ""

$erros = @()
$avisos = @()
$ok = @()

$homeNavPath = "app/src/main/java/com/maxiptv/ui/screens/HomeNav.kt"
$loginScreenPath = "app/src/main/java/com/maxiptv/ui/screens/LoginScreen.kt"

Write-Host "PASSO 1: Verificando se os arquivos existem..." -ForegroundColor Cyan
if (-not (Test-Path $homeNavPath)) {
    Write-Host "   ❌ ERRO: $homeNavPath não encontrado!" -ForegroundColor Red
    exit 1
}
if (-not (Test-Path $loginScreenPath)) {
    Write-Host "   ❌ ERRO: $loginScreenPath não encontrado!" -ForegroundColor Red
    exit 1
}
Write-Host "   ✅ Arquivos encontrados" -ForegroundColor Green

Write-Host ""
Write-Host "PASSO 2: Analisando HomeNav.kt..." -ForegroundColor Cyan

$homeNavContent = Get-Content $homeNavPath -Raw

# Verificar se chama get-pending-code.php
if ($homeNavContent -match "get-pending-code\.php") {
    $ok += "HomeNav chama get-pending-code.php"
    Write-Host "   ✅ Chama get-pending-code.php" -ForegroundColor Green
    
    # Verificar URL
    if ($homeNavContent -match "https://maxiptv-update-1\.onrender\.com/get-pending-code\.php") {
        $ok += "URL de get-pending-code.php está correta"
        Write-Host "   ✅ URL correta: https://maxiptv-update-1.onrender.com/get-pending-code.php" -ForegroundColor Green
    } else {
        $erros += "URL de get-pending-code.php está incorreta em HomeNav.kt"
        Write-Host "   ❌ URL incorreta ou não encontrada!" -ForegroundColor Red
    }
} else {
    $erros += "HomeNav NÃO chama get-pending-code.php"
    Write-Host "   ❌ NÃO chama get-pending-code.php" -ForegroundColor Red
}

# Verificar se processa resposta de get-pending-code.php
if ($homeNavContent -match "pendingJson\.getString\(`"status`"\)") {
    $ok += "HomeNav verifica status da resposta"
    Write-Host "   ✅ Verifica status da resposta" -ForegroundColor Green
    
    if ($homeNavContent -match "status.*==.*`"ok`"") {
        $ok += "HomeNav verifica se status == 'ok'"
        Write-Host "   ✅ Verifica se status == 'ok'" -ForegroundColor Green
    } else {
        $avisos += "HomeNav pode não estar verificando status == 'ok' corretamente"
        Write-Host "   ⚠️ Pode não verificar status == 'ok' corretamente" -ForegroundColor Yellow
    }
    
    if ($homeNavContent -match "pendingJson\.getString\(`"code`"\)") {
        $ok += "HomeNav extrai código da resposta"
        Write-Host "   ✅ Extrai código da resposta" -ForegroundColor Green
    } else {
        $erros += "HomeNav NÃO extrai código da resposta de get-pending-code.php"
        Write-Host "   ❌ NÃO extrai código da resposta" -ForegroundColor Red
    }
} else {
    $erros += "HomeNav NÃO processa resposta de get-pending-code.php"
    Write-Host "   ❌ NÃO processa resposta de get-pending-code.php" -ForegroundColor Red
}

# Verificar se chama auto_login.php
if ($homeNavContent -match "auto_login\.php") {
    $ok += "HomeNav chama auto_login.php"
    Write-Host "   ✅ Chama auto_login.php" -ForegroundColor Green
    
    # Verificar URL
    if ($homeNavContent -match "auto_login\.php\?code=") {
        $ok += "HomeNav passa código para auto_login.php corretamente"
        Write-Host "   ✅ Passa código para auto_login.php: ?code=`$pendingCode" -ForegroundColor Green
    } else {
        $avisos += "Verifique se HomeNav está passando código corretamente para auto_login.php"
        Write-Host "   ⚠️ Verifique se está passando código corretamente" -ForegroundColor Yellow
    }
} else {
    $erros += "HomeNav NÃO chama auto_login.php"
    Write-Host "   ❌ NÃO chama auto_login.php" -ForegroundColor Red
}

# Verificar se processa resposta de auto_login.php
if ($homeNavContent -match "json\.optString\(`"user`"") {
    $ok += "HomeNav extrai campo 'user' da resposta"
    Write-Host "   ✅ Extrai campo 'user'" -ForegroundColor Green
} else {
    $erros += "HomeNav NÃO extrai campo 'user' da resposta de auto_login.php"
    Write-Host "   ❌ NÃO extrai campo 'user'" -ForegroundColor Red
}

if ($homeNavContent -match "json\.optString\(`"password`"") {
    $ok += "HomeNav extrai campo 'password' da resposta"
    Write-Host "   ✅ Extrai campo 'password'" -ForegroundColor Green
} else {
    $erros += "HomeNav NÃO extrai campo 'password' da resposta de auto_login.php"
    Write-Host "   ❌ NÃO extrai campo 'password'" -ForegroundColor Red
}

if ($homeNavContent -match "json\.optString\(`"api`"") {
    $ok += "HomeNav extrai campo 'api' da resposta"
    Write-Host "   ✅ Extrai campo 'api'" -ForegroundColor Green
} else {
    $erros += "HomeNav NÃO extrai campo 'api' da resposta de auto_login.php"
    Write-Host "   ❌ NÃO extrai campo 'api'" -ForegroundColor Red
}

if ($homeNavContent -match "json\.optString\(`"expiryDate`"") {
    $ok += "HomeNav extrai campo 'expiryDate' da resposta"
    Write-Host "   ✅ Extrai campo 'expiryDate'" -ForegroundColor Green
} else {
    $avisos += "HomeNav pode não estar extraindo 'expiryDate'"
    Write-Host "   ⚠️ Pode não extrair 'expiryDate'" -ForegroundColor Yellow
}

# Verificar se valida campos antes de fazer login
if ($homeNavContent -match "user\.isNotBlank\(\)\s*&&\s*pass\.isNotBlank\(\)\s*&&\s*api\.isNotBlank\(\)") {
    $ok += "HomeNav valida campos antes de fazer login"
    Write-Host "   ✅ Valida campos antes de fazer login" -ForegroundColor Green
} else {
    $avisos += "HomeNav pode não estar validando campos antes de fazer login"
    Write-Host "   ⚠️ Pode não validar campos antes de fazer login" -ForegroundColor Yellow
}

# Verificar se valida expiryDate
if ($homeNavContent -match "isExpired\(expiryDate\)") {
    $ok += "HomeNav valida expiryDate"
    Write-Host "   ✅ Valida expiryDate" -ForegroundColor Green
} else {
    $avisos += "HomeNav pode não estar validando expiryDate"
    Write-Host "   ⚠️ Pode não validar expiryDate" -ForegroundColor Yellow
}

# Verificar se chama UserManager.login()
if ($homeNavContent -match "UserManager\.login\(user,\s*pass\)") {
    $ok += "HomeNav chama UserManager.login()"
    Write-Host "   ✅ Chama UserManager.login(user, pass)" -ForegroundColor Green
} else {
    $erros += "HomeNav NÃO chama UserManager.login()"
    Write-Host "   ❌ NÃO chama UserManager.login()" -ForegroundColor Red
}

# Verificar se navega para home após login
if ($homeNavContent -match "shouldNavigateToHome\s*=\s*true") {
    $ok += "HomeNav define shouldNavigateToHome = true"
    Write-Host "   ✅ Define shouldNavigateToHome = true" -ForegroundColor Green
    
    if ($homeNavContent -match "LaunchedEffect\(shouldNavigateToHome\)") {
        $ok += "HomeNav tem LaunchedEffect observando shouldNavigateToHome"
        Write-Host "   ✅ Tem LaunchedEffect observando shouldNavigateToHome" -ForegroundColor Green
        
        if ($homeNavContent -match "nav\.navigate\(`"home`"\)") {
            $ok += "HomeNav navega para 'home'"
            Write-Host "   ✅ Navega para 'home'" -ForegroundColor Green
        } else {
            $erros += "HomeNav NÃO navega para 'home' após login"
            Write-Host "   ❌ NÃO navega para 'home'" -ForegroundColor Red
        }
    } else {
        $erros += "HomeNav NÃO tem LaunchedEffect observando shouldNavigateToHome"
        Write-Host "   ❌ NÃO tem LaunchedEffect observando shouldNavigateToHome" -ForegroundColor Red
    }
} else {
    $erros += "HomeNav NÃO define shouldNavigateToHome após login bem-sucedido"
    Write-Host "   ❌ NÃO define shouldNavigateToHome" -ForegroundColor Red
}

# Verificar se está dentro de LaunchedEffect
if ($homeNavContent -match "LaunchedEffect.*\{[\s\S]*?get-pending-code\.php") {
    $ok += "Chamada a get-pending-code.php está dentro de LaunchedEffect"
    Write-Host "   ✅ Chamada está dentro de LaunchedEffect" -ForegroundColor Green
} else {
    $avisos += "Verifique se chamada a get-pending-code.php está dentro de LaunchedEffect"
    Write-Host "   ⚠️ Verifique se está dentro de LaunchedEffect" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "PASSO 3: Analisando LoginScreen.kt..." -ForegroundColor Cyan

$loginScreenContent = Get-Content $loginScreenPath -Raw

# Verificar se LoginScreen também chama get-pending-code.php (NÃO DEVE)
# Verificar se há chamada real (não apenas comentário)
$hasPendingCodeCall = $false
if ($loginScreenContent -match "java\.net\.URL.*get-pending-code\.php" -or 
    $loginScreenContent -match "openConnection.*get-pending-code\.php" -or
    $loginScreenContent -match "HttpURLConnection.*get-pending-code\.php") {
    $hasPendingCodeCall = $true
}

if ($hasPendingCodeCall) {
    $erros += "LoginScreen também chama get-pending-code.php (deveria ser apenas HomeNav)"
    Write-Host "   ❌ LoginScreen também chama get-pending-code.php (DEVERIA SER APENAS HomeNav)" -ForegroundColor Red
    Write-Host "      CORRECAO: Remover chamada de get-pending-code.php do LoginScreen" -ForegroundColor Yellow
} else {
    $ok += "LoginScreen NÃO chama get-pending-code.php (correto)"
    Write-Host "   ✅ LoginScreen NÃO chama get-pending-code.php (correto)" -ForegroundColor Green
}

Write-Host ""
Write-Host "PASSO 4: Testando servidor com código fornecido..." -ForegroundColor Cyan

$serverUrl = "https://maxiptv-update-1.onrender.com"

# Testar dl.php primeiro
try {
    Write-Host "   Chamando dl.php para criar código pendente..." -ForegroundColor Gray
    $dlUrl = "$serverUrl/dl/$Code"
    try {
        $dlResponse = Invoke-WebRequest -Uri $dlUrl -Method Get -MaximumRedirection 0 -ErrorAction SilentlyContinue
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -eq 302) {
            Write-Host "   ✅ dl.php redirecionou (código pendente criado)" -ForegroundColor Green
            Start-Sleep -Seconds 2
        }
    }
} catch {
    Write-Host "   ⚠️ Erro ao chamar dl.php: $($_.Exception.Message)" -ForegroundColor Yellow
}

# Testar get-pending-code.php
try {
    $pendingUrl = "$serverUrl/get-pending-code.php"
    $pendingResponse = Invoke-RestMethod -Uri $pendingUrl -Method Get -TimeoutSec 10
    
    if ($pendingResponse.status -eq "ok") {
        $ok += "get-pending-code.php retorna status 'ok'"
        Write-Host "   ✅ get-pending-code.php retorna status 'ok'" -ForegroundColor Green
        Write-Host "      Código retornado: $($pendingResponse.code)" -ForegroundColor White
        
        if ($pendingResponse.code -eq $Code) {
            $ok += "Código retornado está correto"
            Write-Host "   ✅ Código retornado está correto" -ForegroundColor Green
        } else {
            $avisos += "Código retornado ($($pendingResponse.code)) diferente do esperado ($Code)"
            Write-Host "   ⚠️ Código retornado diferente" -ForegroundColor Yellow
        }
    } else {
        $erros += "get-pending-code.php retorna status '$($pendingResponse.status)' em vez de 'ok'"
        Write-Host "   ❌ get-pending-code.php retorna status '$($pendingResponse.status)'" -ForegroundColor Red
    }
} catch {
    $erros += "Erro ao chamar get-pending-code.php: $($_.Exception.Message)"
    Write-Host "   ❌ Erro ao chamar get-pending-code.php: $($_.Exception.Message)" -ForegroundColor Red
}

# Testar auto_login.php
try {
    $autoLoginUrl = "$serverUrl/auto_login.php?code=$Code"
    $autoLoginResponse = Invoke-RestMethod -Uri $autoLoginUrl -Method Get -TimeoutSec 10
    
    $camposEsperados = @("user", "password", "api", "expiryDate")
    $camposEncontrados = @()
    
    foreach ($campo in $camposEsperados) {
        if ($autoLoginResponse.PSObject.Properties.Name -contains $campo) {
            $camposEncontrados += $campo
        }
    }
    
    if ($camposEncontrados.Count -eq 4) {
        $ok += "auto_login.php retorna todos os campos obrigatórios"
        Write-Host "   ✅ auto_login.php retorna todos os campos obrigatórios" -ForegroundColor Green
    } else {
        $faltando = $camposEsperados | Where-Object { $_ -notin $camposEncontrados }
        $erros += "auto_login.php NÃO retorna campos: $($faltando -join ', ')"
        Write-Host "   ❌ auto_login.php NÃO retorna campos: $($faltando -join ', ')" -ForegroundColor Red
    }
} catch {
    $erros += "Erro ao chamar auto_login.php: $($_.Exception.Message)"
    Write-Host "   ❌ Erro ao chamar auto_login.php: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== RESUMO DO DIAGNOSTICO ===" -ForegroundColor Cyan
Write-Host ""

if ($ok.Count -gt 0) {
    Write-Host "✅ CORRETO ($($ok.Count) itens):" -ForegroundColor Green
    foreach ($item in $ok) {
        Write-Host "   - $item" -ForegroundColor White
    }
    Write-Host ""
}

if ($avisos.Count -gt 0) {
    Write-Host "⚠️ AVISOS ($($avisos.Count) itens):" -ForegroundColor Yellow
    foreach ($item in $avisos) {
        Write-Host "   - $item" -ForegroundColor White
    }
    Write-Host ""
}

if ($erros.Count -gt 0) {
    Write-Host "❌ ERROS ENCONTRADOS ($($erros.Count) itens):" -ForegroundColor Red
    foreach ($item in $erros) {
        Write-Host "   - $item" -ForegroundColor White
    }
    Write-Host ""
    Write-Host "=== COMO CORRIGIR ===" -ForegroundColor Cyan
    Write-Host ""
    
    foreach ($erro in $erros) {
        Write-Host "ERRO: $erro" -ForegroundColor Red
        Write-Host "CORRECAO:" -ForegroundColor Yellow
        
        if ($erro -match "HomeNav NÃO chama get-pending-code\.php") {
            Write-Host "   Adicione em HomeNav.kt (dentro de LaunchedEffect):" -ForegroundColor White
            Write-Host "   val pendingUrl = `"https://maxiptv-update-1.onrender.com/get-pending-code.php`"" -ForegroundColor Cyan
            Write-Host "   val pendingConnection = java.net.URL(pendingUrl).openConnection() as java.net.HttpURLConnection" -ForegroundColor Cyan
        }
        elseif ($erro -match "HomeNav NÃO extrai código") {
            Write-Host "   Adicione após receber resposta de get-pending-code.php:" -ForegroundColor White
            Write-Host "   val pendingJson = org.json.JSONObject(pendingResponse)" -ForegroundColor Cyan
            Write-Host "   if (pendingJson.getString(`"status`") == `"ok`") {" -ForegroundColor Cyan
            Write-Host "       val pendingCode = pendingJson.getString(`"code`")" -ForegroundColor Cyan
        }
        elseif ($erro -match "HomeNav NÃO chama auto_login\.php") {
            Write-Host "   Adicione após obter código pendente:" -ForegroundColor White
            Write-Host "   val url = `"https://maxiptv-update-1.onrender.com/auto_login.php?code=`$pendingCode`"" -ForegroundColor Cyan
            Write-Host "   val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection" -ForegroundColor Cyan
        }
        elseif ($erro -match "HomeNav NÃO extrai campo") {
            Write-Host "   Adicione após receber resposta de auto_login.php:" -ForegroundColor White
            Write-Host "   val json = org.json.JSONObject(response)" -ForegroundColor Cyan
            Write-Host "   val user = json.optString(`"user`", `"`")" -ForegroundColor Cyan
            Write-Host "   val pass = json.optString(`"password`", `"`")" -ForegroundColor Cyan
            Write-Host "   val api = json.optString(`"api`", `"`")" -ForegroundColor Cyan
            Write-Host "   val expiryDate = json.optString(`"expiryDate`", `"`")" -ForegroundColor Cyan
        }
        elseif ($erro -match "HomeNav NÃO chama UserManager\.login") {
            Write-Host "   Adicione após validar campos:" -ForegroundColor White
            Write-Host "   val (loggedUser, error) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {" -ForegroundColor Cyan
            Write-Host "       UserManager.login(user, pass)" -ForegroundColor Cyan
            Write-Host "   }" -ForegroundColor Cyan
        }
        elseif ($erro -match "HomeNav NÃO navega para 'home'") {
            Write-Host "   Adicione após login bem-sucedido:" -ForegroundColor White
            Write-Host "   shouldNavigateToHome = true" -ForegroundColor Cyan
            Write-Host "   E adicione LaunchedEffect observando shouldNavigateToHome:" -ForegroundColor White
            Write-Host "   LaunchedEffect(shouldNavigateToHome) {" -ForegroundColor Cyan
            Write-Host "       if (shouldNavigateToHome) {" -ForegroundColor Cyan
            Write-Host "           nav.navigate(`"home`") { popUpTo(0) { inclusive = true } }" -ForegroundColor Cyan
            Write-Host "       }" -ForegroundColor Cyan
            Write-Host "   }" -ForegroundColor Cyan
        }
        elseif ($erro -match "LoginScreen também chama get-pending-code\.php") {
            Write-Host "   Remova a chamada de get-pending-code.php do LoginScreen.kt" -ForegroundColor White
            Write-Host "   Apenas HomeNav deve chamar get-pending-code.php" -ForegroundColor White
        }
        
        Write-Host ""
    }
} else {
    Write-Host "✅ NENHUM ERRO ENCONTRADO!" -ForegroundColor Green
    Write-Host ""
    Write-Host "O código do app está correto. Se o login automático não funciona," -ForegroundColor White
    Write-Host "o problema pode ser:" -ForegroundColor White
    Write-Host "1. App não está sendo executado após instalação" -ForegroundColor Yellow
    Write-Host "2. Problema de rede/conectividade" -ForegroundColor Yellow
    Write-Host "3. Problema de timing (LaunchedEffect executando antes do NavHost estar pronto)" -ForegroundColor Yellow
    Write-Host "4. Erro em runtime (exception não capturada)" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Execute: adb logcat | grep -E 'HomeNav|auto_login|get-pending'" -ForegroundColor Cyan
    Write-Host "para ver os logs em tempo real e identificar o problema." -ForegroundColor Cyan
}

Write-Host ""

