# Criador de PNGs válidos para Android
# Cria PNGs reais que o AAPT consegue compilar

Write-Host "🎨 Criando PNGs válidos..." -ForegroundColor Magenta

# PNG válido de 1x1 pixel transparente (base64 real)
$validPNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="

function Create-ValidPNG {
    param(
        [string]$Path,
        [int]$Size
    )
    
    # Converte base64 para bytes
    $bytes = [Convert]::FromBase64String($validPNG)
    
    # Salva o arquivo PNG válido
    [System.IO.File]::WriteAllBytes($Path, $bytes)
    Write-Host "✅ PNG válido criado: $Path" -ForegroundColor Green
}

# Cria PNGs válidos em todas as densidades
Write-Host "📱 Criando PNGs em todas as densidades..." -ForegroundColor Cyan

Create-ValidPNG -Path "app\src\main\res\mipmap-mdpi\ic_launcher.png" -Size 48
Create-ValidPNG -Path "app\src\main\res\mipmap-mdpi\ic_launcher_banner.png" -Size 48

Create-ValidPNG -Path "app\src\main\res\mipmap-hdpi\ic_launcher.png" -Size 72
Create-ValidPNG -Path "app\src\main\res\mipmap-hdpi\ic_launcher_banner.png" -Size 72

Create-ValidPNG -Path "app\src\main\res\mipmap-xhdpi\ic_launcher.png" -Size 96
Create-ValidPNG -Path "app\src\main\res\mipmap-xhdpi\ic_launcher_banner.png" -Size 96

Create-ValidPNG -Path "app\src\main\res\mipmap-xxhdpi\ic_launcher.png" -Size 144
Create-ValidPNG -Path "app\src\main\res\mipmap-xxhdpi\ic_launcher_banner.png" -Size 144

Create-ValidPNG -Path "app\src\main\res\mipmap-xxxhdpi\ic_launcher.png" -Size 192
Create-ValidPNG -Path "app\src\main\res\mipmap-xxxhdpi\ic_launcher_banner.png" -Size 192

Write-Host "🎯 PNGs válidos criados!" -ForegroundColor Yellow
Write-Host "📝 Nota: Estes são placeholders transparentes" -ForegroundColor Gray
Write-Host "🎨 Para melhor qualidade, substitua por ícones reais da TV roxa" -ForegroundColor White
Write-Host "✨ Pronto para compilar!" -ForegroundColor Green
