package com.example.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.SaleEntity
import com.example.data.entity.SaleItemEntity
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.ProductRepository
import com.example.data.repository.SalesRepository
import kotlinx.coroutines.flow.*

sealed class ReportsUiState {
    object Loading : ReportsUiState()
    data class Success(
        val totalRevenue: Double,
        val totalExpenses: Double,
        val netProfit: Double,
        val totalInvoices: Int,
        val avgInvoiceValue: Double,
        val paymentMethodShare: Map<String, Double>,
        val topProducts: List<TopProductItem>,
        val expenseBreakdown: Map<String, Double>
    ) : ReportsUiState()
    data class Error(val message: String) : ReportsUiState()
}

data class TopProductItem(
    val productId: String,
    val productName: String,
    val quantitySold: Double,
    val revenueGenerated: Double
)

class ReportsViewModel(
    private val salesRepository: SalesRepository,
    private val productRepository: ProductRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    // Default predefined categories matching our ExpenseViewModel
    private val defaultCategories = listOf(
        "Rent & Store Leases",
        "Utilities (ECG/Water)",
        "Staff Salaries",
        "Transport & Freight",
        "Inventory Stock Purchase",
        "Marketing & Printing",
        "General Maintenance/Repairs",
        "Others"
    )

    val reportsUiState: StateFlow<ReportsUiState> = combine(
        salesRepository.allSales,
        salesRepository.allSaleItems,
        productRepository.allProducts,
        expenseRepository.allExpenses
    ) { sales, saleItems, products, expenses ->
        try {
            val totalRevenue = sales.sumOf { it.total }
            val totalExpenses = expenses.sumOf { it.amount }
            val netProfit = totalRevenue - totalExpenses
            val totalInvoices = sales.size
            val avgInvoiceValue = if (totalInvoices > 0) totalRevenue / totalInvoices else 0.0

            // 1. Payment Method Share
            val paymentShare = sales.groupBy { it.paymentMethod }
                .mapValues { entry -> entry.value.sumOf { it.total } }

            // 2. Top Selling Products
            val productMap = products.associateBy { it.id }
            val topProducts = saleItems.groupBy { it.productId }
                .map { (productId, items) ->
                    val name = productMap[productId]?.name ?: "Deleted / Custom Product"
                    val qty = items.sumOf { it.quantity }
                    val rev = items.sumOf { it.subtotal }
                    TopProductItem(
                        productId = productId,
                        productName = name,
                        quantitySold = qty,
                        revenueGenerated = rev
                    )
                }
                .sortedByDescending { it.quantitySold }
                .take(10) // Top 10 selling lines

            // 3. Operating Cost Breakdown Group by Category Prefix
            val expenseShare = mutableMapOf<String, Double>()
            expenses.forEach { expense ->
                val categoryName = parseExpenseCategory(expense.description)
                val currentAmt = expenseShare[categoryName] ?: 0.0
                expenseShare[categoryName] = currentAmt + expense.amount
            }

            ReportsUiState.Success(
                totalRevenue = totalRevenue,
                totalExpenses = totalExpenses,
                netProfit = netProfit,
                totalInvoices = totalInvoices,
                avgInvoiceValue = avgInvoiceValue,
                paymentMethodShare = paymentShare,
                topProducts = topProducts,
                expenseBreakdown = expenseShare
            )
        } catch (e: Exception) {
            ReportsUiState.Error(e.message ?: "Failed to generate dynamic ledger sheets")
        }
    }.onStart {
        emit(ReportsUiState.Loading)
    }.catch { err ->
        emit(ReportsUiState.Error(err.message ?: "Critical database pipeline aggregate failure"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState.Loading
    )

    private fun parseExpenseCategory(description: String): String {
        if (description.startsWith("[CAT:")) {
            val closeBracket = description.indexOf("]")
            if (closeBracket != -1) {
                return description.substring(5, closeBracket)
            }
        }
        return "Others"
    }
}

class ReportsViewModelFactory(
    private val salesRepository: SalesRepository,
    private val productRepository: ProductRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportsViewModel(salesRepository, productRepository, expenseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
