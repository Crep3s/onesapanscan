// В файле: app/src/main/java/com/example/warehousescanner/database/HistoryDatabase.kt
package com.example.warehousescanner.database

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.warehousescanner.network.Product
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// --- 1. Описание таблицы в базе данных ---
@Entity(tableName = "order_history")
data class OrderHistoryEntity(
    @PrimaryKey val orderId: String,
    val timestamp: Long,
    val products: List<Product>,
    val comment: String?
)

// --- 2. Конвертер для списка товаров ---
class ProductListConverter {
    @TypeConverter
    fun fromProductList(products: List<Product>?): String? {
        return Gson().toJson(products)
    }

    @TypeConverter
    fun toProductList(json: String?): List<Product>? {
        val type = object : TypeToken<List<Product>>() {}.type
        return Gson().fromJson(json, type)
    }
}

// --- 3. DAO (Data Access Object) - интерфейс для запросов к базе ---
@androidx.room.Dao
interface OrderHistoryDao {
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderHistoryEntity)

    @androidx.room.Query("SELECT * FROM order_history ORDER BY timestamp DESC")
    suspend fun getAllOrders(): List<OrderHistoryEntity>
}

// --- 4. Основной класс базы данных ---
@Database(entities = [OrderHistoryEntity::class], version = 1)
@TypeConverters(ProductListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderHistoryDao(): OrderHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "warehouse_scanner_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}