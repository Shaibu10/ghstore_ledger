package com.example.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.SaleEntity
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.ProductRepository
import com.example.data.repository.SalesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

/**
 * Reactively aggregates metrics from multiple Room repositories to output real-time POS analytics.
 */
class DashboardViewModel(
    private val productRepository: ProductRepository,
    private val salesRepository: SalesRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    // Helper to calculate the start of the current day (00:00)
    private fun getStartOfToday(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    // Combine flows to compute cohesive real-time Success dashboard metrics automatically
    val dashboardState: StateFlow<DashboardUiState> = combine(
        salesRepository.allSales,
        productRepository.allProducts,
        productRepository.lowStockProducts,
        expenseRepository.allExpenses
    ) { salesList: List<SaleEntity>, productsList: List<ProductEntity>, lowStockList: List<ProductEntity>, expensesList: List<ExpenseEntity> ->
        try {
            val todayStart = getStartOfToday()
            val todayEnd = todayStart + (24 * 60 * 60 * 1000)

            // Today's metrics
            val todaySales = salesList.filter { it.createdAt in todayStart until todayEnd }
            val todaySalesCount = todaySales.size
            val todayRevenue = todaySales.sumOf { it.total }

            // General totals
            val totalRevenue = salesList.sumOf { it.total }
            val totalProductsCount = productsList.size
            val lowStockCount = lowStockList.size
            val totalExpenses = expensesList.sumOf { it.amount }
            val netProfit = totalRevenue - totalExpenses

            // Recents limit boundaries
            val recentSales = salesList.take(5)
            val lowStockAlerts = lowStockList.take(5)

            // Dynamic Custom Charts: Last 7 days sales data breakdown
            val last7DaysValues = mutableListOf<Double>()
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            // Calculate daily sums backward from today
            for (i in 6 downTo 0) {
                val dayStart = cal.timeInMillis - (i * 24 * 60 * 60 * 1000L)
                val dayEnd = dayStart + (24 * 60 * 60 * 1000L)
                val daySales = salesList.filter { it.createdAt in dayStart until dayEnd }
                last7DaysValues.add(daySales.sumOf { it.total })
            }

            DashboardUiState.Success(
                todaySalesCount = todaySalesCount,
                todayRevenue = todayRevenue,
                totalRevenue = totalRevenue,
                totalProductsCount = totalProductsCount,
                lowStockCount = lowStockCount,
                totalExpenses = totalExpenses,
                netProfit = netProfit,
                recentSales = recentSales,
                lowStockAlerts = lowStockAlerts,
                weeklySalesValues = last7DaysValues
            )
        } catch (e: Exception) {
            DashboardUiState.Error(e.message ?: "Failed to assemble dashboard analytics")
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState.Loading
    )
}

/**
 * Custom Factory class to construct DashboardViewModel with Room Repository linkages.
 */
class DashboardViewModelFactory(
    private val productRepository: ProductRepository,
    private val salesRepository: SalesRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(productRepository, salesRepository, expenseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
