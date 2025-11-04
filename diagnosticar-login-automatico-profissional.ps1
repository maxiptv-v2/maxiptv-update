# ============================================================================
# Script Profissional de Diagnóstico - Login Automático MaxiPTV
# ============================================================================
# Este script identifica EXATAMENTE onde está o erro no fluxo de login automático
# Autor: Sistema de Diagnóstico MaxiPTV
# Data: $(Get-Date -Format "dd/MM/yyyy HH:mm")
# ============================================================================

param(
    [Parameter(Mandatory=$true)]
    [string]$Code,
    
    [Parameter(Mandatory=$false)]
    [switch]$Verbose
)

# Configurações
$script:serverUrl = "https://maxiptv-update-1.onrender.com"
$script:jsonbinUrl = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b"
$script:jsonbinKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'
$script:headers = @{"X-Master-Key" = $script:jsonbinKey}

# Variáveis de status
$script:erros = @()
$script:avisos = @()
$script:sucessos = @()

# Função para exibir seção
function Show-Section {
    param([string]$Title, [string]$Color = "Cyan")
    Write-Host ""
    Write-Host "=" * 80 -ForegroundColor $Color
    Write-Host "  $Title" -ForegroundColor $Color
    Write-Host "=" * 80 -ForegroundColor $Color
}

# Função para exibir resultado
function Show-Result {
    param(
        [string]$Message,
        [string]$Status, # "OK", "ERRO", "AVISO"
        [string]$Details = ""
    )
    
    $color = switch ($Status) {
        "OK" { "Green" }
        "ERRO" { "Red" }
        "AVISO" { "Yellow" }
        default { "White" }
    }
    
    $symbol = switch ($Status) {
        "OK" { "[OK]" }
        "ERRO" { "[ERRO]" }
        "AVISO" { "[AVISO]" }
        default { "[INFO]" }
    }
    
    Write-Host "  $symbol " -NoNewline -ForegroundColor $color
    Write-Host $Message -ForegroundColor $color
    
    if ($Details) {
        Write-Host "      $Details" -ForegroundColor Gray
    }
    
    # Registrar no status
    switch ($Status) {
        "OK" { $script:sucessos += $Message }
        "ERRO" { $script:erros += $Message }
        "AVISO" { $script:avisos += $Message }
    }
}

# Função para exibir explicação
function Show-Explanation {
    param([string]$Text)
    Write-Host "  [INFO] $Text" -ForegroundColor Cyan
}

# ============================================================================
# INÍCIO DO DIAGNÓSTICO
# ============================================================================

Write-Host ""
Write-Host "==================================================================================" -ForegroundColor Cyan
Write-Host "          DIAGNOSTICO PROFISSIONAL - LOGIN AUTOMATICO MAXIPTV" -ForegroundColor Cyan
Write-Host "==================================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Código de Teste: " -NoNewline -ForegroundColor White
Write-Host $Code -ForegroundColor Yellow
Write-Host "  Data/Hora: $(Get-Date -Format 'dd/MM/yyyy HH:mm:ss')" -ForegroundColor Gray
Write-Host ""

# ============================================================================
# ETAPA 1: VERIFICAÇÃO INICIAL - CÓDIGO NO JSONBIN
# ============================================================================

Show-Section "ETAPA 1: Verificação do Código no JSONBin"

        Show-Explanation "Verificando se o codigo existe e esta valido no JSONBin"

