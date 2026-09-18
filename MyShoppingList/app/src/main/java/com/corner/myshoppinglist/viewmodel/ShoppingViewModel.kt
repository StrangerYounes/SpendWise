package com.corner.myshoppinglist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.corner.myshoppinglist.data.local.dao.ShoppingListWithDetails
import com.corner.myshoppinglist.data.local.entities.ShoppingList
import com.corner.myshoppinglist.data.repository.SettingsRepository
import com.corner.myshoppinglist.data.repository.ShoppingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ShoppingViewModel(
    private val repository: ShoppingRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val shoppingLists: StateFlow<List<ShoppingListWithDetails>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                repository.allShoppingLists
            } else {
                repository.searchShoppingLists(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<com.corner.myshoppinglist.data.local.entities.Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchedItems: StateFlow<List<com.corner.myshoppinglist.data.local.entities.ShoppingItem>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                flowOf(emptyList())
            } else {
                repository.searchItems(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currencySymbol: StateFlow<String> = settingsRepository.settings
        .map { it?.currencySymbol ?: "$" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "$")

    private val _dateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val dateRange: StateFlow<Pair<Long, Long>?> = _dateRange.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val itemStats = _dateRange.flatMapLatest { range ->
        if (range == null) repository.itemStats
        else repository.getItemStats(range.first, range.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val expensiveItems = _dateRange.flatMapLatest { range ->
        if (range == null) repository.expensiveItems
        else repository.getExpensiveItems(range.first, range.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val spentPerMonth = _dateRange.flatMapLatest { range ->
        if (range == null) repository.spentPerMonth
        else repository.getSpentPerMonth(range.first, range.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val spentByCategory = _dateRange.flatMapLatest { range ->
        if (range == null) repository.spentByCategory
        else repository.getSpentByCategory(range.first, range.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val spentByStore = _dateRange.flatMapLatest { range ->
        if (range == null) repository.spentByStore
        else repository.getSpentByStore(range.first, range.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun addShoppingList(name: String, categoryId: Long? = null) {
        viewModelScope.launch {
            repository.insertShoppingList(ShoppingList(name = name, categoryId = categoryId))
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

    fun setListCategory(shoppingList: ShoppingList, categoryId: Long?) {
        viewModelScope.launch {
            repository.updateShoppingList(shoppingList.copy(categoryId = categoryId))
        }
    }

    fun setDateRange(startDate: Long?, endDate: Long?) {
        if (startDate == null || endDate == null) {
            _dateRange.value = null
        } else {
            _dateRange.value = Pair(startDate, endDate)
        }
    }

    fun addCategory(name: String, color: Int) {
        viewModelScope.launch {
            repository.insertCategory(com.corner.myshoppinglist.data.local.entities.Category(name = name, color = color))
        }
    }

    fun updateCategory(category: com.corner.myshoppinglist.data.local.entities.Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(category: com.corner.myshoppinglist.data.local.entities.Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
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
