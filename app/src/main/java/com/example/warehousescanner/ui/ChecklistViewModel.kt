// В файле: app/src/main/java/com/example/warehousescanner/ui/ChecklistViewModel.kt
package com.example.warehousescanner.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehousescanner.network.Product
import com.example.warehousescanner.repository.OrderRepository
import kotlinx.coroutines.launch
import com.example.warehousescanner.BuildConfig
sealed class UiState {
    object Loading : UiState()
    data class Content(val products: List<Product>, val orderComment: String?) : UiState()
    object AllScanned : UiState()
    data class Error(val message: String) : UiState()
}

class ChecklistViewModel : ViewModel() {

    private val apiKey = BuildConfig.SALESDRIVE_API_KEY
    private val repository = OrderRepository()
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    fun loadOrder(orderId: String) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val result = repository.getOrder(apiKey, orderId)
            result.fold(
                onSuccess = { response ->
                    if (response.data != null && response.data.isNotEmpty()) {
                        val order = response.data[0]
                        val productCards = response.meta?.fields?.products?.options ?: emptyList()
                        val productCardsMap = productCards.associateBy { it.productId }

                        val combinedProducts = order.products.map { productInOrder ->
                            val card = productCardsMap[productInOrder.productId]
                            Product(
                                article = productInOrder.article,
                                name = card?.documentName,
                                documentName = card?.documentName,
                                quantity = productInOrder.quantity,
                                photoUrl = card?.photoUrl,
                                stockBalance = card?.stockBalance,
                                mass = card?.mass,
                                manufacturer = card?.manufacturer,
                                description = productInOrder.description,
                                keywords = card?.keywords, // <-- "Склеиваем" ключевые слова
                                isScanned = false,
                                scannedQuantity = 0.0
                            )
                        }
                        _uiState.value = UiState.Content(combinedProducts, order.comment)
                    } else {
                        _uiState.value = UiState.Error("Заказ №$orderId не найден.")
                    }
                },
                onFailure = {
                    _uiState.value = UiState.Error("Ошибка сети: ${it.message}")
                }
            )
        }
    }
}