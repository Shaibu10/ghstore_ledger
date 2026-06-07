package com.example.ui.navigation

/**
 * Centered Navigation Routes for the Gh POS System screens.
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object ProductManagement : Screen("products")
    object SalesCheckout : Screen("checkout")
    object CustomerManagement : Screen("customers")
    object Reports : Screen("reports")
    object Expenses : Screen("expenses")
}
