package com.maxiptv.utils

import android.util.Log

/**
 * Utilitários para manipulação de datas
 */
object DateUtils {
    private const val TAG = "DateUtils"
    
    /**
     * Verificar se data de expiração está vencida (formato DD/MM/YYYY)
     * Função centralizada para evitar duplicação
     */
    fun isExpired(expiryDate: String): Boolean {
        return try {
            if (expiryDate.isBlank()) return false // Se não tem data, não está expirado
            
            val parts = expiryDate.split("/")
            if (parts.size != 3) return true // Formato inválido = considerado expirado
            
            val day = parts[0].toInt()
            val month = parts[1].toInt() - 1 // Calendar months are 0-based
            val year = parts[2].toInt()
            
            val calendar = java.util.Calendar.getInstance()
            calendar.set(year, month, day, 23, 59, 59)
            
            val expiryTime = calendar.timeInMillis
            val currentTime = System.currentTimeMillis()
            
            currentTime > expiryTime
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao verificar expiração: ${e.message}")
            true // Em caso de erro, considerar expirado por segurança
        }
    }
}

