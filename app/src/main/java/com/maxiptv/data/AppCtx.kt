package com.maxiptv.data
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
val Context.dataStore by preferencesDataStore(name = "settings")
object AppCtx { lateinit var ctx: Context }