try {
    $jsonbin = Invoke-RestMethod -Uri "$script:jsonbinUrl/latest" -Headers $script:headers -Method Get -ErrorAction Stop
    
    if ($jsonbin.record.$Code) {
        $codeData = $jsonbin.record.$Code
        Show-Result "Codigo encontrado no JSONBin" "OK" "Username: $($codeData.username)"
        
        # Verificar dados do código
        if ($codeData.username) {
            Show-Result "Username presente" "OK" "Valor: $($codeData.username)"
        } else {
            Show-Result "Username ausente" "ERRO" "Código não tem username associado"
        }
        
        if ($codeData.password) {
            Show-Result "Password presente" "OK"
        } else {
            Show-Result "Password ausente" "ERRO" "Código não tem password associado"
        }
        
        if ($codeData.apiUrl) {
            Show-Result "API URL presente" "OK" "Valor: $($codeData.apiUrl)"
        } else {
            Show-Result "API URL ausente" "ERRO" "Código não tem apiUrl associado"
        }
        
        if ($codeData.expiryDate) {
            Show-Result "Expiry Date presente" "OK" "Valor: $($codeData.expiryDate)"
            
            # Verificar se não expirou
            $parts = $codeData.expiryDate -split '/'
            if ($parts.Count -eq 3) {
                $day = [int]$parts[0]
                $month = [int]$parts[1]
                $year = [int]$parts[2]
                $expiryDate = Get-Date -Year $year -Month $month -Day $day -Hour 23 -Minute 59 -Second 59
                $now = Get-Date
                
                if ($now -gt $expiryDate) {
                    Show-Result "Usuario EXPIRADO" "ERRO" "Data de expiracao: $($codeData.expiryDate)"
                } else {
                    $daysLeft = ($expiryDate - $now).Days
                    Show-Result "Usuario valido" "OK" "Dias restantes: $daysLeft"
                }
            }
        } else {
            Show-Result "Expiry Date ausente" "AVISO" "Código não tem expiryDate (pode ser aceito)"
        }
        
        # Verificar createdAt (validade de 6 horas)
        if ($codeData.createdAt) {
            $createdAt = [DateTimeOffset]::FromUnixTimeMilliseconds($codeData.createdAt).DateTime
            $sixHoursLater = $createdAt.AddHours(6)
            $now = Get-Date
            
            if ($now -gt $sixHoursLater) {
                Show-Result "Codigo EXPIRADO (mais de 6 horas)" "ERRO" "Criado em: $createdAt"
            } else {
                $hoursLeft = ($sixHoursLater - $now).TotalHours
                Show-Result "Codigo valido (menos de 6 horas)" "OK" "Horas restantes: $([math]::Round($hoursLeft, 2))"
            }
        }
        
    } else {
        Show-Result "Codigo NAO encontrado no JSONBin" "ERRO" "Codigo $Code nao existe"
        Write-Host ""
        Write-Host "  SOLUCAO:" -ForegroundColor Yellow
        Write-Host "    1. Abra o app no painel admin (5 toques no logo)" -ForegroundColor White
        Write-Host "    2. Encontre o usuário e clique em 'Gerar Código'" -ForegroundColor White
        Write-Host "    3. O código será salvo automaticamente no JSONBin" -ForegroundColor White
        exit 1
    }
} catch {
    Show-Result "Erro ao acessar JSONBin" "ERRO" $_.Exception.Message
    exit 1
}

# ============================================================================
# ETAPA 2: TESTE DO dl.php (SIMULA DOWNLOADER)
# ============================================================================

Show-Section "ETAPA 2: Teste do dl.php (Simula Downloader)"

Show-Explanation "dl.php deve validar código, salvar em _pending_logins e redirecionar para APK"

