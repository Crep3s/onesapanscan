// В файле: app/src/main/java/com/example/warehousescanner/ui/ProductDetailAdapter.kt
package com.example.warehousescanner.ui

import android.content.Intent
import android.os.Handler      // <-- ИСПРАВЛЕНИЕ 1: ДОБАВЛЕН ИМПОРТ
import android.os.Looper       // <-- ИСПРАВЛЕНИЕ 2: ДОБАВЛЕН ИМПОРТ
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.warehousescanner.ImageGalleryActivity
import com.example.warehousescanner.R
import com.example.warehousescanner.network.Product
import kotlin.math.floor

class ProductDetailAdapter(
    private val products: List<Product>,
    private val onTakeClick: (position: Int, quantity: Double) -> Unit,
    private val onReturnClick: (position: Int) -> Unit,
    private val onQuantityChanged: (position: Int, newQuantity: Double) -> Unit
) : RecyclerView.Adapter<ProductDetailAdapter.ViewHolder>() {

    private val textWatchers = mutableMapOf<Int, TextWatcher>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.detail_product_image)
        val name: TextView = itemView.findViewById(R.id.detail_product_name)
        val article: TextView = itemView.findViewById(R.id.detail_product_article)
        val stock: TextView = itemView.findViewById(R.id.detail_product_stock)
        val weight: TextView = itemView.findViewById(R.id.detail_product_weight)
        val keywords: TextView = itemView.findViewById(R.id.detail_product_keywords)
        val description: TextView = itemView.findViewById(R.id.detail_product_description)
        val quantityInput: EditText = itemView.findViewById(R.id.quantity_input)
        val quantityTotalLabel: TextView = itemView.findViewById(R.id.quantity_total_label)
        val takeButton: Button = itemView.findViewById(R.id.take_button)
        val incrementButton: ImageButton = itemView.findViewById(R.id.increment_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.page_item_product_detail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]

        // --- Биндинг данных ---
        holder.name.text = product.documentName ?: product.name
        holder.image.load(product.photoUrl)
        holder.article.text = product.article ?: "N/A"

        val currentStock = product.stockBalance?.get("1") ?: 0.0
        val stockBeforePicking = currentStock + product.quantity
        holder.stock.text = "${stockBeforePicking.toFormattedString()} шт."

        // --- Логика веса ---
        var totalWeight = 0.0
        var weightUnit = ""
        var isWeightAvailable = false
        val weightFromManufacturer = product.manufacturer?.toDoubleOrNull()

        if (weightFromManufacturer != null && weightFromManufacturer > 0) {
            totalWeight = weightFromManufacturer * product.quantity
            weightUnit = "г"
            isWeightAvailable = true
        } else if (product.mass != null && product.mass > 0) {
            totalWeight = product.mass * product.quantity
            weightUnit = "кг"
            isWeightAvailable = true
        }

        if (isWeightAvailable) {
            holder.weight.text = "${totalWeight.toSmartFormattedString()} $weightUnit"
            (holder.weight.parent as View).visibility = View.VISIBLE // Управляем видимостью родительского LinearLayout
        } else {
            (holder.weight.parent as View).visibility = View.GONE
        }

        // --- Управление видимостью блоков с иконками ---
        val descriptionLayout = holder.itemView.findViewById<LinearLayout>(R.id.description_layout)
        val keywordsLayout = holder.itemView.findViewById<LinearLayout>(R.id.keywords_layout)

        if (!product.description.isNullOrBlank()) {
            holder.description.text = product.description
            descriptionLayout.visibility = View.VISIBLE
        } else {
            descriptionLayout.visibility = View.GONE
        }

        if (!product.keywords.isNullOrBlank()) {
            holder.keywords.text = product.keywords
            keywordsLayout.visibility = View.VISIBLE
        } else {
            keywordsLayout.visibility = View.GONE
        }

        holder.image.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ImageGalleryActivity::class.java).apply {
                val imageUrls = ArrayList<String>()
                if (product.photoUrl != null) {
                    imageUrls.add(product.photoUrl)
                }
                putStringArrayListExtra(ImageGalleryActivity.EXTRA_IMAGE_URLS, imageUrls)
            }
            context.startActivity(intent)
        }

        // --- Логика для EditText и кнопок ---
        holder.quantityTotalLabel.text = "/ ${product.quantity.toFormattedString()} шт."
        textWatchers[holder.adapterPosition]?.let { holder.quantityInput.removeTextChangedListener(it) }
        holder.quantityInput.setText(product.scannedQuantity.toFormattedString())

        if (product.isScanned) {
            // РЕЖИМ "ВЕРНУТЬ"
            holder.takeButton.text = "Вернуть"
            holder.takeButton.isEnabled = true
            holder.takeButton.background = ContextCompat.getDrawable(holder.itemView.context, R.drawable.button_background_disabled)
            holder.quantityInput.isEnabled = false
            holder.incrementButton.isEnabled = false
            holder.takeButton.setOnClickListener { onReturnClick(position) }
        } else {
            // РЕЖИМ "ВЗЯТЬ"
            holder.takeButton.text = "Взять"
            holder.takeButton.background = ContextCompat.getDrawable(holder.itemView.context, R.drawable.button_background_selector)
            holder.quantityInput.isEnabled = true
            holder.incrementButton.isEnabled = true
            holder.takeButton.isEnabled = (product.scannedQuantity == product.quantity)

            val textWatcher = holder.quantityInput.doAfterTextChanged { text ->
                val enteredQuantity = text.toString().toDoubleOrNull() ?: 0.0
                holder.takeButton.isEnabled = (enteredQuantity == product.quantity)
                onQuantityChanged(holder.adapterPosition, enteredQuantity)
            }
            textWatchers[holder.adapterPosition] = textWatcher

            holder.takeButton.setOnClickListener {
                val quantity = holder.quantityInput.text.toString().toDoubleOrNull() ?: 0.0
                onTakeClick(holder.adapterPosition, quantity)
            }

            holder.incrementButton.setOnClickListener {
                val currentQuantity = holder.quantityInput.text.toString().toDoubleOrNull() ?: 0.0
                val nextInteger = floor(currentQuantity).toInt() + 1
                holder.quantityInput.setText(nextInteger.toString())
                if (nextInteger >= product.quantity && product.quantity == 1.0) { // Быстрый клик для товаров в 1 шт.
                    Handler(Looper.getMainLooper()).postDelayed({ holder.takeButton.performClick() }, 100)
                }
            }
        }
    }

    override fun getItemCount() = products.size
}