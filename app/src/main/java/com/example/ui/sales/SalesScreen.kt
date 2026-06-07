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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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

@Composable
fun SalesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val sales by viewModel.sales.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val products by viewModel.products.collectAsState()
    val context = LocalContext.current

    // Navigation and screen-level states
    var activeTab by remember { mutableStateOf("registry") } // "registry" or "catalog"
    var productSearchQuery by remember { mutableStateOf("") }

    // Dialog state controllers
    var showAddSaleDialog by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    // Add Sale input states
    var saleMethodType by remember { mutableStateOf("catalog") } // "catalog" or "custom"
    var amountString by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var customerDropdownExpanded by remember { mutableStateOf(false) }
    
    // Catalog POS shopping cart state variables
    var cartItems by remember { mutableStateOf<List<Pair<Product, Int>>>(emptyList()) }
    var currentSelectedCartProduct by remember { mutableStateOf<Product?>(null) }
    var productDropdownExpanded by remember { mutableStateOf(false) }
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
                                    viewModel.updateProductStock(product.id, product.stockQuantity + 1)
                                    Toast.makeText(context, "${product.name} stock increased +1", Toast.LENGTH_SHORT).show()
                                },
                                onRemoveStock = {
                                    if (product.stockQuantity > 0) {
                                        viewModel.updateProductStock(product.id, product.stockQuantity - 1)
                                        Toast.makeText(context, "${product.name} stock reduced -1", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Stock is already empty", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onEdit = {
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
                                    viewModel.deleteProduct(product.id)
                                    Toast.makeText(context, "${product.name} removed from catalog", Toast.LENGTH_SHORT).show()
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
                    // Reset New Sale Form Dialog
                    amountString = ""
                    description = ""
                    selectedCustomer = null
                    cartItems = emptyList()
                    currentSelectedCartProduct = null
                    selectQtyString = "1"
                    discountType = "none"
                    discountValueString = ""
                    isCreditSelection = false
                    showAddSaleDialog = true
                } else {
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
                            }
                        }
                    ) {
                        Text(if (saleMethodType == "custom") "Save Bill" else "Checkout Basket")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showAddSaleDialog = false }) {
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
                            .wrapContentHeight()
                    ) {
                        // Customer dropdown account selection (shared)
                        Column {
                            Text(
                                text = "Select Customer Link (Optional)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { customerDropdownExpanded = true }
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    .padding(vertical = 10.dp, horizontal = 12.dp)
                                    .testTag("checkout_customer_dropdown"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedCustomer?.name ?: "Cash Walk-In (Retail Handout)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "chevron")
                            }

                            DropdownMenu(
                                expanded = customerDropdownExpanded,
                                onDismissRequest = { customerDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.7f)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Cash Walk-In (Retail Handout)", fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        selectedCustomer = null
                                        customerDropdownExpanded = false
                                    }
                                )
                                customers.forEach { cust ->
                                    DropdownMenuItem(
                                        text = { Text(cust.name) },
                                        onClick = {
                                            selectedCustomer = cust
                                            customerDropdownExpanded = false
                                        }
                                    )
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
                                // Select product dropdown
                                Column {
                                    Text(
                                        text = "Add Item to Basket",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { productDropdownExpanded = true }
                                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                            .padding(vertical = 10.dp, horizontal = 12.dp)
                                            .testTag("add_item_dropdown"),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = currentSelectedCartProduct?.let { "${it.name} (${fmt.format(it.price)}) - Stock: ${it.stockQuantity}" } ?: "Choose Product...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "dropdown")
                                    }

                                    DropdownMenu(
                                        expanded = productDropdownExpanded,
                                        onDismissRequest = { productDropdownExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.7f)
                                    ) {
                                        products.forEach { prod ->
                                            DropdownMenuItem(
                                                text = { Text("${prod.name} (${fmt.format(prod.price)}) [Qty: ${prod.stockQuantity}]") },
                                                onClick = {
                                                    currentSelectedCartProduct = prod
                                                    productDropdownExpanded = false
                                                }
                                            )
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
                        modifier = Modifier.fillMaxWidth()
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
                        modifier = Modifier.fillMaxWidth()
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
    }
}

@Composable
fun SaleRowItem(
    sale: Sale,
    onDelete: () -> Unit,
    onSettle: (() -> Unit)? = null,
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
    onDelete: () -> Unit
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