try {
    $dlUrl = "$script:serverUrl/dl/$Code"
    Write-Host "  URL: $dlUrl" -ForegroundColor Gray
    
    try {
        $dlResponse = Invoke-WebRequest -Uri $dlUrl -Method Get -MaximumRedirection 0 -ErrorAction SilentlyContinue
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        
        if ($statusCode -eq 302) {
            $redirectUrl = $_.Exception.Response.Headers.Location
            Show-Result "dl.php redirecionou (302)" "OK" "Para: $redirectUrl"
            
            if ($redirectUrl -match "maxiptv-release\.apk") {
                Show-Result "URL do APK está correta" "OK"
            } else {
                Show-Result "URL do APK pode estar incorreta" "AVISO" "URL: $redirectUrl"
            }
        } elseif ($statusCode -eq 404) {
            Show-Result "dl.php retornou 404 (Código inválido ou expirado)" "ERRO"
            Write-Host ""
            Write-Host "  POSSIVEIS CAUSAS:" -ForegroundColor Yellow
            Write-Host "    • Código expirou (mais de 6 horas)" -ForegroundColor White
            Write-Host "    • Usuário expirado (expiryDate passou)" -ForegroundColor White
            Write-Host "    • Código não existe no JSONBin" -ForegroundColor White
        } elseif ($statusCode -eq 403) {
            Show-Result "dl.php retornou 403 (Acesso negado)" "ERRO"
            Write-Host ""
            Write-Host "  POSSIVEIS CAUSAS:" -ForegroundColor Yellow
            Write-Host "    • Usuário expirado" -ForegroundColor White
            Write-Host "    • Código revogado" -ForegroundColor White
        } else {
            Show-Result "dl.php retornou status inesperado" "ERRO" "Status: $statusCode"
        }
    }
} catch {
    Show-Result "Erro ao testar dl.php" "ERRO" $_.Exception.Message
}

# ============================================================================
# ETAPA 3: VERIFICAÇÃO DE _pending_logins
# ============================================================================

Show-Section "ETAPA 3: Verificação de _pending_logins"

Show-Explanation "dl.php DEVE salvar o código em _pending_logins para o app buscar depois"

Start-Sleep -Seconds 2

try {
    $jsonbin = Invoke-RestMethod -Uri "$script:jsonbinUrl/latest" -Headers $script:headers -Method Get
    
    if ($jsonbin.record._pending_logins) {
        $pending = $jsonbin.record._pending_logins
        $totalPending = $pending.PSObject.Properties.Count
        Show-Result "_pending_logins encontrado" "OK" "Total de códigos pendentes: $totalPending"
        
        $found = $false
        $pending.PSObject.Properties | ForEach-Object {
            $pendingData = $_.Value
            if ($pendingData.code -eq $Code) {
                $found = $true
                Show-Result "Código $Code encontrado em _pending_logins" "OK"
                Write-Host "      Username: $($pendingData.username)" -ForegroundColor White
                Write-Host "      Timestamp: $($pendingData.timestamp)" -ForegroundColor White
                
                if ($pendingData.expiresAt) {
                    $expiresAt = [DateTimeOffset]::FromUnixTimeSeconds($pendingData.expiresAt).DateTime
                    $now = Get-Date
                    if ($now -gt $expiresAt) {
                        Show-Result "Código pendente EXPIRADO" "ERRO" "Expira em: $expiresAt"
                    } else {
                        $minutesLeft = ($expiresAt - $now).TotalMinutes
                        Show-Result "Código pendente válido" "OK" "Minutos restantes: $([math]::Round($minutesLeft, 1))"
                    }
                }
            }
        }
        
        if (-not $found) {
            Show-Result "Código $Code NÃO encontrado em _pending_logins" "ERRO"
            Write-Host ""
            Write-Host "  PROBLEMA IDENTIFICADO:" -ForegroundColor Red
            Write-Host "     dl.php NÃO salvou o código pendente" -ForegroundColor White
            Write-Host ""
            Write-Host "  VERIFICAR:" -ForegroundColor Yellow
            Write-Host "    1. dl.php está salvando corretamente em _pending_logins?" -ForegroundColor White
            Write-Host "    2. Verificar logs do Render para ver erros do PHP" -ForegroundColor White
            Write-Host "    3. Verificar se usuário não está expirado (dl.php bloqueia se expirado)" -ForegroundColor White
        }
    } else {
        Show-Result "_pending_logins NÃO existe ou está vazio" "ERRO"
        Write-Host ""
        Write-Host "  PROBLEMA IDENTIFICADO:" -ForegroundColor Red
        Write-Host "     dl.php NÃO está salvando códigos pendentes" -ForegroundColor White
        Write-Host ""
        Write-Host "  VERIFICAR:" -ForegroundColor Yellow
        Write-Host "    1. dl.php está executando a parte que salva em _pending_logins?" -ForegroundColor White
        Write-Host "    2. Verificar se há erros no código PHP do dl.php" -ForegroundColor White
        Write-Host "    3. Verificar logs do Render" -ForegroundColor White
    }
} catch {
    Show-Result "Erro ao verificar _pending_logins" "ERRO" $_.Exception.Message
}

