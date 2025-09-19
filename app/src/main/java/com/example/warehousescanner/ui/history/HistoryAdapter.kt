package com.example.warehousescanner.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.warehousescanner.R
import com.example.warehousescanner.database.OrderHistoryEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private var items: List<OrderHistoryEntity>
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val orderIdText: TextView = view.findViewById(R.id.order_id_text)
        val timestampText: TextView = view.findViewById(R.id.timestamp_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.orderIdText.text = "Заказ №${item.orderId}"
        holder.timestampText.text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            .format(Date(item.timestamp))
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<OrderHistoryEntity>) {
        items = newItems
        notifyDataSetChanged()
    }
}