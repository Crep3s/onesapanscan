// В файле: app/src/main/java/com/example/warehousescanner/MainActivity.kt
package com.example.warehousescanner

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var orderIdInput: EditText
    private val barcodeStringBuilder = StringBuilder() // Буфер для символов со сканера

    // ... (scanResultLauncher и onCreate)
    private val scanResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val scannedId = result.data?.getStringExtra("scanned_barcode")
            orderIdInput.setText(scannedId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        orderIdInput = findViewById(R.id.order_id_input)
        val loadOrderButton = findViewById<Button>(R.id.load_order_button)
        val scanButton = findViewById<ImageButton>(R.id.scan_order_id_button)

        loadOrderButton.setOnClickListener {
            loadOrder()
        }

        scanButton.setOnClickListener {
            val intent = Intent(this, ScannerActivity::class.java)
            scanResultLauncher.launch(intent)
        }
    }



    // --- НОВЫЙ МЕТОД ДЛЯ ПЕРЕХВАТА ВВОДА ---
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Нас интересует только событие нажатия (не отпускания)
        if (event.action == KeyEvent.ACTION_DOWN) {
            // Сканер обычно отправляет "Enter" в конце
            if (event.keyCode == KeyEvent.KEYCODE_ENTER) {
                if (barcodeStringBuilder.isNotEmpty()) {
                    val barcode = barcodeStringBuilder.toString()
                    orderIdInput.setText(barcode)
                    barcodeStringBuilder.clear() // Очищаем буфер
                    loadOrder() // Сразу пытаемся загрузить заказ
                    return true // Событие обработано
                }
            } else {
                // Добавляем символ в наш буфер
                barcodeStringBuilder.append(event.displayLabel)
            }
        }
        // Передаем событие дальше для стандартной обработки
        return super.dispatchKeyEvent(event)
    }

    private fun loadOrder() {
        val orderId = orderIdInput.text.toString().trim()
        if (orderId.isNotEmpty()) {
            val intent = Intent(this, ChecklistActivity::class.java)
            intent.putExtra("ORDER_ID", orderId)
            startActivity(intent)
        } else {
            Toast.makeText(this, "Пожалуйста, введите номер заказа", Toast.LENGTH_SHORT).show()
        }
    }
}