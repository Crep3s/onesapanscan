package com.example.warehousescanner

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.warehousescanner.network.Product
import com.example.warehousescanner.ui.ChecklistViewModel
import com.example.warehousescanner.ui.ProductAdapter
import com.example.warehousescanner.ui.SoundManager
import com.example.warehousescanner.ui.UiState
import com.example.warehousescanner.ui.handleScanError

class ChecklistActivity : AppCompatActivity() {

    companion object {
        const val RESULT_EXTRA_NEW_ORDER_ID = "new_order_id"
        const val RESULT_EXTRA_ERROR_MESSAGE = "error_message"
        private const val SCANNER_INPUT_TIMEOUT = 50L
    }

    private val viewModel: ChecklistViewModel by viewModels()
    private lateinit var productAdapter: ProductAdapter
    private lateinit var orderCompleteLayout: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var finishOrderButton: Button
    private lateinit var orderCommentCard: CardView
    private lateinit var orderCommentText: TextView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var emptyOrderMessageText: TextView
    private lateinit var scanFeedbackText: TextView
    private var currentProductList: MutableList<Product> = mutableListOf()
    private var orderComment: String? = null
    private lateinit var errorFlashView: View

    private var isOrderComplete = false
    private val feedbackHandler = Handler(Looper.getMainLooper())
    private var feedbackRunnable: Runnable? = null
    private val barcodeStringBuilder = StringBuilder()
    private val handler = Handler(Looper.getMainLooper())
    private val processBarcodeRunnable = Runnable { processBarcode() }

