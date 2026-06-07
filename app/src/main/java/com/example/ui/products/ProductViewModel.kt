package com.example.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.entity.CategoryEntity
import com.example.data.entity.ProductEntity
import com.example.data.repository.CategoryRepository
import com.example.data.repository.ProductRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

sealed class ProductUiState {
    object Loading : ProductUiState()
    data class Success(
        val products: List<ProductEntity>,
        val categories: List<CategoryEntity>,
        val isSearching: Boolean = false,
        val errorMessage: String? = null
    ) : ProductUiState()
    data class Error(val message: String) : ProductUiState()
}

class ProductViewModel(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryIdFilter = MutableStateFlow<String?>(null)
    val selectedCategoryIdFilter: StateFlow<String?> = _selectedCategoryIdFilter.asStateFlow()

    // Load and seed categories first, then expose reactive list of products and categories combined
    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())

    init {
        // Seed some standard default Ghanaian categories if empty
        viewModelScope.launch {
            categoryRepository.allCategories.collectLatest { list ->
                if (list.isEmpty()) {
                    val defaultCategories = listOf(
                        CategoryEntity("cat_groceries", "Groceries", "Food stuff, spices, cans and household items"),
                        CategoryEntity("cat_beverages", "Beverages", "Carbonated soft drinks, water, local juices"),
                        CategoryEntity("cat_electronics", "Electronics & Phones", "Electrical appliances, power banks, chargers"),
                        CategoryEntity("cat_pharmacy", "Cosmetics & OTC", "Toiletries, soaps, hair products, medicines"),
                        CategoryEntity("cat_provisions", "Provisions", "Milk, sugar, bread, cereals, milk powder")
                    )
                    defaultCategories.forEach { categoryRepository.saveCategory(it) }
                } else {
                    _categories.value = list
                }
            }
        }
    }

    // Reactive products list filtered by search query and category
    val productUiState: StateFlow<ProductUiState> = combine(
        searchQuery,
        selectedCategoryIdFilter,
        _categories
    ) { query, categoryId, categoryList ->
        Triple(query, categoryId, categoryList)
    }.flatMapLatest { (query, categoryId, categoryList) ->
        val baseFlow = if (query.isBlank()) {
            productRepository.allProducts
        } else {
            productRepository.searchProducts(query)
        }

        baseFlow.map { products ->
            val filteredProducts = if (categoryId != null) {
                products.filter { it.categoryId == categoryId }
            } else {
                products
            }
            ProductUiState.Success(
                products = filteredProducts,
                categories = categoryList,
                isSearching = query.isNotBlank()
            ) as ProductUiState
        }.onStart {
            emit(ProductUiState.Loading)
        }.catch { err ->
            emit(ProductUiState.Error(err.message ?: "Failed to load catalog products"))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProductUiState.Loading
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun selectCategoryFilter(categoryId: String?) {
        _selectedCategoryIdFilter.value = categoryId
    }

    fun saveProduct(
        id: String?,
        barcode: String,
        name: String,
        description: String,
        categoryId: String,
        buyPrice: Double,
        sellPrice: Double,
        quantity: Double,
        reorderLevel: Double,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (name.isBlank()) {
                    onComplete(false)
                    return@launch
                }
                
                // If barcode is empty, generate a systematic code or keep empty
                val finalBarcode = if (barcode.isBlank()) {
                    "200" + String.format("%09d", (100000000..999999999).random())
                } else {
                    barcode
                }

                val product = ProductEntity(
                    id = id ?: UUID.randomUUID().toString(),
                    barcode = finalBarcode,
                    name = name,
                    description = description,
                    categoryId = categoryId,
                    buyPrice = buyPrice,
                    sellPrice = sellPrice,
                    quantity = quantity,
                    reorderLevel = reorderLevel,
                    imageUrl = "",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                productRepository.saveProduct(product)
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun deleteProduct(product: ProductEntity, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                productRepository.deleteProduct(product)
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun addEmptyCategory(name: String, description: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                val cat = CategoryEntity(
                    id = "cat_" + UUID.randomUUID().toString().take(6),
                    name = name,
                    description = description
                )
                categoryRepository.saveCategory(cat)
            }
        }
    }
}

class ProductViewModelFactory(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProductViewModel(productRepository, categoryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
