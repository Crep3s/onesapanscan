// В файле: app/src/main/java/com/example/warehousescanner/network/ApiClient.kt

package com.example.warehousescanner.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // !!! ЗАМЕНИТЕ "your-account" НА ВАШ ДОМЕН В SALESDRIVE !!!
    private const val BASE_URL = "https://biks.salesdrive.me/"

    val instance: SalesDriveApi by lazy {
        // Логгер для отладки - будет показывать тела запросов и ответов в Logcat
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(SalesDriveApi::class.java)
    }
}