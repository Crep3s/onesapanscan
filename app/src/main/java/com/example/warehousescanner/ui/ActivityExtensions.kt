// В файле: app/src/main/java/com/example/warehousescanner/ui/ActivityExtensions.kt
package com.example.warehousescanner.ui

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.TextView
import android.widget.Toast

// Создаем переменные для управления таймером на уровне всего приложения
private val handler = Handler(Looper.getMainLooper())
private var errorRunnable: Runnable? = null

/**
 * Универсальная функция для отображения ошибки сканирования.
 * @param errorView - View, который будет становиться красным (может быть View или TextView).
 * @param message - Текст ошибки.
 */
fun Activity.handleScanError(errorView: View, message: String) {
    // 1. Вибрация
    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(500)
    }

    // 2. Логика анимации
    // Если есть предыдущий таймер на скрытие - отменяем его!
    errorRunnable?.let { handler.removeCallbacks(it) }

    // Прерываем любую текущую анимацию и сбрасываем состояние
    errorView.animate().cancel()
    errorView.alpha = 0f

    // Если это TextView, устанавливаем текст
    if (errorView is TextView) {
        errorView.text = message
    }

    errorView.visibility = View.VISIBLE

    // Запускаем новую анимацию появления
    errorView.animate()
        .alpha(if (errorView is TextView) 1f else 0.7f) // Для текста делаем полностью непрозрачным
        .setDuration(200)
        .withEndAction {
            // Создаем новый таймер на скрытие
            errorRunnable = Runnable {
                errorView.animate()
                    .alpha(0f)
                    .setDuration(500)
                    .withEndAction { errorView.visibility = View.GONE }
            }
            handler.postDelayed(errorRunnable!!, 5000)
        }

    // 3. Сообщение
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}