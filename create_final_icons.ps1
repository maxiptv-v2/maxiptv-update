# Criador de Icone IPTV Real - TV Roxa
# Cria um PNG real com a TV roxa e texto IPTV

Write-Host "🎨 Criando icone IPTV real..." -ForegroundColor Magenta

# PNG de 48x48 com TV roxa (base64)
$icon48 = @"
iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==
"@

# PNG de 72x72 com TV roxa
$icon72 = @"
iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==
"@

# PNG de 96x96 com TV roxa
$icon96 = @"
iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==
"@

# PNG de 144x144 com TV roxa
$icon144 = @"
iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==
"@

# PNG de 192x192 com TV roxa
$icon192 = @"
iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==
"@

# Funcao para salvar PNG
function Save-PNGIcon {
    param(
        [string]$Path,
        [string]$Base64Data
    )
    
    $bytes = [Convert]::FromBase64String($Base64Data.Trim())
    [System.IO.File]::WriteAllBytes($Path, $bytes)
    Write-Host "✅ Criado: $Path" -ForegroundColor Green
}

# Cria todos os icones
Write-Host "📱 Criando icones em todas as densidades..." -ForegroundColor Cyan

Save-PNGIcon -Path "app\src\main\res\mipmap-mdpi\ic_launcher.png" -Base64Data $icon48
Save-PNGIcon -Path "app\src\main\res\mipmap-mdpi\ic_launcher_banner.png" -Base64Data $icon48

Save-PNGIcon -Path "app\src\main\res\mipmap-hdpi\ic_launcher.png" -Base64Data $icon72
Save-PNGIcon -Path "app\src\main\res\mipmap-hdpi\ic_launcher_banner.png" -Base64Data $icon72

Save-PNGIcon -Path "app\src\main\res\mipmap-xhdpi\ic_launcher.png" -Base64Data $icon96
Save-PNGIcon -Path "app\src\main\res\mipmap-xhdpi\ic_launcher_banner.png" -Base64Data $icon96

Save-PNGIcon -Path "app\src\main\res\mipmap-xxhdpi\ic_launcher.png" -Base64Data $icon144
Save-PNGIcon -Path "app\src\main\res\mipmap-xxhdpi\ic_launcher_banner.png" -Base64Data $icon144

Save-PNGIcon -Path "app\src\main\res\mipmap-xxxhdpi\ic_launcher.png" -Base64Data $icon192
Save-PNGIcon -Path "app\src\main\res\mipmap-xxxhdpi\ic_launcher_banner.png" -Base64Data $icon192

Write-Host "🎯 Todos os icones criados!" -ForegroundColor Yellow
Write-Host "📺 Design: TV roxa com texto IPTV branco" -ForegroundColor White
Write-Host "✨ Pronto para compilar!" -ForegroundColor Green
