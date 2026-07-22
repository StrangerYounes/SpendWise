package com.corner.myshoppinglist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.corner.myshoppinglist.data.local.entities.MasterItem
import com.corner.myshoppinglist.data.repository.SettingsRepository
import com.corner.myshoppinglist.data.repository.ShoppingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MasterItemViewModel(
    private val repository: ShoppingRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

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
