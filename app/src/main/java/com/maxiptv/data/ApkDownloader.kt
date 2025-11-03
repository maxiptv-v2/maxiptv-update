package com.maxiptv.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

object ApkDownloader {
    private const val TAG = "ApkDownloader"
    
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
     */
    fun downloadAndInstall(context: Context, downloadUrl: String, version: String) {
        Log.i(TAG, "📥 Iniciando download: $downloadUrl")
        
        // Verificar permissão ANTES de baixar
        if (!canInstallPackages(context)) {
            Log.w(TAG, "⚠️ App não tem permissão para instalar APKs")
            requestInstallPermission(context)
            return
        }
        
        // Remover caracteres inválidos da versão para o nome do arquivo
        val safeVersion = version.replace("v", "").replace(".", "_").replace(":", "_")
        val fileName = "maxiptv-$safeVersion.apk"
        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle("MaxiPTV Atualização")
            .setDescription("Baixando versão $version...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)
        
        Log.i(TAG, "✅ Download iniciado com ID: $downloadId")
        
        // Registrar receiver para quando download completar
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    Log.i(TAG, "✅ Download completo! Instalando...")
                    installApk(context, fileName)
                    context.unregisterReceiver(this)
                }
            }
        }
        
        context.registerReceiver(
            onComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_NOT_EXPORTED
        )
    }
    
    /**
     * Instala o APK baixado
     */
    private fun installApk(context: Context, fileName: String) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            
            if (!file.exists()) {
                Log.e(TAG, "❌ APK não encontrado: ${file.absolutePath}")
                
                // Tentar encontrar qualquer APK do MaxiPTV na pasta Downloads
                val downloads = downloadsDir.listFiles { _, name ->
                    name.startsWith("maxiptv", ignoreCase = true) && name.endsWith(".apk", ignoreCase = true)
                }
                
                if (downloads != null && downloads.isNotEmpty()) {
                    // Usar o mais recente
                    val latest = downloads.maxByOrNull { it.lastModified() }
                    if (latest != null) {
                        Log.i(TAG, "📦 Usando APK alternativo: ${latest.name}")
                        installApkFile(context, latest)
                        return
                    }
                }
                return
            }
            
            Log.i(TAG, "📦 APK encontrado: ${file.absolutePath} (${file.length()} bytes)")
            
            // Verificar se o arquivo não está vazio
            if (file.length() == 0L) {
                Log.e(TAG, "❌ APK está vazio ou corrompido!")
                return
            }
            
            installApkFile(context, file)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao instalar APK: ${e.message}", e)
        }
    }
    
    /**
     * Instala um arquivo APK específico
     */
    private fun installApkFile(context: Context, file: File) {
        try {
            val uri: Uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                try {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erro ao criar Uri com FileProvider: ${e.message}")
                    // Fallback: tentar sem FileProvider (pode funcionar em versões antigas)
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N) {
                        Uri.fromFile(file)
                    } else {
                        throw e
                    }
                }
            } else {
                Uri.fromFile(file)
            }
            
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                
                // Para Android 7.0+ (API 24+), adicionar permissão de escrita
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
            }
            
            // Verificar se há um app para lidar com a instalação
            if (installIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(installIntent)
                Log.i(TAG, "✅ Instalação iniciada")
            } else {
                Log.e(TAG, "❌ Nenhum app encontrado para instalar APK")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao instalar APK: ${e.message}", e)
        }
    }
}

