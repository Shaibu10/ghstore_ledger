package com.example.ui.dashboard

import com.example.data.entity.ProductEntity
import com.example.data.entity.SaleEntity

/**
 * Defines the cohesive state models for the POS dashboard metrics.
 */
sealed class DashboardUiState {
    object Loading : DashboardUiState()
    
    data class Success(
        val todaySalesCount: Int,
        val todayRevenue: Double,
        val totalRevenue: Double,
        val totalProductsCount: Int,
        val lowStockCount: Int,
        val totalExpenses: Double,
        val netProfit: Double,
        val recentSales: List<SaleEntity>,
        val lowStockAlerts: List<ProductEntity>,
        val weeklySalesValues: List<Double> // for custom dynamic native analytics charts
    ) : DashboardUiState()

    data class Error(val message: String) : DashboardUiState()
}
