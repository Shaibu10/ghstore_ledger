package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.data.AppDatabase
import com.example.data.CustomerRepository
import com.example.data.ExpenseRepository
import com.example.data.SaleRepository
import com.example.data.ProductRepository
import com.example.data.LoanRepository
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.customers.CustomerManagementScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.expenses.ExpensesScreen
import com.example.ui.reports.ReportsScreen
import com.example.ui.sales.SalesScreen
import com.example.ui.theme.StoreLedgerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Mandatory edge to edge integration matching Material 3 requirements
        enableEdgeToEdge()

        // 1. Initialize local persistent database, DAOs & Repositories
        val database = AppDatabase.getDatabase(this)
        val customerRepo = CustomerRepository(database.customerDao())
        val saleRepo = SaleRepository(database.saleDao())
        val expenseRepo = ExpenseRepository(database.expenseDao())
        val productRepo = ProductRepository(database.productDao())
        val loanRepo = LoanRepository(database.loanDao())

        // 2. Instantiate global MainViewModel using custom factory
        val viewModel: MainViewModel by viewModels {
            MainViewModelFactory(customerRepo, saleRepo, expenseRepo, productRepo, loanRepo)
        }

        setContent {
            StoreLedgerTheme {
                // Safe state-backed lightweight custom router
                var selectedTab by remember { mutableStateOf("dashboard") }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(), // Ensures full edge-to-edge support without screen-notch clashing
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.testTag("main_navigation_bar")
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == "dashboard",
                                onClick = { selectedTab = "dashboard" },
                                label = { Text("Dashboard") },
                                icon = { Icon(Icons.Default.GridView, contentDescription = "Dashboard Screen Icon") },
                                modifier = Modifier.testTag("nav_tab_dashboard")
                            )
                            NavigationBarItem(
                                selected = selectedTab == "customers",
                                onClick = { selectedTab = "customers" },
                                label = { Text("Directory") },
                                icon = { Icon(Icons.Default.Group, contentDescription = "Customers List Icon") },
                                modifier = Modifier.testTag("nav_tab_customers")
                            )
                            NavigationBarItem(
                                selected = selectedTab == "sales",
                                onClick = { selectedTab = "sales" },
                                label = { Text("Sales") },
                                icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Sales Module Icon") },
                                modifier = Modifier.testTag("nav_tab_sales")
                            )
                            NavigationBarItem(
                                selected = selectedTab == "expenses",
                                onClick = { selectedTab = "expenses" },
                                label = { Text("Expenses") },
                                icon = { Icon(Icons.Default.Payments, contentDescription = "Expenses Module Icon") },
                                modifier = Modifier.testTag("nav_tab_expenses")
                            )
                            NavigationBarItem(
                                selected = selectedTab == "reports",
                                onClick = { selectedTab = "reports" },
                                label = { Text("Reports") },
                                icon = { Icon(Icons.Default.Analytics, contentDescription = "Analytical Reports Icon") },
                                modifier = Modifier.testTag("nav_tab_reports")
                            )
                        }
                    }
                ) { innerPadding ->
                    val contentModifier = Modifier.padding(innerPadding)
                    
                    // Render the corresponding fragment/composable screen
                    when (selectedTab) {
                        "dashboard" -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToSales = { selectedTab = "sales" },
                            onNavigateToExpenses = { selectedTab = "expenses" },
                            onNavigateToCustomers = { selectedTab = "customers" },
                            modifier = contentModifier
                        )
                        "customers" -> CustomerManagementScreen(
                            viewModel = viewModel,
                            modifier = contentModifier
                        )
                        "sales" -> SalesScreen(
                            viewModel = viewModel,
                            modifier = contentModifier
                        )
                        "expenses" -> ExpensesScreen(
                            viewModel = viewModel,
                            modifier = contentModifier
                        )
                        "reports" -> ReportsScreen(
                            viewModel = viewModel,
                            modifier = contentModifier
                        )
                    }
                }
            }
        }
    }
}
