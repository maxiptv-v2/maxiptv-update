# Script para analisar problema de atualização Fire OS usando IA
# Cria prompt detalhado e busca soluções

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ANALISE COM IA: Problema Fire OS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Ler código relevante
$apkDownloader = Get-Content "app/src/main/java/com/maxiptv/data/ApkDownloader.kt" -Raw -ErrorAction SilentlyContinue
$updateManager = Get-Content "app/src/main/java/com/maxiptv/data/UpdateManager.kt" -Raw -ErrorAction SilentlyContinue
$manifest = Get-Content "app/src/main/AndroidManifest.xml" -Raw -ErrorAction SilentlyContinue
$buildGradle = Get-Content "app/build.gradle.kts" -Raw -ErrorAction SilentlyContinue

# Extrair trechos relevantes do ApkDownloader
$downloadFunction = ""
$installFunction = ""
$fireOSDetection = ""

if ($apkDownloader) {
    $lines = $apkDownloader -split "`n"
    $inDownload = $false
    $inInstall = $false
    $inFireOS = $false
    $downloadLines = @()
    $installLines = @()
    $fireOSLines = @()
    
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]
        
        if ($line -match "fun downloadAndInstall") {
            $inDownload = $true
            $downloadLines = @()
        }
        if ($line -match "private fun installApkFile") {
            $inInstall = $true
            $installLines = @()
        }
        if ($line -match "private fun isFireOS") {
            $inFireOS = $true
            $fireOSLines = @()
        }
        
        if ($inDownload) {
            $downloadLines += $line
            if ($line -match "^    }" -and $downloadLines.Count -gt 5) {
                $inDownload = $false
            }
        }
        if ($inInstall) {
            $installLines += $line
            if ($line -match "^    }" -and $installLines.Count -gt 5) {
                $inInstall = $false
            }
        }
        if ($inFireOS) {
            $fireOSLines += $line
            if ($line -match "^    }") {
                $inFireOS = $false
            }
        }
    }
    
    $downloadFunction = $downloadLines -join "`n"
    $installFunction = $installLines -join "`n"
    $fireOSDetection = $fireOSLines -join "`n"
}

# Criar prompt detalhado
$prompt = @"
PROBLEMA CRÍTICO: Aplicativo Android não atualiza no Fire OS (Amazon Fire Stick)

CONTEXTO TÉCNICO:
- App desenvolvido em Kotlin com Jetpack Compose
- Target SDK: 34, Min SDK: 21
- Sistema de atualização: DownloadManager + FileProvider
- Assinatura: v1 e v2 habilitadas (enableV1Signing = true, enableV2Signing = true)
- Fire OS requer assinatura v1 e v2 (já configurado)

SINTOMA:
- APK é baixado com sucesso
- Quando usuário clica em "Atualizar", o app fecha
- APK não é instalado
- App continua na versão antiga

ARQUITETURA IMPLEMENTADA:

1. UpdateManager.kt:
   - Verifica atualização via JSON remoto
   - Adiciona timestamp na URL para evitar cache
   - Headers: Cache-Control: no-cache, Pragma: no-cache

2. ApkDownloader.kt:
   - Usa DownloadManager para baixar APK
   - BroadcastReceiver com ApplicationContext (persiste quando app fecha)
   - SharedPreferences para salvar downloadId e fileName
   - Limpa arquivos antigos antes de baixar
   - Adiciona timestamp no nome do arquivo
   - Aguarda 2000ms no Fire OS antes de instalar
   - Usa FileProvider para criar URI seguro
   - Verifica permissão REQUEST_INSTALL_PACKAGES antes de instalar
   - Intent flags específicos para Fire OS: FLAG_ACTIVITY_CLEAR_TOP, FLAG_ACTIVITY_SINGLE_TOP, FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS

3. Detecção Fire OS:
$fireOSDetection

4. Função de Download:
$($downloadFunction.Substring(0, [Math]::Min(2000, $downloadFunction.Length)))

5. Função de Instalação:
$($installFunction.Substring(0, [Math]::Min(2000, $installFunction.Length)))

6. AndroidManifest.xml:
   - Permissão REQUEST_INSTALL_PACKAGES declarada
   - FileProvider configurado com authorities
   - Provider com file_paths.xml

7. build.gradle.kts:
   - enableV1Signing = true
   - enableV2Signing = true

PROBLEMAS IDENTIFICADOS NO CÓDIGO:

1. O app fecha quando clica em "Atualizar" - isso sugere que:
   - O Intent de instalação pode estar causando crash
   - O Fire OS pode estar bloqueando a Activity
   - O contexto pode estar sendo destruído antes da instalação

2. Possíveis causas:
   - Fire OS pode requerer PackageInstaller API em vez de Intent.ACTION_VIEW
   - O FileProvider pode não estar funcionando corretamente no Fire OS
   - O BroadcastReceiver pode não estar persistindo corretamente
   - O arquivo pode não estar completamente escrito quando tenta instalar

