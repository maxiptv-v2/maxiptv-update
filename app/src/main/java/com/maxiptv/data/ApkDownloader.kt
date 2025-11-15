package com.maxiptv.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.maxiptv.MaxiApp
import java.io.File

object ApkDownloader {
    private const val TAG = "ApkDownloader"
    private const val PREFS_NAME = "apk_downloader_prefs"
    private const val KEY_DOWNLOAD_ID = "download_id"
    private const val KEY_FILE_NAME = "file_name"
    
    // Receiver estático para funcionar mesmo quando o app fecha
    private var downloadReceiver: BroadcastReceiver? = null
    
    /**
     * Verifica se está rodando no Fire OS / Fire Stick
     */
    private fun isFireOS(context: Context): Boolean {
        return MaxiApp.isFireStick || 
               android.os.Build.MANUFACTURER.lowercase().contains("amazon") ||
               android.os.Build.BRAND.lowercase().contains("amazon")
    }
    
    /**
     * Verifica se o app tem permissão para instalar APKs
     */
    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true // Versões antigas não precisam dessa permissão
        }
    }
    
    /**
     * Solicita permissão para instalar APKs (abre as Configurações)
     */
    fun requestInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.i(TAG, "🔐 Solicitando permissão para instalar APKs...")
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
    
    /**
     * Baixa e instala o APK usando DownloadManager
     * CORRIGIDO: Usa ApplicationContext para funcionar mesmo quando o app fecha (Fire Stick)
     */
    fun downloadAndInstall(context: Context, downloadUrl: String, version: String) {
        Log.i(TAG, "📥 Iniciando download: $downloadUrl")
        
        // Usar ApplicationContext para persistir mesmo quando Activity fecha
        val appContext = context.applicationContext
        
        // Verificar permissão ANTES de baixar
        if (!canInstallPackages(appContext)) {
            Log.w(TAG, "⚠️ App não tem permissão para instalar APKs")
            requestInstallPermission(appContext)
            return
        }
        
        // Remover caracteres inválidos da versão para o nome do arquivo
        val safeVersion = version.replace("v", "").replace(".", "_").replace(":", "_")
        // Adicionar timestamp ao nome para evitar cache do Fire OS
        val timestamp = System.currentTimeMillis()
        val fileName = "maxiptv-$safeVersion-$timestamp.apk"
        
        // 🔥 LIMPAR ARQUIVOS ANTIGOS ANTES DE BAIXAR (evita cache do Fire OS)
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir.exists() && downloadsDir.isDirectory) {
                downloadsDir.listFiles()?.filter { 
                    it.name.startsWith("maxiptv", ignoreCase = true) && it.name.endsWith(".apk", ignoreCase = true)
                }?.forEach { oldFile ->
                    try {
                        oldFile.delete()
                        Log.d(TAG, "🗑️ Arquivo antigo deletado: ${oldFile.name}")
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Não foi possível deletar arquivo antigo: ${oldFile.name}")
                    }
                }
                
                // Também limpar em caminhos alternativos do Fire OS
                if (isFireOS(appContext)) {
                    val altPath1 = File(Environment.getExternalStorageDirectory(), "Download")
                    val altPath2 = File(Environment.getExternalStorageDirectory(), "Downloads")
                    listOf(altPath1, altPath2).forEach { altDir ->
                        if (altDir.exists() && altDir.isDirectory) {
                            altDir.listFiles()?.filter {
                                it.name.startsWith("maxiptv", ignoreCase = true) && it.name.endsWith(".apk", ignoreCase = true)
                            }?.forEach { oldFile ->
                                try {
                                    oldFile.delete()
                                    Log.d(TAG, "🗑️ Arquivo antigo deletado (alt): ${oldFile.name}")
                                } catch (e: Exception) {
                                    Log.w(TAG, "⚠️ Não foi possível deletar arquivo antigo (alt): ${oldFile.name}")
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Erro ao limpar arquivos antigos: ${e.message}")
        }
        
        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle("MaxiPTV Atualização")
            .setDescription("Baixando versão $version...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        
        val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)
        
        Log.i(TAG, "✅ Download iniciado com ID: $downloadId")
        
        // Salvar downloadId e fileName em SharedPreferences para verificar depois
        saveDownloadInfo(appContext, downloadId, fileName)
        
        // Registrar receiver usando ApplicationContext (persiste mesmo quando app fecha)
        registerDownloadReceiver(appContext, downloadId, fileName)
        
        // Verificar imediatamente se já está completo (caso download rápido)
        checkDownloadStatus(appContext, downloadId, fileName)
    }
    
    /**
     * Salva informações do download para verificar depois
     */
    private fun saveDownloadInfo(context: Context, downloadId: Long, fileName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_DOWNLOAD_ID, downloadId)
            .putString(KEY_FILE_NAME, fileName)
            .apply()
        Log.i(TAG, "💾 Informações do download salvas: ID=$downloadId, File=$fileName")
    }
    
    /**
     * Registra BroadcastReceiver usando ApplicationContext
     */
    private fun registerDownloadReceiver(context: Context, downloadId: Long, fileName: String) {
        // Remover receiver anterior se existir
        unregisterDownloadReceiver(context)
        
        val appContext = context.applicationContext
        val isFire = isFireOS(appContext)
        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
                Log.d(TAG, "📡 BroadcastReceiver recebido: downloadId=$id (esperado=$downloadId)")
                
                if (id == downloadId) {
                    Log.i(TAG, "✅ Download completo! Instalando...")
                    if (isFire) {
                        Log.i(TAG, "🔥 Fire OS detectado - aguardando antes de instalar...")
                        // No Fire OS, aguardar um pouco mais para garantir que arquivo está pronto
                        try {
                            Thread.sleep(1000)
                        } catch (e: InterruptedException) {
                            Log.w(TAG, "⚠️ Interrupção durante espera")
                        }
                    }
                    try {
                        installApk(appContext, fileName)
                        // Limpar informações salvas
                        clearDownloadInfo(appContext)
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Erro ao instalar após download: ${e.message}", e)
                        e.printStackTrace()
                    }
                    // Não fazer unregister aqui - pode causar crash se contexto já foi destruído
                }
            }
        }
        
        try {
            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(
                    downloadReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                @Suppress("DEPRECATION")
                appContext.registerReceiver(downloadReceiver, filter)
            }
            Log.i(TAG, "✅ BroadcastReceiver registrado com ApplicationContext")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao registrar receiver: ${e.message}", e)
        }
    }
    
    /**
     * Remove o BroadcastReceiver registrado
     */
    private fun unregisterDownloadReceiver(context: Context) {
        try {
            downloadReceiver?.let {
                context.applicationContext.unregisterReceiver(it)
                downloadReceiver = null
                Log.d(TAG, "✅ BroadcastReceiver removido")
            }
        } catch (e: Exception) {
            // Ignora erro se receiver não estava registrado
            Log.d(TAG, "Receiver já estava removido ou não registrado")
        }
    }
    
    /**
     * Verifica o status do download manualmente (útil se BroadcastReceiver falhar)
     * CORRIGIDO: Melhor tratamento para Fire OS
     */
    private fun checkDownloadStatus(context: Context, downloadId: Long, fileName: String) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor: Cursor? = downloadManager.query(query)
            
            cursor?.use {
                if (it.moveToFirst()) {
                    val statusIndex = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val status = if (statusIndex >= 0) it.getInt(statusIndex) else -1
                    
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        Log.i(TAG, "✅ Download já está completo! Instalando...")
                        // No Fire OS, aguardar um pouco mais para garantir que arquivo está pronto
                        if (isFireOS(context)) {
                            Thread.sleep(1000)
                        }
                        installApk(context, fileName)
                        clearDownloadInfo(context)
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        val reasonIndex = it.getColumnIndex(DownloadManager.COLUMN_REASON)
                        val reason = if (reasonIndex >= 0) it.getInt(reasonIndex) else -1
                        Log.e(TAG, "❌ Download falhou: $reason")
                        clearDownloadInfo(context)
                    } else {
                        Log.d(TAG, "⏳ Download ainda em progresso (status=$status)")
                    }
                } else {
                    Log.w(TAG, "⚠️ Download ID não encontrado no DownloadManager")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao verificar status do download: ${e.message}", e)
        }
    }
    
    /**
     * Limpa informações do download salvas
     */
    private fun clearDownloadInfo(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_DOWNLOAD_ID)
            .remove(KEY_FILE_NAME)
            .apply()
    }
    
    /**
     * Verifica se há download pendente e tenta instalar (chamado quando app abre)
     */
    fun checkPendingDownload(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val downloadId = prefs.getLong(KEY_DOWNLOAD_ID, -1)
            val fileName = prefs.getString(KEY_FILE_NAME, null)
            
            if (downloadId != -1L && fileName != null) {
                Log.i(TAG, "🔍 Verificando download pendente: ID=$downloadId, File=$fileName")
                checkDownloadStatus(context, downloadId, fileName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao verificar download pendente: ${e.message}", e)
        }
    }
    
    /**
     * Instala o APK baixado
     * CORRIGIDO: Tratamento específico para Fire OS com múltiplos caminhos
     */
    private fun installApk(context: Context, fileName: String) {
        try {
            val appContext = context.applicationContext
            val isFire = isFireOS(appContext)
            
            if (isFire) {
                Log.i(TAG, "🔥 Fire OS detectado - usando tratamento especial")
            }
            
            // Tentar múltiplos caminhos possíveis (Fire OS pode ter caminhos diferentes)
            val possiblePaths = mutableListOf<File>()
            
            // Caminho padrão
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            possiblePaths.add(File(downloadsDir, fileName))
            
            // Para Fire OS, tentar também caminhos alternativos
            if (isFire) {
                // Caminho alternativo 1: Downloads direto
                val altPath1 = File(Environment.getExternalStorageDirectory(), "Download/$fileName")
                possiblePaths.add(altPath1)
                
                // Caminho alternativo 2: Downloads com D maiúsculo
                val altPath2 = File(Environment.getExternalStorageDirectory(), "Downloads/$fileName")
                possiblePaths.add(altPath2)
                
                Log.d(TAG, "🔍 Verificando caminhos possíveis no Fire OS:")
                possiblePaths.forEach { path ->
                    Log.d(TAG, "   - ${path.absolutePath} (existe=${path.exists()})")
                }
            }
            
            // Procurar o arquivo nos caminhos possíveis
            var file: File? = null
            for (possibleFile in possiblePaths) {
                if (possibleFile.exists() && possibleFile.length() > 0) {
                    file = possibleFile
                    Log.i(TAG, "✅ APK encontrado: ${file.absolutePath} (${file.length()} bytes)")
                    break
                }
            }
            
            // Se não encontrou no caminho exato, procurar qualquer APK do MaxiPTV
            if (file == null) {
                Log.w(TAG, "⚠️ Arquivo não encontrado no caminho exato, procurando alternativas...")
                
                val searchDirs = if (isFire) {
                    listOf(
                        downloadsDir,
                        File(Environment.getExternalStorageDirectory(), "Download"),
                        File(Environment.getExternalStorageDirectory(), "Downloads")
                    )
                } else {
                    listOf(downloadsDir)
                }
                
                for (searchDir in searchDirs) {
                    if (searchDir.exists() && searchDir.isDirectory) {
                        val downloads = searchDir.listFiles { _, name ->
                            name.startsWith("maxiptv", ignoreCase = true) && name.endsWith(".apk", ignoreCase = true)
                        }
                        
                        if (downloads != null && downloads.isNotEmpty()) {
                            // Usar o mais recente
                            val latest = downloads.maxByOrNull { it.lastModified() }
                            if (latest != null && latest.length() > 0) {
                                file = latest
                                Log.i(TAG, "📦 Usando APK alternativo encontrado: ${file.absolutePath} (${file.length()} bytes)")
                                break
                            }
                        }
                    }
                }
            }
            
            if (file == null || !file.exists()) {
                Log.e(TAG, "❌ Nenhum APK encontrado para instalar")
                Log.e(TAG, "   Caminhos verificados:")
                possiblePaths.forEach { path ->
                    Log.e(TAG, "     - ${path.absolutePath}")
                }
                return
            }
            
            // Verificar se o arquivo não está vazio
            if (file.length() == 0L) {
                Log.e(TAG, "❌ APK está vazio ou corrompido!")
                return
            }
            
            // No Fire OS, aguardar mais tempo para garantir que arquivo está completamente escrito
            val waitTime = if (isFire) 1500L else 500L
            Log.d(TAG, "⏳ Aguardando ${waitTime}ms para garantir arquivo completo...")
            Thread.sleep(waitTime)
            
            // Verificar novamente se arquivo ainda existe e tem tamanho válido
            if (!file.exists() || file.length() == 0L) {
                Log.e(TAG, "❌ Arquivo desapareceu ou ficou vazio após espera!")
                return
            }
            
            Log.i(TAG, "📦 Instalando APK: ${file.absolutePath} (${file.length()} bytes)")
            installApkFile(appContext, file)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao instalar APK: ${e.message}", e)
            e.printStackTrace()
        }
    }
    
    /**
     * Instala um arquivo APK específico
     * CORRIGIDO: Tratamento específico para Fire OS com múltiplas tentativas
     */
    private fun installApkFile(context: Context, file: File) {
        try {
            val appContext = context.applicationContext
            val isFire = isFireOS(appContext)
            
            Log.i(TAG, "📦 Preparando instalação do APK: ${file.absolutePath}")
            Log.i(TAG, "   Tamanho: ${file.length()} bytes")
            Log.i(TAG, "   Fire OS: $isFire")
            
            // Verificar permissão novamente antes de instalar (especialmente importante no Fire OS)
            if (!canInstallPackages(appContext)) {
                Log.w(TAG, "⚠️ Permissão de instalação não concedida - solicitando...")
                requestInstallPermission(appContext)
                return
            }
            
            val uri: Uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                try {
                    FileProvider.getUriForFile(
                        appContext,
                        "${appContext.packageName}.fileprovider",
                        file
                    ).also {
                        Log.d(TAG, "✅ Uri criado com FileProvider: $it")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erro ao criar Uri com FileProvider: ${e.message}", e)
                    // Fallback: tentar sem FileProvider (pode funcionar em versões antigas do Fire OS)
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N) {
                        Uri.fromFile(file).also {
                            Log.d(TAG, "✅ Uri criado sem FileProvider (fallback): $it")
                        }
                    } else {
                        throw e
                    }
                }
            } else {
                Uri.fromFile(file).also {
                    Log.d(TAG, "✅ Uri criado sem FileProvider (Android < 7.0): $it")
                }
            }
            
            // Criar Intent de instalação
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                
                // Para Android 7.0+ (API 24+), garantir permissões de escrita também
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
            }
            
            // Verificar permissão NOVAMENTE imediatamente antes de instalar (compatibilidade Fire OS)
            if (!canInstallPackages(appContext)) {
                Log.w(TAG, "⚠️ Permissão de instalação não concedida - solicitando novamente...")
                requestInstallPermission(appContext)
                return
            }
            
            // Verificar se há um app para lidar com a instalação
            val resolveInfo = appContext.packageManager.resolveActivity(
                installIntent, 
                PackageManager.MATCH_DEFAULT_ONLY
            )
            
            if (resolveInfo != null) {
                Log.i(TAG, "✅ App de instalação encontrado: ${resolveInfo.activityInfo.packageName}")
                
                try {
                    // No Fire OS, garantir que o Intent tem todas as flags necessárias
                    if (isFire) {
                        installIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        Log.d(TAG, "🔥 Flags adicionais aplicadas para Fire OS")
                        
                        // No Fire OS, aguardar um pouco mais antes de iniciar instalação
                        Thread.sleep(500)
                    }
                    
                    appContext.startActivity(installIntent)
                    Log.i(TAG, "✅ Instalação iniciada com sucesso!")
                    
                } catch (e: SecurityException) {
                    // Fire OS pode lançar SecurityException mesmo com permissão
                    Log.e(TAG, "❌ SecurityException no Fire OS: ${e.message}", e)
                    Log.e(TAG, "   Tentando solicitar permissão novamente...")
                    requestInstallPermission(appContext)
                } catch (e: android.content.ActivityNotFoundException) {
                    // Fire OS pode não ter Activity para instalar
                    Log.e(TAG, "❌ ActivityNotFoundException: ${e.message}", e)
                    Log.e(TAG, "   Fire OS pode não ter PackageInstaller disponível")
                    requestInstallPermission(appContext)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erro ao iniciar instalação: ${e.message}", e)
                    e.printStackTrace()
                    
                    // Tentar fallback apenas se não for Fire OS (Fire OS pode não suportar)
                    if (!isFire) {
                        try {
                            Log.d(TAG, "🔄 Tentando fallback...")
                            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/vnd.android.package-archive")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                                }
                            }
                            appContext.startActivity(fallbackIntent)
                            Log.i(TAG, "✅ Instalação iniciada com fallback")
                        } catch (e2: Exception) {
                            Log.e(TAG, "❌ Erro no fallback também: ${e2.message}", e2)
                        }
                    } else {
                        // No Fire OS, se falhar, tentar solicitar permissão novamente
                        Log.d(TAG, "🔥 Fire OS: Tentando solicitar permissão novamente após erro")
                        requestInstallPermission(appContext)
                    }
                }
            } else {
                Log.e(TAG, "❌ Nenhum app encontrado para instalar APK")
                Log.e(TAG, "   Isso pode indicar que:")
                Log.e(TAG, "   1. Permissão de instalação não foi concedida")
                Log.e(TAG, "   2. Fire OS bloqueou instalação de fontes desconhecidas")
                Log.e(TAG, "   3. PackageInstaller não está disponível")
                
                // No Fire OS, quando resolveInfo é null, tentar solicitar permissão
                if (isFire) {
                    Log.d(TAG, "🔥 Fire OS: resolveInfo é null - solicitando permissão...")
                    requestInstallPermission(appContext)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao instalar APK: ${e.message}", e)
            e.printStackTrace()
        }
    }
}

