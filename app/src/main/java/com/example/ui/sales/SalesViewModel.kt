package com.example.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.entity.CustomerEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.SaleEntity
import com.example.data.entity.SaleItemEntity
import com.example.data.repository.CustomerRepository
import com.example.data.repository.ProductRepository
import com.example.data.repository.SalesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

sealed class CheckoutState {
    object Idle : CheckoutState()
    object Loading : CheckoutState()
    data class Success(val sale: SaleEntity, val items: List<SaleItemEntity>) : CheckoutState()
    data class Error(val message: String) : CheckoutState()
}

class SalesViewModel(
    private val productRepository: ProductRepository,
    private val salesRepository: SalesRepository,
    private val customerRepository: CustomerRepository,
    private val currentCashierId: String = "Staff_General"
) : ViewModel() {

    // Product search results
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredProducts: StateFlow<List<ProductEntity>> = combine(
        _searchQuery,
        productRepository.allProducts
    ) { query, list ->
        if (query.isBlank()) {
            list
        } else {
            list.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.barcode.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart mapping: ProductEntity -> Quantity
    private val _cart = MutableStateFlow<Map<ProductEntity, Double>>(emptyMap())
    val cart: StateFlow<Map<ProductEntity, Double>> = _cart.asStateFlow()

    // Customer loyalty matching
    val customerList: StateFlow<List<CustomerEntity>> = customerRepository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Seed some customer details if empty
        viewModelScope.launch {
            customerRepository.allCustomers.collectLatest { list ->
                if (list.isEmpty()) {
                    val demoCustomers = listOf(
                        CustomerEntity("cust_1", "Kofi Mensah", "0244123456", "kofi@mensahgh.com", "East Legon, Accra"),
                        CustomerEntity("cust_2", "Ama Serwaa", "0558987654", "ama@serwaa.com", "Adum, Kumasi"),
                        CustomerEntity("cust_3", "Yaw Boateng", "0201112223", "yaw@boateng.com", "Takoradi")
                    )
                    demoCustomers.forEach { customerRepository.saveCustomer(it) }
                }
            }
        }
    }

    private val _selectedCustomer = MutableStateFlow<CustomerEntity?>(null)
    val selectedCustomer: StateFlow<CustomerEntity?> = _selectedCustomer.asStateFlow()

    private val _paymentMethod = MutableStateFlow("CASH") // CASH, MOBILE_MONEY, CARD
    val paymentMethod: StateFlow<String> = _paymentMethod.asStateFlow()

    private val _discount = MutableStateFlow(0.0) // Flat discount (GH₵)
    val discount: StateFlow<Double> = _discount.asStateFlow()

    private val _taxRate = MutableStateFlow(0.0) // Tax percentage e.g. 0% or 15% for VAT
    val taxRate: StateFlow<Double> = _taxRate.asStateFlow()

    // Calculations Flow
    val subtotal: StateFlow<Double> = cart.map { map ->
        map.entries.sumOf { (product, qty) -> product.sellPrice * qty }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val total: StateFlow<Double> = combine(subtotal, discount, taxRate) { sub, disc, taxPct ->
        val runningSub = (sub - disc).coerceAtLeast(0.0)
        val taxAmount = runningSub * (taxPct / 100.0)
        runningSub + taxAmount
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Checkout UI operation state
    private val _checkoutState = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
    val checkoutState: StateFlow<CheckoutState> = _checkoutState.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun addToCart(product: ProductEntity) {
        val current = _cart.value.toMutableMap()
        current[product] = (current[product] ?: 0.0) + 1.0
        _cart.value = current
    }

    fun updateQuantity(product: ProductEntity, quantity: Double) {
        val current = _cart.value.toMutableMap()
        if (quantity <= 0.0) {
            current.remove(product)
        } else {
            current[product] = quantity
        }
        _cart.value = current
    }

    fun removeFromCart(product: ProductEntity) {
        val current = _cart.value.toMutableMap()
        current.remove(product)
        _cart.value = current
    }

    fun selectCustomer(customer: CustomerEntity?) {
        _selectedCustomer.value = customer
    }

    fun updatePaymentMethod(method: String) {
        _paymentMethod.value = method
    }

    fun updateDiscount(amount: Double) {
        _discount.value = amount
    }

    fun updateTaxRate(rate: Double) {
        _taxRate.value = rate
    }

    fun clearCart() {
        _cart.value = emptyMap()
        _selectedCustomer.value = null
        _discount.value = 0.0
        _taxRate.value = 0.0
        _checkoutState.value = CheckoutState.Idle
    }

    fun checkoutCart() {
        if (_cart.value.isEmpty()) {
            _checkoutState.value = CheckoutState.Error("Shopping cart is empty.")
            return
        }

        viewModelScope.launch {
            _checkoutState.value = CheckoutState.Loading
            try {
                val randInvNumber = "INV" + System.currentTimeMillis().toString().takeLast(6)
                val saleId = UUID.randomUUID().toString()

                val saleEntity = SaleEntity(
                    id = saleId,
                    invoiceNumber = randInvNumber,
                    customerId = _selectedCustomer.value?.id,
                    subtotal = subtotal.value,
                    discount = discount.value,
                    tax = (subtotal.value - discount.value).coerceAtLeast(0.0) * (taxRate.value / 100.0),
                    total = total.value,
                    paymentMethod = _paymentMethod.value,
                    cashierId = currentCashierId,
                    createdAt = System.currentTimeMillis()
                )

                val saleItems = _cart.value.map { (product, quantity) ->
                    SaleItemEntity(
                        id = UUID.randomUUID().toString(),
                        saleId = saleId,
                        productId = product.id,
                        quantity = quantity,
                        price = product.sellPrice,
                        subtotal = product.sellPrice * quantity
                    )
                }

                // Call unified checkout coordinate
                val success = salesRepository.checkout(saleEntity, saleItems)
                if (success) {
                    _checkoutState.value = CheckoutState.Success(saleEntity, saleItems)
                } else {
                    _checkoutState.value = CheckoutState.Error("Internal checkout query failed. Check logs.")
                }
            } catch (e: Exception) {
                _checkoutState.value = CheckoutState.Error(e.message ?: "An unexpected error occurred during checkout.")
            }
        }
    }

    fun resetCheckoutState() {
        _checkoutState.value = CheckoutState.Idle
    }
}

class SalesViewModelFactory(
    private val productRepository: ProductRepository,
    private val salesRepository: SalesRepository,
    private val customerRepository: CustomerRepository,
    private val currentCashierId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SalesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SalesViewModel(productRepository, salesRepository, customerRepository, currentCashierId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
