package com.example.warehousescanner.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.warehousescanner.database.AppDatabase
import com.example.warehousescanner.database.OrderHistoryEntity
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).orderHistoryDao()

    private val _history = MutableLiveData<List<OrderHistoryEntity>>()
    val history: LiveData<List<OrderHistoryEntity>> = _history

    fun loadHistory() {
        viewModelScope.launch {
            _history.value = dao.getAllOrders()
        }
    }
}