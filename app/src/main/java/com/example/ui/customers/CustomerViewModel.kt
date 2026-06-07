package com.example.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.entity.CustomerEntity
import com.example.data.repository.CustomerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

sealed class CustomerUiState {
    object Loading : CustomerUiState()
    data class Success(
        val customers: List<CustomerEntity>,
        val totalCount: Int
    ) : CustomerUiState()
    data class Error(val message: String) : CustomerUiState()
}

class CustomerViewModel(
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val customerUiState: StateFlow<CustomerUiState> = _searchQuery
        .debounce(100) // Small debounce for lightning fast search updates
        .flatMapLatest { query ->
            if (query.isBlank()) {
                customerRepository.allCustomers
            } else {
                customerRepository.searchCustomers(query)
            }
        }
        .map { list ->
            CustomerUiState.Success(
                customers = list,
                totalCount = list.size
            ) as CustomerUiState
        }
        .onStart {
            emit(CustomerUiState.Loading)
        }
        .catch { err ->
            emit(CustomerUiState.Error(err.message ?: "Failed to load customers"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CustomerUiState.Loading
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun addCustomer(
        name: String,
        phone: String,
        email: String,
        address: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (name.isBlank() || phone.isBlank()) {
                    onComplete(false)
                    return@launch
                }

                val customer = CustomerEntity(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    phone = phone.trim(),
                    email = email.trim(),
                    address = address.trim(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                customerRepository.saveCustomer(customer)
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun updateCustomer(
        id: String,
        name: String,
        phone: String,
        email: String,
        address: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (name.isBlank() || phone.isBlank()) {
                    onComplete(false)
                    return@launch
                }

                val current = customerRepository.getCustomerById(id)
                if (current != null) {
                    val updated = current.copy(
                        name = name.trim(),
                        phone = phone.trim(),
                        email = email.trim(),
                        address = address.trim(),
                        updatedAt = System.currentTimeMillis()
                    )
                    customerRepository.saveCustomer(updated)
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun deleteCustomer(customer: CustomerEntity, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                customerRepository.deleteCustomer(customer)
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }
}

class CustomerViewModelFactory(
    private val customerRepository: CustomerRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CustomerViewModel(customerRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
