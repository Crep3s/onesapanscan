package com.example.warehousescanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.warehousescanner.scanner.BarcodeAnalyzer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService

    // --- ДОБАВЛЕНО ИЗМЕНЕНИЕ 1: Флаг для блокировки повторного сканирования ---
    @Volatile
    private var isBarcodeHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val previewView = findViewById<PreviewView>(R.id.scanner_preview)
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(android.util.Size(1280, 720))
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcode ->
                        // --- ДОБАВЛЕНО ИЗМЕНЕНИЕ 2: Логика блокировки ---
                        // Проверяем, не был ли штрих-код уже обработан
                        if (isBarcodeHandled) {
                            return@BarcodeAnalyzer
                        }
                        // Немедленно блокируем дальнейшую обработку
                        isBarcodeHandled = true

                        // Возвращаем результат только один раз
                        val resultIntent = Intent()
                        resultIntent.putExtra("scanned_barcode", barcode)
                        setResult(RESULT_OK, resultIntent)
                        finish() // Закрываемся и возвращаем результат
                    })
                }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
            } catch (e: Exception) {
                // Handle exception
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Разрешение на камеру не предоставлено", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 11
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}