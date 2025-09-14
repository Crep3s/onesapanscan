// В файле: app/src/main/java/com/example/warehousescanner/ChecklistActivity.kt
package com.example.warehousescanner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.warehousescanner.network.Product
import com.example.warehousescanner.ui.ChecklistViewModel
import com.example.warehousescanner.ui.ProductAdapter
import com.example.warehousescanner.ui.UiState
import java.lang.StringBuilder
import com.example.warehousescanner.ui.handleScanError
class ChecklistActivity : AppCompatActivity() {

    private val viewModel: ChecklistViewModel by viewModels()
    private lateinit var productAdapter: ProductAdapter

    private lateinit var orderCompleteLayout: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var finishOrderButton: Button
    private lateinit var orderCommentCard: CardView
    private lateinit var orderCommentText: TextView

    private var currentProductList: MutableList<Product> = mutableListOf()
    private var orderComment: String? = null
    private lateinit var errorFlashView: View
    private val barcodeStringBuilder = StringBuilder() // Буфер для сканера

    private val detailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val updatedList = result.data?.getSerializableExtra(ProductDetailActivity.EXTRA_PRODUCTS) as? List<Product>
            if (updatedList != null) {
                currentProductList = updatedList.toMutableList()
                productAdapter.updateData(currentProductList)
                checkIfAllScanned()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checklist)

        initViews()

        val orderId = intent.getStringExtra("ORDER_ID")
        if (orderId == null) {
            finish()
            return
        }
        title = "Сборка заказа №$orderId"

        setupRecyclerView()
        observeViewModel()

        findViewById<Button>(R.id.back_button).setOnClickListener { finish() }
        finishOrderButton.setOnClickListener { finish() }

        viewModel.loadOrder(orderId)
    }

    // --- НОВЫЙ МЕТОД ДЛЯ ПЕРЕХВАТА ВВОДА СО СКАНЕРА ---
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // Если это Enter - обрабатываем
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            if (barcodeStringBuilder.isNotEmpty()) {
                val barcode = barcodeStringBuilder.toString()
                handleBarcodeScanOnList(barcode)
                barcodeStringBuilder.clear()
                return true // Поглощаем событие, оно не пойдет дальше
            }
        } else {
            // Для всех остальных клавиш (цифры, буквы) - добавляем символ в буфер
            val char = event.unicodeChar.toChar()
            if (char.isLetterOrDigit()) {
                barcodeStringBuilder.append(char)
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun handleBarcodeScanOnList(barcode: String) {
        val product = currentProductList.find { it.article == barcode }

        if (product == null) {
            // Используем наш новый универсальный обработчик
            handleScanError(errorFlashView, "Товар не из этого заказа!")
            return
        }

        if (product.isScanned) {
            Toast.makeText(this, "${product.name} уже полностью собран", Toast.LENGTH_SHORT).show()
            return
        }

        product.scannedQuantity += 1.0

        if (product.scannedQuantity >= product.quantity) {
            product.scannedQuantity = product.quantity
            product.isScanned = true
            Toast.makeText(this, "${product.name} - СОБРАНО ПОЛНОСТЬЮ!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Отмечен: ${product.name}", Toast.LENGTH_SHORT).show()
        }

        productAdapter.updateData(currentProductList)
        checkIfAllScanned()
    }


    private fun initViews() {
        orderCompleteLayout = findViewById(R.id.order_complete_layout)
        recyclerView = findViewById(R.id.products_recycler_view)
        finishOrderButton = findViewById(R.id.finish_order_button)
        orderCommentCard = findViewById(R.id.order_comment_card)
        orderCommentText = findViewById(R.id.order_comment_text)
        // --- УБЕДИТЕСЬ, ЧТО ID СОВПАДАЕТ ---
        errorFlashView = findViewById(R.id.error_flash_view)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        productAdapter = ProductAdapter(emptyList()) { position ->
            val intent = Intent(this, ProductDetailActivity::class.java).apply {
                putExtra(ProductDetailActivity.EXTRA_PRODUCTS, ArrayList(currentProductList))
                putExtra(ProductDetailActivity.EXTRA_POSITION, position)
                putExtra(ProductDetailActivity.EXTRA_ORDER_COMMENT, orderComment)
            }
            detailLauncher.launch(intent)
        }
        recyclerView.adapter = productAdapter
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    Toast.makeText(this, "Загрузка...", Toast.LENGTH_SHORT).show()
                }
                is UiState.Content -> {
                    recyclerView.visibility = View.VISIBLE
                    orderCompleteLayout.visibility = View.GONE
                    currentProductList = state.products.toMutableList()
                    orderComment = state.orderComment
                    productAdapter.updateData(currentProductList)

                    if (!orderComment.isNullOrBlank()) {
                        orderCommentText.text = orderComment
                        orderCommentCard.visibility = View.VISIBLE
                    } else {
                        orderCommentCard.visibility = View.GONE
                    }

                    checkIfAllScanned()
                }
                is UiState.AllScanned -> {
                    showOrderCompleteScreen()
                }
                is UiState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun checkIfAllScanned() {
        val allDone = currentProductList.isNotEmpty() && currentProductList.all { it.isScanned }
        finishOrderButton.visibility = if (allDone) View.VISIBLE else View.GONE
        if (allDone) {
            showOrderCompleteScreen()
        }
    }

    private fun showOrderCompleteScreen() {
        orderCommentCard.visibility = View.GONE
        recyclerView.visibility = View.GONE
        finishOrderButton.visibility = View.GONE
        orderCompleteLayout.visibility = View.VISIBLE
    }
}