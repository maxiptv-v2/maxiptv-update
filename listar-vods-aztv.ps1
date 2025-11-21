# ========================================
# LISTAR VODs disponíveis em aztv.cx
# ========================================

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "LISTAR VODs: aztv.cx" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Pedir credenciais
Write-Host "Informe as credenciais:" -ForegroundColor Yellow
$Username = Read-Host "Username"
$Password = Read-Host "Password" -AsSecureString
$Password = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($Password))

# URLs possíveis
$BaseUrls = @(
    "https://aztv.cx",
    "http://aztv.cx",
    "https://aztv.cx:8080",
    "http://aztv.cx:8080",
    "https://aztv.cx:25463",
    "http://aztv.cx:25463"
)

Write-Host "`nTestando diferentes URLs..." -ForegroundColor Yellow

foreach ($BaseUrl in $BaseUrls) {
    Write-Host "`nTentando: $BaseUrl" -ForegroundColor Cyan
    
    $ApiUrl = "$BaseUrl/player_api.php?username=$Username&password=$Password&action=get_vod_streams"
    
    try {
        $response = Invoke-RestMethod -Uri $ApiUrl -Method Get -ContentType "application/json" -ErrorAction Stop -TimeoutSec 10
        
        Write-Host "✅ SUCESSO! URL funcionando: $BaseUrl" -ForegroundColor Green
        Write-Host ""
        
        if ($response -is [array] -and $response.Count -gt 0) {
            Write-Host "📺 Total de VODs encontrados: $($response.Count)" -ForegroundColor Green
            Write-Host ""
            Write-Host "Primeiros 20 VODs:" -ForegroundColor Yellow
            Write-Host ""
            
            $response | Select-Object -First 20 | ForEach-Object {
                $id = $_.stream_id
                $name = $_.name
                Write-Host "   ID: $id - $name" -ForegroundColor White
            }
            
            Write-Host ""
            Write-Host "💡 Use um dos IDs acima para testar o rating!" -ForegroundColor Cyan
            Write-Host "   Exemplo: ID $($response[0].stream_id) - $($response[0].name)" -ForegroundColor Gray
            
            # Sugerir alguns IDs
            Write-Host ""
            Write-Host "SUGESTÕES DE IDs PARA TESTAR:" -ForegroundColor Yellow
            Write-Host "   • Primeiro VOD: $($response[0].stream_id)" -ForegroundColor White
            if ($response.Count -gt 1) {
                Write-Host "   • Segundo VOD: $($response[1].stream_id)" -ForegroundColor White
            }
            if ($response.Count -gt 9) {
                Write-Host "   • Décimo VOD: $($response[9].stream_id)" -ForegroundColor White
            }
            if ($response.Count -gt 99) {
                Write-Host "   • Centésimo VOD: $($response[99].stream_id)" -ForegroundColor White
            }
            
        } else {
            Write-Host "⚠️ Resposta vazia ou formato inesperado" -ForegroundColor Yellow
        }
        
        break  # Se funcionou, para de testar outras URLs
        
    } catch {
        Write-Host "   ❌ Erro: $($_.Exception.Message)" -ForegroundColor Red
        continue
    }
}

Write-Host "`n========================================`n" -ForegroundColor Cyan

