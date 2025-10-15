# MaxiPTV Release Build Script
# Uso: .\build-release.ps1 [debug|release]

param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("debug", "release")]
    [string]$BuildType
)

# Configurações
$GitHubToken = $env:GITHUB_TOKEN # Token será fornecido via variável de ambiente
$GitHubRepo = "maxiptv-v2/maxiptv-update"
$GitHubUrl = "https://github.com/$GitHubRepo.git"

if (-not $GitHubToken) {
    Write-Host "❌ ERRO: Token do GitHub não configurado!" -ForegroundColor Red
    Write-Host "Configure a variável de ambiente GITHUB_TOKEN antes de executar." -ForegroundColor Yellow
    Write-Host 'Exemplo: $env:GITHUB_TOKEN = "seu_token_aqui"' -ForegroundColor Cyan
    exit 1
}

Write-Host "🚀 MaxiPTV Build Script - Tipo: $BuildType" -ForegroundColor Cyan

if ($BuildType -eq "debug") {
    Write-Host "🧪 Modo DEBUG - Compilando para testes..." -ForegroundColor Yellow
    Write-Host "❌ NÃO será enviado para GitHub" -ForegroundColor Red
    
    # Compilar apenas em debug
    .\gradlew.bat assembleDebug
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ APK Debug compilado com sucesso!" -ForegroundColor Green
        Write-Host "📱 Local: app/build/outputs/apk/debug/app-debug.apk" -ForegroundColor Blue
    } else {
        Write-Host "❌ Erro na compilação debug!" -ForegroundColor Red
        exit 1
    }
    
} elseif ($BuildType -eq "release") {
    Write-Host "🚀 Modo RELEASE - Compilando versão oficial..." -ForegroundColor Green
    
    # 1. Ler versão atual
    $versionJson = Get-Content "version.json" | ConvertFrom-Json
    $currentVersion = $versionJson.version
    $currentVersionCode = $versionJson.versionCode
    
    # 2. Incrementar versão
    $newVersionCode = $currentVersionCode + 1
    $versionParts = $currentVersion -split '\.'
    $major = [int]$versionParts[0].Substring(1) # Remove 'v'
    $minor = [int]$versionParts[1]
    $patch = [int]$versionParts[2]
    
    # Incrementar patch (v1.0.0 -> v1.0.1)
    $patch++
    $newVersion = "v$major.$minor.$patch"
    
    Write-Host "📊 Versão atual: $currentVersion" -ForegroundColor Blue
    Write-Host "📈 Nova versão: $newVersion" -ForegroundColor Green
    
    # 3. Atualizar version.json
    $versionJson.version = $newVersion
    $versionJson.versionCode = $newVersionCode
    $versionJson.buildNumber = $newVersionCode
    $versionJson.lastUpdated = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ssZ")
    
    $versionJson | ConvertTo-Json -Depth 10 | Set-Content "version.json"
    Write-Host "✅ version.json atualizado" -ForegroundColor Green
    
    # 4. Atualizar update.json
    $updateJson = Get-Content "update.json" | ConvertFrom-Json
    $updateJson.version = $newVersion
    $updateJson.versionCode = $newVersionCode
    $updateJson.buildNumber = $newVersionCode
    $updateJson.downloadUrl = "https://github.com/$GitHubRepo/releases/download/$newVersion/maxiptv-release.apk"
    $updateJson.lastUpdated = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ssZ")
    
    $updateJson | ConvertTo-Json -Depth 10 | Set-Content "update.json"
    Write-Host "✅ update.json atualizado" -ForegroundColor Green
    
    # 5. Compilar release
    Write-Host "🔨 Compilando APK Release..." -ForegroundColor Yellow
    .\gradlew.bat assembleRelease
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Erro na compilação release!" -ForegroundColor Red
        exit 1
    }
    
    # 6. Renomear APK
    $releaseApk = "app/build/outputs/apk/release/app-release-unsigned.apk"
    $finalApk = "maxiptv-release.apk"
    
    if (Test-Path $releaseApk) {
        Copy-Item $releaseApk $finalApk -Force
        Write-Host "✅ APK renomeado para: $finalApk" -ForegroundColor Green
    } else {
        Write-Host "❌ APK release não encontrado!" -ForegroundColor Red
        exit 1
    }
    
    # 7. Configurar Git
    Write-Host "🔧 Configurando Git..." -ForegroundColor Yellow
    git config user.email "maiptv1987@gmail.com"
    git config user.name "MaxiPTV Update System"
    
    # 8. Commit e Push
    Write-Host "📤 Enviando para GitHub..." -ForegroundColor Yellow
    git add .
    git commit -m "🚀 Release $newVersion - Build $newVersionCode"
    
    # Push com token
    $remoteUrl = "https://$GitHubToken@github.com/$GitHubRepo.git"
    git remote set-url origin $remoteUrl
    git push origin main
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "🎉 RELEASE $newVersion ENVIADO COM SUCESSO!" -ForegroundColor Green
        Write-Host "📱 APK: maxiptv-release.apk" -ForegroundColor Blue
        Write-Host "🌐 GitHub: https://github.com/$GitHubRepo" -ForegroundColor Blue
        Write-Host "📊 Versão: $newVersion (Build $newVersionCode)" -ForegroundColor Blue
    } else {
        Write-Host "❌ Erro ao enviar para GitHub!" -ForegroundColor Red
        exit 1
    }
}

Write-Host "✨ Build concluído!" -ForegroundColor Cyan
