// В файле: app/src/main/java/com/example/warehousescanner/ui/ProductAdapter.kt
package com.example.warehousescanner.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.warehousescanner.R
import com.example.warehousescanner.network.Product
import com.google.android.material.card.MaterialCardView

class ProductAdapter(
    private var products: List<Product>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.item_layout)
        val name: TextView = itemView.findViewById(R.id.product_name)
        val image: ImageView = itemView.findViewById(R.id.product_image)
        val checkmark: ImageView = itemView.findViewById(R.id.checkmark_icon)
        val article: TextView = itemView.findViewById(R.id.product_article)
        val quantity: TextView = itemView.findViewById(R.id.product_quantity)

        fun bind(product: Product, clickListener: (Int) -> Unit) {
            name.text = product.documentName ?: product.name
            article.text = product.article ?: "N/A" // Убираем префикс
            quantity.text = "${product.scannedQuantity.toFormattedString()}/${product.quantity.toFormattedString()}"

            image.load(product.photoUrl) {
                placeholder(R.drawable.ic_launcher_background)
                error(R.drawable.ic_launcher_foreground)
            }

            when {
                // Полностью собран -> зеленый
                product.isScanned -> {
                    checkmark.visibility = View.VISIBLE
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.product_full_collect))
                }
                // Частично собран -> желтый
                product.scannedQuantity > 0 -> {
                    checkmark.visibility = View.GONE
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.product_partial_collect))
                }
                // Не собран -> белый
                else -> {
                    checkmark.visibility = View.GONE
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
                }
            }
            itemView.setOnClickListener {
                clickListener(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position], onItemClick)
    }

    override fun getItemCount(): Int = products.size

    fun updateData(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
}