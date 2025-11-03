# Script para diagnosticar problemas no app Android
# Verifica o codigo do HomeNav.kt e identifica possiveis problemas

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  DIAGNOSTICO: ERROS NO APP ANDROID" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

$homeNavPath = "app/src/main/java/com/maxiptv/ui/screens/HomeNav.kt"

if (-not (Test-Path $homeNavPath)) {
    Write-Host "ERRO: Arquivo HomeNav.kt nao encontrado!" -ForegroundColor Red
    exit 1
}

Write-Host "Analisando: $homeNavPath" -ForegroundColor Yellow
Write-Host ""

$content = Get-Content $homeNavPath -Raw
$lines = Get-Content $homeNavPath

# Verificacoes
$erros = @()
$avisos = @()

Write-Host "VERIFICACOES:" -ForegroundColor Green
Write-Host ""

# 1. Verificar se LaunchedEffect existe
Write-Host "1. Verificando LaunchedEffect..." -ForegroundColor Cyan
if ($content -match "LaunchedEffect") {
    Write-Host "   LaunchedEffect encontrado" -ForegroundColor Green
    
    # Verificar se tem Unit como parametro
    if ($content -match "LaunchedEffect\(Unit\)") {
        Write-Host "   Parametro Unit correto" -ForegroundColor Green
    } else {
        $erros += "LaunchedEffect pode nao estar executando na inicializacao"
        Write-Host "   AVISO: Verificar parametro do LaunchedEffect" -ForegroundColor Yellow
    }
} else {
    $erros += "LaunchedEffect nao encontrado - login automatico nao sera executado"
    Write-Host "   ERRO: LaunchedEffect nao encontrado!" -ForegroundColor Red
}

Write-Host ""

# 2. Verificar se get-pending-code.php esta sendo chamado
Write-Host "2. Verificando chamada para get-pending-code.php..." -ForegroundColor Cyan
if ($content -match "get-pending-code\.php") {
    Write-Host "   Chamada para get-pending-code.php encontrada" -ForegroundColor Green
    
    # Verificar timeout
    if ($content -match "connectTimeout.*10000") {
        Write-Host "   Timeout configurado: 10 segundos" -ForegroundColor Green
    } else {
        $avisos += "Timeout pode estar muito baixo"
        Write-Host "   AVISO: Verificar timeout" -ForegroundColor Yellow
    }
    
    # Verificar tratamento de resposta
    if ($content -match 'pendingJson\.getString\("status"\)') {
        Write-Host "   Tratamento de resposta JSON encontrado" -ForegroundColor Green
    } else {
        $erros += "Tratamento de resposta do get-pending-code.php pode estar incorreto"
        Write-Host "   ERRO: Verificar tratamento de resposta" -ForegroundColor Red
    }
} else {
    $erros += "Chamada para get-pending-code.php nao encontrada"
    Write-Host "   ERRO: get-pending-code.php nao esta sendo chamado!" -ForegroundColor Red
}

Write-Host ""

# 3. Verificar se auto_login.php esta sendo chamado
Write-Host "3. Verificando chamada para auto_login.php..." -ForegroundColor Cyan
if ($content -match "auto_login\.php") {
    Write-Host "   Chamada para auto_login.php encontrada" -ForegroundColor Green
    
    # Verificar se pega o campo correto
    if (($content -match 'json\.optString\("user"') -or ($content -match 'json\.optString\("usuario"')) {
        Write-Host "   Campo user/usuario sendo lido corretamente" -ForegroundColor Green
    } else {
        $erros += "Campo user pode nao estar sendo lido corretamente"
        Write-Host "   ERRO: Verificar leitura do campo user" -ForegroundColor Red
    }
} else {
    $erros += "Chamada para auto_login.php nao encontrada"
    Write-Host "   ERRO: auto_login.php nao esta sendo chamado!" -ForegroundColor Red
}

Write-Host ""

