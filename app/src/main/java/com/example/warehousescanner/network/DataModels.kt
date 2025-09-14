// В файле: app/src/main/java/com/example/warehousescanner/network/DataModels.kt
package com.example.warehousescanner.network

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// --- Структуры для REST API (Заказы) ---
data class SalesDriveResponse(
    val data: List<Order>?,
    val meta: Meta?
)

data class Order(
    @SerializedName("id")
    val id: Int,
    @SerializedName("products")
    val products: List<ProductInOrder>,
    @SerializedName("comment")
    val comment: String?
)

data class ProductInOrder(
    @SerializedName("productId")
    val productId: Int,
    @SerializedName("sku")
    val article: String?,
    @SerializedName("amount")
    val quantity: Double,
    @SerializedName("description")
    val description: String?
)

data class Meta(
    val fields: Fields?
)

data class Fields(
    val products: ProductOptions?
)

data class ProductOptions(
    val options: List<ProductCard>?
)

// "Карточка" товара с детальной информацией, которую мы получаем из meta
data class ProductCard(
    @SerializedName("value")
    val productId: Int,
    @SerializedName("photo")
    val photoUrl: String?,
    @SerializedName("stockBalance")
    val stockBalance: Map<String, Double>?,
    @SerializedName("mass")
    val mass: Double?,
    @SerializedName("documentName")
    val documentName: String?,
    @SerializedName("manufacturer")
    val manufacturer: String?,
    // --- ДОБАВЛЯЕМ ПОЛЕ ДЛЯ КЛЮЧЕВЫХ СЛОВ ---
    @SerializedName("keywords")
    val keywords: String?
)

// --- ФИНАЛЬНЫЙ, ОБЪЕДИНЕННЫЙ КЛАСС ДЛЯ НАШЕГО UI ---
data class Product(
    val article: String?,
    val name: String?,
    val documentName: String?,
    val quantity: Double,
    val photoUrl: String?,
    val stockBalance: Map<String, Double>?,
    val mass: Double?,
    val manufacturer: String?,
    val description: String?,
    val keywords: String?, // <-- Добавляем новое поле
    var isScanned: Boolean = false,
    var scannedQuantity: Double = 0.0
) : Serializable