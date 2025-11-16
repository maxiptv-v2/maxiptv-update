# Script Automático para Criar AVD Android TV (Fire Stick HD)
# Tenta criar via linha de comando ou fornece instruções claras

$ErrorActionPreference = "Continue"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CRIACAO AUTOMATICA DE AVD" -ForegroundColor Cyan
Write-Host "Perfil: Fire Stick HD (720p/1080p)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Caminhos
$sdkPath = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
$avdManager = "$sdkPath\cmdline-tools\latest\bin\avdmanager.bat"
$sdkManager = "$sdkPath\cmdline-tools\latest\bin\sdkmanager.bat"
$emulator = "$sdkPath\emulator\emulator.exe"
$avdName = "FireStick_HD_Test"
$packageId = "system-images;android-33;google_apis;x86_64"

# Verificar SDK
if (-not (Test-Path $sdkPath)) {
    Write-Host "[ERRO] Android SDK nao encontrado!" -ForegroundColor Red
    Write-Host "Por favor, instale o Android Studio primeiro." -ForegroundColor Yellow
    exit 1
}

Write-Host "[OK] Android SDK: $sdkPath" -ForegroundColor Green

# Verificar avdmanager
if (-not (Test-Path $avdManager)) {
    $avdManager = "$sdkPath\tools\bin\avdmanager.bat"
}

if (-not (Test-Path $avdManager)) {
    Write-Host ""
    Write-Host "[AVISO] AVD Manager nao encontrado via linha de comando" -ForegroundColor Yellow
    Write-Host "Vou criar instrucoes para usar o Android Studio..." -ForegroundColor Yellow
    Write-Host ""
    
    # Criar arquivo de instruções
    $instrucoes = @"
========================================
INSTRUCOES PARA CRIAR AVD NO ANDROID STUDIO
========================================

1. Abra o Android Studio
2. Vá em: Tools > Device Manager
3. Clique em 'Create Device' (ou botao '+' no canto superior)
4. Categoria: TV
5. Selecione: 'TV (1080p)' ou 'TV (720p)'
6. Clique 'Next'
7. System Image: Escolha 'API 33' ou superior (Google APIs)
   Se nao tiver, clique 'Download' e aguarde
8. Clique 'Next'
9. AVD Name: FireStick_HD_Test
10. Clique 'Finish'

APOS CRIAR:
- Execute: .\start-emulator-1080p.bat
- Ou: .\start-emulator-720p.bat

"@
    
    $instrucoes | Out-File -FilePath "INSTRUCOES_AVD.txt" -Encoding UTF8
    Write-Host $instrucoes
    Write-Host "[OK] Instrucoes salvas em: INSTRUCOES_AVD.txt" -ForegroundColor Green
    exit 0
}

Write-Host "[OK] AVD Manager encontrado" -ForegroundColor Green
Write-Host ""

# Verificar se AVD já existe
Write-Host "Verificando AVDs existentes..." -ForegroundColor Yellow
$existingAvds = & $avdManager list avd 2>&1
if ($existingAvds -match $avdName) {
    Write-Host "[OK] AVD '$avdName' ja existe!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Para iniciar:" -ForegroundColor Cyan
    Write-Host "  .\start-emulator-1080p.bat" -ForegroundColor White
    Write-Host "  .\start-emulator-720p.bat" -ForegroundColor White
    exit 0
}

# Verificar se imagem do sistema está instalada
Write-Host "Verificando imagem do sistema Android TV..." -ForegroundColor Yellow
$installed = & $sdkManager --list_installed 2>&1
if ($installed -notmatch [regex]::Escape($packageId)) {
    Write-Host "[AVISO] Imagem do sistema nao encontrada!" -ForegroundColor Yellow
    Write-Host "Instalando imagem do sistema (pode demorar alguns minutos)..." -ForegroundColor Yellow
    Write-Host ""
    
    & $sdkManager $packageId --accept-licenses 2>&1 | Write-Host
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "[ERRO] Falha ao instalar imagem do sistema!" -ForegroundColor Red
        Write-Host ""
        Write-Host "SOLUCAO MANUAL:" -ForegroundColor Yellow
        Write-Host "1. Abra Android Studio" -ForegroundColor White
        Write-Host "2. Tools > SDK Manager" -ForegroundColor White
        Write-Host "3. SDK Platforms > Android 13 (API 33)" -ForegroundColor White
        Write-Host "4. Marque 'Google APIs Intel x86 Atom_64 System Image'" -ForegroundColor White
        Write-Host "5. Apply > OK" -ForegroundColor White
        exit 1
    }
    
    Write-Host "[OK] Imagem do sistema instalada" -ForegroundColor Green
} else {
    Write-Host "[OK] Imagem do sistema ja instalada" -ForegroundColor Green
}

Write-Host ""

# Criar AVD
Write-Host "Criando AVD '$avdName'..." -ForegroundColor Yellow
Write-Host "Isso pode demorar alguns segundos..." -ForegroundColor Yellow
Write-Host ""

# Tentar criar com perfil TV
$createOutput = & $avdManager create avd -n $avdName -k $packageId -d "tv_1080p" --force 2>&1

# Verificar se foi criado
$avds = & $avdManager list avd 2>&1
if ($avds -match $avdName) {
    Write-Host ""
    Write-Host "[OK] AVD criado com sucesso!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Para iniciar o emulador:" -ForegroundColor Cyan
    Write-Host "  .\start-emulator-1080p.bat  (Full HD)" -ForegroundColor White
    Write-Host "  .\start-emulator-720p.bat   (HD)" -ForegroundColor White
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "[AVISO] Nao foi possivel criar AVD automaticamente" -ForegroundColor Yellow
    Write-Host "Siga as instrucoes em: INSTRUCOES_AVD.txt" -ForegroundColor Yellow
}

