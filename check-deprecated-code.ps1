# Script Profissional: Verificacao de Codigo Deprecated
# Identifica codigo deprecated, APIs antigas e sugestoes de atualizacao

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "VERIFICACAO: Codigo Deprecated" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$deprecatedItems = @()
$warnings = @()
$filesChecked = 0

# Padroes de codigo deprecated comum no Android/Kotlin
$deprecatedPatterns = @{
    "systemUiVisibility" = "Usar WindowInsetsControllerCompat (API 30+)"
    "@Suppress.*DEPRECATED" = "Codigo deprecated suprimido - considerar atualizar"
    "Handler\(" = "Usar Handler(Looper.getMainLooper()) ou Coroutines"
    "Handler\\(\\)" = "Handler() sem Looper esta deprecated"
    "getSystemService\\(Context\\.DOWNLOAD_SERVICE\\)" = "Verificar se precisa atualizar para DownloadManager moderno"
    "registerReceiver.*null" = "Usar Context.RECEIVER_NOT_EXPORTED (API 33+)"
    "PendingIntent\\.FLAG_" = "Verificar se flags sao imutaveis (API 23+)"
    "FLAG_IMMUTABLE|FLAG_MUTABLE" = "Verificar se PendingIntent usa flags corretas"
    "Environment\\.getExternalStorageDirectory" = "Usar Context.getExternalFilesDir() ou MediaStore (API 29+)"
    "requestPermissions" = "Verificar se precisa ActivityResultLauncher (API 23+)"
    "startActivityForResult" = "Usar ActivityResultLauncher (API 30+)"
    "onActivityResult" = "Usar ActivityResultLauncher (API 30+)"
    "AsyncTask" = "Usar Coroutines ou ExecutorService"
    "WebView\\.setWebViewClient" = "Verificar versao do WebView"
    "NotificationCompat\\.Builder" = "Verificar se usa NotificationChannel (API 26+)"
    "MediaPlayer\\(context, resId\\)" = "Verificar se precisa atualizar para ExoPlayer"
    "SimpleDateFormat" = "Usar DateTimeFormatter (API 26+) ou biblioteca de data"
    "Date\\(\\d+\\)" = "Usar Instant ou LocalDateTime"
    "Calendar\\.getInstance" = "Usar LocalDate/LocalDateTime"
    "getColor\\(.*\\)" = "Verificar se usa ContextCompat.getColor() ou MaterialTheme"
    "getDrawable\\(.*\\)" = "Verificar se usa ContextCompat.getDrawable()"
    "getString\\(.*,.*\\)" = "Verificar se precisa atualizar para ResourcesCompat"
    "FLAG_ACTIVITY_NEW_TASK.*without.*FLAG" = "Verificar combinacao de flags"
    "startForegroundService" = "Verificar se precisa startForegroundService() ou startService() (API 26+)"
    "createChooser" = "Verificar se precisa atualizar para ShareCompat"
    "MediaStore\\.Images\\.Media" = "Verificar se precisa Scoped Storage (API 29+)"
    "WRITE_EXTERNAL_STORAGE" = "Verificar se precisa para Android 10+ (Scoped Storage)"
    "READ_EXTERNAL_STORAGE" = "Verificar se precisa para Android 10+ (Scoped Storage)"
}

# Verificar arquivos Kotlin
Write-Host "[1] Verificando arquivos Kotlin..." -ForegroundColor Yellow
$kotlinFiles = Get-ChildItem -Path "app\src\main\java" -Filter "*.kt" -Recurse

