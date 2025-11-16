# Script para Configurar Emulador Android TV Similar ao Fire Stick
# Cria um AVD com perfil de TV HD (720p e 1080p) para testar layouts

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CONFIGURAÇÃO DE EMULADOR ANDROID TV" -ForegroundColor Cyan
Write-Host "Perfil: Fire Stick HD (720p/1080p)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar se Android SDK está instalado
$sdkPath = $env:ANDROID_HOME
if (-not $sdkPath) {
    $sdkPath = "$env:LOCALAPPDATA\Android\Sdk"
    if (-not (Test-Path $sdkPath)) {
        Write-Host "ERRO: Android SDK não encontrado!" -ForegroundColor Red
        Write-Host "Por favor, instale o Android Studio primeiro." -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Caminhos verificados:" -ForegroundColor Yellow
        Write-Host "  - ANDROID_HOME: $env:ANDROID_HOME" -ForegroundColor Gray
        Write-Host "  - Padrão: $sdkPath" -ForegroundColor Gray
        exit 1
    }
}

Write-Host "[OK] Android SDK encontrado: $sdkPath" -ForegroundColor Green
Write-Host ""

# Caminhos dos executáveis
$avdManager = "$sdkPath\cmdline-tools\latest\bin\avdmanager.bat"
$emulator = "$sdkPath\emulator\emulator.exe"
$sdkManager = "$sdkPath\cmdline-tools\latest\bin\sdkmanager.bat"

# Verificar se avdmanager existe
if (-not (Test-Path $avdManager)) {
    Write-Host "AVD Manager não encontrado. Tentando caminho alternativo..." -ForegroundColor Yellow
    $avdManager = "$sdkPath\tools\bin\avdmanager.bat"
    if (-not (Test-Path $avdManager)) {
        Write-Host "ERRO: AVD Manager não encontrado!" -ForegroundColor Red
        Write-Host "Por favor, instale o Android SDK Command-line Tools." -ForegroundColor Yellow
        exit 1
    }
}

Write-Host "[OK] AVD Manager encontrado" -ForegroundColor Green
Write-Host ""

# Nome do AVD
$avdName = "FireStick_HD_Test"
$packageId = "system-images;android-33;google_apis;x86_64"

Write-Host "Configurando emulador:" -ForegroundColor Cyan
Write-Host "  Nome: $avdName" -ForegroundColor White
Write-Host "  Perfil: Android TV (Fire Stick HD)" -ForegroundColor White
Write-Host "  Resoluções: 720p e 1080p" -ForegroundColor White
Write-Host ""

# Verificar se AVD já existe
Write-Host "Verificando AVDs existentes..." -ForegroundColor Yellow
$existingAvds = & $avdManager list avd 2>&1
if ($existingAvds -match $avdName) {
    Write-Host "[AVISO] AVD '$avdName' já existe!" -ForegroundColor Yellow
    $response = Read-Host "Deseja deletar e recriar? (S/N)"
    if ($response -eq "S" -or $response -eq "s") {
        Write-Host "Deletando AVD existente..." -ForegroundColor Yellow
        & $avdManager delete avd -n $avdName 2>&1 | Out-Null
        Write-Host "[OK] AVD deletado" -ForegroundColor Green
    } else {
        Write-Host "Usando AVD existente." -ForegroundColor Green
        Write-Host ""
        Write-Host "Para iniciar o emulador, execute:" -ForegroundColor Cyan
        Write-Host "  $emulator -avd $avdName" -ForegroundColor White
        exit 0
    }
}

# Verificar se a imagem do sistema está instalada
Write-Host "Verificando imagem do sistema Android TV..." -ForegroundColor Yellow
$installedPackages = & $sdkManager --list_installed 2>&1
if ($installedPackages -notmatch "system-images;android-33;google_apis;x86_64") {
    Write-Host "[AVISO] Imagem do sistema Android TV não encontrada!" -ForegroundColor Yellow
    Write-Host "Instalando imagem do sistema..." -ForegroundColor Yellow
    Write-Host "Isso pode demorar alguns minutos..." -ForegroundColor Yellow
    Write-Host ""
    
    & $sdkManager $packageId --accept-licenses 2>&1
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERRO: Falha ao instalar imagem do sistema!" -ForegroundColor Red
        Write-Host "Tente instalar manualmente pelo Android Studio:" -ForegroundColor Yellow
        Write-Host "  Tools > SDK Manager > SDK Platforms > Android 13 (API 33)" -ForegroundColor Gray
        Write-Host "  Tools > SDK Manager > SDK Tools > Android TV Intel x86 Atom System Image" -ForegroundColor Gray
        exit 1
    }
    
    Write-Host "[OK] Imagem do sistema instalada" -ForegroundColor Green
} else {
    Write-Host "[OK] Imagem do sistema já instalada" -ForegroundColor Green
}

Write-Host ""

# Criar AVD com configurações específicas
Write-Host "Criando AVD..." -ForegroundColor Yellow

# Criar arquivo de configuração do AVD
$avdDir = "$env:USERPROFILE\.android\avd"
$avdConfigDir = "$avdDir\$avdName.avd"
$avdConfigFile = "$avdConfigDir\config.ini"

