package com.corner.myshoppinglist.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.corner.myshoppinglist.data.local.entities.ShoppingItem
import com.corner.myshoppinglist.data.model.ScannedItem
import com.corner.myshoppinglist.data.repository.SettingsRepository
import com.corner.myshoppinglist.data.repository.ShoppingRepository
import com.corner.myshoppinglist.util.ReceiptParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.corner.myshoppinglist.util.GeminiReceiptClient
import kotlinx.coroutines.CancellationException
import com.corner.myshoppinglist.BuildConfig


class ReceiptViewModel(
    private val repository: ShoppingRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReceiptUiState>(ReceiptUiState.Idle)
    val uiState: StateFlow<ReceiptUiState> = _uiState.asStateFlow()

    private val _scannedItems = MutableStateFlow<List<ScannedItem>>(emptyList())
    val scannedItems: StateFlow<List<ScannedItem>> = _scannedItems.asStateFlow()

    private val _conversionRate = MutableStateFlow(1.0)
    val conversionRate: StateFlow<Double> = _conversionRate.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.first()?.let {
                _conversionRate.value = it.lastConversionRate
            }
        }
    }

    fun processReceiptImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ReceiptUiState.Processing
            try {
                var items: List<ScannedItem> = emptyList()

                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isNotBlank()) {
                    try {
                        items = GeminiReceiptClient.parseReceipt(context, uri, apiKey)
                        Log.d("ReceiptViewModel", "Gemini parsed ${items.size} items")
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w("ReceiptViewModel", "Gemini failed, using ML Kit: ${e.message}")
                    }
                }

                if (items.isEmpty()) {
                    val image = InputImage.fromFilePath(context, uri)
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    val result = recognizer.process(image).await()
                    Log.d("ReceiptViewModel", "--- RAW OCR START ---")
                    result.text.lines().forEach { Log.d("ReceiptViewModel", "Line: $it") }
                    Log.d("ReceiptViewModel", "--- RAW OCR END ---")
                    items = ReceiptParser.parse(result)
                }

                _scannedItems.value = items
                _uiState.value = ReceiptUiState.Reviewing
            } catch (e: CancellationException) {
                throw e
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

    fun removeItem(itemId: String) {
        _scannedItems.value = _scannedItems.value.filter { it.id != itemId }
    }

    fun setConversionRate(rate: Double) {
        _conversionRate.value = rate
        viewModelScope.launch {
            val currentSettings = settingsRepository.settings.first() ?: com.corner.myshoppinglist.data.local.entities.AppSettings()
            settingsRepository.updateSettings(currentSettings.copy(lastConversionRate = rate))
        }
    }

    fun saveItemsToList(listId: Long, onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = ReceiptUiState.Saving
            val selectedItems = _scannedItems.value.filter { it.isSelected }
            val rate = _conversionRate.value
            
            selectedItems.forEach { scanned ->
                val totalInBaseCurrency = (scanned.totalPrice ?: 0.0) / rate
                val unitPriceInBaseCurrency = totalInBaseCurrency / scanned.quantity
                
                // Round to 2 decimal places before saving to DB
                val roundedPrice = Math.round(unitPriceInBaseCurrency * 100.0) / 100.0
                val roundedQty = Math.round(scanned.quantity * 100.0) / 100.0

                val shoppingItem = ShoppingItem(
                    listId = listId,
                    itemName = scanned.name,
                    actualPrice = roundedPrice,
                    estimatedPrice = roundedPrice,
                    quantity = roundedQty,
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
        // We don't reset conversion rate here because the user wants to remember it
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

class ReceiptViewModelFactory(
    private val repository: ShoppingRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReceiptViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReceiptViewModel(repository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
