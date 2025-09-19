package com.example.warehousescanner.ui.scanner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.warehousescanner.ChecklistActivity
import com.example.warehousescanner.R

class ScannerFragment : Fragment() {

    companion object {
        private const val SCANNER_INPUT_TIMEOUT = 50L
    }

    private lateinit var orderIdInput: EditText
    private lateinit var loadOrderButton: Button

    private val barcodeStringBuilder = StringBuilder()
    private val handler = Handler(Looper.getMainLooper())
    private val processBarcodeRunnable = Runnable { processBarcode() }

    private val checklistLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val newOrderId = result.data?.getStringExtra(ChecklistActivity.RESULT_EXTRA_NEW_ORDER_ID)
            if (!newOrderId.isNullOrEmpty()) {
                launchChecklistActivity(newOrderId)
            }
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            val errorMessage = result.data?.getStringExtra(ChecklistActivity.RESULT_EXTRA_ERROR_MESSAGE)
            if (!errorMessage.isNullOrEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Загружаем layout для этого фрагмента
        return inflater.inflate(R.layout.fragment_scanner, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        orderIdInput = view.findViewById(R.id.order_id_input)
        loadOrderButton = view.findViewById(R.id.load_order_button)

        loadOrderButton.setOnClickListener {
            val orderId = orderIdInput.text.toString().trim()
            if (orderId.isNotEmpty()) {
                launchChecklistActivity(orderId)
            } else {
                Toast.makeText(requireContext(), "Введите номер заказа", Toast.LENGTH_SHORT).show()
            }
        }

        // Перехватываем события клавиатуры на уровне View фрагмента
        view.isFocusableInTouchMode = true
        view.requestFocus()
        view.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    handler.removeCallbacks(processBarcodeRunnable)
                    processBarcode()
                    return@setOnKeyListener true
                }

                val char = event.unicodeChar.toChar()
                if (char.isLetterOrDigit() || char in "-/\\_.") {
                    barcodeStringBuilder.append(char)
                }

                handler.removeCallbacks(processBarcodeRunnable)
                handler.postDelayed(processBarcodeRunnable, SCANNER_INPUT_TIMEOUT)
            }
            return@setOnKeyListener false
        }
    }

    override fun onResume() {
        super.onResume()
        clearInput()
        gainFocusAndHideKeyboard()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(processBarcodeRunnable)
    }

    private fun processBarcode() {
        handler.removeCallbacks(processBarcodeRunnable)
        val builderContent = barcodeStringBuilder.toString().trim()
        val editTextContent = orderIdInput.text.toString().trim()
        val barcode = if (builderContent.isNotEmpty()) builderContent else editTextContent

        barcodeStringBuilder.clear()
        orderIdInput.text.clear()

        if (barcode.isNotEmpty()) {
            launchChecklistActivity(barcode)
        }
    }

    private fun clearInput() {
        orderIdInput.text.clear()
        barcodeStringBuilder.clear()
        handler.removeCallbacks(processBarcodeRunnable)
    }

    private fun gainFocusAndHideKeyboard() {
        orderIdInput.requestFocus()
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(orderIdInput.windowToken, 0)
    }

    private fun launchChecklistActivity(orderId: String) {
        orderIdInput.text.clear()
        val intent = Intent(requireContext(), ChecklistActivity::class.java).apply {
            putExtra("ORDER_ID", orderId)
        }
        checklistLauncher.launch(intent)
    }
}