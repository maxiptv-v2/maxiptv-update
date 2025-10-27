# Script para testar o download.php no Render.com
# Testa se o codigo retorna as credenciais corretamente

param(
    [string]$code = ""
)

if ($code -eq "") {
    Write-Host "Uso: .\test-download.ps1 -code 1234" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Exemplos:" -ForegroundColor Cyan
    Write-Host "  .\test-download.ps1 -code 1234"
    Write-Host "  .\test-download.ps1 -code 5678"
    exit
}

Write-Host "Testando codigo: $code" -ForegroundColor Cyan
Write-Host ""

$url = "https://maxiptv-update.onrender.com/download.php?code=$code"

Write-Host "Fazendo requisicao para: $url" -ForegroundColor Gray
Write-Host ""

try {
    $response = Invoke-WebRequest -Uri $url -Method GET -ErrorAction Stop
    
    Write-Host "Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host ""
    
    $content = $response.Content
    Write-Host "Resposta do servidor:" -ForegroundColor Cyan
    Write-Host ""
    Write-Host $content
    Write-Host ""
    
    # Tentar parsear como JSON
    try {
        $json = $content | ConvertFrom-Json
        
        if ($json.success) {
            Write-Host "Login automatico configurado para:" -ForegroundColor Green
            Write-Host "  Usuario: $($json.usuario)" -ForegroundColor White
            Write-Host "  Senha: $($json.senha)" -ForegroundColor White
            Write-Host "  API: $($json.api)" -ForegroundColor White
            Write-Host "  Expira em: $($json.expira_em)" -ForegroundColor White
        } else {
            Write-Host "Codigo invalido ou inativo" -ForegroundColor Red
        }
    } catch {
        Write-Host "Resposta nao e JSON valido" -ForegroundColor Yellow
    }
    
} catch {
    Write-Host "Erro ao conectar:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $errorContent = $reader.ReadToEnd()
        Write-Host ""
        Write-Host "Erro do servidor:" -ForegroundColor Yellow
        Write-Host $errorContent
    }
}