# Criar diretório se não existir
if (-not (Test-Path $avdConfigDir)) {
    New-Item -ItemType Directory -Path $avdConfigDir -Force | Out-Null
}

# Criar AVD usando avdmanager
Write-Host "Criando AVD com perfil Android TV..." -ForegroundColor Yellow

$createCommand = "echo no | $avdManager create avd -n `"$avdName`" -k `"$packageId`" -d `"tv_1080p`" --force"
Invoke-Expression $createCommand

# Verificar se foi criado
$avds = & $avdManager list avd 2>&1
if ($avds -notmatch $avdName) {
    Write-Host "ERRO: Falha ao criar AVD!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Tentando método alternativo..." -ForegroundColor Yellow
    
    # Método alternativo: criar manualmente
    Write-Host "Criando AVD manualmente..." -ForegroundColor Yellow
    
    # Criar arquivo .ini do AVD
    $avdIniFile = "$avdDir\$avdName.ini"
    @"
avd.ini.encoding=UTF-8
target=android-33
path=$avdConfigDir
"@ | Out-File -FilePath $avdIniFile -Encoding UTF8
    
    # Criar config.ini com configurações específicas do Fire Stick
    @"
avd.ini.encoding=UTF-8
PlayStore.enabled=false
abi.type=x86_64
avd.ini.displayname=Fire Stick HD Test
disk.dataPartition.size=2048M
disk.ramdisk.path=system-images\android-33\google_apis\x86_64\ramdisk.img
disk.systemPartition.size=512M
hw.accelerometer=yes
hw.audioInput=yes
hw.battery=yes
hw.camera.back=emulated
hw.camera.front=emulated
hw.cpu.arch=x86_64
hw.cpu.ncore=4
hw.dPad=yes
hw.device.manufacturer=Amazon
hw.device.name=Fire TV Stick
hw.gps=yes
hw.gpu.enabled=yes
hw.gpu.mode=auto
hw.initialOrientation=landscape
hw.keyboard=yes
hw.lcd.density=320
hw.lcd.height=1080
hw.lcd.width=1920
hw.mainKeys=no
hw.ramSize=2048
hw.screen=multi-touch
hw.sdCard=yes
hw.sensors.orientation=yes
hw.sensors.proximity=yes
hw.trackBall=no
image.sysdir.1=system-images\android-33\google_apis\x86_64\
runtime.network.latency=none
runtime.network.speed=full
skin.dynamic=yes
skin.name=1920x1080
skin.path=_no_skin
tag.display=Google APIs
tag.id=google_apis
vm.heapSize=256
"@ | Out-File -FilePath $avdConfigFile -Encoding UTF8
    
    Write-Host "[OK] AVD criado manualmente" -ForegroundColor Green
} else {
    Write-Host "[OK] AVD criado com sucesso" -ForegroundColor Green
}

Write-Host ""

# Configurar resoluções específicas
Write-Host "Configurando resoluções de teste..." -ForegroundColor Yellow

# Criar script para iniciar com diferentes resoluções
$startScript720 = @"
@echo off
echo Iniciando emulador Fire Stick HD 720p...
"$emulator" -avd $avdName -skin 1280x720 -dpi-device 213
"@

$startScript1080 = @"
@echo off
echo Iniciando emulador Fire Stick HD 1080p...
"$emulator" -avd $avdName -skin 1920x1080 -dpi-device 320
"@

$startScript720 | Out-File -FilePath "start-emulator-720p.bat" -Encoding ASCII
$startScript1080 | Out-File -FilePath "start-emulator-1080p.bat" -Encoding ASCII

Write-Host "[OK] Scripts de inicialização criados:" -ForegroundColor Green
Write-Host "  - start-emulator-720p.bat (1280x720)" -ForegroundColor White
Write-Host "  - start-emulator-1080p.bat (1920x1080)" -ForegroundColor White
Write-Host ""

# Resumo
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CONFIGURAÇÃO CONCLUÍDA!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "AVD criado: $avdName" -ForegroundColor Green
Write-Host ""
Write-Host "Para iniciar o emulador:" -ForegroundColor Cyan
Write-Host "  720p:  .\start-emulator-720p.bat" -ForegroundColor White
Write-Host "  1080p: .\start-emulator-1080p.bat" -ForegroundColor White
Write-Host ""
Write-Host "Ou manualmente:" -ForegroundColor Cyan
Write-Host "  $emulator -avd $avdName" -ForegroundColor White
Write-Host ""
Write-Host "Configurações do emulador:" -ForegroundColor Cyan
Write-Host "  - Resolução: 1920x1080 (padrão)" -ForegroundColor White
Write-Host "  - DPI: 320 (similar ao Fire Stick)" -ForegroundColor White
Write-Host "  - RAM: 2GB" -ForegroundColor White
Write-Host "  - CPU: x86_64 (4 cores)" -ForegroundColor White
Write-Host "  - Android: 13 (API 33)" -ForegroundColor White
Write-Host "  - Perfil: Android TV" -ForegroundColor White
Write-Host ""

