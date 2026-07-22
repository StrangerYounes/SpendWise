package com.corner.myshoppinglist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.corner.myshoppinglist.data.local.dao.PurchaseHistoryItem
import com.corner.myshoppinglist.data.local.dao.StorePriceDetail
import com.corner.myshoppinglist.data.local.entities.MasterItem
import com.corner.myshoppinglist.data.repository.SettingsRepository
import com.corner.myshoppinglist.data.repository.ShoppingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MasterItemDetail(
    val item: MasterItem,
    val storePrices: List<StorePriceDetail>,
    val purchaseHistory: List<PurchaseHistoryItem>
)

@OptIn(ExperimentalCoroutinesApi::class)
class MasterItemViewModel(
    private val repository: ShoppingRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedItemId = MutableStateFlow<Long?>(null)
    
    val itemDetail: StateFlow<MasterItemDetail?> = _selectedItemId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else {
                repository.allMasterItems.map { list -> list.find { it.id == id } }
                    .flatMapLatest { item ->
                        if (item == null) flowOf(null)
                        else {
                            combine(
                                repository.getStorePricesWithNames(id),
                                repository.getPurchaseHistory(item.name)
                            ) { prices, history ->
                                MasterItemDetail(item, prices, history)
                            }
                        }
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currencySymbol: StateFlow<String> = settingsRepository.settings
        .map { it?.currencySymbol ?: "$" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "$")

    val masterItems: StateFlow<List<MasterItem>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                repository.allMasterItems
            } else {
                repository.searchMasterItems(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun selectItem(itemId: Long?) {
        _selectedItemId.value = itemId
    }

    fun updateMasterItem(item: MasterItem) {
        viewModelScope.launch {
            repository.updateMasterItemManual(item)
        }
    }

    fun addMasterItem(name: String, price: Double) {
        viewModelScope.launch {
            repository.insertMasterItem(MasterItem(name = name, lastPrice = price, averagePrice = price))
        }
    }

    fun deleteMasterItem(item: MasterItem) {
        viewModelScope.launch {
            repository.deleteMasterItem(item)
        }
    }
}

class MasterItemViewModelFactory(
    private val repository: ShoppingRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MasterItemViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MasterItemViewModel(repository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