PERGUNTAS ESPECÍFICAS:

1. O Fire OS 8 requer PackageInstaller API em vez de Intent.ACTION_VIEW para instalação?
2. Como garantir que o BroadcastReceiver persista mesmo quando o app fecha no Fire OS?
3. O FileProvider funciona corretamente no Fire OS ou precisa de configuração especial?
4. Há algum flag adicional necessário no Intent para Fire OS?
5. O Fire OS bloqueia Intent.ACTION_VIEW para instalação de APKs?
6. Devo usar PackageInstaller.Session em vez de Intent.ACTION_VIEW?
7. Há algum tempo mínimo necessário entre download completo e tentativa de instalação no Fire OS?
8. O Fire OS requer que o app tenha permissão especial além de REQUEST_INSTALL_PACKAGES?

SOLUÇÕES SUGERIDAS:

Por favor, forneça:
1. Código Kotlin completo para usar PackageInstaller API se necessário
2. Verificações adicionais necessárias para Fire OS
3. Melhor forma de garantir que BroadcastReceiver persista
4. Configurações adicionais no AndroidManifest.xml se necessário
5. Tratamento de erros específico para Fire OS

CÓDIGO ATUAL DE INSTALAÇÃO (trecho crítico):

```kotlin
val installIntent = Intent(Intent.ACTION_VIEW).apply {
    setDataAndType(uri, "application/vnd.android.package-archive")
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }
}

if (isFire) {
    installIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    installIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    installIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    Thread.sleep(2000)
}

appContext.startActivity(installIntent)
```

O que está faltando ou errado neste código para Fire OS?
"@

# Salvar prompt
$prompt | Out-File -FilePath "prompt-ia-fire-os-detalhado.txt" -Encoding UTF8

Write-Host "✅ Prompt detalhado criado em: prompt-ia-fire-os-detalhado.txt" -ForegroundColor Green
Write-Host ""

# Mostrar resumo
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO DA ANALISE" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "PROBLEMA:" -ForegroundColor Yellow
Write-Host "  App fecha quando clica em 'Atualizar' no Fire OS" -ForegroundColor White
Write-Host ""

Write-Host "POSSIVEIS CAUSAS:" -ForegroundColor Yellow
Write-Host "  1. Fire OS pode requerer PackageInstaller API em vez de Intent.ACTION_VIEW" -ForegroundColor White
Write-Host "  2. BroadcastReceiver pode não estar persistindo corretamente" -ForegroundColor White
Write-Host "  3. FileProvider pode não estar funcionando no Fire OS" -ForegroundColor White
Write-Host "  4. Context pode estar sendo destruído antes da instalação" -ForegroundColor White
Write-Host ""

Write-Host "SOLUCOES SUGERIDAS:" -ForegroundColor Yellow
Write-Host "  1. Implementar PackageInstaller API para Fire OS" -ForegroundColor White
Write-Host "  2. Usar Service em vez de BroadcastReceiver para persistência" -ForegroundColor White
Write-Host "  3. Verificar se FileProvider está configurado corretamente" -ForegroundColor White
Write-Host "  4. Adicionar mais verificações antes de iniciar instalação" -ForegroundColor White
Write-Host ""

Write-Host "PROXIMOS PASSOS:" -ForegroundColor Yellow
Write-Host "  1. Consulte o prompt em: prompt-ia-fire-os-detalhado.txt" -ForegroundColor Cyan
Write-Host "  2. Cole o prompt em uma IA (ChatGPT, Claude, etc.)" -ForegroundColor Cyan
Write-Host "  3. Implemente as soluções sugeridas" -ForegroundColor Cyan
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CODIGO PARA IMPLEMENTAR PACKAGEINSTALLER" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$packageInstallerCode = @"
// Exemplo de como usar PackageInstaller API para Fire OS
// (Precisa ser adaptado ao código existente)

import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.Session
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.OutputStream

private fun installApkWithPackageInstaller(context: Context, file: File) {
    val packageInstaller = context.packageManager.packageInstaller
    val sessionParams = PackageInstaller.SessionParams(
        PackageInstaller.SessionParams.MODE_FULL_INSTALL
    )
    
    val sessionId = packageInstaller.createSession(sessionParams)
    val session = packageInstaller.openSession(sessionId)
    
    val inputStream = FileInputStream(file)
    val outputStream = session.openWrite("package", 0, -1)
    
    inputStream.copyTo(outputStream)
    session.fsync(outputStream)
    inputStream.close()
    outputStream.close()
    
    val intent = Intent(context, InstallReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    
    session.commit(pendingIntent.intentSender)
    session.close()
}
"@

Write-Host $packageInstallerCode -ForegroundColor Gray
Write-Host ""

Write-Host "✅ Script concluído!" -ForegroundColor Green
Write-Host ""

