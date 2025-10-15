package com.maxiptv.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

object ApkDownloader {
    private const val TAG = "ApkDownloader"
    
    /**
     * Baixa e instala o APK usando DownloadManager
     */
    fun downloadAndInstall(context: Context, downloadUrl: String, version: String) {
        Log.i(TAG, "📥 Iniciando download: $downloadUrl")
        
        val fileName = "maxiptv-$version.apk"
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
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
            
            if (!file.exists()) {
                Log.e(TAG, "❌ APK não encontrado: ${file.absolutePath}")
                return
            }
            
            Log.i(TAG, "📦 APK encontrado: ${file.absolutePath}")
            
            val uri: Uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }
            
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            
            context.startActivity(installIntent)
            Log.i(TAG, "✅ Instalação iniciada")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao instalar APK: ${e.message}", e)
        }
    }
}

