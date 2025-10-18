# Criador de Icones PNG Basicos para Android
# Cria PNGs validos que o AAPT consegue compilar

Write-Host "Criando PNGs basicos..." -ForegroundColor Magenta

# PNG valido de 1x1 pixel transparente (base64 real)
$validPNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="

function Create-ValidPNG {
    param(
        [string]$Path,
        [int]$Size
    )
    
    # Converte base64 para bytes
    $bytes = [Convert]::FromBase64String($validPNG)
    
    # Salva o arquivo PNG valido
    [System.IO.File]::WriteAllBytes($Path, $bytes)
    Write-Host "PNG valido criado: $Path" -ForegroundColor Green
}

# Cria PNGs validos em todas as densidades
Write-Host "Criando PNGs em todas as densidades..." -ForegroundColor Cyan

Create-ValidPNG -Path "app\src\main\res\mipmap-mdpi\ic_launcher.png" -Size 48
Create-ValidPNG -Path "app\src\main\res\mipmap-hdpi\ic_launcher.png" -Size 72
Create-ValidPNG -Path "app\src\main\res\mipmap-xhdpi\ic_launcher.png" -Size 96
Create-ValidPNG -Path "app\src\main\res\mipmap-xxhdpi\ic_launcher.png" -Size 144
Create-ValidPNG -Path "app\src\main\res\mipmap-xxxhdpi\ic_launcher.png" -Size 192

Write-Host "PNGs basicos criados!" -ForegroundColor Yellow
Write-Host "Nota: Estes sao placeholders transparentes" -ForegroundColor Gray
Write-Host "Para melhor qualidade, substitua por icones reais" -ForegroundColor White
Write-Host "Pronto para compilar!" -ForegroundColor Green
