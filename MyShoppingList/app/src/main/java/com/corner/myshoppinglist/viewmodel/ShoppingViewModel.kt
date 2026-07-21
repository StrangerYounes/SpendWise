package com.corner.myshoppinglist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.corner.myshoppinglist.data.local.dao.ShoppingListWithDetails
import com.corner.myshoppinglist.data.local.entities.ShoppingList
import com.corner.myshoppinglist.data.repository.SettingsRepository
import com.corner.myshoppinglist.data.repository.ShoppingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ShoppingViewModel(
    private val repository: ShoppingRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _shoppingLists = MutableStateFlow<List<ShoppingListWithDetails>>(emptyList())
    val shoppingLists: StateFlow<List<ShoppingListWithDetails>> = _shoppingLists.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchedItems = MutableStateFlow<List<com.corner.myshoppinglist.data.local.entities.ShoppingItem>>(emptyList())
    val searchedItems: StateFlow<List<com.corner.myshoppinglist.data.local.entities.ShoppingItem>> = _searchedItems.asStateFlow()

    val currencySymbol: StateFlow<String> = settingsRepository.settings
        .map { it?.currencySymbol ?: "$" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "$")

    init {
        viewModelScope.launch {
            _searchQuery.collectLatest { query ->
                if (query.isEmpty()) {
                    repository.allShoppingLists.collectLatest {
                        _shoppingLists.value = it
                    }
                    _searchedItems.value = emptyList()
                } else {
                    launch {
                        repository.searchShoppingLists(query).collectLatest {
                            _shoppingLists.value = it
                        }
                    }
                    launch {
                        repository.searchItems(query).collectLatest {
                            _searchedItems.value = it
                        }
                    }
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun addShoppingList(name: String) {
        viewModelScope.launch {
            repository.insertShoppingList(ShoppingList(name = name))
        }
    }

    fun updateShoppingList(shoppingList: ShoppingList) {
        viewModelScope.launch {
            repository.updateShoppingList(shoppingList)
        }
    }

    fun deleteShoppingList(shoppingList: ShoppingList) {
        viewModelScope.launch {
            repository.deleteShoppingList(shoppingList)
        }
    }

    fun renameShoppingList(shoppingList: ShoppingList, newName: String) {
        viewModelScope.launch {
            repository.updateShoppingList(shoppingList.copy(name = newName))
        }
    }

    fun duplicateList(shoppingList: ShoppingList) {
        viewModelScope.launch {
            val items = repository.getItemsForList(shoppingList.id).first()
            repository.duplicateList(shoppingList, items)
        }
    }
}

class ShoppingViewModelFactory(
    private val repository: ShoppingRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShoppingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShoppingViewModel(repository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
