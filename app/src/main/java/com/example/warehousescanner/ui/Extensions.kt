// В файле: app/src/main/java/com/example/warehousescanner/ui/Extensions.kt
package com.example.warehousescanner.ui

import java.text.DecimalFormat

/**
 * Преобразует Double в String. Если число целое (1.0, 2.0),
 * то отбрасывает ".0".
 */
fun Double.toFormattedString(): String {
    return if (this % 1.0 == 0.0) {
        this.toInt().toString()
    } else {
        this.toString()
    }
}

/**
 * Преобразует Double в String, убирая лишние нули в конце.
 * Например, 0.1000 станет "0.1", а 1.0000 станет "1".
 */
fun Double.toSmartFormattedString(): String {
    // Формат #.#### означает "показывать до 4 знаков после запятой, только если они не нули"
    val df = DecimalFormat("#.####")
    return df.format(this)
}