# ============================================================================
# ETAPA 4: TESTE DO get-pending-code.php
# ============================================================================

Show-Section "ETAPA 4: Teste do get-pending-code.php"

Show-Explanation "App chama isso quando abre. Deve retornar código pendente se existir"

try {
    $pendingUrl = "$script:serverUrl/get-pending-code.php"
    Write-Host "  URL que app chama: $pendingUrl" -ForegroundColor Gray
    
    $pendingResponse = Invoke-RestMethod -Uri $pendingUrl -Method Get -TimeoutSec 10 -ErrorAction Stop
    
    if ($Verbose) {
        Write-Host "  Resposta completa:" -ForegroundColor Yellow
        $pendingResponse | ConvertTo-Json -Depth 5 | Write-Host
        Write-Host ""
    }
    
    if ($pendingResponse.status -eq "ok") {
        Show-Result "get-pending-code.php retornou status 'ok'" "OK"
        Show-Result "Código retornado" "OK" "Valor: $($pendingResponse.code)"
        
        if ($pendingResponse.code -eq $Code) {
            Show-Result "Código correto" "OK"
        } else {
            Show-Result "Código retornado diferente do esperado" "AVISO" "Esperado: $Code, Recebido: $($pendingResponse.code)"
        }
        
        if ($pendingResponse.username) {
            Show-Result "Username retornado" "OK" "Valor: $($pendingResponse.username)"
        }
    } else {
        Show-Result "get-pending-code.php retornou status diferente de 'ok'" "ERRO" "Status: $($pendingResponse.status)"
        Write-Host ""
        Write-Host "  PROBLEMA IDENTIFICADO:" -ForegroundColor Red
        Write-Host "     get-pending-code.php não encontrou código pendente" -ForegroundColor White
        Write-Host ""
        Write-Host "  POSSIVEIS CAUSAS:" -ForegroundColor Yellow
        Write-Host "    • Código não foi salvo em _pending_logins (ver ETAPA 3)" -ForegroundColor White
        Write-Host "    • Código expirou (mais de 15 minutos)" -ForegroundColor White
        Write-Host "    • IP diferente (se get-pending-code.php busca por IP)" -ForegroundColor White
    }
} catch {
    Show-Result "Erro ao chamar get-pending-code.php" "ERRO" $_.Exception.Message
    Write-Host ""
    Write-Host "  VERIFICAR:" -ForegroundColor Yellow
    Write-Host "    • Servidor Render está online?" -ForegroundColor White
    Write-Host "    • URL está correta?" -ForegroundColor White
    Write-Host "    • Há erros nos logs do Render?" -ForegroundColor White
}

# ============================================================================
# ETAPA 5: TESTE DO auto_login.php
# ============================================================================

Show-Section "ETAPA 5: Teste do auto_login.php"

Show-Explanation "App chama isso com o código recebido. Deve retornar credenciais no formato JSON correto"

