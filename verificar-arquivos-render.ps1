# Script para verificar se os arquivos no Render estao corretos
# Compara o que deveria retornar com o que realmente retorna

param(
    [Parameter(Mandatory=$true)]
    [string]$Code
)

Write-Host "==================================================================================" -ForegroundColor Cyan
Write-Host "          VERIFICACAO DOS ARQUIVOS NO SERVIDOR RENDER" -ForegroundColor Cyan
Write-Host "==================================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Codigo: $Code" -ForegroundColor Yellow
Write-Host ""

$serverUrl = "https://maxiptv-update-1.onrender.com"

# ============================================================================
# TESTE 1: Verificar se auto_login.php retorna formato correto
# ============================================================================

Write-Host "TESTE 1: Verificando auto_login.php no servidor..." -ForegroundColor Cyan
Write-Host ""

try {
    $url = "$serverUrl/auto_login.php?code=$Code"
    Write-Host "  URL: $url" -ForegroundColor Gray
    
    $response = Invoke-RestMethod -Uri $url -Method Get -TimeoutSec 10
    
    Write-Host "  Resposta do servidor:" -ForegroundColor Yellow
    $response | ConvertTo-Json -Depth 5 | Write-Host
    Write-Host ""
    
    # Verificar formato esperado
    Write-Host "  Verificando formato..." -ForegroundColor Cyan
    
    $formatoCorreto = $true
    $problemas = @()
    
    # Deve ter status
    if (-not $response.status) {
        $formatoCorreto = $false
        $problemas += "Campo 'status' FALTANDO"
    } elseif ($response.status -ne "success") {
        $formatoCorreto = $false
        $problemas += "Status incorreto: $($response.status) (deveria ser 'success')"
    } else {
        Write-Host "  [OK] Status: success" -ForegroundColor Green
    }
    
    # Deve ter autologin
    if (-not $response.autologin) {
        $formatoCorreto = $false
        $problemas += "Objeto 'autologin' FALTANDO"
    } else {
        Write-Host "  [OK] Objeto 'autologin' presente" -ForegroundColor Green
        
        $autologin = $response.autologin
        
        # Verificar campos dentro de autologin
        $camposEsperados = @{
            "username" = "string"
            "password" = "string"
            "api_url" = "string"
            "expires_in" = "number"
            "expiryDate" = "string"
        }
        
        foreach ($campo in $camposEsperados.Keys) {
            if ($autologin.PSObject.Properties.Name -contains $campo) {
                $valor = $autologin.$campo
                if ($valor) {
                    Write-Host "  [OK] Campo 'autologin.$campo': $valor" -ForegroundColor Green
                } else {
                    $problemas += "Campo 'autologin.$campo' esta vazio"
                    $formatoCorreto = $false
                }
            } else {
                $problemas += "Campo 'autologin.$campo' FALTANDO"
                $formatoCorreto = $false
            }
        }
    }
    
    Write-Host ""
    
    if ($formatoCorreto) {
        Write-Host "  [OK] FORMATO CORRETO! auto_login.php esta retornando o formato esperado." -ForegroundColor Green
    } else {
        Write-Host "  [ERRO] FORMATO INCORRETO!" -ForegroundColor Red
        Write-Host ""
        Write-Host "  Problemas encontrados:" -ForegroundColor Yellow
        foreach ($problema in $problemas) {
            Write-Host "    - $problema" -ForegroundColor Red
        }
        Write-Host ""
        Write-Host "  FORMATO ESPERADO:" -ForegroundColor Cyan
        Write-Host "  {" -ForegroundColor White
        Write-Host '    "status": "success",' -ForegroundColor White
        Write-Host '    "autologin": {' -ForegroundColor White
        Write-Host '      "username": "...",' -ForegroundColor White
        Write-Host '      "password": "...",' -ForegroundColor White
        Write-Host '      "api_url": "...",' -ForegroundColor White
        Write-Host '      "expires_in": 21600,' -ForegroundColor White
        Write-Host '      "expiryDate": "DD/MM/YYYY"' -ForegroundColor White
        Write-Host "    }" -ForegroundColor White
        Write-Host "  }" -ForegroundColor White
        Write-Host ""
        Write-Host "  CORRECAO:" -ForegroundColor Yellow
        Write-Host "  1. Verifique se auto_login.php foi deployado corretamente no Render" -ForegroundColor White
        Write-Host "  2. Verifique os logs do Render para ver erros do PHP" -ForegroundColor White
        Write-Host "  3. Faça novo deploy se necessario" -ForegroundColor White
    }
    
} catch {
    Write-Host "  [ERRO] Nao foi possivel testar auto_login.php" -ForegroundColor Red
    Write-Host "  Erro: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "==================================================================================" -ForegroundColor Cyan
Write-Host "TESTE 2: Verificando dl.php no servidor..." -ForegroundColor Cyan
Write-Host ""

try {
    $url = "$serverUrl/dl/$Code"
    Write-Host "  URL: $url" -ForegroundColor Gray
    
    try {
        $response = Invoke-WebRequest -Uri $url -Method Get -MaximumRedirection 0 -ErrorAction SilentlyContinue
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        
        if ($statusCode -eq 302) {
            $redirectUrl = $_.Exception.Response.Headers.Location
            Write-Host "  [OK] dl.php redirecionou (302)" -ForegroundColor Green
            Write-Host "  [OK] URL de redirecionamento: $redirectUrl" -ForegroundColor Green
            
            if ($redirectUrl -match "maxiptv-release\.apk") {
                Write-Host "  [OK] URL do APK esta correta" -ForegroundColor Green
            } else {
                Write-Host "  [AVISO] URL do APK pode estar incorreta" -ForegroundColor Yellow
                Write-Host "    URL recebida: $redirectUrl" -ForegroundColor Yellow
            }
        } else {
            Write-Host "  [ERRO] dl.php retornou status: $statusCode" -ForegroundColor Red
        }
    }
} catch {
    Write-Host "  [ERRO] Nao foi possivel testar dl.php" -ForegroundColor Red
    Write-Host "  Erro: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "==================================================================================" -ForegroundColor Cyan
Write-Host "TESTE 3: Verificando get-pending-code.php no servidor..." -ForegroundColor Cyan
Write-Host ""

try {
    $url = "$serverUrl/get-pending-code.php"
    Write-Host "  URL: $url" -ForegroundColor Gray
    
    $response = Invoke-RestMethod -Uri $url -Method Get -TimeoutSec 10
    
    Write-Host "  Resposta:" -ForegroundColor Yellow
    $response | ConvertTo-Json -Depth 5 | Write-Host
    Write-Host ""
    
    if ($response.status -eq "ok") {
        Write-Host "  [OK] Status: ok" -ForegroundColor Green
        Write-Host "  [OK] Codigo retornado: $($response.code)" -ForegroundColor Green
    } else {
        Write-Host "  [AVISO] Status diferente de 'ok': $($response.status)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  [ERRO] Nao foi possivel testar get-pending-code.php" -ForegroundColor Red
    Write-Host "  Erro: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "==================================================================================" -ForegroundColor Cyan
Write-Host "RESUMO" -ForegroundColor Cyan
Write-Host ""
Write-Host "Se algum teste falhou, verifique:" -ForegroundColor Yellow
Write-Host "1. Arquivos foram deployados corretamente no Render?" -ForegroundColor White
Write-Host "2. Render esta rodando a versao mais recente do codigo?" -ForegroundColor White
Write-Host "3. Logs do Render mostram algum erro?" -ForegroundColor White
Write-Host ""
Write-Host "Para verificar logs do Render:" -ForegroundColor Cyan
Write-Host "  Acesse: https://dashboard.render.com" -ForegroundColor White
Write-Host "  Vá para o servico maxiptv-update-1" -ForegroundColor White
Write-Host "  Clique em 'Logs' para ver erros do PHP" -ForegroundColor White
Write-Host ""

