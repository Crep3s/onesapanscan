// В файле: app/src/main/java/com/example/warehousescanner/repository/OrderRepository.kt

package com.example.warehousescanner.repository

import com.example.warehousescanner.network.ApiClient
import com.example.warehousescanner.network.SalesDriveResponse

class OrderRepository {

    suspend fun getOrder(apiKey: String, orderId: String): Result<SalesDriveResponse> {
        return try {
            // Теперь вызываем новый метод, передавая ID заказа в оба поля фильтра
            val response = ApiClient.instance.getOrderById(
                apiKey = apiKey,
                orderIdFrom = orderId,
                orderIdTo = orderId
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Ошибка загрузки: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}