# 4. Verificar se UserManager.login esta sendo chamado
Write-Host "4. Verificando chamada para UserManager.login..." -ForegroundColor Cyan
if ($content -match "UserManager\.login") {
    Write-Host "   UserManager.login encontrado" -ForegroundColor Green
    
    # Verificar se esta dentro de withContext
    if ($content -match 'withContext.*UserManager\.login') {
        Write-Host "   UserManager.login esta dentro de withContext (correto)" -ForegroundColor Green
    } else {
        $avisos += "UserManager.login pode precisar estar em withContext"
        Write-Host "   AVISO: Verificar se esta em coroutine" -ForegroundColor Yellow
    }
} else {
    $erros += "UserManager.login nao encontrado - login nao sera executado"
    Write-Host "   ERRO: UserManager.login nao encontrado!" -ForegroundColor Red
}

Write-Host ""

# 5. Verificar navegacao para home
Write-Host "5. Verificando navegacao para home..." -ForegroundColor Cyan
if ($content -match 'nav\.navigate\("home"\)') {
    Write-Host "   Navegacao para home encontrada" -ForegroundColor Green
} else {
    $erros += "Navegacao para home nao encontrada - app nao ira para tela inicial"
    Write-Host "   ERRO: Navegacao para home nao encontrada!" -ForegroundColor Red
}

Write-Host ""

# 6. Verificar tratamento de erros
Write-Host "6. Verificando tratamento de erros..." -ForegroundColor Cyan
if ($content -match "catch.*Exception") {
    Write-Host "   Blocos try-catch encontrados" -ForegroundColor Green
} else {
    $avisos += "Pode faltar tratamento de erros"
    Write-Host "   AVISO: Verificar se tem tratamento de erros adequado" -ForegroundColor Yellow
}

Write-Host ""

# 7. Verificar logs
Write-Host "7. Verificando logs de debug..." -ForegroundColor Cyan
$logCount = ([regex]::Matches($content, "android\.util\.Log\.")).Count
Write-Host "   Total de logs encontrados: $logCount" -ForegroundColor $(if ($logCount -gt 10) { "Green" } else { "Yellow" })

Write-Host ""

# 8. Verificar se initialRoute e usado corretamente
Write-Host "8. Verificando uso de initialRoute..." -ForegroundColor Cyan
if ($content -match 'initialRoute.*=.*"home"') {
    Write-Host "   initialRoute sendo definido para home" -ForegroundColor Green
} else {
    $avisos += "initialRoute pode nao estar sendo definido corretamente"
    Write-Host "   AVISO: Verificar definicao de initialRoute" -ForegroundColor Yellow
}

Write-Host ""

# 9. Verificar se SessionManager.tryLogin esta sendo chamado
Write-Host "9. Verificando SessionManager.tryLogin..." -ForegroundColor Cyan
if ($content -match "SessionManager\.tryLogin") {
    Write-Host "   SessionManager.tryLogin encontrado" -ForegroundColor Green
} else {
    $avisos += "SessionManager.tryLogin pode nao estar sendo chamado"
    Write-Host "   AVISO: Verificar se sessao esta sendo criada no JSONBin" -ForegroundColor Yellow
}

Write-Host ""

# Resumo
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  RESUMO DO DIAGNOSTICO" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

if ($erros.Count -eq 0) {
    Write-Host "NENHUM ERRO CRITICO ENCONTRADO!" -ForegroundColor Green
} else {
    Write-Host "ERROS CRITICOS ENCONTRADOS:" -ForegroundColor Red
    $i = 1
    foreach ($erro in $erros) {
        Write-Host "  $i. $erro" -ForegroundColor Red
        $i++
    }
}

Write-Host ""

if ($avisos.Count -gt 0) {
    Write-Host "AVISOS:" -ForegroundColor Yellow
    $i = 1
    foreach ($aviso in $avisos) {
        Write-Host "  $i. $aviso" -ForegroundColor Yellow
        $i++
    }
}

Write-Host ""

# Sugestoes
Write-Host "SUGESTOES DE DEBUG:" -ForegroundColor Cyan
Write-Host "  1. Verificar logs do app usando: adb logcat | Select-String 'HomeNav'" -ForegroundColor White
Write-Host "  2. Verificar se get-pending-code.php retorna codigo antes do app chamar" -ForegroundColor White
Write-Host "  3. Verificar se o IP do app e o mesmo do download (pode ser diferente)" -ForegroundColor White
Write-Host "  4. Testar com delay maior entre download e abertura do app" -ForegroundColor White
Write-Host ""

