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
    
    // Callback para erros (apenas Fire OS)
    private var errorCallback: ((String) -> Unit)? = null
    
    /**
     * Define callback para receber erros de instalação (apenas Fire OS)
     */
    fun setErrorCallback(callback: (String) -> Unit) {
        errorCallback = callback
    }
    
    /**
     * Remove callback de erros
     */
    fun clearErrorCallback() {
        errorCallback = null
    }
    
    /**
     * Notifica erro através do callback (apenas Fire OS)
     */
    private fun notifyError(context: Context, message: String) {
        if (isFireOS(context)) {
            errorCallback?.invoke(message)
            Log.e(TAG, "🔥 Erro no Fire OS: $message")
        }
    }
    
    /**
     * Verifica se está rodando no Fire OS / Fire Stick
     */
    private fun isFireOS(context: Context): Boolean {
        return MaxiApp.isFireStick || 
               android.os.Build.MANUFACTURER.lowercase().contains("amazon") ||
               android.os.Build.BRAND.lowercase().contains("amazon")
    }
    
    /**
     * Obtém diretório de Downloads de forma compatível (API moderna quando disponível)
     * ATUALIZADO: Usa métodos modernos para Android 10+ (API 29+)
     */
    private fun getDownloadsDirectory(context: Context): File? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ (API 29+): Usar método moderno
            try {
                // Tentar obter diretório público de Downloads via MediaStore
                val downloadsUri = android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val projection = arrayOf(android.provider.MediaStore.Downloads._ID)
                val cursor = context.contentResolver.query(
                    downloadsUri,
                    projection,
                    null,
                    null,
                    null
                )
                cursor?.use {
                    // Se conseguir acessar MediaStore, usar diretório padrão
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                } ?: run {
                    // Fallback: usar diretório do app (sempre funciona)
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        ?: context.getExternalFilesDir(null)?.let { File(it, "Downloads") }
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Erro ao obter diretório moderno, usando fallback: ${e.message}")
                // Fallback para método antigo se necessário
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    @Suppress("DEPRECATION")
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                } else {
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        ?: context.getExternalFilesDir(null)?.let { File(it, "Downloads") }
                }
            }
        } else {
            // Android < 10: Usar método antigo (ainda funciona)
            @Suppress("DEPRECATION")
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }
    }
    
    /**
     * Obtém diretório raiz de armazenamento externo (compatível)
     * ATUALIZADO: Usa métodos modernos quando disponível
     */
    private fun getExternalStorageRoot(context: Context): File? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+: Usar diretório do app como base
            context.getExternalFilesDir(null)?.parentFile?.parentFile
        } else {
            // Android < 10: Usar método antigo
            @Suppress("DEPRECATION")
            Environment.getExternalStorageDirectory()
        }
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
        // ATUALIZADO: Usa métodos modernos compatíveis com Android 10+
        try {
            val downloadsDir = getDownloadsDirectory(appContext)
            downloadsDir?.let { dir ->
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles()?.filter { 
                        it.name.startsWith("maxiptv", ignoreCase = true) && it.name.endsWith(".apk", ignoreCase = true)
                    }?.forEach { oldFile ->
                        try {
                            oldFile.delete()
                            Log.d(TAG, "🗑️ Arquivo antigo deletado: ${oldFile.name}")
                        } catch (e: Exception) {
                            Log.w(TAG, "⚠️ Não foi possível deletar arquivo antigo: ${oldFile.name}")
                        }
                    }
                }
            }
            
            // Também limpar em caminhos alternativos do Fire OS
            if (isFireOS(appContext)) {
                val storageRoot = getExternalStorageRoot(appContext)
                storageRoot?.let { root ->
                    val altPath1 = File(root, "Download")
                    val altPath2 = File(root, "Downloads")
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
                    Log.i(TAG, "✅ Download completo! BroadcastReceiver recebido corretamente")
                    Log.d(TAG, "   Download ID: $id")
                    Log.d(TAG, "   FileName: $fileName")
                    Log.d(TAG, "   Fire OS: $isFire")
                    
                    // Aguardar um pouco para garantir que arquivo está pronto (Fire OS e TV Box)
                    val waitTime = if (isFire) 2000L else 1000L
                    Log.d(TAG, "⏳ Aguardando ${waitTime}ms antes de instalar...")
                    try {
                        Thread.sleep(waitTime)
                    } catch (e: InterruptedException) {
                        Log.w(TAG, "⚠️ Interrupção durante espera")
                    }
                    
                    try {
                        Log.i(TAG, "🚀 Iniciando instalação do APK...")
                        installApk(appContext, fileName)
                        // ✅ CORRIGIDO: Não fechar app imediatamente - deixar usuário fechar manualmente
                        // O delay aumentado no HomeScreen (5s + 3 tentativas) garante que versão será verificada corretamente
                        // Fechar app muito cedo pode causar problemas com PackageManager não atualizando versão
                        Log.i(TAG, "✅ Instalação iniciada - app continuará rodando para verificar versão corretamente")
                        // Limpar informações salvas apenas se instalação iniciou com sucesso
                        // (não limpar aqui - deixar installApk limpar após sucesso)
                    } catch (e: Exception) {
                        val errorMsg = "Erro ao instalar após download: ${e.message ?: "Erro desconhecido"}. Tente baixar novamente."
                        Log.e(TAG, "❌ Erro ao instalar após download: ${e.message}", e)
                        Log.e(TAG, "   Stack trace: ${e.stackTraceToString()}")
                        e.printStackTrace()
                        notifyError(appContext, errorMsg)
                        // Não limpar informações - tentar novamente na próxima vez que app abrir
                    }
                    // Não fazer unregister aqui - pode causar crash se contexto já foi destruído
                } else {
                    Log.d(TAG, "📡 BroadcastReceiver recebido, mas ID não corresponde (recebido=$id, esperado=$downloadId)")
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
                        // Aguardar um pouco para garantir que arquivo está pronto (Fire OS e TV Box)
                        val waitTime = if (isFireOS(context)) 2000L else 1000L
                        Log.d(TAG, "⏳ Aguardando ${waitTime}ms antes de instalar...")
                        Thread.sleep(waitTime)
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
            
            Log.i(TAG, "📦 Iniciando processo de instalação...")
            Log.d(TAG, "   FileName: $fileName")
            Log.d(TAG, "   Fire OS: $isFire")
            Log.d(TAG, "   Context: ${appContext.javaClass.simpleName}")
            
            // Verificar permissão ANTES de procurar arquivo
            if (!canInstallPackages(appContext)) {
                Log.w(TAG, "⚠️ Permissão de instalação não concedida - solicitando...")
                requestInstallPermission(appContext)
                return
            }
            Log.d(TAG, "✅ Permissão de instalação verificada")
            
            if (isFire) {
                Log.i(TAG, "🔥 Fire OS detectado - usando tratamento especial")
            } else {
                Log.i(TAG, "📺 TV Box/Android detectado - usando tratamento padrão")
            }
            
            // Tentar múltiplos caminhos possíveis (Fire OS pode ter caminhos diferentes)
            // ATUALIZADO: Usa métodos modernos compatíveis com Android 10+
            val possiblePaths = mutableListOf<File>()
            
            // Caminho padrão (usando método moderno)
            val downloadsDir = getDownloadsDirectory(appContext)
            downloadsDir?.let { dir ->
                possiblePaths.add(File(dir, fileName))
            }
            
            // Para Fire OS e TV Box, tentar também caminhos alternativos
            val storageRoot = getExternalStorageRoot(appContext)
            storageRoot?.let { root ->
                // Caminho alternativo 1: Downloads direto
                val altPath1 = File(root, "Download/$fileName")
                possiblePaths.add(altPath1)
                
                // Caminho alternativo 2: Downloads com D maiúsculo
                val altPath2 = File(root, "Downloads/$fileName")
                possiblePaths.add(altPath2)
            }
            
            Log.d(TAG, "🔍 Verificando caminhos possíveis:")
            possiblePaths.forEach { path ->
                val exists = path.exists()
                val size = if (exists) path.length() else 0L
                Log.d(TAG, "   - ${path.absolutePath} (existe=$exists, tamanho=$size bytes)")
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
            // ATUALIZADO: Usa métodos modernos compatíveis
            val searchDirs = mutableListOf<File>()
            downloadsDir?.let { searchDirs.add(it) }
            
            // Para TV Box também, procurar em caminhos alternativos (reutilizar storageRoot já declarado)
            storageRoot?.let { root ->
                searchDirs.add(File(root, "Download"))
                searchDirs.add(File(root, "Downloads"))
            }
            
            if (file == null) {
                Log.w(TAG, "⚠️ Arquivo não encontrado no caminho exato, procurando alternativas...")
                
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
                    Log.e(TAG, "     - ${path.absolutePath} (existe=${path.exists()})")
                }
                searchDirs.forEach { dir ->
                    Log.e(TAG, "     - Diretório de busca: ${dir.absolutePath} (existe=${dir.exists()})")
                }
                // Não limpar informações - tentar novamente na próxima vez
                return
            }
            
            // Verificar se o arquivo não está vazio
            val fileSize = file.length()
            if (fileSize == 0L) {
                Log.e(TAG, "❌ APK está vazio ou corrompido! (tamanho: $fileSize bytes)")
                return
            }
            
            Log.i(TAG, "✅ APK encontrado e válido: ${file.absolutePath} (${fileSize} bytes)")
            
            // Aguardar um pouco para garantir que arquivo está completamente escrito (Fire OS e TV Box)
            val waitTime = if (isFire) 2000L else 1000L
            Log.d(TAG, "⏳ Aguardando ${waitTime}ms para garantir arquivo completo...")
            Thread.sleep(waitTime)
            
            // Verificar novamente se arquivo ainda existe e tem tamanho válido
            if (!file.exists()) {
                Log.e(TAG, "❌ Arquivo desapareceu após espera!")
                return
            }
            
            val finalSize = file.length()
            if (finalSize == 0L) {
                Log.e(TAG, "❌ Arquivo ficou vazio após espera! (tamanho: $finalSize bytes)")
                return
            }
            
            if (finalSize != fileSize) {
                Log.w(TAG, "⚠️ Tamanho do arquivo mudou após espera (antes: $fileSize, depois: $finalSize)")
            }
            
            Log.i(TAG, "📦 Instalando APK: ${file.absolutePath} (${finalSize} bytes)")
            installApkFile(appContext, file)
            
            // Limpar informações apenas se chegou até aqui (instalação iniciou)
            clearDownloadInfo(appContext)
            
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
                val errorMsg = "Permissão de instalação não concedida. Por favor, habilite 'Instalar apps de fontes desconhecidas' nas configurações."
                Log.w(TAG, "⚠️ $errorMsg")
                notifyError(appContext, errorMsg)
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
                    val errorMsg = "Erro ao preparar arquivo para instalação: ${e.message ?: "Erro desconhecido"}. Verifique se o arquivo foi baixado corretamente."
                    notifyError(appContext, errorMsg)
                    // Fallback: tentar sem FileProvider (pode funcionar em versões antigas do Fire OS)
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N) {
                        Uri.fromFile(file).also {
                            Log.d(TAG, "✅ Uri criado sem FileProvider (fallback): $it")
                        }
                    } else {
                        return // Não lançar exceção, apenas retornar
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
                val errorMsg = "Permissão de instalação não concedida. Por favor, habilite 'Instalar apps de fontes desconhecidas' nas configurações."
                Log.w(TAG, "⚠️ $errorMsg")
                notifyError(appContext, errorMsg)
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
                        installIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        installIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        Log.d(TAG, "🔥 Flags adicionais aplicadas para Fire OS")
                        
                        // No Fire OS, aguardar mais tempo antes de iniciar instalação (evita app fechar)
                        // Isso dá tempo para o sistema processar o arquivo completamente
                        Log.d(TAG, "⏳ Aguardando 2000ms antes de iniciar instalação no Fire OS...")
                        Thread.sleep(2000)
                        
                        // Verificar novamente se arquivo ainda existe e tem tamanho válido
                        if (!file.exists() || file.length() == 0L) {
                            val errorMsg = "Arquivo de atualização desapareceu ou está corrompido. Tente baixar novamente."
                            Log.e(TAG, "❌ $errorMsg")
                            notifyError(appContext, errorMsg)
                            return
                        }
                        
                        Log.d(TAG, "✅ Arquivo ainda válido após espera: ${file.length()} bytes")
                    }
                    
                    Log.d(TAG, "🚀 Iniciando Activity de instalação...")
                    Log.d(TAG, "   Intent: $installIntent")
                    Log.d(TAG, "   Uri: $uri")
                    Log.d(TAG, "   ResolveInfo: ${resolveInfo.activityInfo.packageName}")
                    
                    appContext.startActivity(installIntent)
                    Log.i(TAG, "✅ Instalação iniciada com sucesso!")
                    Log.i(TAG, "   O sistema deve mostrar diálogo de instalação agora")
                    
                    // ✅ CORRIGIDO: Não fechar app automaticamente após instalação
                    // O delay aumentado no HomeScreen (5s + 3 tentativas) garante que versão será verificada corretamente
                    // Fechar app muito cedo pode causar problemas com PackageManager não atualizando versão
                    Log.i(TAG, "✅ Instalação iniciada - app continuará rodando para verificar versão corretamente")
                    
                } catch (e: SecurityException) {
                    // Fire OS pode lançar SecurityException mesmo com permissão
                    val errorMsg = "Erro de segurança ao instalar: ${e.message ?: "Permissão negada"}. Verifique se 'Instalar apps de fontes desconhecidas' está habilitado nas configurações."
                    Log.e(TAG, "❌ SecurityException no Fire OS: ${e.message}", e)
                    notifyError(appContext, errorMsg)
                    requestInstallPermission(appContext)
                } catch (e: android.content.ActivityNotFoundException) {
                    // Fire OS pode não ter Activity para instalar
                    val errorMsg = "Sistema de instalação não encontrado. O Fire OS pode não ter PackageInstaller disponível. Tente instalar manualmente pelo arquivo baixado."
                    Log.e(TAG, "❌ ActivityNotFoundException: ${e.message}", e)
                    notifyError(appContext, errorMsg)
                    requestInstallPermission(appContext)
                } catch (e: Exception) {
                    val errorMsg = "Erro ao iniciar instalação: ${e.message ?: "Erro desconhecido"}. Verifique as configurações do Fire OS."
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
                        // No Fire OS, se falhar, mostrar erro
                        notifyError(appContext, errorMsg)
                        Log.d(TAG, "🔥 Fire OS: Erro capturado e notificado")
                    }
                }
            } else {
                val errorMsg = "Nenhum aplicativo encontrado para instalar o APK. Possíveis causas:\n" +
                        "1. Permissão de instalação não foi concedida\n" +
                        "2. Fire OS bloqueou instalação de fontes desconhecidas\n" +
                        "3. PackageInstaller não está disponível\n\n" +
                        "Por favor, habilite 'Instalar apps de fontes desconhecidas' nas configurações."
                Log.e(TAG, "❌ Nenhum app encontrado para instalar APK")
                Log.e(TAG, "   Isso pode indicar que:")
                Log.e(TAG, "   1. Permissão de instalação não foi concedida")
                Log.e(TAG, "   2. Fire OS bloqueou instalação de fontes desconhecidas")
                Log.e(TAG, "   3. PackageInstaller não está disponível")
                
                // No Fire OS, quando resolveInfo é null, mostrar erro
                if (isFire) {
                    Log.d(TAG, "🔥 Fire OS: resolveInfo é null - mostrando erro...")
                    notifyError(appContext, errorMsg)
                    requestInstallPermission(appContext)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao instalar APK: ${e.message}", e)
            e.printStackTrace()
        }
    }
}

