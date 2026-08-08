package com.corner.myshoppinglist.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.corner.myshoppinglist.data.local.entities.ShoppingItem
import com.corner.myshoppinglist.data.model.ScannedItem
import com.corner.myshoppinglist.data.repository.ShoppingRepository
import com.corner.myshoppinglist.util.ReceiptParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ReceiptViewModel(private val repository: ShoppingRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ReceiptUiState>(ReceiptUiState.Idle)
    val uiState: StateFlow<ReceiptUiState> = _uiState.asStateFlow()

    private val _scannedItems = MutableStateFlow<List<ScannedItem>>(emptyList())
    val scannedItems: StateFlow<List<ScannedItem>> = _scannedItems.asStateFlow()

    fun processReceiptImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ReceiptUiState.Processing
            try {
                val image = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val result = recognizer.process(image).await()
                
                Log.d("ReceiptViewModel", "--- RAW OCR START ---")
                result.text.lines().forEach { Log.d("ReceiptViewModel", "Line: $it") }
                Log.d("ReceiptViewModel", "--- RAW OCR END ---")
                
                val items = ReceiptParser.parse(result)
                Log.d("ReceiptViewModel", "Parsed Items: ${items.size}")
                _scannedItems.value = items
                _uiState.value = ReceiptUiState.Reviewing
            } catch (e: Exception) {
                _uiState.value = ReceiptUiState.Error(e.message ?: "Failed to process image")
            }
        }
    }

    fun updateScannedItem(updatedItem: ScannedItem) {
        _scannedItems.value = _scannedItems.value.map {
            if (it.id == updatedItem.id) updatedItem else it
        }
    }

    fun toggleItemSelected(itemId: String) {
        _scannedItems.value = _scannedItems.value.map {
            if (it.id == itemId) it.copy(isSelected = !it.isSelected) else it
        }
    }

    fun saveItemsToList(listId: Long, onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = ReceiptUiState.Saving
            val selectedItems = _scannedItems.value.filter { it.isSelected }
            
            selectedItems.forEach { scanned ->
                val shoppingItem = ShoppingItem(
                    listId = listId,
                    itemName = scanned.name,
                    actualPrice = scanned.totalPrice,
                    estimatedPrice = scanned.unitPrice,
                    quantity = scanned.quantity,
                    unit = scanned.unit,
                    purchased = true // Receipt scanning implies purchase
                )
                repository.insertShoppingItem(shoppingItem)
            }
            
            _uiState.value = ReceiptUiState.Success
            onComplete()
        }
    }

    fun reset() {
        _uiState.value = ReceiptUiState.Idle
        _scannedItems.value = emptyList()
    }
}

sealed class ReceiptUiState {
    object Idle : ReceiptUiState()
    object Processing : ReceiptUiState()
    object Reviewing : ReceiptUiState()
    object Saving : ReceiptUiState()
    object Success : ReceiptUiState()
    data class Error(val message: String) : ReceiptUiState()
}

class ReceiptViewModelFactory(private val repository: ShoppingRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReceiptViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReceiptViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