foreach ($file in $kotlinFiles) {
    $filesChecked++
    $content = Get-Content $file.FullName -Raw -ErrorAction SilentlyContinue
    
    if ($content) {
        $fileRelative = $file.FullName.Replace((Get-Location).Path + "\", "")
        
        foreach ($pattern in $deprecatedPatterns.Keys) {
            if ($content -match $pattern) {
                $matches = [regex]::Matches($content, $pattern)
                foreach ($match in $matches) {
                    $lineNumber = ($content.Substring(0, $match.Index) -split "`n").Count
                    $deprecatedItems += "$fileRelative (linha ~$lineNumber): $pattern - $($deprecatedPatterns[$pattern])"
                }
            }
        }
        
        # Verificar imports deprecated especificos
        if ($content -match "import.*android\.os\.AsyncTask") {
            $deprecatedItems += "$fileRelative : AsyncTask importado - usar Coroutines"
        }
        
        if ($content -match "import.*android\.webkit\.WebView") {
            $warnings += "$fileRelative : WebView - verificar se versao esta atualizada"
        }
        
        # Verificar anotacoes @Deprecated
        if ($content -match "@Deprecated|@kotlin\.Deprecated") {
            $deprecatedItems += "$fileRelative : Contem anotacao @Deprecated"
        }
        
        # Verificar uso de Handler sem Looper
        if ($content -match "Handler\s*\(" -and $content -notmatch "Handler\s*\(.*Looper") {
            $deprecatedItems += "$fileRelative : Handler() sem Looper - usar Handler(Looper.getMainLooper())"
        }
        
        # Verificar PendingIntent sem FLAG_IMMUTABLE ou FLAG_MUTABLE
        if ($content -match "PendingIntent\." -and $content -notmatch "FLAG_IMMUTABLE|FLAG_MUTABLE") {
            $deprecatedItems += "$fileRelative : PendingIntent sem FLAG_IMMUTABLE/FLAG_MUTABLE (necessario API 23+)"
        }
        
        # Verificar registerReceiver sem RECEIVER_NOT_EXPORTED
        if ($content -match "registerReceiver" -and $content -notmatch "RECEIVER_NOT_EXPORTED|RECEIVER_EXPORTED") {
            $api33Check = $content -match "Build\.VERSION\.SDK_INT.*33|TIRAMISU"
            if (-not $api33Check) {
                $warnings += "$fileRelative : registerReceiver pode precisar RECEIVER_NOT_EXPORTED (API 33+)"
            }
        }
        
        # Verificar Environment.getExternalStorageDirectory
        if ($content -match "Environment\.getExternalStorageDirectory") {
            $deprecatedItems += "$fileRelative : Environment.getExternalStorageDirectory() deprecated (API 29+) - usar Context.getExternalFilesDir()"
        }
        
        # Verificar SimpleDateFormat
        if ($content -match "SimpleDateFormat") {
            $deprecatedItems += "$fileRelative : SimpleDateFormat - considerar usar DateTimeFormatter ou biblioteca de data"
        }
        
        # Verificar Date() construtor
        if ($content -match "Date\s*\([^)]*\)" -and $content -notmatch "Date\s*\(\)") {
            $deprecatedItems += "$fileRelative : Date() com parametros - usar Instant ou LocalDateTime"
        }
    }
}

Write-Host "  OK $filesChecked arquivos Kotlin verificados" -ForegroundColor Green

# Verificar build.gradle
Write-Host ""
Write-Host "[2] Verificando build.gradle..." -ForegroundColor Yellow
$buildGradle = Get-Content "app\build.gradle.kts" -Raw -ErrorAction SilentlyContinue
if ($buildGradle) {
    Write-Host "  OK build.gradle.kts encontrado" -ForegroundColor Green
    
    # Verificar versoes antigas de bibliotecas
    if ($buildGradle -match "compileSdk\s+\d+") {
        $compileSdk = [regex]::Match($buildGradle, "compileSdk\s+(\d+)").Groups[1].Value
        if ([int]$compileSdk -lt 34) {
            $warnings += "build.gradle.kts : compileSdk $compileSdk - considerar atualizar para 34+"
        }
    }
    
    if ($buildGradle -match "targetSdk\s+\d+") {
        $targetSdk = [regex]::Match($buildGradle, "targetSdk\s+(\d+)").Groups[1].Value
        if ([int]$targetSdk -lt 34) {
            $warnings += "build.gradle.kts : targetSdk $targetSdk - considerar atualizar para 34+"
        }
    }
    
    if ($buildGradle -match "minSdk\s+\d+") {
        $minSdk = [regex]::Match($buildGradle, "minSdk\s+(\d+)").Groups[1].Value
        Write-Host "  OK minSdk: $minSdk" -ForegroundColor Green
    }
    
    # Verificar bibliotecas antigas
    if ($buildGradle -match "androidx\.compose.*:.*:.*") {
        $composeVersion = [regex]::Match($buildGradle, "androidx\.compose.*:.*:([\d.]+)").Groups[1].Value
        Write-Host "  OK Compose version encontrada" -ForegroundColor Green
    }
} else {
    $warnings += "build.gradle.kts : Arquivo nao encontrado"
}

# Verificar AndroidManifest.xml
Write-Host ""
Write-Host "[3] Verificando AndroidManifest.xml..." -ForegroundColor Yellow
$manifest = Get-Content "app\src\main\AndroidManifest.xml" -Raw -ErrorAction SilentlyContinue
if ($manifest) {
    Write-Host "  OK AndroidManifest.xml encontrado" -ForegroundColor Green
    
    # Verificar permissoes deprecated
    if ($manifest -match "READ_PHONE_STATE" -and $manifest -notmatch "READ_PHONE_NUMBERS") {
        $warnings += "AndroidManifest.xml : READ_PHONE_STATE pode precisar READ_PHONE_NUMBERS (API 26+)"
    }
    
    # Verificar uses-sdk deprecated
    if ($manifest -match "uses-sdk") {
        $deprecatedItems += "AndroidManifest.xml : uses-sdk deprecated - usar build.gradle"
    }
    
    # Verificar android:configChanges antigos
    if ($manifest -match "configChanges" -and $manifest -notmatch "screenLayout|screenSize") {
        $warnings += "AndroidManifest.xml : configChanges pode estar incompleto"
    }
} else {
    $warnings += "AndroidManifest.xml : Arquivo nao encontrado"
}

# Verificar ProGuard/R8
Write-Host ""
Write-Host "[4] Verificando ProGuard/R8..." -ForegroundColor Yellow
$proguardFiles = @("proguard-rules.pro", "proguard.pro")
foreach ($proguardFile in $proguardFiles) {
    if (Test-Path "app\$proguardFile") {
        Write-Host "  OK $proguardFile encontrado" -ForegroundColor Green
    }
}

# Verificar uso de APIs antigas especificas
Write-Host ""
Write-Host "[5] Verificando APIs antigas especificas..." -ForegroundColor Yellow

# Verificar se usa ViewBinding/DataBinding
$usesViewBinding = $false
$usesDataBinding = $false
if ($buildGradle) {
    if ($buildGradle -match "viewBinding\s*=\s*true|buildFeatures.*viewBinding") {
        $usesViewBinding = $true
        Write-Host "  OK ViewBinding habilitado" -ForegroundColor Green
    }
    if ($buildGradle -match "dataBinding\s*=\s*true|buildFeatures.*dataBinding") {
        $usesDataBinding = $true
        Write-Host "  OK DataBinding habilitado" -ForegroundColor Green
    }
}

# Verificar se usa Kotlinx Coroutines
$usesCoroutines = $false
if ($buildGradle -match "kotlinx-coroutines") {
    $usesCoroutines = $true
    Write-Host "  OK Coroutines encontrado" -ForegroundColor Green
} else {
    $warnings += "build.gradle.kts : Coroutines nao encontrado - considerar adicionar para substituir AsyncTask"
}

# Verificar se usa Material3
$usesMaterial3 = $false
if ($buildGradle -match "material3|compose-material3") {
    $usesMaterial3 = $true
    Write-Host "  OK Material3 encontrado" -ForegroundColor Green
} else {
    $warnings += "build.gradle.kts : Material3 nao encontrado - considerar atualizar de Material2"
}

# Resumo
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO DA VERIFICACAO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Arquivos verificados: $filesChecked" -ForegroundColor White
Write-Host ""

if ($deprecatedItems.Count -gt 0) {
    Write-Host "CODIGO DEPRECATED ENCONTRADO ($($deprecatedItems.Count)):" -ForegroundColor Red
    Write-Host ""
    foreach ($item in $deprecatedItems) {
        Write-Host "  X $item" -ForegroundColor Red
    }
    Write-Host ""
} else {
    Write-Host "OK NENHUM CODIGO DEPRECATED CRITICO ENCONTRADO!" -ForegroundColor Green
    Write-Host ""
}

if ($warnings.Count -gt 0) {
    Write-Host "AVISOS E SUGESTOES ($($warnings.Count)):" -ForegroundColor Yellow
    Write-Host ""
    foreach ($warning in $warnings) {
        Write-Host "  ! $warning" -ForegroundColor Yellow
    }
    Write-Host ""
}

# Recomendacoes gerais
Write-Host "RECOMENDACOES GERAIS:" -ForegroundColor Magenta
Write-Host ""
Write-Host "1. ATUALIZAR BIBLIOTECAS:" -ForegroundColor White
Write-Host "   - androidx.compose.* para versoes mais recentes" -ForegroundColor Gray
Write-Host "   - androidx.core:core-ktx para versao mais recente" -ForegroundColor Gray
Write-Host "   - androidx.lifecycle para versoes mais recentes" -ForegroundColor Gray
Write-Host ""
Write-Host "2. SUBSTITUIR APIs DEPRECATED:" -ForegroundColor White
Write-Host "   - AsyncTask -> Coroutines" -ForegroundColor Gray
Write-Host "   - Handler() -> Handler(Looper.getMainLooper()) ou Coroutines" -ForegroundColor Gray
Write-Host "   - SimpleDateFormat -> DateTimeFormatter" -ForegroundColor Gray
Write-Host "   - Date() -> Instant/LocalDateTime" -ForegroundColor Gray
Write-Host "   - Environment.getExternalStorageDirectory() -> Context.getExternalFilesDir()" -ForegroundColor Gray
Write-Host ""
Write-Host "3. ATUALIZAR PERMISSOES:" -ForegroundColor White
Write-Host "   - Verificar permissoes de storage (Scoped Storage API 29+)" -ForegroundColor Gray
Write-Host "   - Verificar permissoes de runtime (API 23+)" -ForegroundColor Gray
Write-Host ""
Write-Host "4. VERIFICAR COMPATIBILIDADE:" -ForegroundColor White
Write-Host "   - compileSdk 34+ (Android 14)" -ForegroundColor Gray
Write-Host "   - targetSdk 34+ (Android 14)" -ForegroundColor Gray
Write-Host "   - minSdk baseado no suporte necessario" -ForegroundColor Gray
Write-Host ""

if ($deprecatedItems.Count -eq 0 -and $warnings.Count -eq 0) {
    Write-Host "PARABENS! O app esta atualizado!" -ForegroundColor Green
    exit 0
} elseif ($deprecatedItems.Count -eq 0) {
    Write-Host "Codigo deprecated corrigido! Apenas avisos menores." -ForegroundColor Green
    exit 0
} else {
    Write-Host "ATENCAO: Corrija o codigo deprecated antes de compilar!" -ForegroundColor Red
    exit 1
}