try {
    $autoLoginUrl = "$script:serverUrl/auto_login.php?code=$Code"
    Write-Host "  URL que app chama: $autoLoginUrl" -ForegroundColor Gray
    
    $autoLoginResponse = Invoke-RestMethod -Uri $autoLoginUrl -Method Get -TimeoutSec 10 -ErrorAction Stop
    
    if ($Verbose) {
        Write-Host "  Resposta JSON completa:" -ForegroundColor Yellow
        $autoLoginResponse | ConvertTo-Json -Depth 5 | Write-Host
        Write-Host ""
    }
    
    # Verificar novo formato: { "status": "success", "autologin": { ... } }
    if ($autoLoginResponse.status -eq "success") {
        Show-Result "Status: 'success'" "OK"
        
        if ($autoLoginResponse.autologin) {
            $autologin = $autoLoginResponse.autologin
            Show-Result "Objeto 'autologin' presente" "OK"
            
            # Verificar cada campo
            $campos = @{
                "username" = $autologin.username
                "password" = $autologin.password
                "api_url" = $autologin.api_url
                "expires_in" = $autologin.expires_in
                "expiryDate" = $autologin.expiryDate
            }
            
            $camposOk = 0
            $camposFaltando = @()
            
            foreach ($campo in $campos.Keys) {
                if ($campos[$campo]) {
                    Show-Result "Campo 'autologin.$campo' presente" "OK" "Valor: $($campos[$campo])"
                    $camposOk++
                } else {
                    Show-Result "Campo 'autologin.$campo' FALTANDO" "ERRO"
                    $camposFaltando += $campo
                }
            }
            
            if ($camposOk -eq 5) {
                Show-Result "FORMATO CORRETO - Todos os campos obrigatórios presentes" "OK"
            } else {
                Show-Result "FORMATO INCORRETO - Faltam campos obrigatorios" "ERRO" "Campos faltando: $($camposFaltando -join ', ')"
            }
        } else {
            Show-Result "Objeto 'autologin' FALTANDO" "ERRO"
            Write-Host ""
            Write-Host "  CORRECAO NO auto_login.php:" -ForegroundColor Yellow
            Write-Host "     echo json_encode(array(" -ForegroundColor White
            Write-Host "         'status' => 'success'," -ForegroundColor White
            Write-Host "         'autologin' => array(" -ForegroundColor White
            Write-Host "             'username' => user['username']," -ForegroundColor White
            Write-Host "             'password' => user['password']," -ForegroundColor White
            Write-Host "             'api_url' => user['apiUrl']," -ForegroundColor White
            Write-Host "             'expires_in' => 21600," -ForegroundColor White
            Write-Host "             'expiryDate' => expiryDate" -ForegroundColor White
            Write-Host "         )" -ForegroundColor White
            Write-Host "     ));" -ForegroundColor White
        }
    } elseif ($autoLoginResponse.status -eq "expired") {
        Show-Result "Status: expired - Usuario expirado" "ERRO" "Mensagem: $($autoLoginResponse.message)"
    } else {
        Show-Result "Status diferente de success" "ERRO" "Status: $($autoLoginResponse.status), Mensagem: $($autoLoginResponse.mensagem)"
    }
} catch {
    Show-Result "Erro ao chamar auto_login.php" "ERRO" $_.Exception.Message
}

# ============================================================================
# ETAPA 6: VERIFICAÇÃO DO CÓDIGO DO APP
# ============================================================================

Show-Section "ETAPA 6: Verificação do Código do App (HomeNav.kt)"

Show-Explanation "Verificando se o app está processando corretamente o novo formato JSON"

$homeNavPath = "app/src/main/java/com/maxiptv/ui/screens/HomeNav.kt"

