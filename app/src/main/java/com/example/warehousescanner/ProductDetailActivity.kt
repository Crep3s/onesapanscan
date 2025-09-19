// В файле: app/src/main/java/com/example/warehousescanner/ProductDetailActivity.kt
package com.example.warehousescanner

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.warehousescanner.network.Product
import com.example.warehousescanner.scanner.BarcodeAnalyzer
import com.example.warehousescanner.ui.ProductDetailAdapter
import com.example.warehousescanner.ui.SoundManager
import com.example.warehousescanner.ui.handleScanError
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var products: MutableList<Product>
    private lateinit var adapter: ProductDetailAdapter
    private lateinit var cameraExecutor: ExecutorService
    private var orderComment: String? = null
    private lateinit var cameraContainer: FrameLayout
    private var cameraProvider: ProcessCameraProvider? = null
    private val barcodeStringBuilder = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)
        SoundManager.init(this)
        initViews()

        products = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_PRODUCTS, java.util.ArrayList::class.java) as? MutableList<Product>
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_PRODUCTS) as? MutableList<Product>
        } ?: mutableListOf()

        val startPosition = intent.getIntExtra(EXTRA_POSITION, 0)
        orderComment = intent.getStringExtra(EXTRA_ORDER_COMMENT)

        adapter = ProductDetailAdapter(
            products = products,
            onTakeClick = { position, quantity -> confirmCollection(position, quantity) },
            onReturnClick = { position -> returnCollection(position) },
            onQuantityChanged = { position, newQuantity ->
                if (position >= 0 && position < products.size) {
                    products[position].scannedQuantity = newQuantity
                }
            }
        )
        viewPager.adapter = adapter
        viewPager.setCurrentItem(startPosition, false)

        findViewById<FloatingActionButton>(R.id.detail_scan_fab).setOnClickListener { openCamera() }
        findViewById<Button>(R.id.close_camera_button).setOnClickListener { hideCamera() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                setResultAndFinish()
                finish()
            }
        })
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (cameraContainer.visibility != View.VISIBLE) {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                if (barcodeStringBuilder.isNotEmpty()) {
                    val barcode = barcodeStringBuilder.toString()
                    onBarcodeDecoded(barcode)
                    barcodeStringBuilder.clear()
                    return true
                }
            } else {
                val char = event.unicodeChar.toChar()
                if (char.isLetterOrDigit() || char == '-') {
                    barcodeStringBuilder.append(char)
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun initViews() {
        viewPager = findViewById(R.id.product_detail_view_pager)
        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraContainer = findViewById(R.id.camera_container)
    }

    private fun returnCollection(position: Int) {
        val product = products[position]
        product.scannedQuantity = 0.0
        product.isScanned = false
        adapter.notifyItemChanged(position)
    }

    // --- ИЗМЕНЕНИЕ: ПОЛНОСТЬЮ НОВАЯ ЛОГИКА ОБРАБОТКИ ШТРИХ-КОДА ---
    private fun onBarcodeDecoded(barcode: String) {
        val currentPosition = viewPager.currentItem
        val currentProduct = products[currentPosition]

        // Сценарий 1: Отсканирован штрих-код ТЕКУЩЕГО товара (самый частый случай)
        if (currentProduct.article == barcode) {
            if (currentProduct.scannedQuantity < currentProduct.quantity) {
                currentProduct.scannedQuantity += 1.0
                adapter.notifyItemChanged(currentPosition)
                if (currentProduct.scannedQuantity >= currentProduct.quantity) {
                    confirmCollection(currentPosition, currentProduct.scannedQuantity)
                }
            } else {
                Toast.makeText(this, "Уже собрано достаточное количество", Toast.LENGTH_SHORT).show()
            }
            return // Выходим, задача выполнена
        }

        // Сценарий 2: Отсканирован штрих-код ДРУГОГО товара, но из ЭТОГО ЖЕ заказа
        val otherProductInOrder = products.find { it.article == barcode }
        if (otherProductInOrder != null) {
            val newPosition = products.indexOf(otherProductInOrder)
            if (newPosition != -1) {
                viewPager.setCurrentItem(newPosition, true) // Просто переключаемся на него
                Toast.makeText(this, "Переключено на: ${otherProductInOrder.name}", Toast.LENGTH_SHORT).show()
            }
            return // Выходим, задача выполнена
        }

        // Сценарий 3: Отсканирован штрих-код, которого НЕТ в этом заказе
        SoundManager.playErrorSound()
        handleScanErrorLocal("Товар не из этого заказа!")
    }

    private fun confirmCollection(position: Int, quantity: Double) {
        val product = products[position]
        if (quantity != product.quantity) {
            Toast.makeText(this, "Введено неверное количество", Toast.LENGTH_SHORT).show()
            return
        }
        product.scannedQuantity = quantity
        product.isScanned = true
        adapter.notifyItemChanged(position)

        Handler(Looper.getMainLooper()).postDelayed({
            val allProductsScanned = products.all { it.isScanned }
            if (allProductsScanned) {
                Toast.makeText(this, "Заказ собран!", Toast.LENGTH_LONG).show()
                setResultAndFinish()
                finish()
            } else {
                val nextUnscannedPosition = products.indexOfFirst { !it.isScanned }
                if (nextUnscannedPosition != -1) {
                    viewPager.setCurrentItem(nextUnscannedPosition, true)
                }
            }
        }, 300)
    }

    private fun setResultAndFinish() {
        val resultIntent = Intent()
        resultIntent.putExtra(EXTRA_PRODUCTS, ArrayList(products))
        setResult(Activity.RESULT_OK, resultIntent)
    }

    // --- ИЗМЕНЕНИЕ: ВОЗВРАЩАЕМ ЭТОТ МЕТОД ДЛЯ ОТОБРАЖЕНИЯ ОШИБКИ ---
    private fun handleScanErrorLocal(message: String) {
        val currentView = (viewPager.getChildAt(0) as? RecyclerView)?.layoutManager?.findViewByPosition(viewPager.currentItem)
        if (currentView == null) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            return
        }
        val scanErrorLayout = currentView.findViewById<View>(R.id.scan_error_layout)
        val scanErrorText = currentView.findViewById<TextView>(R.id.scan_error_text)
        scanErrorText.text = message
        handleScanError(scanErrorLayout)
    }

    private fun openCamera() { /* ... без изменений ... */
        if (allPermissionsGranted()) {
            cameraContainer.visibility = View.VISIBLE
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun hideCamera() { /* ... без изменений ... */
        cameraContainer.visibility = View.GONE
        cameraProvider?.unbindAll()
    }

    private fun startCamera() { /* ... без изменений ... */
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val previewView = findViewById<PreviewView>(R.id.camera_preview_view)
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(android.util.Size(1280, 720))
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcode ->
                        runOnUiThread {
                            hideCamera()
                            onBarcodeDecoded(barcode)
                        }
                    })
                }
            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e(TAG, "Не удалось запустить камеру", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all { /* ... без изменений ... */
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) { /* ... без изменений ... */
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                openCamera()
            } else {
                Toast.makeText(this, "Разрешение на камеру не предоставлено.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() { /* ... без изменений ... */
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "ProductDetailActivity"
        const val EXTRA_PRODUCTS = "products_list"
        const val EXTRA_POSITION = "start_position"
        const val EXTRA_ORDER_COMMENT = "order_comment"
        private const val REQUEST_CODE_PERMISSIONS = 12
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}