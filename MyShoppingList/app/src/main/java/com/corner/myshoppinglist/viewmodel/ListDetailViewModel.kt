package com.corner.myshoppinglist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.corner.myshoppinglist.data.local.dao.*
import com.corner.myshoppinglist.data.local.entities.*
import com.corner.myshoppinglist.data.repository.SettingsRepository
import com.corner.myshoppinglist.data.repository.ShoppingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ItemSuggestion(
    val masterItem: MasterItem,
    val bestPrice: Double? = null,
    val bestStoreName: String? = null,
    val listStorePrice: Double? = null
)

class ListDetailViewModel(
    private val repository: ShoppingRepository,
    private val settingsRepository: SettingsRepository,
    private val listId: Long
) : ViewModel() {

    private val _shoppingList = MutableStateFlow<ShoppingList?>(null)
    val shoppingList: StateFlow<ShoppingList?> = _shoppingList.asStateFlow()

    private val _items = MutableStateFlow<List<ShoppingItem>>(emptyList())
    val items: StateFlow<List<ShoppingItem>> = _items.asStateFlow()

    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    val photos: StateFlow<List<Photo>> = _photos.asStateFlow()

    private val _stores = MutableStateFlow<List<Store>>(emptyList())
    val stores: StateFlow<List<Store>> = _stores.asStateFlow()

    private val _suggestions = MutableStateFlow<List<ItemSuggestion>>(emptyList())
    val suggestions: StateFlow<List<ItemSuggestion>> = _suggestions.asStateFlow()

    val currencySymbol: StateFlow<String> = settingsRepository.settings
        .map { it?.currencySymbol ?: "$" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "$")

    val currentStore: StateFlow<Store?> = combine(_shoppingList, _stores) { list, stores ->
        stores.find { it.id == list?.storeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val estimatedTotal: StateFlow<Double> = _items.map { items ->
        items.sumOf { (it.estimatedPrice ?: 0.0) * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val actualTotal: StateFlow<Double> = _items.map { items ->
        items.filter { it.purchased }.sumOf { (it.actualPrice ?: it.estimatedPrice ?: 0.0) * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        viewModelScope.launch {
            _shoppingList.value = repository.getShoppingListById(listId)
            
            repository.getItemsForList(listId).collectLatest {
                _items.value = it
            }
        }
        viewModelScope.launch {
            repository.getPhotosForList(listId).collectLatest {
                _photos.value = it
            }
        }
        viewModelScope.launch {
            repository.allStores.collectLatest {
                _stores.value = it
            }
        }
    }

    fun searchSuggestions(query: String) {
        viewModelScope.launch {
            if (query.length >= 1) {
                repository.searchMasterItems(query).collectLatest { masterItems ->
                    val listStoreId = _shoppingList.value?.storeId
                    val suggested = masterItems.map { master ->
                        val cheapest = repository.getCheapestPriceForItem(master.id)
                        val cheapestStore = cheapest?.let { repository.getStoreById(it.storeId) }
                        val atListStore = listStoreId?.let { repository.getPriceForItemAtStore(master.id, it) }
                        
                        ItemSuggestion(
                            masterItem = master,
                            bestPrice = cheapest?.lastPrice,
                            bestStoreName = cheapestStore?.name,
                            listStorePrice = atListStore?.lastPrice
                        )
                    }
                    _suggestions.value = suggested
                }
            } else {
                _suggestions.value = emptyList()
            }
        }
    }

    fun addItem(
        name: String,
        estimatedPrice: Double? = null,
        quantity: Double = 1.0,
        unit: String? = null,
        notes: String? = null
    ) {
        viewModelScope.launch {
            val orderIndex = (_items.value.maxOfOrNull { it.orderIndex } ?: -1) + 1
            repository.insertShoppingItem(
                ShoppingItem(
                    listId = listId,
                    itemName = name,
                    estimatedPrice = estimatedPrice,
                    quantity = quantity,
                    unit = unit,
                    notes = notes,
                    orderIndex = orderIndex
                )
            )
        }
    }

    fun updateItem(item: ShoppingItem) {
        viewModelScope.launch {
            repository.updateShoppingItem(item)
        }
    }

    fun deleteItem(item: ShoppingItem) {
        viewModelScope.launch {
            repository.deleteShoppingItem(item)
        }
    }

    fun clearAllItems() {
        viewModelScope.launch {
            _items.value.forEach {
                repository.deleteShoppingItem(it)
            }
        }
    }

    fun addPhoto(uri: String) {
        viewModelScope.launch {
            repository.insertPhoto(Photo(shoppingListId = listId, uri = uri))
        }
    }

    fun deletePhoto(photo: Photo) {
        viewModelScope.launch {
            repository.deletePhoto(photo)
        }
    }

    fun completeList() {
        viewModelScope.launch {
            _shoppingList.value?.let {
                repository.updateShoppingList(it.copy(isCompleted = true, completedDate = System.currentTimeMillis()))
                _shoppingList.value = repository.getShoppingListById(listId)
            }
        }
    }

    fun uncompleteList() {
        viewModelScope.launch {
            _shoppingList.value?.let {
                repository.updateShoppingList(it.copy(isCompleted = false, completedDate = null))
                _shoppingList.value = repository.getShoppingListById(listId)
            }
        }
    }

    fun renameList(newName: String) {
        viewModelScope.launch {
            _shoppingList.value?.let {
                repository.updateShoppingList(it.copy(name = newName))
                _shoppingList.value = repository.getShoppingListById(listId)
            }
        }
    }

    fun updateListStore(storeId: Long?) {
        viewModelScope.launch {
            _shoppingList.value?.let {
                repository.updateShoppingList(it.copy(storeId = storeId))
                _shoppingList.value = repository.getShoppingListById(listId)
            }
        }
    }

    fun addStore(name: String) {
        viewModelScope.launch {
            repository.insertStore(Store(name = name))
        }
    }
}

class ListDetailViewModelFactory(
    private val repository: ShoppingRepository,
    private val settingsRepository: SettingsRepository,
    private val listId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ListDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ListDetailViewModel(repository, settingsRepository, listId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
