# Script Profissional: Verificacao de Dependencias ExoPlayer
# Verifica se o ExoPlayer esta completo e atualizado para todos os dispositivos suportados

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "VERIFICACAO: Dependencias ExoPlayer" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$issues = @()
$warnings = @()
$recommendations = @()

# 1. Verificar build.gradle.kts
Write-Host "[1] Analisando build.gradle.kts..." -ForegroundColor Yellow
$buildGradle = Get-Content "app\build.gradle.kts" -Raw -ErrorAction SilentlyContinue

if (-not $buildGradle) {
    $issues += "build.gradle.kts nao encontrado!"
    Write-Host "  X Arquivo nao encontrado" -ForegroundColor Red
    exit 1
}

Write-Host "  OK Arquivo encontrado" -ForegroundColor Green

# Extrair versao do ExoPlayer
$exoPlayerVersion = ""
if ($buildGradle -match 'media3-exoplayer:([\d.]+)') {
    $exoPlayerVersion = $matches[1]
    Write-Host "  OK Versao ExoPlayer encontrada: $exoPlayerVersion" -ForegroundColor Green
} else {
    $issues += "Versao do ExoPlayer nao encontrada no build.gradle.kts"
    Write-Host "  X Versao do ExoPlayer nao encontrada" -ForegroundColor Red
}

# Verificar dependencias essenciais do ExoPlayer
Write-Host ""
Write-Host "[2] Verificando dependencias essenciais do ExoPlayer..." -ForegroundColor Yellow

$requiredDependencies = @{
    "media3-exoplayer" = "Core do ExoPlayer (obrigatorio)"
    "media3-exoplayer-hls" = "Suporte HLS (obrigatorio para streaming)"
    "media3-ui" = "UI components (obrigatorio)"
    "media3-common" = "Componentes comuns (obrigatorio)"
}

$optionalDependencies = @{
    "media3-exoplayer-dash" = "Suporte DASH (recomendado)"
    "media3-exoplayer-smoothstreaming" = "Suporte SmoothStreaming (opcional)"
    "media3-exoplayer-rtsp" = "Suporte RTSP (opcional)"
    "media3-datasource-okhttp" = "DataSource OkHttp (recomendado para melhor performance)"
    "media3-datasource-cronet" = "DataSource Cronet (opcional, melhor para Chrome)"
    "media3-decoder-ffmpeg" = "Decoder FFmpeg (opcional, mais codecs)"
    "media3-decoder-gav1" = "Decoder AV1 (opcional, codec moderno)"
}

$foundDependencies = @{}
$missingRequired = @()
$missingOptional = @()

# Verificar dependencias obrigatorias
foreach ($dep in $requiredDependencies.Keys) {
    if ($buildGradle -match $dep) {
        $foundDependencies[$dep] = $true
        Write-Host "  OK $dep encontrado" -ForegroundColor Green
    } else {
        $foundDependencies[$dep] = $false
        $missingRequired += $dep
        $issues += "Dependencia obrigatoria faltando: $dep - $($requiredDependencies[$dep])"
        Write-Host "  X $dep NAO encontrado" -ForegroundColor Red
    }
}

# Verificar dependencias opcionais recomendadas
Write-Host ""
Write-Host "[3] Verificando dependencias opcionais recomendadas..." -ForegroundColor Yellow

foreach ($dep in $optionalDependencies.Keys) {
    if ($buildGradle -match $dep) {
        $foundDependencies[$dep] = $true
        Write-Host "  OK $dep encontrado" -ForegroundColor Green
    } else {
        $foundDependencies[$dep] = $false
        $missingOptional += $dep
        if ($dep -eq "media3-exoplayer-dash" -or $dep -eq "media3-datasource-okhttp") {
            $warnings += "Dependencia recomendada faltando: $dep - $($optionalDependencies[$dep])"
            Write-Host "  ! $dep NAO encontrado (recomendado)" -ForegroundColor Yellow
        } else {
            Write-Host "  - $dep nao encontrado (opcional)" -ForegroundColor Gray
        }
    }
}

