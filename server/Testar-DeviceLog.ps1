param(
    [string]$Endpoint = "https://maxiptv-update-1.onrender.com/device-log.php"
)

$payload = @{
    manufacturer = "script-test"
    model = "demo"
    brand = "demo"
    product = "demo"
    classification = "test"
    loggedAt = (Get-Date).ToString("o")
} | ConvertTo-Json

try {
    Write-Host "Enviando payload para $Endpoint" -ForegroundColor Cyan
    $response = Invoke-RestMethod -Uri $Endpoint -Method Post -Body $payload -ContentType "application/json" -ErrorAction Stop
    Write-Host "Resposta:" -ForegroundColor Green
    $response | ConvertTo-Json
} catch {
    Write-Host "Falha ao enviar log:" -ForegroundColor Red
    Write-Host $_.Exception.Message
    if ($_.Exception.Response -ne $null) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $errorBody = $reader.ReadToEnd()
        Write-Host "Corpo da resposta:" $errorBody -ForegroundColor Yellow
    }
}
