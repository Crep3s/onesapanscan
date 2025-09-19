package com.example.warehousescanner

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Принудительно отключаем ночной режим для всего приложения.
        // Это не даст системному движку Nokia переключать цветовые профили.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}