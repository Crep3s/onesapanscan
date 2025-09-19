package com.example.warehousescanner.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.warehousescanner.R

class HistoryFragment : Fragment() {

    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.history_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        historyAdapter = HistoryAdapter(emptyList())
        recyclerView.adapter = historyAdapter

        viewModel.history.observe(viewLifecycleOwner) { historyList ->
            historyAdapter.updateData(historyList)
        }
    }

    override fun onResume() {
        super.onResume()
        // Обновляем историю каждый раз, когда фрагмент становится видимым
        viewModel.loadHistory()
    }
}