# Verificar versao do OkHttp (se usado)
Write-Host ""
Write-Host "[4] Verificando versao do OkHttp..." -ForegroundColor Yellow
if ($buildGradle -match 'okhttp3:okhttp:([\d.]+)') {
    $okHttpVersion = $matches[1]
    Write-Host "  OK OkHttp versao: $okHttpVersion" -ForegroundColor Green
    
    # Verificar se versao e recente (4.10+)
    $versionParts = $okHttpVersion -split '\.'
    $majorVersion = [int]$versionParts[0]
    $minorVersion = [int]$versionParts[1]
    
    if ($majorVersion -lt 4 -or ($majorVersion -eq 4 -and $minorVersion -lt 10)) {
        $warnings += "OkHttp versao $okHttpVersion pode estar desatualizada (recomendado 4.11+)"
        Write-Host "  ! Versao pode estar desatualizada" -ForegroundColor Yellow
    } else {
        Write-Host "  OK Versao esta atualizada" -ForegroundColor Green
    }
} else {
    Write-Host "  - OkHttp nao encontrado (usando DefaultHttpDataSource)" -ForegroundColor Gray
}

# Verificar versao do ExoPlayer
Write-Host ""
Write-Host "[5] Verificando versao do ExoPlayer..." -ForegroundColor Yellow
if ($exoPlayerVersion) {
    Write-Host "  OK Versao atual: $exoPlayerVersion" -ForegroundColor Green
    
    # Verificar se versao e recente (1.4.0+)
    $versionParts = $exoPlayerVersion -split '\.'
    $majorVersion = [int]$versionParts[0]
    $minorVersion = [int]$versionParts[1]
    
    if ($majorVersion -lt 1 -or ($majorVersion -eq 1 -and $minorVersion -lt 4)) {
        $warnings += "ExoPlayer versao $exoPlayerVersion pode estar desatualizada (recomendado 1.4.1+)"
        Write-Host "  ! Versao pode estar desatualizada" -ForegroundColor Yellow
    } else {
        Write-Host "  OK Versao esta atualizada" -ForegroundColor Green
    }
}

# Verificar compatibilidade com dispositivos
Write-Host ""
Write-Host "[6] Verificando compatibilidade com dispositivos..." -ForegroundColor Yellow

# Verificar minSdk
if ($buildGradle -match 'minSdk\s*=\s*(\d+)') {
    $minSdk = [int]$matches[1]
    Write-Host "  OK minSdk: $minSdk" -ForegroundColor Green
    
    if ($minSdk -lt 21) {
        $warnings += "minSdk $minSdk pode ser muito baixo para ExoPlayer Media3 (recomendado 21+)"
        Write-Host "  ! minSdk pode ser muito baixo" -ForegroundColor Yellow
    }
} else {
    $warnings += "minSdk nao encontrado"
    Write-Host "  ! minSdk nao encontrado" -ForegroundColor Yellow
}

# Verificar arquiteturas suportadas
if ($buildGradle -match 'abiFilters.*armeabi-v7a.*arm64-v8a') {
    Write-Host "  OK Arquiteturas suportadas: armeabi-v7a, arm64-v8a" -ForegroundColor Green
} else {
    $warnings += "Arquiteturas podem nao estar configuradas corretamente"
    Write-Host "  ! Verificar abiFilters" -ForegroundColor Yellow
}

# Verificar se todas as dependencias usam a mesma versao
Write-Host ""
Write-Host "[7] Verificando consistencia de versoes..." -ForegroundColor Yellow

$allMedia3Versions = [regex]::Matches($buildGradle, 'media3-[^:]+:([\d.]+)')
$uniqueVersions = $allMedia3Versions | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique

if ($uniqueVersions.Count -eq 1) {
    Write-Host "  OK Todas as dependencias Media3 usam a mesma versao: $($uniqueVersions[0])" -ForegroundColor Green
} else {
    $issues += "Dependencias Media3 com versoes diferentes: $($uniqueVersions -join ', ')"
    Write-Host "  X Versoes inconsistentes encontradas:" -ForegroundColor Red
    foreach ($version in $uniqueVersions) {
        Write-Host "     - $version" -ForegroundColor Red
    }
}

# Verificar se ha dependencias obsoletas
Write-Host ""
Write-Host "[8] Verificando dependencias obsoletas..." -ForegroundColor Yellow

$obsoletePatterns = @{
    "com\.google\.android\.exoplayer:exoplayer" = "ExoPlayer antigo (usar androidx.media3)"
    "exoplayer-core" = "ExoPlayer antigo (usar media3-exoplayer)"
    "implementation.*exoplayer-hls[^:]" = "ExoPlayer antigo (usar media3-exoplayer-hls)"
    "implementation.*exoplayer-ui[^:]" = "ExoPlayer antigo (usar media3-ui)"
}

$foundObsolete = $false
foreach ($pattern in $obsoletePatterns.Keys) {
    # Verificar se e uma dependencia real, nao apenas um comentario
    if ($buildGradle -match "implementation.*$pattern" -and $buildGradle -notmatch "media3") {
        $foundObsolete = $true
        $issues += "Dependencia obsoleta encontrada: $pattern - $($obsoletePatterns[$pattern])"
        Write-Host "  X Dependencia obsoleta: $pattern" -ForegroundColor Red
    }
}

if (-not $foundObsolete) {
    Write-Host "  OK Nenhuma dependencia obsoleta encontrada" -ForegroundColor Green
}

# Verificar suporte para codecs
Write-Host ""
Write-Host "[9] Verificando suporte para codecs..." -ForegroundColor Yellow

$codecSupport = @{
    "H.264/AVC" = "Suportado nativamente"
    "H.265/HEVC" = "Suportado nativamente (Android 5.0+)"
    "VP8" = "Suportado nativamente"
    "VP9" = "Suportado nativamente (Android 7.0+)"
    "AV1" = "Requer media3-decoder-gav1 ou Android 12+"
    "AAC" = "Suportado nativamente"
    "MP3" = "Suportado nativamente"
    "Opus" = "Suportado nativamente (Android 5.0+)"
}

Write-Host "  Codecs suportados nativamente:" -ForegroundColor Gray
foreach ($codec in $codecSupport.Keys) {
    Write-Host "    - $codec" -ForegroundColor Gray
}

if ($foundDependencies["media3-decoder-ffmpeg"] -or $foundDependencies["media3-decoder-gav1"]) {
    Write-Host "  OK Decoders extras encontrados (mais codecs suportados)" -ForegroundColor Green
} else {
    Write-Host "  - Decoders extras nao encontrados (opcional)" -ForegroundColor Gray
}

# Verificar suporte para protocolos de streaming
Write-Host ""
Write-Host "[10] Verificando suporte para protocolos..." -ForegroundColor Yellow

$protocols = @{
    "HLS" = if ($foundDependencies["media3-exoplayer-hls"]) { "OK" } else { "FALTANDO" }
    "DASH" = if ($foundDependencies["media3-exoplayer-dash"]) { "OK" } else { "OPCIONAL" }
    "SmoothStreaming" = if ($foundDependencies["media3-exoplayer-smoothstreaming"]) { "OK" } else { "OPCIONAL" }
    "RTSP" = if ($foundDependencies["media3-exoplayer-rtsp"]) { "OK" } else { "OPCIONAL" }
    "HTTP Progressive" = "OK (suportado nativamente)"
}

foreach ($protocol in $protocols.Keys) {
    $status = $protocols[$protocol]
    if ($status -eq "OK") {
        Write-Host "  OK $protocol suportado" -ForegroundColor Green
    } elseif ($status -eq "FALTANDO") {
        Write-Host "  X $protocol NAO suportado (obrigatorio!)" -ForegroundColor Red
        $issues += "Protocolo obrigatorio faltando: $protocol"
    } else {
        Write-Host "  - $protocol nao suportado (opcional)" -ForegroundColor Gray
    }
}

