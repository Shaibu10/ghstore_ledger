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
import com.example.data.entity.UserEntity
import com.example.data.repository.UserRepository
import com.example.data.repository.AuthRepository
import com.example.data.local.SessionManager
import com.example.data.entity.ActivityLogEntity
import com.example.data.repository.ActivityLogRepository
import com.example.util.HashUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val loanRepository: LoanRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val activityLogRepository: ActivityLogRepository
) : ViewModel() {

    // Activity Logs stream
    val activityLogs: StateFlow<List<ActivityLogEntity>> = activityLogRepository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun logActivity(actionType: String, description: String) {
        viewModelScope.launch {
            val username = sessionManager.loggedInUsername ?: "System"
            activityLogRepository.logAction(username, actionType, description)
        }
    }

    fun clearActivityLogs() {
        viewModelScope.launch {
            val username = sessionManager.loggedInUsername ?: "System"
            activityLogRepository.logAction(username, "ACCESS", "Cleared all activity audit trail records")
            activityLogRepository.clearLogs()
        }
    }

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

    val users: StateFlow<List<UserEntity>> = userRepository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Session/Role/Permissions states
    private val _isLoggedIn = MutableStateFlow(sessionManager.isLoggedIn)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _loggedInUsername = MutableStateFlow(sessionManager.loggedInUsername ?: "")
    val loggedInUsername = _loggedInUsername.asStateFlow()

    private val _loggedInUserRole = MutableStateFlow(sessionManager.loggedInUserRole ?: "")
    val loggedInUserRole = _loggedInUserRole.asStateFlow()

    private val _canLogProducts = MutableStateFlow(sessionManager.canLogProducts)
    val canLogProducts = _canLogProducts.asStateFlow()

    private val _canProcessPurchases = MutableStateFlow(sessionManager.canProcessPurchases)
    val canProcessPurchases = _canProcessPurchases.asStateFlow()

    private val _canAddClients = MutableStateFlow(sessionManager.canAddClients)
    val canAddClients = _canAddClients.asStateFlow()

    private val _canManageExpenses = MutableStateFlow(sessionManager.canManageExpenses)
    val canManageExpenses = _canManageExpenses.asStateFlow()

    private val _canViewReports = MutableStateFlow(sessionManager.canViewReports)
    val canViewReports = _canViewReports.asStateFlow()

    private val _storeName = MutableStateFlow(sessionManager.storeName)
    val storeName = _storeName.asStateFlow()

    private val _storePhone = MutableStateFlow(sessionManager.storePhone)
    val storePhone = _storePhone.asStateFlow()

    private val _storeLocation = MutableStateFlow(sessionManager.storeLocation)
    val storeLocation = _storeLocation.asStateFlow()

    private val _storeFooter = MutableStateFlow(sessionManager.storeFooter)
    val storeFooter = _storeFooter.asStateFlow()

    private val _storeTaxId = MutableStateFlow(sessionManager.storeTaxId)
    val storeTaxId = _storeTaxId.asStateFlow()

    fun refreshSessionState() {
        _isLoggedIn.value = sessionManager.isLoggedIn
        _loggedInUsername.value = sessionManager.loggedInUsername ?: ""
        _loggedInUserRole.value = sessionManager.loggedInUserRole ?: ""
        _canLogProducts.value = sessionManager.canLogProducts
        _canProcessPurchases.value = sessionManager.canProcessPurchases
        _canAddClients.value = sessionManager.canAddClients
        _canManageExpenses.value = sessionManager.canManageExpenses
        _canViewReports.value = sessionManager.canViewReports
        _storeName.value = sessionManager.storeName
        _storePhone.value = sessionManager.storePhone
        _storeLocation.value = sessionManager.storeLocation
        _storeFooter.value = sessionManager.storeFooter
        _storeTaxId.value = sessionManager.storeTaxId
    }

    fun updateStoreInfo(name: String, phone: String, location: String, footer: String, taxId: String) {
        if (syncMode.value == "CLIENT") {
            viewModelScope.launch {
                val json = org.json.JSONObject().apply {
                    put("action", "updateStoreInfo")
                    put("name", name)
                    put("phone", phone)
                    put("location", location)
                    put("footer", footer)
                    put("taxId", taxId)
                }
                postActionToHost(json)
            }
        } else {
            sessionManager.storeName = name
            sessionManager.storePhone = phone
            sessionManager.storeLocation = location
            sessionManager.storeFooter = footer
            sessionManager.storeTaxId = taxId

            _storeName.value = name
            _storePhone.value = phone
            _storeLocation.value = location
            _storeFooter.value = footer
            _storeTaxId.value = taxId

            logActivity("ACCESS", "Store configuration modified: $name")
        }
    }

    fun logout() {
        val currentUsername = sessionManager.loggedInUsername ?: "System"
        viewModelScope.launch {
            activityLogRepository.logAction(currentUsername, "AUTH", "Logged out of system")
        }
        authRepository.logout()
        refreshSessionState()
    }

    // --- WI-FI SYNC STATES & SEAMLESS DISTRIBUTED TOPOLOGY ---

    private val _syncMode = MutableStateFlow(sessionManager.syncMode)
    val syncMode = _syncMode.asStateFlow()

    private val _clientHostIp = MutableStateFlow(sessionManager.clientHostIp)
    val clientHostIp = _clientHostIp.asStateFlow()

    private val _clientHostPort = MutableStateFlow(sessionManager.clientHostPort)
    val clientHostPort = _clientHostPort.asStateFlow()

    private val _serverPort = MutableStateFlow(sessionManager.serverPort)
    val serverPort = _serverPort.asStateFlow()

    private val _serverRunning = MutableStateFlow(false)
    val serverRunning = _serverRunning.asStateFlow()

    private val _clientConnected = MutableStateFlow(false)
    val clientConnected = _clientConnected.asStateFlow()

    private val _syncError = MutableStateFlow("")
    val syncError = _syncError.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime = _lastSyncTime.asStateFlow()

    init {
        // If Host mode was previously enabled, auto-start the server!
        if (sessionManager.syncMode == "HOST") {
            startHostServer()
        } else if (sessionManager.syncMode == "CLIENT") {
            startClientPolling()
        }
    }

    private var clientPollJob: kotlinx.coroutines.Job? = null

    fun startClientPolling() {
        clientPollJob?.cancel()
        clientPollJob = viewModelScope.launch {
            while (true) {
                if (syncMode.value == "CLIENT") {
                    pullAllDataFromHost(
                        onSuccess = {
                            // Checked and synced successfully
                        },
                        onError = {
                            // Gracefully trace / fail silently to avoid toast spam
                        }
                    )
                }
                kotlinx.coroutines.delay(10000) // Poll every 10 seconds securely
            }
        }
    }

    fun stopClientPolling() {
        clientPollJob?.cancel()
        clientPollJob = null
    }

    fun updateSyncSettings(mode: String, hostIp: String, hostPort: Int, sPort: Int) {
        sessionManager.syncMode = mode
        sessionManager.clientHostIp = hostIp
        sessionManager.clientHostPort = hostPort
        sessionManager.serverPort = sPort

        _syncMode.value = mode
        _clientHostIp.value = hostIp
        _clientHostPort.value = hostPort
        _serverPort.value = sPort

        if (mode == "HOST") {
            startHostServer()
            stopClientPolling()
        } else if (mode == "CLIENT") {
            stopHostServer()
            startClientPolling()
        } else {
            stopHostServer()
            stopClientPolling()
        }
    }

    fun startHostServer() {
        val port = serverPort.value
        if (port < 1024 || port > 65535) {
            _serverRunning.value = false
            _syncError.value = "EPERM: Ports below 1024 are restricted by the Android OS sandbox. Please configure a port between 1024 and 65535 (e.g. 8080)."
            return
        }
        com.example.util.WifiSyncManager.startHostServer(
            port = port,
            onGetSyncData = {
                generateBackupString()
            },
            onIncomingAction = { json ->
                handleHostIncomingAction(json)
            },
            onSuccess = {
                viewModelScope.launch {
                    _serverRunning.value = true
                    _syncError.value = ""
                    logActivity("ACCESS", "Wi-Fi terminal sync server started on port $port")
                }
            },
            onError = { err ->
                viewModelScope.launch {
                    _serverRunning.value = false
                    _syncError.value = err
                    logActivity("ACCESS", "Failed starting Wi-Fi sync server: $err")
                }
            }
        )
    }

    fun stopHostServer() {
        com.example.util.WifiSyncManager.stopHostServer()
        _serverRunning.value = false
        logActivity("ACCESS", "Wi-Fi terminal sync server stopped")
    }

    fun pullAllDataFromHost(onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        _syncError.value = ""
        com.example.util.WifiSyncManager.fetchSyncDataFromHost(
            ip = clientHostIp.value,
            port = clientHostPort.value,
            onSuccess = { json ->
                viewModelScope.launch {
                    restoreBackup(
                        jsonStr = json,
                        overwrite = true,
                        onSuccess = {
                            _clientConnected.value = true
                            _lastSyncTime.value = System.currentTimeMillis()
                            _syncError.value = ""
                            logActivity("ACCESS", "Successfully pulled complete network database sync from Host")
                            onSuccess()
                        },
                        onError = { err ->
                            _syncError.value = "Format mismatch in sync response: $err"
                            onError(err)
                        }
                    )
                }
            },
            onError = { err ->
                viewModelScope.launch {
                    _syncError.value = err
                    onError(err)
                }
            }
        )
    }

    private fun postActionToHost(payload: org.json.JSONObject, onDone: () -> Unit = {}) {
        _syncError.value = ""
        com.example.util.WifiSyncManager.postActionToHost(
            ip = clientHostIp.value,
            port = clientHostPort.value,
            payload = payload,
            onSuccess = {
                viewModelScope.launch {
                    pullAllDataFromHost(
                        onSuccess = { onDone() },
                        onError = { /* Logged by pull */ }
                    )
                }
            },
            onError = { err ->
                viewModelScope.launch {
                    _syncError.value = "Transaction failed on Host: $err"
                    logActivity("ACCESS", "Network trace failure updating server state: $err")
                }
            }
        )
    }

    fun testClientConnection(onSuccess: () -> Unit, onError: (String) -> Unit) {
        com.example.util.WifiSyncManager.fetchSyncDataFromHost(
            ip = clientHostIp.value,
            port = clientHostPort.value,
            onSuccess = {
                viewModelScope.launch {
                    _clientConnected.value = true
                    _syncError.value = ""
                    onSuccess()
                }
            },
            onError = { err ->
                viewModelScope.launch {
                    _clientConnected.value = false
                    _syncError.value = err
                    onError(err)
                }
            }
        )
    }

    private fun handleHostIncomingAction(json: org.json.JSONObject): Boolean = kotlinx.coroutines.runBlocking {
        try {
            val action = json.optString("action")
                when (action) {
                    "addCustomer" -> {
                        val name = json.getString("name")
                        val email = json.getString("email")
                        val phone = json.getString("phone")
                        customerRepository.insert(Customer(name = name, email = email, phone = phone))
                        logActivity("CLIENTS", "Registered client (via remote terminal): $name ($phone)")
                    }
                    "deleteCustomer" -> {
                        val id = json.getInt("id")
                        customerRepository.delete(id)
                        logActivity("CLIENTS", "Deleted customer account ID (via remote terminal): $id")
                    }
                    "addSale" -> {
                        val customerId = if (json.isNull("customerId")) null else json.getInt("customerId")
                        val customerName = json.getString("customerName")
                        val amount = json.getDouble("amount")
                        val description = json.getString("description")
                        val isCredit = json.optBoolean("isCredit", false)
                        val creditPaid = json.optBoolean("creditPaid", false)
                        saleRepository.insert(
                            Sale(customerId = customerId, customerName = customerName, amount = amount, description = description, isCredit = isCredit, creditPaid = creditPaid)
                        )
                        val creditText = if (isCredit) {
                            if (creditPaid) " (Credit, settled immediately)" else " (Credit, outstanding)"
                        } else ""
                        logActivity("SALES", "Recorded sale of GHC $amount to $customerName (via remote terminal) - $description$creditText")
                    }
                    "addSaleWithProductDeductions" -> {
                        val customerId = if (json.isNull("customerId")) null else json.getInt("customerId")
                        val customerName = json.getString("customerName")
                        val amount = json.getDouble("amount")
                        val description = json.getString("description")
                        val isCredit = json.optBoolean("isCredit", false)
                        val creditPaid = json.optBoolean("creditPaid", false)
                        val deductionsArr = json.getJSONArray("productDeductions")
                        val productDeductions = mutableListOf<Pair<Int, Int>>()
                        for (i in 0 until deductionsArr.length()) {
                            val item = deductionsArr.getJSONObject(i)
                            productDeductions.add(Pair(item.getInt("id"), item.getInt("quantity")))
                        }
                        saleRepository.insert(
                            Sale(customerId = customerId, customerName = customerName, amount = amount, description = description, isCredit = isCredit, creditPaid = creditPaid)
                        )
                        for (deduction in productDeductions) {
                            productRepository.reduceStock(deduction.first, deduction.second)
                        }
                        val creditText = if (isCredit) {
                            if (creditPaid) " (Credit, settled immediately)" else " (Credit, outstanding)"
                        } else ""
                        logActivity("SALES", "Recorded receipt of GHC $amount from $customerName (via remote terminal catalog purchase) - $description$creditText")
                    }
                    "settleCreditSale" -> {
                        val id = json.getInt("id")
                        saleRepository.updateCreditPaymentStatus(id, true)
                        logActivity("SALES", "Settled outstanding credit sale record ID (via remote terminal): $id")
                    }
                    "deleteSale" -> {
                        val id = json.getInt("id")
                        saleRepository.delete(id)
                        logActivity("SALES", "Removed sale record ID: $id (via remote terminal)")
                    }
                    "addExpense" -> {
                        val amount = json.getDouble("amount")
                        val category = json.getString("category")
                        val description = json.getString("description")
                        expenseRepository.insert(Expense(amount = amount, category = category, description = description))
                        logActivity("EXPENSES", "Logged business expense (via remote terminal): category $category, amount GHC $amount ($description)")
                    }
                    "deleteExpense" -> {
                        val id = json.getInt("id")
                        expenseRepository.delete(id)
                        logActivity("EXPENSES", "Removed business expense record ID: $id (via remote terminal)")
                    }
                    "addProduct" -> {
                        val name = json.getString("name")
                        val category = json.getString("category")
                        val sku = json.getString("sku")
                        val price = json.getDouble("price")
                        val costPrice = json.getDouble("costPrice")
                        val stockQuantity = json.getInt("stockQuantity")
                        productRepository.insert(Product(name = name, category = category, sku = sku, price = price, costPrice = costPrice, stockQuantity = stockQuantity))
                        logActivity("CATALOG", "Added new catalog product (via remote terminal): $name, SKU: $sku, inventory quantity: $stockQuantity units")
                    }
                    "updateProduct" -> {
                        val id = json.getInt("id")
                        val name = json.getString("name")
                        val category = json.getString("category")
                        val sku = json.getString("sku")
                        val price = json.getDouble("price")
                        val costPrice = json.getDouble("costPrice")
                        val stockQuantity = json.getInt("stockQuantity")
                        val timestamp = json.optLong("timestamp", System.currentTimeMillis())
                        productRepository.insert(Product(id = id, name = name, category = category, sku = sku, price = price, costPrice = costPrice, stockQuantity = stockQuantity, timestamp = timestamp))
                        logActivity("CATALOG", "Updated parameters (via remote terminal) for product: $name (SKU: $sku)")
                    }
                    "deleteProduct" -> {
                        val id = json.getInt("id")
                        productRepository.delete(id)
                        logActivity("CATALOG", "Deleted catalog product ID (via remote terminal): $id")
                    }
                    "updateProductStock" -> {
                        val id = json.getInt("id")
                        val newStock = json.getInt("newStock")
                        productRepository.updateStock(id, newStock)
                        logActivity("CATALOG", "Adjusted inventory count (via remote terminal) of product ID $id to $newStock units")
                    }
                    "writeOffSpoiledStock" -> {
                        val productId = json.getInt("productId")
                        val productName = json.getString("productName")
                        val sku = json.getString("sku")
                        val costPrice = json.getDouble("costPrice")
                        val quantity = json.getInt("quantity")
                        val newStock = json.getInt("newStock")
                        val reason = json.optString("reason", "")
                        
                        productRepository.updateStock(productId, newStock)
                        val totalLoss = costPrice * quantity
                        val descriptionString = if (reason.isNotBlank()) {
                            "Inventory Loss: $quantity x $productName (SKU: $sku) - Reason: $reason"
                        } else {
                            "Inventory Loss: $quantity x $productName (SKU: $sku)"
                        }
                        expenseRepository.insert(Expense(amount = totalLoss, category = "Inventory Loss (Spoilage)", description = descriptionString))
                        logActivity("CATALOG", "Wrote off $quantity units of $productName (SKU: $sku) (via remote terminal) as damaged/loss. Estimated loss cost: GHC $totalLoss ($reason)")
                    }
                    "giveLoan" -> {
                        val customerId = json.getInt("customerId")
                        val customerName = json.getString("customerName")
                        val amount = json.getDouble("amount")
                        val interestRate = json.getDouble("interestRate")
                        val dueDate = json.getLong("dueDate")
                        loanRepository.insert(Loan(customerId = customerId, customerName = customerName, amount = amount, interestRate = interestRate, dueDate = dueDate))
                        logActivity("CLIENTS", "Issued client loan (via remote terminal) of GHC $amount to $customerName at $interestRate% interest rate")
                    }
                    "recordLoanRepayment" -> {
                        val id = json.getInt("id")
                        val repaidAmount = json.optDouble("repaidAmount", 0.0)
                        val isRepaid = json.optBoolean("isRepaid", false)
                        val customerName = json.getString("customerName")
                        val paymentAmount = json.getDouble("paymentAmount")
                        
                        loanRepository.updateRepayment(id, repaidAmount, isRepaid)
                        val repaymentStatus = if (isRepaid) "Fully Repaid" else "Partially Paid"
                        logActivity("CLIENTS", "Recorded installment (via remote terminal) of GHC $paymentAmount from $customerName (Status: $repaymentStatus)")
                    }
                    "deleteLoan" -> {
                        val id = json.getInt("id")
                        loanRepository.delete(id)
                        logActivity("CLIENTS", "Deleted loan agreement record ID (via remote terminal): $id")
                    }
                    "createUser" -> {
                        val name = json.getString("name")
                        val usernameLog = json.getString("usernameLog")
                        val passwordLog = json.getString("passwordLog")
                        val role = json.getString("role")
                        val canLog = json.getBoolean("canLog")
                        val canProcess = json.getBoolean("canProcess")
                        val canAdd = json.getBoolean("canAdd")
                        val canExpenses = json.getBoolean("canExpenses")
                        val canReports = json.getBoolean("canReports")

                        val user = UserEntity(
                            id = java.util.UUID.randomUUID().toString(),
                            name = name,
                            username = usernameLog,
                            passwordHash = HashUtils.sha256(passwordLog),
                            role = role,
                            canLogProducts = canLog,
                            canProcessPurchases = canProcess,
                            canAddClients = canAdd,
                            canManageExpenses = canExpenses,
                            canViewReports = canReports
                        )
                        userRepository.saveUser(user)
                        logActivity("ACCESS", "Created user profile (via remote terminal): $name ($usernameLog) as $role")
                    }
                    "deleteUser" -> {
                        val userId = json.getString("userId")
                        val targetUser = userRepository.getUserById(userId)
                        val desc = if (targetUser != null) {
                            "${targetUser.name} (${targetUser.username})"
                        } else {
                            "ID: $userId"
                        }
                        userRepository.deleteUserById(userId)
                        logActivity("ACCESS", "Deleted user profile (via remote terminal): $desc")
                    }
                    "updateUserPermissions" -> {
                        val userId = json.getString("userId")
                        val name = json.getString("name")
                        val usernameLog = json.getString("usernameLog")
                        val passwordHash = json.getString("passwordHash")
                        val role = json.getString("role")
                        val canLog = json.getBoolean("canLog")
                        val canProcess = json.getBoolean("canProcess")
                        val canAdd = json.getBoolean("canAdd")
                        val canExpenses = json.getBoolean("canExpenses")
                        val canReports = json.getBoolean("canReports")

                        val user = UserEntity(
                            id = userId,
                            name = name,
                            username = usernameLog,
                            passwordHash = passwordHash,
                            role = role,
                            canLogProducts = canLog,
                            canProcessPurchases = canProcess,
                            canAddClients = canAdd,
                            canManageExpenses = canExpenses,
                            canViewReports = canReports
                        )
                        userRepository.saveUser(user)
                        if (userId == sessionManager.loggedInUserId) {
                            sessionManager.createSession(user)
                            refreshSessionState()
                        }
                        logActivity("ACCESS", "Updated permissions/details (via remote terminal) for user: $name ($usernameLog)")
                    }
                    "updateStoreInfo" -> {
                        val name = json.getString("name")
                        val phone = json.getString("phone")
                        val location = json.getString("location")
                        val footer = json.getString("footer")
                        val taxId = json.getString("taxId")

                        sessionManager.storeName = name
                        sessionManager.storePhone = phone
                        sessionManager.storeLocation = location
                        sessionManager.storeFooter = footer
                        sessionManager.storeTaxId = taxId

                        _storeName.value = name
                        _storePhone.value = phone
                        _storeLocation.value = location
                        _storeFooter.value = footer
                        _storeTaxId.value = taxId

                        logActivity("ACCESS", "Store configuration modified (via remote terminal): $name")
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
    }

    override fun onCleared() {
        super.onCleared()
        stopHostServer()
        stopClientPolling()
    }

    fun createUser(name: String, usernameLog: String, passwordLog: String, role: String, canLog: Boolean, canProcess: Boolean, canAdd: Boolean, canExpenses: Boolean, canReports: Boolean) {
        viewModelScope.launch {
            if (syncMode.value == "CLIENT") {
                val json = org.json.JSONObject().apply {
                    put("action", "createUser")
                    put("name", name)
                    put("usernameLog", usernameLog)
                    put("passwordLog", passwordLog)
                    put("role", role)
                    put("canLog", canLog)
                    put("canProcess", canProcess)
                    put("canAdd", canAdd)
                    put("canExpenses", canExpenses)
                    put("canReports", canReports)
                }
                postActionToHost(json)
            } else {
                val user = UserEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name,
                    username = usernameLog,
                    passwordHash = HashUtils.sha256(passwordLog),
                    role = role,
                    canLogProducts = canLog,
                    canProcessPurchases = canProcess,
                    canAddClients = canAdd,
                    canManageExpenses = canExpenses,
                    canViewReports = canReports
                )
                userRepository.saveUser(user)
                logActivity("ACCESS", "Created user profile: $name ($usernameLog) as $role")
            }
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            if (syncMode.value == "CLIENT") {
                val json = org.json.JSONObject().apply {
                    put("action", "deleteUser")
                    put("userId", userId)
                }
                postActionToHost(json)
            } else {
                val targetUser = userRepository.getUserById(userId)
                val desc = if (targetUser != null) {
                    "${targetUser.name} (${targetUser.username})"
                } else {
                    "ID: $userId"
                }
                userRepository.deleteUserById(userId)
                logActivity("ACCESS", "Deleted user profile: $desc")
            }
        }
    }

    fun updateUserPermissions(
        userId: String,
        name: String,
        usernameLog: String,
        passwordHash: String,
        role: String,
        canLog: Boolean,
        canProcess: Boolean,
        canAdd: Boolean,
        canExpenses: Boolean,
        canReports: Boolean
    ) {
        viewModelScope.launch {
            if (syncMode.value == "CLIENT") {
                val json = org.json.JSONObject().apply {
                    put("action", "updateUserPermissions")
                    put("userId", userId)
                    put("name", name)
                    put("usernameLog", usernameLog)
                    put("passwordHash", passwordHash)
                    put("role", role)
                    put("canLog", canLog)
                    put("canProcess", canProcess)
                    put("canAdd", canAdd)
                    put("canExpenses", canExpenses)
                    put("canReports", canReports)
                }
                postActionToHost(json)
            } else {
                val user = UserEntity(
                    id = userId,
                    name = name,
                    username = usernameLog,
                    passwordHash = passwordHash,
                    role = role,
                    canLogProducts = canLog,
                    canProcessPurchases = canProcess,
                    canAddClients = canAdd,
                    canManageExpenses = canExpenses,
                    canViewReports = canReports
                )
                userRepository.saveUser(user)
                if (userId == sessionManager.loggedInUserId) {
                    // Refresh local session instantly
                    sessionManager.createSession(user)
                    refreshSessionState()
                }
                logActivity("ACCESS", "Updated permissions/details for user: $name ($usernameLog)")
            }
        }
    }

    // Derived Financial States
    val financialStats: StateFlow<FinancialStats> = combine(sales, expenses, loans, products) { saleList, expenseList, loanList, productList ->
        val totalSales = saleList.sumOf { it.amount }
        val totalExpenses = expenseList.sumOf { it.amount }
        val totalLoansOutstanding = loanList.sumOf { if (!it.isRepaid) it.amount - it.repaidAmount else 0.0 }
        val totalUnpaidCreditSales = saleList.sumOf { if (it.isCredit && !it.creditPaid) it.amount else 0.0 }
        
        val netProfit = totalSales - totalExpenses
        val netRetainedBalance = netProfit - totalLoansOutstanding
        val moneyAtHand = netRetainedBalance - totalUnpaidCreditSales

        val totalInventoryValueCost = productList.sumOf { it.costPrice * it.stockQuantity }
        val totalInventoryValueRetail = productList.sumOf { it.price * it.stockQuantity }
        val totalAssets = moneyAtHand + totalUnpaidCreditSales + totalInventoryValueCost
        
        FinancialStats(
            totalSales = totalSales,
            totalExpenses = totalExpenses,
            totalLoansOutstanding = totalLoansOutstanding,
            totalUnpaidCreditSales = totalUnpaidCreditSales,
            netProfit = netProfit,
            netRetainedBalance = netRetainedBalance,
            moneyAtHand = moneyAtHand,
            totalInventoryValueCost = totalInventoryValueCost,
            totalInventoryValueRetail = totalInventoryValueRetail,
            totalAssets = totalAssets
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
            if (syncMode.value == "CLIENT") {
                val json = org.json.JSONObject().apply {
                    put("action", "addCustomer")
                    put("name", name)
                    put("email", email)
                    put("phone", phone)
                }
                postActionToHost(json)
            } else {
                customerRepository.insert(Customer(name = name, email = email, phone = phone))
                logActivity("CLIENTS", "Registered client: $name ($phone)")
            }
        }
    }

    fun deleteCustomer(id: Int) {
        viewModelScope.launch {
            if (syncMode.value == "CLIENT") {
                val json = org.json.JSONObject().apply {
                    put("action", "deleteCustomer")
                    put("id", id)
                }
                postActionToHost(json)
            } else {
                customerRepository.delete(id)
                logActivity("CLIENTS", "Deleted customer account ID: $id")
            }
        }
    }

    fun addSale(customerId: Int?, customerName: String, amount: Double, description: String, isCredit: Boolean = false, creditPaid: Boolean = false) {
        viewModelScope.launch {
            if (syncMode.value == "CLIENT") {
                val json = org.json.JSONObject().apply {
                    put("action", "addSale")
                    put("customerId", customerId ?: org.json.JSONObject.NULL)
                    put("customerName", customerName)
                    put("amount", amount)
                    put("description", description)
                    put("isCredit", isCredit)
                    put("creditPaid", creditPaid)
                }
                postActionToHost(json)
            } else {
                saleRepository.insert(
                    Sale(customerId = customerId, customerName = customerName, amount = amount, description = description, isCredit = isCredit, creditPaid = creditPaid)
                )
                val creditText = if (isCredit) {
                    if (creditPaid) " (Credit, settled immediately)" else " (Credit, outstanding)"
                } else ""
                logActivity("SALES", "Recorded sale of GHC $amount to $customerName - $description$creditText")
            }
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
            if (syncMode.value == "CLIENT") {
                val json = org.json.JSONObject().apply {
                    put("action", "addSaleWithProductDeductions")
                    put("customerId", customerId ?: org.json.JSONObject.NULL)
                    put("customerName", customerName)
                    put("amount", amount)
                    put("description", description)
                    put("isCredit", isCredit)
                    put("creditPaid", creditPaid)
                    val deductionsArr = org.json.JSONArray()
                    for (deduction in productDeductions) {
                        val item = org.json.JSONObject()
                        item.put("id", deduction.first)
                        item.put("quantity", deduction.second)
                        deductionsArr.put(item)
                    }
                    put("productDeductions", deductionsArr)
                }
                postActionToHost(json)
            } else {
                // First save the sale
                saleRepository.insert(
                    Sale(customerId = customerId, customerName = customerName, amount = amount, description = description, isCredit = isCredit, creditPaid = creditPaid)
                )
                // Deduct inventory items
                for (deduction in productDeductions) {
                    productRepository.reduceStock(deduction.first, deduction.second)
                }
                val creditText = if (isCredit) {
                    if (creditPaid) " (Credit, settled immediately)" else " (Credit, outstanding)"
                } else ""
                logActivity("SALES", "Recorded receipt of GHC $amount from $customerName (via catalog purchase) - $description$creditText")
            }
        }
    }

    fun settleCreditSale(id: Int) {
        viewModelScope.launch {
            if (syncMode.value == "CLIENT") {
                val json = org.json.JSONObject().apply {
                    put("action", "settleCreditSale")
                    put("id", id)
                }
                postActionToHost(json)
            } else {
                saleRepository.updateCreditPaymentStatus(id, true)
                logActivity("SALES", "Settled outstanding credit sale record ID: $id")
            }
        }
    }

    fun deleteSale(id: Int) {
        viewModelScope.launch {
            if (syncMode.value == "CLIENT") {
                val json = org.json.JSONObject().apply {
                    put("action", "deleteSale")
                    put("id", id)
                }
                postActionToHost(json)
            } else {
                saleRepository.delete(id)
                logActivity("SALES", "Removed sale record ID: $id from database")
            }
        }
    }

    fun addExpense(amount: Double, category: String, description: String) {
        viewModelScope.launch {
            if (syncMode.value == "CLIENT") {
                val json = org.json.JSONObject().apply {
                    put("action", "addExpense")
                    put("amount", amount)
                    put("category", category)
                    put("description", description)
                }
                postActionToHost(json)
            } else {
                expenseRepository.insert(
                    Expense(amount = amount, category = category, description = description)
                )
                logActivity("EXPENSES", "Logged business expense: category $category, amount GHC $amount ($description)")
            }
        }
    }

    fun deleteExpense(id: Int) {
        viewModelScope.launch {
            if (syncMode.value == "CLIENT") {
                val json = org.json.JSONObject().apply {
                    put("action", "deleteExpense")
                    put("id", id)
                }
                postActionToHost(json)
            } else {
                expenseRepository.delete(id)
                logActivity("EXPENSES", "Removed business expense record ID: $id")
            }
        }
    }

    // --- PRODUCT OPERATIONS ---

    fun addProduct(name: String, category: String, sku: String, price: Double, costPrice: Double, stockQuantity: Int) {
        viewModelScope.launch {
            if (syncMode.value == "CLIENT") {
                val json = org.json.JSONObject().apply {
                    put("action", "addProduct")
                    put("name", name)
                    put("category", category)
                    put("sku", sku)
                    put("price", price)
                    put("costPrice", costPrice)
                    put("stockQuantity", stockQuantity)
                }
                postActionToHost(json)
            } else {
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
                logActivity("CATALOG", "Added new catalog product: $name, SKU: $sku, inventory quantity: $stockQuantity units")
            }
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            if (syncMode.value == "CLIENT") {
                val json = org.json.JSONObject().apply {
                    put("action", "updateProduct")
                    put("id", product.id)
                    put("name", product.name)
                    put("category", product.category)
                    put("sku", product.sku)
                    put("price", product.price)
                    put("costPrice", product.costPrice)
                    put("stockQuantity", product.stockQuantity)
                    put("timestamp", product.timestamp)
                }
                postActionToHost(json)
            } else {
                productRepository.insert(product)
                logActivity("CATALOG", "Updated parameters for product: ${product.name} (SKU: ${product.sku})")
            }
        }
    }

    fun deleteProduct(id: Int) {
        viewModelScope.launch {
            if (syncMode.value == "CLIENT") {
                val json = org.json.JSONObject().apply {
                    put("action", "deleteProduct")
                    put("id", id)
                }
                postActionToHost(json)
            } else {
                productRepository.delete(id)
                logActivity("CATALOG", "Deleted catalog product ID: $id")
            }
        }
    }

    fun updateProductStock(id: Int, newStock: Int) {
        viewModelScope.launch {
            if (syncMode.value == "CLIENT") {
                val json = org.json.JSONObject().apply {
                    put("action", "updateProductStock")
                    put("id", id)
                    put("newStock", newStock)
                }
                postActionToHost(json)
            } else {
                productRepository.updateStock(id, newStock)
                logActivity("CATALOG", "Adjusted inventory count of product ID $id to $newStock units")
            }
        }
    }

    fun writeOffSpoiledStock(productId: Int, productName: String, sku: String, costPrice: Double, quantity: Int, newStock: Int, reason: String) {
        viewModelScope.launch {
            if (syncMode.value == "CLIENT") {
                val json = org.json.JSONObject().apply {
                    put("action", "writeOffSpoiledStock")
                    put("productId", productId)
                    put("productName", productName)
                    put("sku", sku)
                    put("costPrice", costPrice)
                    put("quantity", quantity)
                    put("newStock", newStock)
                    put("reason", reason)
                }
                postActionToHost(json)
            } else {
                productRepository.updateStock(productId, newStock)
                val totalLoss = costPrice * quantity
                val descriptionString = if (reason.isNotBlank()) {
                    "Inventory Loss: $quantity x $productName (SKU: $sku) - Reason: $reason"
                } else {
                    "Inventory Loss: $quantity x $productName (SKU: $sku)"
                }
                expenseRepository.insert(
                    Expense(
                        amount = totalLoss,
                        category = "Inventory Loss (Spoilage)",
                        description = descriptionString
                    )
                )
                logActivity("CATALOG", "Wrote off $quantity units of $productName (SKU: $sku) as damaged/loss. Estimated loss cost: GHC $totalLoss ($reason)")
            }
        }
    }

    // --- LOAN ACTIONS ---

    fun giveLoan(customerId: Int, customerName: String, amount: Double, interestRate: Double, dueDate: Long) {
        viewModelScope.launch {
            if (syncMode.value == "CLIENT") {
                val json = org.json.JSONObject().apply {
                    put("action", "giveLoan")
                    put("customerId", customerId)
                    put("customerName", customerName)
                    put("amount", amount)
                    put("interestRate", interestRate)
                    put("dueDate", dueDate)
                }
                postActionToHost(json)
            } else {
                loanRepository.insert(
                    Loan(
                        customerId = customerId,
                        customerName = customerName,
                        amount = amount,
                        interestRate = interestRate,
                        dueDate = dueDate
                    )
                )
                logActivity("CLIENTS", "Issued client loan of GHC $amount to $customerName at $interestRate% interest rate")
            }
        }
    }

    fun recordLoanRepayment(loan: Loan, paymentAmount: Double) {
        viewModelScope.launch {
            if (syncMode.value == "CLIENT") {
                val totalRepaid = loan.repaidAmount + paymentAmount
                val totalDue = loan.amount * (1 + loan.interestRate / 100.0)
                val isRepaid = totalRepaid >= totalDue - 0.01
                val json = org.json.JSONObject().apply {
                    put("action", "recordLoanRepayment")
                    put("id", loan.id)
                    put("repaidAmount", totalRepaid)
                    put("isRepaid", isRepaid)
                    put("customerName", loan.customerName)
                    put("paymentAmount", paymentAmount)
                }
                postActionToHost(json)
            } else {
                val totalRepaid = loan.repaidAmount + paymentAmount
                val totalDue = loan.amount * (1 + loan.interestRate / 100.0)
                val isRepaid = totalRepaid >= totalDue - 0.01 // handle precision
                loanRepository.updateRepayment(loan.id, totalRepaid, isRepaid)
                val repaymentStatus = if (isRepaid) "Fully Repaid" else "Partially Paid"
                logActivity("CLIENTS", "Recorded installment of GHC $paymentAmount from ${loan.customerName} (Status: $repaymentStatus)")
            }
        }
    }

    fun deleteLoan(id: Int) {
        viewModelScope.launch {
            if (syncMode.value == "CLIENT") {
                val json = org.json.JSONObject().apply {
                    put("action", "deleteLoan")
                    put("id", id)
                }
                postActionToHost(json)
            } else {
                loanRepository.delete(id)
                logActivity("CLIENTS", "Deleted loan agreement record ID: $id")
            }
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

            // --- Store Info Settings ---
            val storeInfoObj = org.json.JSONObject().apply {
                put("storeName", storeName.value)
                put("storePhone", storePhone.value)
                put("storeLocation", storeLocation.value)
                put("storeFooter", storeFooter.value)
                put("storeTaxId", storeTaxId.value)
            }
            root.put("storeInfo", storeInfoObj)

            // --- Users ---
            val usersArray = org.json.JSONArray()
            users.value.forEach { u ->
                val j = org.json.JSONObject().apply {
                    put("id", u.id)
                    put("name", u.name)
                    put("username", u.username)
                    put("passwordHash", u.passwordHash)
                    put("role", u.role)
                    put("createdAt", u.createdAt)
                    put("updatedAt", u.updatedAt)
                    put("lastSyncedAt", u.lastSyncedAt)
                    put("canLogProducts", u.canLogProducts)
                    put("canProcessPurchases", u.canProcessPurchases)
                    put("canAddClients", u.canAddClients)
                    put("canManageExpenses", u.canManageExpenses)
                    put("canViewReports", u.canViewReports)
                }
                usersArray.put(j)
            }
            root.put("users", usersArray)

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

                // Parse storeInfo
                if (root.has("storeInfo")) {
                    val info = root.getJSONObject("storeInfo")
                    val name = info.optString("storeName", sessionManager.storeName)
                    val phone = info.optString("storePhone", sessionManager.storePhone)
                    val location = info.optString("storeLocation", sessionManager.storeLocation)
                    val footer = info.optString("storeFooter", sessionManager.storeFooter)
                    val taxId = info.optString("storeTaxId", sessionManager.storeTaxId)

                    sessionManager.storeName = name
                    sessionManager.storePhone = phone
                    sessionManager.storeLocation = location
                    sessionManager.storeFooter = footer
                    sessionManager.storeTaxId = taxId

                    _storeName.value = name
                    _storePhone.value = phone
                    _storeLocation.value = location
                    _storeFooter.value = footer
                    _storeTaxId.value = taxId
                }

                // Parse users
                val userList = mutableListOf<UserEntity>()
                if (root.has("users")) {
                    val arr = root.getJSONArray("users")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        userList.add(
                            UserEntity(
                                id = o.getString("id"),
                                name = o.optString("name", ""),
                                username = o.getString("username"),
                                passwordHash = o.getString("passwordHash"),
                                role = o.optString("role", "CASHIER"),
                                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                                updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                                lastSyncedAt = o.optLong("lastSyncedAt", 0L),
                                canLogProducts = o.optBoolean("canLogProducts", true),
                                canProcessPurchases = o.optBoolean("canProcessPurchases", true),
                                canAddClients = o.optBoolean("canAddClients", true),
                                canManageExpenses = o.optBoolean("canManageExpenses", true),
                                canViewReports = o.optBoolean("canViewReports", true)
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
                    userRepository.clear()
                }

                // Bulk insert
                customerList.forEach { customerRepository.insert(it) }
                saleList.forEach { saleRepository.insert(it) }
                expenseList.forEach { expenseRepository.insert(it) }
                productList.forEach { productRepository.insert(it) }
                loanList.forEach { loanRepository.insert(it) }
                userList.forEach { userRepository.saveUser(it) }

                // Post-sync live-session verification check
                if (sessionManager.isLoggedIn) {
                    val currentUserId = sessionManager.loggedInUserId
                    val matchedUser = userList.find { it.id == currentUserId }
                    if (matchedUser == null) {
                        // Crucial: Active user has been excised/deleted on the host. Log out immediately!
                        logout()
                        logActivity("AUTH", "Active session terminally severed because the user account was deleted from the central terminal.")
                    } else {
                        // Check if role, permissions, or core details was updated
                        if (matchedUser.username != sessionManager.loggedInUsername ||
                            matchedUser.role != sessionManager.loggedInUserRole ||
                            matchedUser.canLogProducts != sessionManager.canLogProducts ||
                            matchedUser.canProcessPurchases != sessionManager.canProcessPurchases ||
                            matchedUser.canAddClients != sessionManager.canAddClients ||
                            matchedUser.canManageExpenses != sessionManager.canManageExpenses ||
                            matchedUser.canViewReports != sessionManager.canViewReports
                        ) {
                            sessionManager.createSession(matchedUser)
                            refreshSessionState()
                            logActivity("AUTH", "Active session permissions safely adapted to match updated server authorization rules.")
                        }
                    }
                }

                onSuccess()
                logActivity("ACCESS", "Restored system database from security backup JSON")
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
    val totalUnpaidCreditSales: Double = 0.0,
    val netProfit: Double = 0.0,
    val netRetainedBalance: Double = 0.0,
    val moneyAtHand: Double = 0.0,
    val totalInventoryValueCost: Double = 0.0,
    val totalInventoryValueRetail: Double = 0.0,
    val totalAssets: Double = 0.0
)

class MainViewModelFactory(
    private val customerRepository: CustomerRepository,
    private val saleRepository: SaleRepository,
    private val expenseRepository: ExpenseRepository,
    private val productRepository: ProductRepository,
    private val loanRepository: LoanRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val activityLogRepository: ActivityLogRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(
                customerRepository,
                saleRepository,
                expenseRepository,
                productRepository,
                loanRepository,
                userRepository,
                authRepository,
                sessionManager,
                activityLogRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
