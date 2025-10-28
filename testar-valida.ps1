# Script para testar valida.php no Render.com

param(
    [string]$code = "1234"
)

Write-Host "=== Testando valida.php no Render.com ===" -ForegroundColor Cyan
Write-Host "Codigo: $code" -ForegroundColor Yellow
Write-Host ""

$url = "https://maxiptv-update.onrender.com/valida.php?code=$code"

Write-Host "URL: $url" -ForegroundColor Gray
Write-Host ""
Write-Host "Fazendo requisicao..." -ForegroundColor Cyan
Write-Host ""

try {
    $response = Invoke-WebRequest -Uri $url -Method GET -ErrorAction Stop
    
    Write-Host "Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host ""
    Write-Host "Resposta JSON:" -ForegroundColor Yellow
    Write-Host $response.Content
    Write-Host ""
    
    # Tentar parsear JSON
    try {
        $json = $response.Content | ConvertFrom-Json
        
        if ($json.status -eq "ok") {
            Write-Host "SUCESSO! Dados retornados:" -ForegroundColor Green
            Write-Host "  Usuario: $($json.usuario)"
            Write-Host "  Senha: $($json.senha)"
            Write-Host "  Expira: $($json.expira_em)"
            Write-Host "  APK: $($json.apk)"
        } elseif ($json.status -eq "erro") {
            Write-Host "ERRO: $($json.mensagem)" -ForegroundColor Red
        }
    } catch {
        Write-Host "Resposta nao e JSON valido ou formato diferente" -ForegroundColor Yellow
    }
    
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "Status: $statusCode" -ForegroundColor Red
    
    if ($_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $content = $reader.ReadToEnd()
        $reader.Close()
        $stream.Close()
        
        Write-Host ""
        Write-Host "Erro:" -ForegroundColor Red
        Write-Host $content
        
        if ($statusCode -eq 404) {
            Write-Host ""
            Write-Host "ERRO 404: Arquivo valida.php nao encontrado!" -ForegroundColor Red
            Write-Host "O deploy ainda pode estar em andamento." -ForegroundColor Yellow
        }
    } else {
        Write-Host "Erro ao conectar: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=== Fim do teste ===" -ForegroundColor Cyan