    private val detailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val updatedList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getSerializableExtra(ProductDetailActivity.EXTRA_PRODUCTS, java.util.ArrayList::class.java) as? List<Product>
            } else {
                @Suppress("DEPRECATION")
                result.data?.getSerializableExtra(ProductDetailActivity.EXTRA_PRODUCTS) as? List<Product>
            }
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
        SoundManager.init(this)
        initViews()
        val orderId = intent.getStringExtra("ORDER_ID")
        if (orderId == null) { finish(); return }
        title = "Сборка заказа №$orderId"
        setupRecyclerView()
        observeViewModel()
        findViewById<Button>(R.id.back_button).setOnClickListener { finish() }
        finishOrderButton.setOnClickListener { finish() } // Эта кнопка теперь только для возврата вручную
        viewModel.loadOrder(orderId)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (event.keyCode == KeyEvent.KEYCODE_ENTER) {
                processBarcode()
                return true
            }
            val char = event.unicodeChar.toChar()
            if (char.isLetterOrDigit() || char in "-/\\_.") {
                barcodeStringBuilder.append(char)
            }
            handler.removeCallbacks(processBarcodeRunnable)
            handler.postDelayed(processBarcodeRunnable, SCANNER_INPUT_TIMEOUT)
        }
        return super.dispatchKeyEvent(event)
    }
    private fun processBarcode() {
        handler.removeCallbacks(processBarcodeRunnable)
        val barcode = barcodeStringBuilder.toString().trim()
        barcodeStringBuilder.clear()

        if (barcode.isNotEmpty()) {
            if (isOrderComplete) {
                // Если заказ завершен, любой скан - это номер НОВОГО заказа
                returnNewOrderIdAndFinish(barcode)
            } else {
                // Иначе, это скан товара из текущего заказа
                handleBarcodeScanOnList(barcode)
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        // Очищаем handler, чтобы избежать утечек памяти
        handler.removeCallbacksAndMessages(null)
        feedbackHandler.removeCallbacksAndMessages(null)
    }
    private fun handleBarcodeScanOnList(barcode: String) {
        val product = currentProductList.find { it.article == barcode }
        if (product == null) {
            SoundManager.playErrorSound()
            handleScanError(errorFlashView)
            return
        }
        if (product.isScanned) {
            showScanFeedback("${product.name} уже полностью собран")
            // Даже если товар собран, звук успеха не помешает
            return
        }
        val productIndex = currentProductList.indexOf(product)
        if (productIndex == -1) return

        product.scannedQuantity += 1.0

        if (product.scannedQuantity >= product.quantity) {
            product.scannedQuantity = product.quantity // Выравниваем на случай "лишнего" скана
            product.isScanned = true
            showScanFeedback("${product.name} - СОБРАНО ПОЛНОСТЬЮ!")
        } else {
            showScanFeedback("Отмечен: ${product.name}")
        }

        productAdapter.notifyItemChanged(productIndex)
        checkIfAllScanned()
    }

    private fun initViews() {
        orderCompleteLayout = findViewById(R.id.order_complete_layout)
        recyclerView = findViewById(R.id.products_recycler_view)
        finishOrderButton = findViewById(R.id.finish_order_button)
        orderCommentCard = findViewById(R.id.order_comment_card)
        orderCommentText = findViewById(R.id.order_comment_text)
        errorFlashView = findViewById(R.id.error_flash_view)
        loadingProgressBar = findViewById(R.id.loading_progress_bar)
        emptyOrderMessageText = findViewById(R.id.empty_order_message_text)
        scanFeedbackText = findViewById(R.id.scan_feedback_text)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.itemAnimator = null
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
            loadingProgressBar.visibility = View.GONE; recyclerView.visibility = View.GONE
            orderCompleteLayout.visibility = View.GONE; emptyOrderMessageText.visibility = View.GONE
            orderCommentCard.visibility = View.GONE
            when (state) {
                is UiState.Loading -> { loadingProgressBar.visibility = View.VISIBLE }
                is UiState.Content -> {
                    if (state.products.isNotEmpty()) {
                        recyclerView.visibility = View.VISIBLE
                        currentProductList = state.products.toMutableList()
                        orderComment = state.orderComment
                        productAdapter.updateData(currentProductList)
                        if (!orderComment.isNullOrBlank()) {
                            orderCommentText.text = orderComment
                            orderCommentCard.visibility = View.VISIBLE
                        }
                        checkIfAllScanned()
                    } else { emptyOrderMessageText.visibility = View.VISIBLE }
                }
                is UiState.AllScanned -> { showOrderCompleteScreen() }
                is UiState.Error -> {
                    val resultIntent = Intent().apply { putExtra(RESULT_EXTRA_ERROR_MESSAGE, state.message) }
                    setResult(Activity.RESULT_CANCELED, resultIntent); finish()
                }
            }
        }
    }

    private fun showScanFeedback(message: String) {
        feedbackRunnable?.let { feedbackHandler.removeCallbacks(it) }
        scanFeedbackText.text = message; scanFeedbackText.alpha = 1f
        scanFeedbackText.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        scanFeedbackText.visibility = View.VISIBLE
        feedbackRunnable = Runnable {
            scanFeedbackText.animate().alpha(0f).setDuration(300).withEndAction {
                scanFeedbackText.visibility = View.GONE
                scanFeedbackText.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            }.start()
        }
        feedbackHandler.postDelayed(feedbackRunnable!!, 2000)
    }

    private fun returnNewOrderIdAndFinish(orderId: String) {
        val resultIntent = Intent().apply { putExtra(RESULT_EXTRA_NEW_ORDER_ID, orderId) }
        setResult(Activity.RESULT_OK, resultIntent); finish()
    }

    private fun checkIfAllScanned() {
        val allDone = currentProductList.isNotEmpty() && currentProductList.all { it.isScanned }
        // Кнопка "Завершить" больше не нужна, так как все происходит автоматически. Скроем ее.
        // finishOrderButton.visibility = if (allDone) View.VISIBLE else View.GONE
        if (allDone) {
            // Небольшая задержка перед показом экрана "Готово", чтобы кладовщик увидел
            // фидбэк о последнем отсканированном товаре.
            handler.postDelayed({ showOrderCompleteScreen() }, 1000)
        }
    }

    private fun showOrderCompleteScreen() {
        isOrderComplete = true
        orderCommentCard.visibility = View.GONE
        recyclerView.visibility = View.GONE
        finishOrderButton.visibility = View.GONE // Кнопка не нужна
        scanFeedbackText.visibility = View.GONE // Скрываем фидбэк, если он еще висит
        orderCompleteLayout.visibility = View.VISIBLE
    }
}