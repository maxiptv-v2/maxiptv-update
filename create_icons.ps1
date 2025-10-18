# Gerador de Icones IPTV - TV Roxa
# Este script cria os icones PNG em diferentes resolucoes

Write-Host "Criando icones IPTV com TV roxa..." -ForegroundColor Magenta

# Funcao para criar icone PNG simples usando PowerShell
function Create-IconPNG {
    param(
        [string]$Path,
        [int]$Size,
        [string]$Color = "#8B5CF6"
    )
    
    # Cria um arquivo PNG basico (placeholder)
    # Em producao, voce usaria uma ferramenta como ImageMagick ou GIMP
    $content = @"
# Placeholder para icone ${Size}x${Size}
# Cor: $Color
# Este arquivo deve ser substituido por um PNG real
"@
    
    Set-Content -Path $Path -Value $content -Encoding UTF8
    Write-Host "Criado: $Path" -ForegroundColor Green
}

# Cria icones em diferentes densidades
Write-Host "Criando icones para diferentes densidades..." -ForegroundColor Cyan

# MDPI (48x48)
Create-IconPNG -Path "app\src\main\res\mipmap-mdpi\ic_launcher.png" -Size 48
Create-IconPNG -Path "app\src\main\res\mipmap-mdpi\ic_launcher_banner.png" -Size 48

# HDPI (72x72)
Create-IconPNG -Path "app\src\main\res\mipmap-hdpi\ic_launcher.png" -Size 72
Create-IconPNG -Path "app\src\main\res\mipmap-hdpi\ic_launcher_banner.png" -Size 72

# XHDPI (96x96)
Create-IconPNG -Path "app\src\main\res\mipmap-xhdpi\ic_launcher.png" -Size 96
Create-IconPNG -Path "app\src\main\res\mipmap-xhdpi\ic_launcher_banner.png" -Size 96

# XXHDPI (144x144)
Create-IconPNG -Path "app\src\main\res\mipmap-xxhdpi\ic_launcher.png" -Size 144
Create-IconPNG -Path "app\src\main\res\mipmap-xxhdpi\ic_launcher_banner.png" -Size 144

# XXXHDPI (192x192)
Create-IconPNG -Path "app\src\main\res\mipmap-xxxhdpi\ic_launcher.png" -Size 192
Create-IconPNG -Path "app\src\main\res\mipmap-xxxhdpi\ic_launcher_banner.png" -Size 192

Write-Host "Icones criados! Agora voce precisa:" -ForegroundColor Yellow
Write-Host "1. Substituir os placeholders por PNGs reais" -ForegroundColor White
Write-Host "2. Usar ferramenta como GIMP, Photoshop ou online" -ForegroundColor White
Write-Host "3. Design: TV roxa com texto IPTV branco" -ForegroundColor White
Write-Host "4. Banner TV: 1280x720px" -ForegroundColor White

Write-Host "Configuracao completa!" -ForegroundColor Green