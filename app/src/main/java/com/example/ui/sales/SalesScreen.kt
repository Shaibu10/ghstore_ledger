package com.example.ui.sales

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Customer
import com.example.data.Product
import com.example.data.Sale
import com.example.ui.MainViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.sp

@Composable
fun SalesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val sales by viewModel.sales.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val products by viewModel.products.collectAsState()
    val context = LocalContext.current

    val canLogProducts by viewModel.canLogProducts.collectAsState()
    val canProcessPurchases by viewModel.canProcessPurchases.collectAsState()
    val canManageExpenses by viewModel.canManageExpenses.collectAsState()

    // Navigation and screen-level states
    var activeTab by remember { mutableStateOf("registry") } // "registry" or "catalog"
    var productSearchQuery by remember { mutableStateOf("") }

    // Dialog state controllers
    var showAddSaleDialog by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var writeOffProduct by remember { mutableStateOf<Product?>(null) }
    var writeOffQtyString by remember { mutableStateOf("1") }
    var writeOffReason by remember { mutableStateOf("Spoiled") }

    // Add Sale input states
    var saleMethodType by remember { mutableStateOf("catalog") } // "catalog" or "custom"
    var amountString by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var dialogCustomerSearchQuery by remember { mutableStateOf("") }
    
    // Catalog POS shopping cart state variables
    var cartItems by remember { mutableStateOf<List<Pair<Product, Int>>>(emptyList()) }
    var currentSelectedCartProduct by remember { mutableStateOf<Product?>(null) }
    var productDropdownExpanded by remember { mutableStateOf(false) }
    var dialogProductSearchQuery by remember { mutableStateOf("") }
    var selectQtyString by remember { mutableStateOf("1") }

    // Discount Feature State variables
    var discountType by remember { mutableStateOf("none") } // "none", "percentage", "fixed"
    var discountValueString by remember { mutableStateOf("") }
    var isCreditSelection by remember { mutableStateOf(false) }

    // Add Product states
    var prodName by remember { mutableStateOf("") }
    var prodCategory by remember { mutableStateOf("Electronics") }
    var prodSku by remember { mutableStateOf("") }
    var prodPriceString by remember { mutableStateOf("") }
    var prodCostString by remember { mutableStateOf("") }
    var prodStockString by remember { mutableStateOf("") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    // Thermal Receipt State Variables
    var activeReceiptSale by remember { mutableStateOf<Sale?>(null) }
    var activeReceiptCartItems by remember { mutableStateOf<List<Pair<Product, Int>>?>(null) }
    var activeReceiptDiscountAmount by remember { mutableStateOf(0.0) }
    var activeReceiptTaxAmount by remember { mutableStateOf(0.0) }
    var showThermalReceiptDialog by remember { mutableStateOf(false) }

    val categoriesList = listOf("Electronics", "Groceries", "Clothing", "Utilities", "Office Supplies", "Services", "Other")

    // Formatter variables
    val fmt = java.text.DecimalFormat("GH₵#,##0.00")
    val df = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US)

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("sales_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header Title
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = "Store Operations",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (activeTab == "registry") "Monitor revenues and process customer purchases" else "Manage store products catalog and real-time inventory",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            // Custom Segmented Control Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .background(
                            color = if (activeTab == "registry") MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { activeTab = "registry" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sales Journal",
                        color = if (activeTab == "registry") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .background(
                            color = if (activeTab == "catalog") MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { activeTab = "catalog" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Products Catalog",
                        color = if (activeTab == "catalog") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Screen content dependent on active Tab
            if (activeTab == "registry") {
                // TAB 1: SALES REGISTRY JOURNAL
                
                // Cumulative Sum Summary Card
                val totalSalesSum = sales.sumOf { it.amount }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("sales_summary_banner"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "AGGREGATE REVENUES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = fmt.format(totalSalesSum),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (sales.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No sales recorded yet",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "Checkout basket items or submit custom receipts via the dynamic log",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(sales) { s ->
                            SaleRowItem(
                                sale = s,
                                onDelete = {
                                    viewModel.deleteSale(s.id)
                                    Toast.makeText(context, "Sale entry removed", Toast.LENGTH_SHORT).show()
                                },
                                onSettle = {
                                    viewModel.settleCreditSale(s.id)
                                    Toast.makeText(context, "Payment credit settled!", Toast.LENGTH_SHORT).show()
                                },
                                onPrint = {
                                    activeReceiptSale = s
                                    activeReceiptCartItems = null
                                    // Parse discount if present in description e.g. "Saved GH₵ 5.00"
                                    val regex = "Saved GH₵\\s*([\\d.,]+)".toRegex()
                                    val match = regex.find(s.description)
                                    val discAmt = match?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                                    activeReceiptDiscountAmount = discAmt
                                    activeReceiptTaxAmount = 0.0
                                    showThermalReceiptDialog = true
                                },
                                dateFormat = df,
                                currencyFormat = fmt
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(84.dp)) // Padding to clear FAB
                        }
                    }
                }
            } else {
                // TAB 2: PRODUCTS CATALOG & INVENTORY
                
                // Top Search Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = productSearchQuery,
                        onValueChange = { productSearchQuery = it },
                        label = { Text("Search product name, SKU, or category...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("catalog_search_bar"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (productSearchQuery.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { productSearchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                }

                // Inventory Metrics Ribbon
                val catalogValuation = products.sumOf { it.price * it.stockQuantity }
                val outOfStockCount = products.count { it.stockQuantity == 0 }
                val lowStockCount = products.count { it.stockQuantity in 1..9 }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("Total Items", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                            Text("${products.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.2f)) {
                            Text("Stock Valuation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                            Text(fmt.format(catalogValuation), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("Out of Stock", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                            Text(
                                text = "$outOfStockCount",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (outOfStockCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // Filtering list
                val filteredProducts = products.filter {
                    it.name.contains(productSearchQuery, ignoreCase = true) ||
                    it.sku.contains(productSearchQuery, ignoreCase = true) ||
                    it.category.contains(productSearchQuery, ignoreCase = true)
                }

                if (filteredProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (products.isEmpty()) "No products created yet" else "No matching products found",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                            Text(
                                text = if (products.isEmpty()) "Add your first inventory product catalog item in seconds" else "Try adjusting your query filter keywords",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredProducts) { product ->
                            ProductRowItem(
                                product = product,
                                format = fmt,
                                onAddStock = {
                                    if (!canLogProducts) {
                                        Toast.makeText(context, "Access Restraints: You do not have permission to modify catalogs.", Toast.LENGTH_SHORT).show()
                                        return@ProductRowItem
                                    }
                                    viewModel.updateProductStock(product.id, product.stockQuantity + 1)
                                    Toast.makeText(context, "${product.name} stock increased +1", Toast.LENGTH_SHORT).show()
                                },
                                onRemoveStock = {
                                    if (!canLogProducts) {
                                        Toast.makeText(context, "Access Restraints: You do not have permission to modify catalogs.", Toast.LENGTH_SHORT).show()
                                        return@ProductRowItem
                                    }
                                    if (product.stockQuantity > 0) {
                                        viewModel.updateProductStock(product.id, product.stockQuantity - 1)
                                        Toast.makeText(context, "${product.name} stock reduced -1", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Stock is already empty", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onEdit = {
                                    if (!canLogProducts) {
                                        Toast.makeText(context, "Access Restraints: You do not have permission to edit catalogs.", Toast.LENGTH_SHORT).show()
                                        return@ProductRowItem
                                    }
                                    editingProduct = product
                                    // Populate update dialog fields
                                    prodName = product.name
                                    prodCategory = product.category
                                    prodSku = product.sku
                                    prodPriceString = product.price.toString()
                                    prodCostString = product.costPrice.toString()
                                    prodStockString = product.stockQuantity.toString()
                                },
                                onDelete = {
                                    if (!canLogProducts) {
                                        Toast.makeText(context, "Access Restraints: You do not have permission to delete catalog items.", Toast.LENGTH_SHORT).show()
                                        return@ProductRowItem
                                    }
                                    viewModel.deleteProduct(product.id)
                                    Toast.makeText(context, "${product.name} removed from catalog", Toast.LENGTH_SHORT).show()
                                },
                                onWriteOffSpoilage = {
                                    if (!canManageExpenses) {
                                        Toast.makeText(context, "Access Restraints: You do not have permission to log losses (Manage Expenses).", Toast.LENGTH_SHORT).show()
                                        return@ProductRowItem
                                    }
                                    writeOffProduct = product
                                    writeOffQtyString = "1"
                                    writeOffReason = "Spoiled"
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(84.dp))
                        }
                    }
                }
            }
        }

        // DYNAMIC FLOATING ACTION BUTTONS ACCORDING TO CURRENT CONTEXT
        FloatingActionButton(
            onClick = {
                if (activeTab == "registry") {
                    if (!canProcessPurchases) {
                        Toast.makeText(context, "Access Restraints: You do not have permission to record transactions (Process Sales).", Toast.LENGTH_LONG).show()
                        return@FloatingActionButton
                    }
                    // Reset New Sale Form Dialog
                    amountString = ""
                    description = ""
                    selectedCustomer = null
                    customerDropdownExpanded = false
                    dialogCustomerSearchQuery = ""
                    cartItems = emptyList()
                    currentSelectedCartProduct = null
                    productDropdownExpanded = false
                    dialogProductSearchQuery = ""
                    selectQtyString = "1"
                    discountType = "none"
                    discountValueString = ""
                    isCreditSelection = false
                    showAddSaleDialog = true
                } else {
                    if (!canLogProducts) {
                        Toast.makeText(context, "Access Restraints: You do not have permission to create items (Log Products).", Toast.LENGTH_LONG).show()
                        return@FloatingActionButton
                    }
                    // Reset New Product Form Dialog
                    prodName = ""
                    prodCategory = "Electronics"
                    prodSku = ""
                    prodPriceString = ""
                    prodCostString = ""
                    prodStockString = ""
                    showAddProductDialog = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag(if (activeTab == "registry") "fab_add_sale" else "fab_add_product"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (activeTab == "registry") "Log Sale" else "Create Product"
            )
        }

        // --- NEW DIALOG: LOG SALE ---
        if (showAddSaleDialog) {
            AlertDialog(
                onDismissRequest = { showAddSaleDialog = false },
                confirmButton = {
                    Button(
                        modifier = Modifier.testTag("dialog_confirm_add_sale"),
                        onClick = {
                            val cid = selectedCustomer?.id
                            val custName = selectedCustomer?.name ?: "Cash Walk-In"

                            if (saleMethodType == "custom") {
                                // Manual accounting bypass
                                val amount = amountString.toDoubleOrNull()
                                if (amount == null || amount <= 0.0) {
                                    Toast.makeText(context, "Please enter a valid sales amount", Toast.LENGTH_SHORT).show()
                                } else if (description.isBlank()) {
                                    Toast.makeText(context, "Please insert a custom sales description", Toast.LENGTH_SHORT).show()
                                } else {
                                    val provisionalSale = Sale(
                                        id = 0,
                                        customerId = cid,
                                        customerName = custName,
                                        amount = amount,
                                        description = description,
                                        timestamp = System.currentTimeMillis(),
                                        isCredit = isCreditSelection,
                                        creditPaid = false
                                    )
                                    viewModel.addSale(
                                        customerId = cid,
                                        customerName = custName,
                                        amount = amount,
                                        description = description,
                                        isCredit = isCreditSelection,
                                        creditPaid = false
                                    )
                                    showAddSaleDialog = false
                                    val msgType = if (isCreditSelection) "credit sale" else "receipt"
                                    Toast.makeText(context, "Custom $msgType of ${fmt.format(amount)} saved", Toast.LENGTH_SHORT).show()
                                    
                                    // Trigger electronic receipt dialog
                                    activeReceiptSale = provisionalSale
                                    activeReceiptCartItems = null
                                    activeReceiptDiscountAmount = 0.0
                                    activeReceiptTaxAmount = 0.0
                                    showThermalReceiptDialog = true
                                }
                            } else {
                                // Product inventory basket flow
                                if (cartItems.isEmpty()) {
                                    Toast.makeText(context, "Please add products to your POS basket first", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val subtotal = cartItems.sumOf { it.first.price * it.second }
                                val discountVal = discountValueString.toDoubleOrNull() ?: 0.0
                                val calculatedDiscount = when (discountType) {
                                    "percentage" -> (subtotal * (discountVal / 100.0)).coerceIn(0.0, subtotal)
                                    "fixed" -> discountVal.coerceAtLeast(0.0).coerceAtMost(subtotal)
                                    else -> 0.0
                                }
                                val cartTotal = (subtotal - calculatedDiscount).coerceAtLeast(0.0)
                                
                                val discInfo = if (discountType != "none" && discountVal > 0.0) {
                                    val discLabel = if (discountType == "percentage") "$discountValueString%" else fmt.format(discountVal)
                                    " | Discount: $discLabel (Saved ${fmt.format(calculatedDiscount)})"
                                } else {
                                    ""
                                }
                                val cartDesc = "Products: " + cartItems.joinToString { "${it.second}x ${it.first.name}" } + discInfo
                                val deductions = cartItems.map { Pair(it.first.id, it.second) }

                                val provisionalSale = Sale(
                                    id = 0,
                                    customerId = cid,
                                    customerName = custName,
                                    amount = cartTotal,
                                    description = cartDesc,
                                    timestamp = System.currentTimeMillis(),
                                    isCredit = isCreditSelection,
                                    creditPaid = false
                                )

                                viewModel.addSaleWithProductDeductions(
                                    customerId = cid,
                                    customerName = custName,
                                    amount = cartTotal,
                                    description = cartDesc,
                                    productDeductions = deductions,
                                    isCredit = isCreditSelection,
                                    creditPaid = false
                                )
                                showAddSaleDialog = false
                                val label = if (isCreditSelection) "Logged credit sale of ${fmt.format(cartTotal)}." else "Logged ${fmt.format(cartTotal)} basket sale."
                                Toast.makeText(context, "$label Inventory updated!", Toast.LENGTH_LONG).show()

                                // Trigger receipt dialog and clear active checkout cart
                                activeReceiptSale = provisionalSale
                                activeReceiptCartItems = cartItems.toList()
                                activeReceiptDiscountAmount = calculatedDiscount
                                activeReceiptTaxAmount = 0.0
                                cartItems = emptyList()
                                showThermalReceiptDialog = true
                            }
                        }
                    ) {
                        Text(if (saleMethodType == "custom") "Save Bill" else "Checkout Basket")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { 
                        showAddSaleDialog = false 
                        dialogCustomerSearchQuery = ""
                        dialogProductSearchQuery = ""
                        customerDropdownExpanded = false
                        productDropdownExpanded = false
                    }) {
                        Text("Cancel")
                    }
                },
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Process Customer Purchase", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .wrapContentHeight()
                    ) {
                        // Customer selection (Dynamic Autocomplete Search)
                        Column {
                            Text(
                                text = "Select Customer Link (Optional)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            OutlinedTextField(
                                value = dialogCustomerSearchQuery,
                                onValueChange = { 
                                    dialogCustomerSearchQuery = it
                                    customerDropdownExpanded = true 
                                },
                                placeholder = { Text(selectedCustomer?.name ?: "Search customer name or phone...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (dialogCustomerSearchQuery.isNotEmpty() || selectedCustomer != null) {
                                            IconButton(onClick = { 
                                                dialogCustomerSearchQuery = "" 
                                                selectedCustomer = null
                                            }) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                                            }
                                        }
                                        IconButton(onClick = { customerDropdownExpanded = !customerDropdownExpanded }) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown, 
                                                contentDescription = "Toggle list",
                                                modifier = Modifier.rotate(if (customerDropdownExpanded) 180f else 0f)
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("checkout_dialog_customer_search"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            if (customerDropdownExpanded) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Card(
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 160.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    LazyColumn(
                                        modifier = Modifier.padding(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        item {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedCustomer = null
                                                        dialogCustomerSearchQuery = ""
                                                        customerDropdownExpanded = false
                                                    },
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                     "Cash Walk-In (Retail Handout)",
                                                     modifier = Modifier.padding(12.dp),
                                                     style = MaterialTheme.typography.bodyMedium,
                                                     fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        val filteredCustomers = customers.filter { cust ->
                                            dialogCustomerSearchQuery.isEmpty() ||
                                            cust.name.contains(dialogCustomerSearchQuery, ignoreCase = true) ||
                                            cust.phone.contains(dialogCustomerSearchQuery, ignoreCase = true)
                                        }
                                        if (filteredCustomers.isEmpty()) {
                                            item {
                                                Box(
                                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("No customers match query", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        } else {
                                            items(filteredCustomers) { cust ->
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            selectedCustomer = cust
                                                            dialogCustomerSearchQuery = cust.name
                                                            customerDropdownExpanded = false
                                                        },
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = if (selectedCustomer?.id == cust.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                                    ),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp)) {
                                                        Text(cust.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                        Text(cust.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Payment Type Toggle Option (BNPL / Credit Sales features)
                        Column {
                            Text(
                                text = "Payment Status",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(32.dp)
                                        .background(
                                            color = if (!isCreditSelection) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { isCreditSelection = false },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (!isCreditSelection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Paid Handout", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (!isCreditSelection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(32.dp)
                                        .background(
                                            color = if (isCreditSelection) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { isCreditSelection = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isCreditSelection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Credit (Pay Later)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (isCreditSelection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                            if (isCreditSelection && selectedCustomer == null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "⚠️ Warning: Buying on credit as Walk-In. We suggest selecting a registered customer to track outstanding debt ledger.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Selector methods (Tabs inside the dialog: "Buy Products" vs "Manual Receipt")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .background(
                                        color = if (saleMethodType == "catalog") MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { saleMethodType = "catalog" },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Selected Products", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (saleMethodType == "catalog") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .background(
                                        color = if (saleMethodType == "custom") MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { saleMethodType = "custom" },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Custom Revenue", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (saleMethodType == "custom") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        if (saleMethodType == "custom") {
                            // OPTION B: CUSTOM REVENUE FIELDS
                            OutlinedTextField(
                                value = amountString,
                                onValueChange = { amountString = it },
                                label = { Text("Revenue Amount (GH₵)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_sale_amount"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Revenue Description / Item Reference") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_sale_description"),
                                singleLine = true
                            )
                        } else {
                            // OPTION A: INTEGRATED POS CATALOG CART
                            if (products.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Catalog list is empty. Create products in the 'Products Catalog' tab first!",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                // Select product dynamic search autocomplete
                                Column {
                                    Text(
                                        text = "Add Item to Basket",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    OutlinedTextField(
                                        value = dialogProductSearchQuery,
                                        onValueChange = { 
                                            dialogProductSearchQuery = it
                                            productDropdownExpanded = true 
                                        },
                                        placeholder = { 
                                            Text(
                                                currentSelectedCartProduct?.let { "${it.name} (${fmt.format(it.price)})" } 
                                                    ?: "Search product name, SKU, or category..."
                                            ) 
                                        },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                        trailingIcon = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (dialogProductSearchQuery.isNotEmpty() || currentSelectedCartProduct != null) {
                                                    IconButton(onClick = { 
                                                        dialogProductSearchQuery = "" 
                                                        currentSelectedCartProduct = null
                                                    }) {
                                                        Icon(Icons.Default.Close, contentDescription = "Clear Product Selection")
                                                    }
                                                }
                                                IconButton(onClick = { productDropdownExpanded = !productDropdownExpanded }) {
                                                    Icon(
                                                        imageVector = Icons.Default.ArrowDropDown, 
                                                        contentDescription = "Toggle list",
                                                        modifier = Modifier.rotate(if (productDropdownExpanded) 180f else 0f)
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("checkout_dialog_product_search"),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    if (productDropdownExpanded) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Card(
                                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 160.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            LazyColumn(
                                                modifier = Modifier.padding(4.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                val filteredProducts = products.filter { prod ->
                                                    dialogProductSearchQuery.isEmpty() ||
                                                    prod.name.contains(dialogProductSearchQuery, ignoreCase = true) ||
                                                    prod.sku.contains(dialogProductSearchQuery, ignoreCase = true) ||
                                                    prod.category.contains(dialogProductSearchQuery, ignoreCase = true)
                                                }
                                                if (filteredProducts.isEmpty()) {
                                                    item {
                                                        Box(
                                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text("No matches found", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    }
                                                } else {
                                                    items(filteredProducts) { prod ->
                                                        Card(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    currentSelectedCartProduct = prod
                                                                    dialogProductSearchQuery = prod.name
                                                                    productDropdownExpanded = false
                                                                },
                                                            colors = CardDefaults.cardColors(
                                                                containerColor = if (currentSelectedCartProduct?.id == prod.id) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                                            ),
                                                            shape = RoundedCornerShape(8.dp)
                                                        ) {
                                                            Column(modifier = Modifier.padding(10.dp)) {
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Text(prod.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                                                    Text(fmt.format(prod.price), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                                }
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Text("SKU: ${prod.sku} | ${prod.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                                    Text("Stock: ${prod.stockQuantity}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = if (prod.stockQuantity > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Quantity picker row & Add Button
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = selectQtyString,
                                        onValueChange = { selectQtyString = it },
                                        label = { Text("Qty") },
                                        modifier = Modifier
                                            .width(70.dp),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Button(
                                        onClick = {
                                            val prod = currentSelectedCartProduct
                                            if (prod == null) {
                                                Toast.makeText(context, "Please select an item first", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            val qty = selectQtyString.toIntOrNull()
                                            if (qty == null || qty <= 0) {
                                                Toast.makeText(context, "Invalid Quantity", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            if (qty > prod.stockQuantity) {
                                                Toast.makeText(context, "Only ${prod.stockQuantity} units left in stock!", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }

                                            // Add or update quantity in cart list
                                            val existingIndex = cartItems.indexOfFirst { it.first.id == prod.id }
                                            if (existingIndex >= 0) {
                                                val currentQtyInCart = cartItems[existingIndex].second
                                                if (currentQtyInCart + qty > prod.stockQuantity) {
                                                    Toast.makeText(context, "Cannot exceed total available stock (${prod.stockQuantity} units)!", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                val updatedList = cartItems.toMutableList()
                                                updatedList[existingIndex] = Pair(prod, currentQtyInCart + qty)
                                                cartItems = updatedList
                                            } else {
                                                val updatedList = cartItems.toMutableList()
                                                updatedList.add(Pair(prod, qty))
                                                cartItems = updatedList
                                            }

                                            Toast.makeText(context, "Added ${qty}x ${prod.name} to basket", Toast.LENGTH_SHORT).show()
                                            selectQtyString = "1"
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.ShoppingCart, contentDescription = "Add to basket", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add to Basket")
                                    }
                                }

                                // Scrollable Cart Items Panel inside Dialog
                                if (cartItems.isNotEmpty()) {
                                    Text(
                                        text = "Basket Items",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 4.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            .padding(6.dp)
                                            .wrapContentHeight()
                                    ) {
                                        cartItems.forEachIndexed { index, pair ->
                                            val (item, qty) = pair
                                            val subtotal = item.price * qty
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(item.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text("${qty}x ${fmt.format(item.price)} each = ${fmt.format(subtotal)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val updated = cartItems.toMutableList()
                                                        updated.removeAt(index)
                                                        cartItems = updated
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Remove item", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // Discount configuration inputs for professional control
                                        Text(
                                            text = "Apply Sale Discount",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        // Segmented Row for Discount Type: None, %, Fixed ($)
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            val types = listOf("none" to "No Disc.", "percentage" to "Percent %", "fixed" to "Cash GH₵")
                                            types.forEach { (typeKey, label) ->
                                                val isSelected = discountType == typeKey
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(28.dp)
                                                        .background(
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                            shape = RoundedCornerShape(6.dp)
                                                        )
                                                        .clickable { discountType = typeKey }
                                                        .padding(vertical = 4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = label,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }

                                        if (discountType != "none") {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            OutlinedTextField(
                                                value = discountValueString,
                                                onValueChange = { discountValueString = it },
                                                label = { Text(if (discountType == "percentage") "Discount Percentage (%)" else "Discount Value (GH₵)") },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("discount_value_input"),
                                                singleLine = true,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        val cartValuation = cartItems.sumOf { it.first.price * it.second }
                                        val discountVal = discountValueString.toDoubleOrNull() ?: 0.0
                                        val calculatedDiscount = when (discountType) {
                                            "percentage" -> (cartValuation * (discountVal / 100.0)).coerceIn(0.0, cartValuation)
                                            "fixed" -> discountVal.coerceAtLeast(0.0).coerceAtMost(cartValuation)
                                            else -> 0.0
                                        }
                                        val cartFinalTotal = (cartValuation - calculatedDiscount).coerceAtLeast(0.0)

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                                .padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Subtotal:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                                Text(fmt.format(cartValuation), style = MaterialTheme.typography.bodySmall)
                                            }
                                            if (calculatedDiscount > 0.0) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    val discText = if (discountType == "percentage") "Discount ($discountValueString%):" else "Discount (Cash):"
                                                    Text(discText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                                                    Text("- ${fmt.format(calculatedDiscount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("TOTAL DUE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                                                Text(fmt.format(cartFinalTotal), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // --- NEW DIALOG: ADD PRODUCT ---
        if (showAddProductDialog) {
            AlertDialog(
                onDismissRequest = { showAddProductDialog = false },
                confirmButton = {
                    Button(
                        modifier = Modifier.testTag("dialog_confirm_add_product"),
                        onClick = {
                            val price = prodPriceString.toDoubleOrNull()
                            val cost = prodCostString.toDoubleOrNull()
                            val stock = prodStockString.toIntOrNull()

                            if (prodName.isBlank()) {
                                Toast.makeText(context, "Product Name cannot be empty", Toast.LENGTH_SHORT).show()
                            } else if (prodSku.isBlank()) {
                                Toast.makeText(context, "Stock Keeping Unit SKU required", Toast.LENGTH_SHORT).show()
                            } else if (price == null || price < 0.0) {
                                Toast.makeText(context, "Enter a valid product retail price", Toast.LENGTH_SHORT).show()
                            } else if (cost == null || cost < 0.0) {
                                Toast.makeText(context, "Enter a valid item product cost", Toast.LENGTH_SHORT).show()
                            } else if (stock == null || stock < 0) {
                                Toast.makeText(context, "Enter a valid initial stock amount", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.addProduct(
                                    name = prodName,
                                    category = prodCategory,
                                    sku = prodSku,
                                    price = price,
                                    costPrice = cost,
                                    stockQuantity = stock
                                )
                                showAddProductDialog = false
                                Toast.makeText(context, "Product ${prodName} added successfully", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Add Product")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showAddProductDialog = false }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Log New Product Catalog", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        OutlinedTextField(
                            value = prodName,
                            onValueChange = { prodName = it },
                            label = { Text("Product/Item Name") },
                            modifier = Modifier.fillMaxWidth().testTag("add_product_input_name"),
                            singleLine = true
                        )

                        // Category Dropdown Custom Trigger
                        Column {
                            Text(
                                text = "Category Label Selection",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { categoryDropdownExpanded = true }
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    .padding(vertical = 12.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = prodCategory,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "dropdown trigger")
                            }

                            DropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.6f)
                            ) {
                                categoriesList.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            prodCategory = cat
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = prodSku,
                            onValueChange = { prodSku = it },
                            label = { Text("Stock Keeping Unit (SKU)") },
                            modifier = Modifier.fillMaxWidth().testTag("add_product_input_sku"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = prodPriceString,
                            onValueChange = { prodPriceString = it },
                            label = { Text("Selling Price (GH₵)") },
                            modifier = Modifier.fillMaxWidth().testTag("add_product_input_price"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = prodCostString,
                            onValueChange = { prodCostString = it },
                            label = { Text("Wholesale Cost Price (GH₵)") },
                            modifier = Modifier.fillMaxWidth().testTag("add_product_input_cost"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = prodStockString,
                            onValueChange = { prodStockString = it },
                            label = { Text("Initial Stock Quantity") },
                            modifier = Modifier.fillMaxWidth().testTag("add_product_input_stock"),
                            singleLine = true
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // --- NEW DIALOG: EDIT PRODUCT ---
        if (editingProduct != null) {
            AlertDialog(
                onDismissRequest = { editingProduct = null },
                confirmButton = {
                    Button(
                        modifier = Modifier.testTag("dialog_confirm_update_product"),
                        onClick = {
                            val price = prodPriceString.toDoubleOrNull()
                            val cost = prodCostString.toDoubleOrNull()
                            val stock = prodStockString.toIntOrNull()
                            val currentProd = editingProduct

                            if (currentProd != null) {
                                if (prodName.isBlank()) {
                                    Toast.makeText(context, "Product Name cannot be empty", Toast.LENGTH_SHORT).show()
                                } else if (prodSku.isBlank()) {
                                    Toast.makeText(context, "SKU required", Toast.LENGTH_SHORT).show()
                                } else if (price == null || price < 0.0) {
                                    Toast.makeText(context, "Enter a valid selling price", Toast.LENGTH_SHORT).show()
                                } else if (cost == null || cost < 0.0) {
                                    Toast.makeText(context, "Enter a valid purchasing cost", Toast.LENGTH_SHORT).show()
                                } else if (stock == null || stock < 0) {
                                    Toast.makeText(context, "Enter a valid stock level", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.updateProduct(
                                        Product(
                                            id = currentProd.id,
                                            name = prodName,
                                            category = prodCategory,
                                            sku = prodSku,
                                            price = price,
                                            costPrice = cost,
                                            stockQuantity = stock,
                                            timestamp = currentProd.timestamp
                                        )
                                    )
                                    editingProduct = null
                                    Toast.makeText(context, "Product inventory ledger updated", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text("Save Updates")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { editingProduct = null }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Update Catalog Information", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        OutlinedTextField(
                            value = prodName,
                            onValueChange = { prodName = it },
                            label = { Text("Product Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Category Dropdown Custom Trigger
                        Column {
                            Text(
                                text = "Category Selection",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { categoryDropdownExpanded = true }
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    .padding(vertical = 12.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = prodCategory,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "dropdown")
                            }

                            DropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.6f)
                            ) {
                                categoriesList.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            prodCategory = cat
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = prodSku,
                            onValueChange = { prodSku = it },
                            label = { Text("SKU Reference") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = prodPriceString,
                            onValueChange = { prodPriceString = it },
                            label = { Text("Retail Price (GH₵)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = prodCostString,
                            onValueChange = { prodCostString = it },
                            label = { Text("Cost Price (GH₵)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = prodStockString,
                            onValueChange = { prodStockString = it },
                            label = { Text("Inventory stock count") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // --- NEW DIALOG: WRITE OFF SPOILED/DAMAGED STOCK ---
        if (writeOffProduct != null) {
            val productToAdjust = writeOffProduct!!
            AlertDialog(
                onDismissRequest = { writeOffProduct = null },
                confirmButton = {
                    Button(
                        modifier = Modifier.testTag("dialog_confirm_write_off"),
                        onClick = {
                            val qty = writeOffQtyString.toIntOrNull()
                            if (qty == null || qty <= 0) {
                                Toast.makeText(context, "Enter a valid positive quantity", Toast.LENGTH_SHORT).show()
                            } else if (qty > productToAdjust.stockQuantity) {
                                Toast.makeText(context, "Cannot write off more than current stock (${productToAdjust.stockQuantity})", Toast.LENGTH_SHORT).show()
                            } else {
                                val remainingStock = productToAdjust.stockQuantity - qty
                                viewModel.writeOffSpoiledStock(
                                    productId = productToAdjust.id,
                                    productName = productToAdjust.name,
                                    sku = productToAdjust.sku,
                                    costPrice = productToAdjust.costPrice,
                                    quantity = qty,
                                    newStock = remainingStock,
                                    reason = writeOffReason
                                )
                                writeOffProduct = null
                                Toast.makeText(context, "Logged ${qty} unit(s) of ${productToAdjust.name} as Inventory Loss.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Confirm Loss Write-Off", color = MaterialTheme.colorScheme.onError)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { writeOffProduct = null }) {
                        Text("Cancel")
                    }
                },
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text("Write Off Loss (Spoilage/Damage)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Reducing stock of items due to damage, spoilage, or expiration will adjust the ledger by recording the total product cost as a direct business expense.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Product Details Summary
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(productToAdjust.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("SKU: ${productToAdjust.sku} • Current Stock: ${productToAdjust.stockQuantity} units available", style = MaterialTheme.typography.bodySmall)
                                Text("Cost Price: ${fmt.format(productToAdjust.costPrice)} each", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Input: Spoilage Quantity
                        OutlinedTextField(
                            value = writeOffQtyString,
                            onValueChange = { writeOffQtyString = it },
                            label = { Text("Quantity Spoiled/Damaged (Units)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        // Input: Reason for Write-Off
                        OutlinedTextField(
                            value = writeOffReason,
                            onValueChange = { writeOffReason = it },
                            label = { Text("Reason for Loss / Damage") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Quick selection reasons
                        Text(
                            text = "QUICK REASONS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val presetReasons = listOf("Spoiled", "Expired", "Damaged", "Stolen")
                            presetReasons.forEach { r ->
                                FilterChip(
                                    selected = writeOffReason == r,
                                    onClick = { writeOffReason = r },
                                    label = { Text(r) }
                                )
                            }
                        }

                        // Financial Loss estimation
                        val inputQty = writeOffQtyString.toIntOrNull() ?: 0
                        val rawLossAmount = productToAdjust.costPrice * inputQty
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "IMPACT ON ASSETS & NET PROFIT:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "-" + fmt.format(rawLossAmount),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        if (showThermalReceiptDialog && activeReceiptSale != null) {
            ThermalReceiptDialog(
                sale = activeReceiptSale!!,
                cartItems = activeReceiptCartItems,
                discountAmount = activeReceiptDiscountAmount,
                taxAmount = activeReceiptTaxAmount,
                viewModel = viewModel,
                onClose = {
                    showThermalReceiptDialog = false
                    activeReceiptSale = null
                    activeReceiptCartItems = null
                }
            )
        }
    }
}

@Composable
fun SaleRowItem(
    sale: Sale,
    onDelete: () -> Unit,
    onSettle: (() -> Unit)? = null,
    onPrint: () -> Unit,
    dateFormat: SimpleDateFormat,
    currencyFormat: NumberFormat
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sale_card_${sale.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Circle UI indicator
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sale.customerName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = sale.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dateFormat.format(Date(sale.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )

                    if (sale.isCredit) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (sale.creditPaid) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = androidx.compose.ui.graphics.Color(0xFFE8F5E9), // subtle light green
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Settled Credit",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = androidx.compose.ui.graphics.Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = androidx.compose.ui.graphics.Color(0xFFFFF3E0), // subtle light orange
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Unpaid Credit (BNPL)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = androidx.compose.ui.graphics.Color(0xFFE65100),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .clickable { onSettle?.invoke() }
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Settle Now",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "+${currencyFormat.format(sale.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onPrint,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("print_sale_${sale.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "Print thermal receipt",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_sale_${sale.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete sale record",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProductRowItem(
    product: Product,
    format: NumberFormat,
    onAddStock: () -> Unit,
    onRemoveStock: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onWriteOffSpoilage: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // First Row: Title, Category label and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Category Badge
                    Text(
                        text = product.category.uppercase(Locale.US),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "SKU: ${product.sku}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onWriteOffSpoilage, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = "Write-off spoiled or damaged stock", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit product details", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete product", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Second Row: Price markup profit margin & Inventory stock metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Costs Information and Margin
                Column {
                    Text(
                        text = "Retail: ${format.format(product.price)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Cost: ${format.format(product.costPrice)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    
                    // Markup Margin calculating
                    val rawProfit = product.price - product.costPrice
                    val marginPercentage = if (product.price > 0.0) (rawProfit / product.price) * 100.0 else 0.0
                    Text(
                        text = String.format(Locale.US, "Profit: %s (%.0f%% Margin)", format.format(rawProfit), marginPercentage),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (rawProfit > 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }

                // Stock management section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quick edit stock buttons
                    IconButton(onClick = onRemoveStock, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.RemoveCircle, contentDescription = "reduce stock quantity", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }

                    // Stock Quantity label styled with background depending on inventory severity
                    val stockBgColor = when {
                        product.stockQuantity == 0 -> MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                        product.stockQuantity in 1..9 -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    }
                    val stockTextColor = when {
                        product.stockQuantity == 0 -> MaterialTheme.colorScheme.error
                        product.stockQuantity in 1..9 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                    val stockLabel = when {
                        product.stockQuantity == 0 -> "OUT OF STOCK"
                        product.stockQuantity in 1..9 -> "${product.stockQuantity} LOW TEMP"
                        else -> "${product.stockQuantity} IN STOCK"
                    }

                    Box(
                        modifier = Modifier
                            .background(stockBgColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stockLabel,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = stockTextColor
                        )
                    }

                    IconButton(onClick = onAddStock, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.AddCircle, contentDescription = "increase stock quantity", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun ThermalReceiptDialog(
    sale: Sale,
    cartItems: List<Pair<Product, Int>>?,
    discountAmount: Double,
    taxAmount: Double,
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val storeName by viewModel.storeName.collectAsState()
    val storePhone by viewModel.storePhone.collectAsState()
    val storeLocation by viewModel.storeLocation.collectAsState()
    val storeFooter by viewModel.storeFooter.collectAsState()
    val storeTaxId by viewModel.storeTaxId.collectAsState()

    val receiptText = remember(sale, cartItems, discountAmount, taxAmount, storeName, storePhone, storeLocation, storeFooter, storeTaxId) {
        com.example.util.ReceiptPrinter.formatThermalReceipt(
            sale = sale,
            cartItems = cartItems,
            discountAmount = discountAmount,
            taxAmount = taxAmount,
            storeName = storeName,
            storePhone = storePhone,
            storeLocation = storeLocation,
            storeFooter = storeFooter,
            storeTaxId = storeTaxId
        )
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header of Dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Thermal Receipt Preview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Dialog")
                    }
                }

                // Interactive receipt roll preview (White slip mimicking real thermal paper)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFF9F9F9)),
                    border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFE2E2E2)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        val scrollState = rememberScrollState()
                        Text(
                            text = receiptText,
                            style = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = androidx.compose.ui.graphics.Color.Black,
                                lineHeight = 14.sp
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        )
                    }
                }

                // Primary Quick Actions (Copy to clipboard/Share/Native print)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Button 1: Copy to Clipboard
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("POS Thermal Receipt", receiptText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Receipt text copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("copy_receipt_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }

                    // Button 2: Native Android Print Spooler
                    Button(
                        onClick = {
                            com.example.util.ReceiptPrinter.printNativeSystemsReceipt(
                                context,
                                sale,
                                cartItems,
                                discountAmount,
                                taxAmount
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("print_receipt_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print POS", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                }

                // Button 3: Share (whatsapp/mail)
                OutlinedButton(
                    onClick = {
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, receiptText)
                            type = "text/plain"
                        }
                        val shareIntent = android.content.Intent.createChooser(sendIntent, "Share Receipt Via")
                        context.startActivity(shareIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("share_receipt_btn")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Electronic Receipt")
                }
            }
        }
    }
}
