package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Customer
import com.example.data.CustomerRepository
import com.example.data.Expense
import com.example.data.ExpenseRepository
import com.example.data.Sale
import com.example.data.SaleRepository
import com.example.data.Product
import com.example.data.ProductRepository
import com.example.data.Loan
import com.example.data.LoanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(
    private val customerRepository: CustomerRepository,
    private val saleRepository: SaleRepository,
    private val expenseRepository: ExpenseRepository,
    private val productRepository: ProductRepository,
    private val loanRepository: LoanRepository
) : ViewModel() {

    // Streams of data from repositories
    val customers: StateFlow<List<Customer>> = customerRepository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sales: StateFlow<List<Sale>> = saleRepository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<Expense>> = expenseRepository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<Product>> = productRepository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loans: StateFlow<List<Loan>> = loanRepository.allLoans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Derived Financial States
    val financialStats: StateFlow<FinancialStats> = combine(sales, expenses, loans) { saleList, expenseList, loanList ->
        val totalSales = saleList.sumOf { it.amount }
        val totalExpenses = expenseList.sumOf { it.amount }
        val totalLoansOutstanding = loanList.sumOf { if (!it.isRepaid) it.amount - it.repaidAmount else 0.0 }
        val netProfit = totalSales - totalExpenses
        val netRetainedBalance = netProfit - totalLoansOutstanding
        FinancialStats(
            totalSales = totalSales,
            totalExpenses = totalExpenses,
            totalLoansOutstanding = totalLoansOutstanding,
            netProfit = netProfit,
            netRetainedBalance = netRetainedBalance
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialStats())

    init {
        // Seed realistic historical data if database is initial empty
        viewModelScope.launch {
            seedInitialDataIfEmpty()
        }
    }

    private suspend fun seedInitialDataIfEmpty() {
        // Check if database tables are empty
        val existingCustomers = customerRepository.allCustomers.first()
        val existingSales = saleRepository.allSales.first()
        val existingExpenses = expenseRepository.allExpenses.first()
        val existingProducts = productRepository.allProducts.first()

        if (existingCustomers.isEmpty() && existingSales.isEmpty() && existingExpenses.isEmpty() && existingProducts.isEmpty()) {
            // Create nice historical dates
            val cal = Calendar.getInstance()

            // 1. Seed Products first so they are available
            productRepository.insert(Product(name = "Premium Wireless Mouse", category = "Electronics", sku = "MS-WIRE-77", price = 49.99, costPrice = 18.00, stockQuantity = 35))
            productRepository.insert(Product(name = "Mechanical RGB Keyboard", category = "Electronics", sku = "KB-MECH-88", price = 89.99, costPrice = 32.50, stockQuantity = 20))
            productRepository.insert(Product(name = "USB-C Multiport Adaptor", category = "Accessories", sku = "AD-USBC-12", price = 24.50, costPrice = 8.00, stockQuantity = 50))
            productRepository.insert(Product(name = "Ergonomic Memory Mesh Chair", category = "Office Supplies", sku = "CH-ERGO-01", price = 199.00, costPrice = 85.00, stockQuantity = 15))
            productRepository.insert(Product(name = "Dual Monitor Desktop Stand", category = "Office Supplies", sku = "ST-DUAL-09", price = 65.00, costPrice = 24.00, stockQuantity = 22))

            // 2. Seed standard starter customers
            val idJohn = customerRepository.insert(Customer(name = "John Doe", email = "john@example.com", phone = "+1 (555) 124-5678"))
            val idJane = customerRepository.insert(Customer(name = "Jane Smith", email = "jane.smith@gmail.com", phone = "+1 (555) 987-6543"))
            val idAlice = customerRepository.insert(Customer(name = "Alice Johnson", email = "alice.j@corp.com", phone = "+1 (555) 345-6789"))

            // 3. Seed realistic historic expenses (last 5 days)
            cal.add(Calendar.DAY_OF_YEAR, -5)
            expenseRepository.insert(Expense(amount = 1200.0, category = "Rent", description = "Monthly retail space lease", timestamp = cal.timeInMillis))
            
            cal.add(Calendar.DAY_OF_YEAR, 1)
            expenseRepository.insert(Expense(amount = 180.0, category = "Utilities", description = "High-speed broadband + Electricity", timestamp = cal.timeInMillis))
            
            cal.add(Calendar.DAY_OF_YEAR, 1)
            expenseRepository.insert(Expense(amount = 320.0, category = "Inventory", description = "Premium raw supplies restocking", timestamp = cal.timeInMillis))
            
            cal.add(Calendar.DAY_OF_YEAR, 1)
            expenseRepository.insert(Expense(amount = 250.0, category = "Marketing", description = "Targeted digital advertising campaign", timestamp = cal.timeInMillis))
            
            cal.add(Calendar.DAY_OF_YEAR, 1)
            expenseRepository.insert(Expense(amount = 95.0, category = "Other", description = "Store cleaning products & supplies", timestamp = cal.timeInMillis))

            // Reset calendar to 5 days ago for Sales
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.DAY_OF_YEAR, -4)
            saleRepository.insert(Sale(customerId = idJohn.toInt(), customerName = "John Doe", amount = 650.0, description = "Acoustic bulk setup order", timestamp = cal.timeInMillis, isCredit = true, creditPaid = false))
            
            cal.add(Calendar.DAY_OF_YEAR, 1)
            saleRepository.insert(Sale(customerId = null, customerName = "Cash Walk-In", amount = 145.0, description = "Counter accessories items", timestamp = cal.timeInMillis))
            
            cal.add(Calendar.DAY_OF_YEAR, 1)
            saleRepository.insert(Sale(customerId = idJane.toInt(), customerName = "Jane Smith", amount = 890.0, description = "High-end bespoke system contract", timestamp = cal.timeInMillis, isCredit = true, creditPaid = true))
            
            cal.add(Calendar.DAY_OF_YEAR, 1)
            saleRepository.insert(Sale(customerId = idAlice.toInt(), customerName = "Alice Johnson", amount = 1250.0, description = "Consulting & workflow solution setup", timestamp = cal.timeInMillis))
            
            cal.add(Calendar.DAY_OF_YEAR, 1)
            saleRepository.insert(Sale(customerId = null, customerName = "Cash Walk-In", amount = 210.0, description = "Standard equipment unit retail", timestamp = cal.timeInMillis))
        }
    }


    // --- OPERATIONS ---

    fun addCustomer(name: String, email: String, phone: String) {
        viewModelScope.launch {
            customerRepository.insert(Customer(name = name, email = email, phone = phone))
        }
    }

    fun deleteCustomer(id: Int) {
        viewModelScope.launch {
            customerRepository.delete(id)
        }
    }

    fun addSale(customerId: Int?, customerName: String, amount: Double, description: String, isCredit: Boolean = false, creditPaid: Boolean = false) {
        viewModelScope.launch {
            saleRepository.insert(
                Sale(customerId = customerId, customerName = customerName, amount = amount, description = description, isCredit = isCredit, creditPaid = creditPaid)
            )
        }
    }

    fun addSaleWithProductDeductions(
        customerId: Int?,
        customerName: String,
        amount: Double,
        description: String,
        productDeductions: List<Pair<Int, Int>>,
        isCredit: Boolean = false,
        creditPaid: Boolean = false
    ) {
        viewModelScope.launch {
            // First save the sale
            saleRepository.insert(
                Sale(customerId = customerId, customerName = customerName, amount = amount, description = description, isCredit = isCredit, creditPaid = creditPaid)
            )
            // Deduct inventory items
            for (deduction in productDeductions) {
                productRepository.reduceStock(deduction.first, deduction.second)
            }
        }
    }

    fun settleCreditSale(id: Int) {
        viewModelScope.launch {
            saleRepository.updateCreditPaymentStatus(id, true)
        }
    }

    fun deleteSale(id: Int) {
        viewModelScope.launch {
            saleRepository.delete(id)
        }
    }

    fun addExpense(amount: Double, category: String, description: String) {
        viewModelScope.launch {
            expenseRepository.insert(
                Expense(amount = amount, category = category, description = description)
            )
        }
    }

    fun deleteExpense(id: Int) {
        viewModelScope.launch {
            expenseRepository.delete(id)
        }
    }

    // --- PRODUCT OPERATIONS ---

    fun addProduct(name: String, category: String, sku: String, price: Double, costPrice: Double, stockQuantity: Int) {
        viewModelScope.launch {
            productRepository.insert(
                Product(
                    name = name,
                    category = category,
                    sku = sku,
                    price = price,
                    costPrice = costPrice,
                    stockQuantity = stockQuantity
                )
            )
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            productRepository.insert(product)
        }
    }

    fun deleteProduct(id: Int) {
        viewModelScope.launch {
            productRepository.delete(id)
        }
    }

    fun updateProductStock(id: Int, newStock: Int) {
        viewModelScope.launch {
            productRepository.updateStock(id, newStock)
        }
    }

    // --- LOAN ACTIONS ---

    fun giveLoan(customerId: Int, customerName: String, amount: Double, interestRate: Double, dueDate: Long) {
        viewModelScope.launch {
            loanRepository.insert(
                Loan(
                    customerId = customerId,
                    customerName = customerName,
                    amount = amount,
                    interestRate = interestRate,
                    dueDate = dueDate
                )
            )
        }
    }

    fun recordLoanRepayment(loan: Loan, paymentAmount: Double) {
        viewModelScope.launch {
            val totalRepaid = loan.repaidAmount + paymentAmount
            // When calculating interest, let's say total repayment outstanding is amount * (1 + interestRate/100)
            val totalDue = loan.amount * (1 + loan.interestRate / 100.0)
            val isRepaid = totalRepaid >= totalDue - 0.01 // handle precision
            loanRepository.updateRepayment(loan.id, totalRepaid, isRepaid)
        }
    }

    fun deleteLoan(id: Int) {
        viewModelScope.launch {
            loanRepository.delete(id)
        }
    }

    // --- SYSTEM DATA BACKUP & RESTORATION ---

    fun generateBackupString(): String {
        return try {
            val root = org.json.JSONObject()
            root.put("backupVersion", 1)
            root.put("timestamp", System.currentTimeMillis())

            // --- Customers ---
            val customersArray = org.json.JSONArray()
            customers.value.forEach { c ->
                val j = org.json.JSONObject()
                j.put("id", c.id)
                j.put("name", c.name)
                j.put("email", c.email)
                j.put("phone", c.phone)
                j.put("createdAt", c.createdAt)
                customersArray.put(j)
            }
            root.put("customers", customersArray)

            // --- Sales ---
            val salesArray = org.json.JSONArray()
            sales.value.forEach { s ->
                val j = org.json.JSONObject()
                j.put("id", s.id)
                j.put("customerId", s.customerId ?: org.json.JSONObject.NULL)
                j.put("customerName", s.customerName)
                j.put("amount", s.amount)
                j.put("description", s.description)
                j.put("timestamp", s.timestamp)
                j.put("isCredit", s.isCredit)
                j.put("creditPaid", s.creditPaid)
                salesArray.put(j)
            }
            root.put("sales", salesArray)

            // --- Expenses ---
            val expensesArray = org.json.JSONArray()
            expenses.value.forEach { e ->
                val j = org.json.JSONObject()
                j.put("id", e.id)
                j.put("amount", e.amount)
                j.put("category", e.category)
                j.put("description", e.description)
                j.put("timestamp", e.timestamp)
                expensesArray.put(j)
            }
            root.put("expenses", expensesArray)

            // --- Products ---
            val productsArray = org.json.JSONArray()
            products.value.forEach { p ->
                val j = org.json.JSONObject()
                j.put("id", p.id)
                j.put("name", p.name)
                j.put("category", p.category)
                j.put("sku", p.sku)
                j.put("price", p.price)
                j.put("costPrice", p.costPrice)
                j.put("stockQuantity", p.stockQuantity)
                j.put("timestamp", p.timestamp)
                productsArray.put(j)
            }
            root.put("products", productsArray)

            // --- Loans ---
            val loansArray = org.json.JSONArray()
            loans.value.forEach { l ->
                val j = org.json.JSONObject()
                j.put("id", l.id)
                j.put("customerId", l.customerId)
                j.put("customerName", l.customerName)
                j.put("amount", l.amount)
                j.put("interestRate", l.interestRate)
                j.put("dueDate", l.dueDate)
                j.put("timestamp", l.timestamp)
                j.put("isRepaid", l.isRepaid)
                j.put("repaidAmount", l.repaidAmount)
                loansArray.put(j)
            }
            root.put("loans", loansArray)

            root.toString(2)
        } catch (e: Exception) {
            "Error generating backup: ${e.message}"
        }
    }

    fun restoreBackup(jsonStr: String, overwrite: Boolean, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val root = org.json.JSONObject(jsonStr)
                if (!root.has("backupVersion")) {
                    onError("Invalid backup signature: backupVersion property missing.")
                    return@launch
                }

                // Parse customers
                val customerList = mutableListOf<Customer>()
                if (root.has("customers")) {
                    val arr = root.getJSONArray("customers")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        customerList.add(
                            Customer(
                                id = o.optInt("id", 0),
                                name = o.optString("name", ""),
                                email = o.optString("email", ""),
                                phone = o.optString("phone", ""),
                                createdAt = o.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                }

                // Parse sales
                val saleList = mutableListOf<Sale>()
                if (root.has("sales")) {
                    val arr = root.getJSONArray("sales")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val custIdOpt = if (o.isNull("customerId")) null else o.optInt("customerId")
                        saleList.add(
                            Sale(
                                id = o.optInt("id", 0),
                                customerId = custIdOpt,
                                customerName = o.optString("customerName", "Walk-in"),
                                amount = o.optDouble("amount", 0.0),
                                description = o.optString("description", ""),
                                timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                                isCredit = o.optBoolean("isCredit", false),
                                creditPaid = o.optBoolean("creditPaid", false)
                            )
                        )
                    }
                }

                // Parse expenses
                val expenseList = mutableListOf<Expense>()
                if (root.has("expenses")) {
                    val arr = root.getJSONArray("expenses")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        expenseList.add(
                            Expense(
                                id = o.optInt("id", 0),
                                amount = o.optDouble("amount", 0.0),
                                category = o.optString("category", "Other"),
                                description = o.optString("description", ""),
                                timestamp = o.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                }

                // Parse products
                val productList = mutableListOf<Product>()
                if (root.has("products")) {
                    val arr = root.getJSONArray("products")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        productList.add(
                            Product(
                                id = o.optInt("id", 0),
                                name = o.optString("name", ""),
                                category = o.optString("category", "Other"),
                                sku = o.optString("sku", ""),
                                price = o.optDouble("price", 0.0),
                                costPrice = o.optDouble("costPrice", 0.0),
                                stockQuantity = o.optInt("stockQuantity", 0),
                                timestamp = o.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                }

                // Parse loans
                val loanList = mutableListOf<Loan>()
                if (root.has("loans")) {
                    val arr = root.getJSONArray("loans")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        loanList.add(
                            Loan(
                                id = o.optInt("id", 0),
                                customerId = o.optInt("customerId", 0),
                                customerName = o.optString("customerName", ""),
                                amount = o.optDouble("amount", 0.0),
                                interestRate = o.optDouble("interestRate", 0.0),
                                dueDate = o.optLong("dueDate", System.currentTimeMillis()),
                                timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                                isRepaid = o.optBoolean("isRepaid", false),
                                repaidAmount = o.optDouble("repaidAmount", 0.0)
                            )
                        )
                    }
                }

                // Execute database changes in background
                if (overwrite) {
                    customerRepository.clear()
                    saleRepository.clear()
                    expenseRepository.clear()
                    productRepository.clear()
                    loanRepository.clear()
                }

                // Bulk insert
                customerList.forEach { customerRepository.insert(it) }
                saleList.forEach { saleRepository.insert(it) }
                expenseList.forEach { expenseRepository.insert(it) }
                productList.forEach { productRepository.insert(it) }
                loanList.forEach { loanRepository.insert(it) }

                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Invalid JSON backup data template")
            }
        }
    }
}

data class FinancialStats(
    val totalSales: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalLoansOutstanding: Double = 0.0,
    val netProfit: Double = 0.0,
    val netRetainedBalance: Double = 0.0
)

class MainViewModelFactory(
    private val customerRepository: CustomerRepository,
    private val saleRepository: SaleRepository,
    private val expenseRepository: ExpenseRepository,
    private val productRepository: ProductRepository,
    private val loanRepository: LoanRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(customerRepository, saleRepository, expenseRepository, productRepository, loanRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
