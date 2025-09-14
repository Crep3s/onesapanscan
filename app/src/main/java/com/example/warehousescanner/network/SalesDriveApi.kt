package com.example.warehousescanner.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface SalesDriveApi {

    // Меняем endpoint на правильный и возвращаем метод GET
    @GET("api/order/list/")
    suspend fun getOrderById(
        // API ключ теперь передается как заголовок (Header)
        @Header("Form-Api-Key") apiKey: String,

        // ID заказа передаем как фильтр "от" и "до" с одинаковым значением
        @Query("filter[id][from]") orderIdFrom: String,
        @Query("filter[id][to]") orderIdTo: String,

        // Также запрашиваем товары. Название параметра может быть другим,
        // но оставим 'products' как предположение. Если товары не придут,
        // нужно будет уточнить этот параметр в документации.
        @Query("products") includeProducts: Int = 1
    ): Response<SalesDriveResponse> // Ответ может иметь другую структуру, но пока оставим эту
}