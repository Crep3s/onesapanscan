// В файле: app/src/main/java/com/example/warehousescanner/ui/SoundManager.kt
package com.example.warehousescanner.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.warehousescanner.R

// Убираем enum, так как звук теперь только один
object SoundManager {

    private var soundPool: SoundPool? = null
    private var isLoaded = false
    private var errorSoundId: Int = 0

    fun init(context: Context) {
        if (soundPool != null) return

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1) // Достаточно одного потока для одного звука
            .setAudioAttributes(audioAttributes)
            .build()

        // Загружаем ТОЛЬКО звук ошибки
        errorSoundId = soundPool!!.load(context, R.raw.sound_error, 1)

        soundPool?.setOnLoadCompleteListener { _, _, _ ->
            isLoaded = true
        }
    }

    // Метод теперь не принимает тип, он всегда проигрывает звук ошибки
    fun playErrorSound() {
        if (!isLoaded || soundPool == null || errorSoundId == 0) return
        soundPool?.play(errorSoundId, 1f, 1f, 1, 0, 1f)
    }
}