# Resumo e recomendacoes
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO DA VERIFICACAO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($issues.Count -gt 0) {
    Write-Host "PROBLEMAS CRITICOS ENCONTRADOS ($($issues.Count)):" -ForegroundColor Red
    Write-Host ""
    foreach ($issue in $issues) {
        Write-Host "  X $issue" -ForegroundColor Red
    }
    Write-Host ""
}

if ($warnings.Count -gt 0) {
    Write-Host "AVISOS ($($warnings.Count)):" -ForegroundColor Yellow
    Write-Host ""
    foreach ($warning in $warnings) {
        Write-Host "  ! $warning" -ForegroundColor Yellow
    }
    Write-Host ""
}

# Recomendacoes especificas
Write-Host "RECOMENDACOES PARA TODOS OS DISPOSITIVOS:" -ForegroundColor Magenta
Write-Host ""
Write-Host "1. DEPENDENCIAS OBRIGATORIAS (devem estar presentes):" -ForegroundColor White
Write-Host "   - androidx.media3:media3-exoplayer" -ForegroundColor Gray
Write-Host "   - androidx.media3:media3-exoplayer-hls (HLS e essencial para IPTV)" -ForegroundColor Gray
Write-Host "   - androidx.media3:media3-ui" -ForegroundColor Gray
Write-Host "   - androidx.media3:media3-common" -ForegroundColor Gray
Write-Host ""
Write-Host "2. DEPENDENCIAS RECOMENDADAS:" -ForegroundColor White
Write-Host "   - androidx.media3:media3-exoplayer-dash (para DASH streams)" -ForegroundColor Gray
Write-Host "   - androidx.media3:media3-datasource-okhttp (melhor performance)" -ForegroundColor Gray
Write-Host ""
Write-Host "3. DEPENDENCIAS OPCIONAIS:" -ForegroundColor White
Write-Host "   - androidx.media3:media3-exoplayer-smoothstreaming (SmoothStreaming)" -ForegroundColor Gray
Write-Host "   - androidx.media3:media3-exoplayer-rtsp (RTSP streams)" -ForegroundColor Gray
Write-Host "   - androidx.media3:media3-decoder-ffmpeg (mais codecs)" -ForegroundColor Gray
Write-Host "   - androidx.media3:media3-decoder-gav1 (codec AV1)" -ForegroundColor Gray
Write-Host ""
Write-Host "4. VERSAO RECOMENDADA:" -ForegroundColor White
Write-Host "   - ExoPlayer Media3: 1.4.1+ (mais recente estavel)" -ForegroundColor Gray
Write-Host "   - OkHttp: 4.11.0+ (se usar datasource-okhttp)" -ForegroundColor Gray
Write-Host ""
Write-Host "5. COMPATIBILIDADE:" -ForegroundColor White
Write-Host "   - minSdk 21+ (Android 5.0 Lollipop)" -ForegroundColor Gray
Write-Host "   - Arquiteturas: armeabi-v7a, arm64-v8a" -ForegroundColor Gray
Write-Host "   - Suporta: Fire Stick, TV Box, Smartphones, Tablets" -ForegroundColor Gray
Write-Host ""

if ($issues.Count -eq 0 -and $warnings.Count -eq 0) {
    Write-Host "PARABENS! ExoPlayer esta completo e atualizado!" -ForegroundColor Green
    Write-Host ""
    Write-Host "O app esta pronto para funcionar em todos os dispositivos suportados." -ForegroundColor Green
    exit 0
} elseif ($issues.Count -eq 0) {
    Write-Host "ExoPlayer esta funcional, mas ha avisos menores." -ForegroundColor Yellow
    Write-Host "Recomendado corrigir avisos para melhor compatibilidade." -ForegroundColor Yellow
    exit 0
} else {
    Write-Host "ATENCAO: Corrija os problemas criticos antes de compilar!" -ForegroundColor Red
    exit 1
}