if (Test-Path $homeNavPath) {
    $homeNavContent = Get-Content $homeNavPath -Raw
    
    # Verificar se processa status
    if ($homeNavContent -match "status.*==.*`"success`"") {
        Show-Result "App verifica status == 'success'" "OK"
    } else {
        Show-Result "App NÃO verifica status == 'success'" "ERRO"
    }
    
    # Verificar se extrai autologin
    if ($homeNavContent -match "optJSONObject\(`"autologin`"\)") {
        Show-Result "App extrai objeto 'autologin'" "OK"
    } else {
        Show-Result "App NÃO extrai objeto 'autologin'" "ERRO"
    }
    
    # Verificar se extrai username
    if ($homeNavContent -match "autologin\.optString\(`"username`"") {
        Show-Result "App extrai autologin.username" "OK"
    } else {
        Show-Result "App NÃO extrai autologin.username" "ERRO"
    }
    
    # Verificar se extrai password
    if ($homeNavContent -match "autologin\.optString\(`"password`"") {
        Show-Result "App extrai autologin.password" "OK"
    } else {
        Show-Result "App NÃO extrai autologin.password" "ERRO"
    }
    
    # Verificar se extrai api_url
    if ($homeNavContent -match "autologin\.optString\(`"api_url`"") {
        Show-Result "App extrai autologin.api_url" "OK"
    } else {
        Show-Result "App NÃO extrai autologin.api_url" "ERRO"
    }
    
    # Verificar se chama UserManager.login
    if ($homeNavContent -match "UserManager\.login\(user,\s*pass\)") {
        Show-Result "App chama UserManager.login()" "OK"
    } else {
        Show-Result "App NÃO chama UserManager.login()" "ERRO"
    }
    
    # Verificar se navega para home
    if ($homeNavContent -match "shouldNavigateToHome\s*=\s*true") {
        Show-Result "App define shouldNavigateToHome = true" "OK"
    } else {
        Show-Result "App NÃO define shouldNavigateToHome" "ERRO"
    }
    
} else {
    Show-Result "Arquivo HomeNav.kt não encontrado" "AVISO" "Caminho: $homeNavPath"
}

# ============================================================================
# RESUMO FINAL
# ============================================================================

Show-Section "RESUMO FINAL DO DIAGNÓSTICO"

Write-Host ""
Write-Host "  [OK] Sucessos: $($script:sucessos.Count)" -ForegroundColor Green
Write-Host "  [ERRO] Erros: $($script:erros.Count)" -ForegroundColor Red
Write-Host "  [AVISO] Avisos: $($script:avisos.Count)" -ForegroundColor Yellow
Write-Host ""

if ($script:erros.Count -eq 0) {
    Write-Host "  [OK] SERVIDOR FUNCIONANDO CORRETAMENTE!" -ForegroundColor Green
    Write-Host ""
    Write-Host "  SE O LOGIN AUTOMATICO NAO FUNCIONA, O PROBLEMA ESTA NO APP:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "    1. App nao esta chamando get-pending-code.php?" -ForegroundColor White
    Write-Host "    2. App nao esta processando o novo formato JSON?" -ForegroundColor White
    Write-Host "    3. App nao esta fazendo UserManager.login?" -ForegroundColor White
    Write-Host "    4. App nao esta navegando para home?" -ForegroundColor White
    Write-Host ""
    Write-Host "  SOLUCAO:" -ForegroundColor Cyan
    Write-Host "    • Verificar logs do app com: adb logcat | grep -E 'HomeNav|auto_login|get-pending'" -ForegroundColor White
    Write-Host "    • Verificar se app foi compilado com as alteracoes mais recentes" -ForegroundColor White
    Write-Host "    • Verificar se app esta usando a versao correta do codigo" -ForegroundColor White
} else {
    Write-Host "  [ERRO] ERROS ENCONTRADOS NO SERVIDOR:" -ForegroundColor Red
    Write-Host ""
    foreach ($erro in $script:erros) {
        Write-Host "    • $erro" -ForegroundColor Red
    }
    Write-Host ""
    Write-Host "  ACAO NECESSARIA:" -ForegroundColor Yellow
    Write-Host "    • Corrigir erros no servidor antes de testar no app" -ForegroundColor White
    Write-Host "    • Verificar logs do Render para mais detalhes" -ForegroundColor White
}

if ($script:avisos.Count -gt 0) {
    Write-Host ""
    Write-Host "  [AVISO] AVISOS:" -ForegroundColor Yellow
    foreach ($aviso in $script:avisos) {
        Write-Host "    • $aviso" -ForegroundColor Yellow
    }
}

Write-Host ""
$separator = '=' * 80
Write-Host $separator -ForegroundColor Cyan
Write-Host "  Diagnóstico concluído em: $(Get-Date -Format 'dd/MM/yyyy HH:mm:ss')" -ForegroundColor Gray
$separator = '=' * 80
Write-Host $separator -ForegroundColor Cyan
Write-Host ""

