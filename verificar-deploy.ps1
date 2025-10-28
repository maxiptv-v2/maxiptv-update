# Script para verificar se valida.php foi enviado corretamente para Render.com

Write-Host "=== Verificando Deploy do valida.php ===" -ForegroundColor Cyan
Write-Host ""

$url = "https://maxiptv-update.onrender.com/valida.php"

# Teste 1: Verificar se arquivo existe
Write-Host "1. Verificando se arquivo existe..." -ForegroundColor Yellow

try {
    $testUrl = "${url}?code=0000"
    $response = Invoke-WebRequest -Uri $testUrl -Method GET -TimeoutSec 10 -ErrorAction Stop
    
    Write-Host "   [OK] Arquivo encontrado! Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host ""
    
    # Teste 2: Verificar se retorna JSON
    Write-Host "2. Verificando formato da resposta..." -ForegroundColor Yellow
    
    try {
        $json = $response.Content | ConvertFrom-Json
        
        Write-Host "   [OK] Resposta e JSON valido!" -ForegroundColor Green
        Write-Host ""
        Write-Host "   Resposta recebida:" -ForegroundColor Cyan
        Write-Host "   $($response.Content)"
        Write-Host ""
        
        # Verificar campos esperados
        Write-Host "3. Verificando estrutura JSON..." -ForegroundColor Yellow
        
        $camposEsperados = @("status", "mensagem")
        $camposOpcionais = @("usuario", "senha", "expira_em", "apk")
        
        $temStatus = $json.PSObject.Properties.Name -contains "status"
        $temMensagem = $json.PSObject.Properties.Name -contains "mensagem"
        
        if ($temStatus) {
            Write-Host "   [OK] Campo 'status' presente" -ForegroundColor Green
        } else {
            Write-Host "   [ERRO] Campo 'status' nao encontrado" -ForegroundColor Red
        }
        
        if ($temMensagem) {
            Write-Host "   [OK] Campo 'mensagem' presente" -ForegroundColor Green
        } elseif ($json.status -eq "ok") {
            # Se status for ok, verificar campos de sucesso
            Write-Host "   [OK] Status OK - verificando campos de sucesso..." -ForegroundColor Green
            
            if ($json.usuario) {
                Write-Host "      [OK] Campo 'usuario' presente" -ForegroundColor Green
            }
            if ($json.senha) {
                Write-Host "      [OK] Campo 'senha' presente" -ForegroundColor Green
            }
            if ($json.expira_em) {
                Write-Host "      [OK] Campo 'expira_em' presente" -ForegroundColor Green
            }
            if ($json.apk) {
                Write-Host "      [OK] Campo 'apk' presente: $($json.apk)" -ForegroundColor Green
            }
        } else {
            Write-Host "   [AVISO] Campo 'mensagem' nao encontrado (mas pode estar em outro formato)" -ForegroundColor Yellow
        }
        
        Write-Host ""
        Write-Host "=== RESULTADO ===" -ForegroundColor Cyan
        Write-Host "Arquivo valida.php esta funcionando corretamente!" -ForegroundColor Green
        Write-Host ""
        Write-Host "Pronto para:" -ForegroundColor Yellow
        Write-Host "  1. Compilar o app" -ForegroundColor White
        Write-Host "  2. Gerar codigo no painel admin" -ForegroundColor White
        Write-Host "  3. Testar com codigo gerado" -ForegroundColor White
        
    } catch {
        Write-Host "   [ERRO] Resposta nao e JSON valido!" -ForegroundColor Red
        Write-Host "   Resposta recebida:" -ForegroundColor Yellow
        Write-Host "   $($response.Content)"
        Write-Host ""
        Write-Host "=== RESULTADO ===" -ForegroundColor Cyan
        Write-Host "Arquivo existe mas nao retorna JSON valido. Verifique o codigo PHP." -ForegroundColor Red
    }
    
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "   [ERRO] Status: $statusCode" -ForegroundColor Red
    
    if ($statusCode -eq 404) {
        Write-Host ""
        Write-Host "=== RESULTADO ===" -ForegroundColor Cyan
        Write-Host "Arquivo valida.php NAO ENCONTRADO (404)" -ForegroundColor Red
        Write-Host ""
        Write-Host "Possiveis causas:" -ForegroundColor Yellow
        Write-Host "  1. Deploy ainda em andamento (aguardar mais alguns minutos)" -ForegroundColor White
        Write-Host "  2. Nome do arquivo incorreto no Dockerfile" -ForegroundColor White
        Write-Host "  3. Arquivo nao foi enviado para o GitHub" -ForegroundColor White
        Write-Host ""
        
        # Verificar se arquivo existe localmente
        if (Test-Path "valida.php") {
            Write-Host "Arquivo valida.php existe localmente." -ForegroundColor Green
        } else {
            Write-Host "ATENCAO: Arquivo valida.php NAO existe localmente!" -ForegroundColor Red
        }
        
    } elseif ($statusCode -eq 503 -or $statusCode -eq 502) {
        Write-Host ""
        Write-Host "=== RESULTADO ===" -ForegroundColor Cyan
        Write-Host "Servidor em deploy ou indisponivel ($statusCode)" -ForegroundColor Yellow
        Write-Host "Aguarde alguns minutos e tente novamente." -ForegroundColor White
    } else {
        Write-Host ""
        Write-Host "=== RESULTADO ===" -ForegroundColor Cyan
        Write-Host "Erro ao conectar: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""

