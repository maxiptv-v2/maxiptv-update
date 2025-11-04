# Script para testar dl.php completo e verificar se está validando corretamente

param(
    [Parameter(Mandatory=$true)]
    [string]$Code
)

Write-Host "=== TESTE COMPLETO DO dl.php ===" -ForegroundColor Cyan
Write-Host "Codigo: $Code" -ForegroundColor Yellow
Write-Host ""

$serverUrl = "https://maxiptv-update-1.onrender.com"
$jsonbinUrl = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b"
$jsonbinKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'
$headers = @{"X-Master-Key" = $jsonbinKey}

Write-Host "PASSO 1: Verificando código no JSONBin..." -ForegroundColor Cyan
try {
    $jsonbin = Invoke-RestMethod -Uri "$jsonbinUrl/latest" -Headers $headers -Method Get
    
    if ($jsonbin.record.$Code) {
        $codeData = $jsonbin.record.$Code
        Write-Host "   ✅ Código encontrado" -ForegroundColor Green
        Write-Host "      Username: $($codeData.username)" -ForegroundColor White
        Write-Host "      Password: $($codeData.password)" -ForegroundColor White
        Write-Host "      API URL: $($codeData.apiUrl)" -ForegroundColor White
        Write-Host "      Expiry Date: $($codeData.expiryDate)" -ForegroundColor White
        
        # Verificar se está expirado
        if ($codeData.expiryDate) {
            $parts = $codeData.expiryDate -split '/'
            if ($parts.Count -eq 3) {
                $day = [int]$parts[0]
                $month = [int]$parts[1]
                $year = [int]$parts[2]
                $expiryDate = Get-Date -Year $year -Month $month -Day $day -Hour 23 -Minute 59 -Second 59
                $now = Get-Date
                
                if ($now -gt $expiryDate) {
                    Write-Host "   ⚠️ AVISO: Usuário está EXPIRADO!" -ForegroundColor Red
                    Write-Host "      Data de expiração: $($codeData.expiryDate)" -ForegroundColor Red
                    Write-Host "      Data atual: $(Get-Date -Format 'dd/MM/yyyy')" -ForegroundColor Red
                } else {
                    Write-Host "   ✅ Usuário está VÁLIDO" -ForegroundColor Green
                    $daysLeft = ($expiryDate - $now).Days
                    Write-Host "      Dias restantes: $daysLeft" -ForegroundColor White
                }
            }
        }
        
        # Verificar createdAt (6 horas)
        if ($codeData.createdAt) {
            $createdAt = [DateTimeOffset]::FromUnixTimeMilliseconds($codeData.createdAt).DateTime
            $sixHoursLater = $createdAt.AddHours(6)
            $now = Get-Date
            
            if ($now -gt $sixHoursLater) {
                Write-Host "   ⚠️ AVISO: Código expirou (mais de 6 horas)" -ForegroundColor Red
                Write-Host "      Criado em: $createdAt" -ForegroundColor Red
                Write-Host "      Expira em: $sixHoursLater" -ForegroundColor Red
            } else {
                Write-Host "   ✅ Código está válido (menos de 6 horas)" -ForegroundColor Green
                $hoursLeft = ($sixHoursLater - $now).TotalHours
                Write-Host "      Horas restantes: $([math]::Round($hoursLeft, 2))" -ForegroundColor White
            }
        }
    } else {
        Write-Host "   ❌ Código NÃO encontrado no JSONBin!" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "   ❌ Erro ao acessar JSONBin: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "PASSO 2: Testando dl.php (deve redirecionar para APK)..." -ForegroundColor Cyan
try {
    $dlUrl = "$serverUrl/dl/$Code"
    Write-Host "   URL: $dlUrl" -ForegroundColor Gray
    
    try {
        $dlResponse = Invoke-WebRequest -Uri $dlUrl -Method Get -MaximumRedirection 0 -ErrorAction SilentlyContinue
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq 302) {
            $redirectUrl = $_.Exception.Response.Headers.Location
            Write-Host "   ✅ dl.php redirecionou (302)" -ForegroundColor Green
            Write-Host "      Para: $redirectUrl" -ForegroundColor White
            
            if ($redirectUrl -match "maxiptv-release\.apk") {
                Write-Host "   ✅ URL do APK está correta" -ForegroundColor Green
            } else {
                Write-Host "   ⚠️ URL do APK pode estar incorreta" -ForegroundColor Yellow
            }
        } elseif ($statusCode -eq 404) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $errorContent = $reader.ReadToEnd()
            Write-Host "   ❌ dl.php retornou 404" -ForegroundColor Red
            Write-Host "      Resposta: $errorContent" -ForegroundColor Red
            Write-Host ""
            Write-Host "   POSSÍVEIS CAUSAS:" -ForegroundColor Yellow
            Write-Host "   1. Código não existe no JSONBin" -ForegroundColor White
            Write-Host "   2. Código expirou (mais de 6 horas)" -ForegroundColor White
            Write-Host "   3. Usuário expirou (expiryDate passou)" -ForegroundColor White
        } else {
            Write-Host "   ❌ dl.php retornou status: $statusCode" -ForegroundColor Red
        }
    }
} catch {
    Write-Host "   ❌ Erro ao testar dl.php: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "PASSO 3: Verificando se código foi salvo em _pending_logins..." -ForegroundColor Cyan
Start-Sleep -Seconds 2
try {
    $jsonbin = Invoke-RestMethod -Uri "$jsonbinUrl/latest" -Headers $headers -Method Get
    
    if ($jsonbin.record._pending_logins) {
        $pending = $jsonbin.record._pending_logins
        Write-Host "   ✅ _pending_logins encontrado" -ForegroundColor Green
        Write-Host "      Total de códigos pendentes: $($pending.PSObject.Properties.Count)" -ForegroundColor White
        
        $found = $false
        $pending.PSObject.Properties | ForEach-Object {
            $pendingData = $_.Value
            if ($pendingData.code -eq $Code) {
                $found = $true
                Write-Host "   ✅ Código $Code encontrado em _pending_logins!" -ForegroundColor Green
                Write-Host "      Username: $($pendingData.username)" -ForegroundColor White
                Write-Host "      Timestamp: $pendingData.timestamp" -ForegroundColor White
                Write-Host "      Expires At: $pendingData.expiresAt" -ForegroundColor White
                
                $expiresAt = [DateTimeOffset]::FromUnixTimeSeconds($pendingData.expiresAt).DateTime
                $now = Get-Date
                if ($now -gt $expiresAt) {
                    Write-Host "      ⚠️ Código pendente EXPIRADO!" -ForegroundColor Red
                } else {
                    $minutesLeft = ($expiresAt - $now).TotalMinutes
                    Write-Host "      ✅ Código pendente válido por mais $([math]::Round($minutesLeft, 1)) minutos" -ForegroundColor Green
                }
            }
        }
        
        if (-not $found) {
            Write-Host "   ⚠️ Código $Code NÃO encontrado em _pending_logins" -ForegroundColor Yellow
            Write-Host "      Isso significa que dl.php pode não estar salvando corretamente" -ForegroundColor Yellow
        }
    } else {
        Write-Host "   ⚠️ _pending_logins não existe ou está vazio" -ForegroundColor Yellow
        Write-Host "      Isso significa que dl.php não salvou o código pendente" -ForegroundColor Yellow
    }
} catch {
    Write-Host "   ❌ Erro ao verificar _pending_logins: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "PASSO 4: Testando auto_login.php (validação completa)..." -ForegroundColor Cyan
try {
    $autoLoginUrl = "$serverUrl/auto_login.php?code=$Code"
    Write-Host "   URL: $autoLoginUrl" -ForegroundColor Gray
    
    $autoLoginResponse = Invoke-RestMethod -Uri $autoLoginUrl -Method Get -TimeoutSec 10
    
    Write-Host "   ✅ auto_login.php respondeu" -ForegroundColor Green
    Write-Host "   Resposta:" -ForegroundColor Yellow
    $autoLoginResponse | ConvertTo-Json -Depth 5 | Write-Host
    
    # Verificar se tem campos obrigatórios
    $camposEsperados = @("user", "password", "api", "expiryDate")
    $camposEncontrados = @()
    
    foreach ($campo in $camposEsperados) {
        if ($autoLoginResponse.PSObject.Properties.Name -contains $campo) {
            $camposEncontrados += $campo
        }
    }
    
    if ($camposEncontrados.Count -eq 4) {
        Write-Host ""
        Write-Host "   ✅ Todos os campos obrigatórios presentes" -ForegroundColor Green
    } else {
        $faltando = $camposEsperados | Where-Object { $_ -notin $camposEncontrados }
        Write-Host ""
        Write-Host "   ❌ Campos faltando: $($faltando -join ', ')" -ForegroundColor Red
    }
    
    # Verificar se tem status de erro
    if ($autoLoginResponse.status) {
        if ($autoLoginResponse.status -eq "expired") {
            Write-Host ""
            Write-Host "   ❌ ERRO: Usuário EXPIRADO!" -ForegroundColor Red
            Write-Host "      Mensagem: $($autoLoginResponse.message)" -ForegroundColor Red
        } elseif ($autoLoginResponse.status -eq "erro") {
            Write-Host ""
            Write-Host "   ❌ ERRO: $($autoLoginResponse.mensagem)" -ForegroundColor Red
        }
    }
    
} catch {
    Write-Host "   ❌ Erro ao testar auto_login.php: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "   Status HTTP: $statusCode" -ForegroundColor Red
        
        try {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $errorContent = $reader.ReadToEnd()
            Write-Host "   Resposta: $errorContent" -ForegroundColor Red
        } catch {
            Write-Host "   Não foi possível ler a resposta" -ForegroundColor Red
        }
    }
}

Write-Host ""
Write-Host "=== RESUMO ===" -ForegroundColor Cyan
Write-Host "Se o login automático não funciona, verifique:" -ForegroundColor Yellow
Write-Host "1. dl.php está salvando código em _pending_logins?" -ForegroundColor White
Write-Host "2. auto_login.php está validando expiryDate corretamente?" -ForegroundColor White
Write-Host "3. auto_login.php está retornando todos os campos?" -ForegroundColor White
Write-Host "4. O app está chamando auto_login.php com o código correto?" -ForegroundColor White
Write-Host ""

