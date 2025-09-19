// В файле: app/src/main/java/com/example/warehousescanner/ui/UiUtils.kt
package com.example.warehousescanner.ui

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import androidx.core.content.ContextCompat

// Длительность, в течение которой экран будет оставаться красным (в миллисекундах)
private const val ERROR_VISIBLE_DURATION_MS = 300L
// Длительность анимации появления/исчезновения
private const val FADE_DURATION_MS = 150L

// Убрали неиспользуемый параметр 'message'
fun handleScanError(view: View) {
    val vibrator = ContextCompat.getSystemService(view.context, Vibrator::class.java)
    vibrator?.let {
        // Проверяем версию Android на устройстве
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Новый, безопасный метод для Android 8.0 (API 26) и выше
            it.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            // Старый метод для версий до Android 8.0
            @Suppress("DEPRECATION") // Подавляем предупреждение об устаревшем методе
            it.vibrate(200)
        }
    }

    view.apply {
        alpha = 0f
        visibility = View.VISIBLE
        animate()
            .alpha(1f)
            .setDuration(FADE_DURATION_MS)
            .withEndAction {
                // После появления, ждем ERROR_VISIBLE_DURATION_MS и запускаем исчезновение
                animate()
                    .alpha(0f)
                    .setDuration(FADE_DURATION_MS)
                    .setStartDelay(ERROR_VISIBLE_DURATION_MS)
                    .withEndAction { visibility = View.GONE }
                    .start()
            }
            .start()
    }
}