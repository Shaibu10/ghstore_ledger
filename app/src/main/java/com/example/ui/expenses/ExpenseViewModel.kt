package com.example.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.entity.ExpenseEntity
import com.example.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

sealed class ExpenseUiState {
    object Loading : ExpenseUiState()
    data class Success(
        val expenses: List<ExpenseEntity>,
        val totalExpenseAmount: Double,
        val selectedCategoryFilter: String? = null
    ) : ExpenseUiState()
    data class Error(val message: String) : ExpenseUiState()
}

class ExpenseViewModel(
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Pre-seeded expense categories matching typical shop operational overheads
    val expenseCategories = listOf(
        "Rent & Store Leases",
        "Utilities (ECG/Water)",
        "Staff Salaries",
        "Transport & Freight",
        "Inventory Stock Purchase",
        "Marketing & Printing",
        "General Maintenance/Repairs",
        "Others"
    )

    val expenseUiState: StateFlow<ExpenseUiState> = combine(
        expenseRepository.allExpenses,
        _selectedCategoryFilter,
        _searchQuery
    ) { expenses, category, query ->
        Triple(expenses, category, query)
    }.map { (expenses, category, query) ->
        val filteredByCategory = if (category != null) {
            expenses.filter { it.description.startsWith("[CAT:$category]") || it.title.contains(category, ignoreCase = true) }
        } else {
            expenses
        }

        val filteredByQuery = if (query.isNotBlank()) {
            filteredByCategory.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
            }
        } else {
            filteredByCategory
        }

        val totalAmount = filteredByQuery.sumOf { it.amount }

        ExpenseUiState.Success(
            expenses = filteredByQuery,
            totalExpenseAmount = totalAmount,
            selectedCategoryFilter = category
        ) as ExpenseUiState
    }.onStart {
        emit(ExpenseUiState.Loading)
    }.catch { err ->
        emit(ExpenseUiState.Error(err.message ?: "Failed to load expenses"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExpenseUiState.Loading
    )

    fun selectCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = category
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun addExpense(
        title: String,
        amount: Double,
        category: String,
        description: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (title.isBlank() || amount <= 0.0) {
                    onComplete(false)
                    return@launch
                }

                // Code Category prefix in description to cleanly classify it without structural schema changes
                val finalDesc = "[CAT:$category] $description"

                val expense = ExpenseEntity(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    amount = amount,
                    description = finalDesc,
                    date = System.currentTimeMillis()
                )

                expenseRepository.saveExpense(expense)
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun deleteExpense(expense: ExpenseEntity, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                expenseRepository.deleteExpense(expense)
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }
}

class ExpenseViewModelFactory(
    private val expenseRepository: ExpenseRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(expenseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
