package com.example.ui.sales

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.CustomerEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.SaleEntity
import com.example.data.entity.SaleItemEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesCheckoutScreen(
    viewModel: SalesViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredProducts by viewModel.filteredProducts.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val customerList by viewModel.customerList.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val paymentMethod by viewModel.paymentMethod.collectAsState()
    val discount by viewModel.discount.collectAsState()
    val taxRate by viewModel.taxRate.collectAsState()
    val subtotal by viewModel.subtotal.collectAsState()
    val total by viewModel.total.collectAsState()
    val checkoutState by viewModel.checkoutState.collectAsState()

    var showDiscountDialog by remember { mutableStateOf(false) }
    var showCustomerDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }

    // When checkout succeeds, show receipt
    LaunchedEffect(checkoutState) {
        if (checkoutState is CheckoutState.Success) {
            showReceiptDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Checkout Terminal",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Text(
                            text = "Dynamic Cart & Real-time Stock Deduction",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("checkout_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::clearCart) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Cart")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("checkout_main_bar")
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isTablet = maxWidth > 650.dp

            if (isTablet) {
                // Expanded Dual-Pane side-by-side catalog and checkout
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1.1f).fillMaxHeight()) {
                        ProductSelectorPane(
                            searchQuery = searchQuery,
                            onQueryChanged = viewModel::onSearchQueryChanged,
                            products = filteredProducts,
                            onProductSelected = viewModel::addToCart
                        )
                    }
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Box(modifier = Modifier.weight(0.9f).fillMaxHeight()) {
                        CheckoutCartPane(
                            cart = cart,
                            selectedCustomer = selectedCustomer,
                            paymentMethod = paymentMethod,
                            discount = discount,
                            taxRate = taxRate,
                            subtotal = subtotal,
                            total = total,
                            onUpdateQty = viewModel::updateQuantity,
                            onRemoveItem = viewModel::removeFromCart,
                            onChangeCustomer = { showCustomerDialog = true },
                            onSelectPayment = viewModel::updatePaymentMethod,
                            onAddDiscount = { showDiscountDialog = true },
                            onCheckout = viewModel::checkoutCart
                        )
                    }
                }
            } else {
                // Mobile layout - scrollable list with collapse states or layout panels. Let's do a beautiful stack.
                var activeTabIsCart by remember { mutableStateOf(false) }

                Column(modifier = Modifier.fillMaxSize()) {
                    TabRow(selectedTabIndex = if (activeTabIsCart) 1 else 0) {
                        Tab(
                            selected = !activeTabIsCart,
                            onClick = { activeTabIsCart = false },
                            text = { Text("1. Select Products (${filteredProducts.size})") }
                        )
                        Tab(
                            selected = activeTabIsCart,
                            onClick = { activeTabIsCart = true },
                            text = { Text("2. Cart (${cart.values.sum().toInt()} Items)") },
                            modifier = Modifier.testTag("mobile_cart_tab")
                        )
                    }

                    if (!activeTabIsCart) {
                        Box(modifier = Modifier.weight(1f)) {
                            ProductSelectorPane(
                                searchQuery = searchQuery,
                                onQueryChanged = viewModel::onSearchQueryChanged,
                                products = filteredProducts,
                                onProductSelected = {
                                    viewModel.addToCart(it)
                                    // Visual Toast/Notice option can go here. We increment cart state.
                                }
                            )
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f)) {
                            CheckoutCartPane(
                                cart = cart,
                                selectedCustomer = selectedCustomer,
                                paymentMethod = paymentMethod,
                                discount = discount,
                                taxRate = taxRate,
                                subtotal = subtotal,
                                total = total,
                                onUpdateQty = viewModel::updateQuantity,
                                onRemoveItem = viewModel::removeFromCart,
                                onChangeCustomer = { showCustomerDialog = true },
                                onSelectPayment = viewModel::updatePaymentMethod,
                                onAddDiscount = { showDiscountDialog = true },
                                onCheckout = viewModel::checkoutCart
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal: Customer Selection Picker Dialog
    if (showCustomerDialog) {
        AlertDialog(
            onDismissRequest = { showCustomerDialog = false },
            title = { Text("Select Loyalty Customer") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectCustomer(null)
                                    showCustomerDialog = false
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                "Anonymous Customer (Walk-in)",
                                modifier = Modifier.padding(16.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    items(customerList) { customer ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectCustomer(customer)
                                    showCustomerDialog = false
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(customer.name, fontWeight = FontWeight.Bold)
                                Text(customer.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCustomerDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Modal: Add Discount Dialog
    if (showDiscountDialog) {
        var discountInput by remember { mutableStateOf(discount.toString()) }
        AlertDialog(
            onDismissRequest = { showDiscountDialog = false },
            title = { Text("Apply Flat Discount") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter total discount amount in Ghanaian Cedis (GH₵)", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = discountInput,
                        onValueChange = { discountInput = it },
                        label = { Text("Discount (GH₵)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("discount_input_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = discountInput.toDoubleOrNull() ?: 0.0
                        viewModel.updateDiscount(parsed)
                        showDiscountDialog = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal: Printed Receipt Customer Ticket Dialogue
    if (showReceiptDialog && checkoutState is CheckoutState.Success) {
        val successData = checkoutState as CheckoutState.Success
        CustomerReceiptTicketDialog(
            sale = successData.sale,
            items = successData.items,
            products = filteredProducts + cart.keys.toList(), // combine to locate catalog descriptions
            customer = selectedCustomer,
            onClose = {
                showReceiptDialog = false
                viewModel.clearCart()
                viewModel.resetCheckoutState()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductSelectorPane(
    searchQuery: String,
    onQueryChanged: (String) -> Unit,
    products: List<ProductEntity>,
    onProductSelected: (ProductEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // High polish instant search text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChanged,
            placeholder = { Text("Filter catalog products...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onQueryChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("checkout_search_bar")
        )

        if (products.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No products found. Use Product Catalog to register new stock items.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("checkout_product_selector_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(products) { product ->
                    val isOutOfStock = product.quantity <= 0.0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isOutOfStock) { onProductSelected(product) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isOutOfStock) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Barcode: ${product.barcode} | In Stock: ${product.quantity}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isOutOfStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "GH₵ " + String.format(Locale.getDefault(), "%,.2f", product.sellPrice),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                if (isOutOfStock) {
                                    Text("OUT OF STOCK", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                } else {
                                    Text("Click to Add", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CheckoutCartPane(
    cart: Map<ProductEntity, Double>,
    selectedCustomer: CustomerEntity?,
    paymentMethod: String,
    discount: Double,
    taxRate: Double,
    subtotal: Double,
    total: Double,
    onUpdateQty: (ProductEntity, Double) -> Unit,
    onRemoveItem: (ProductEntity) -> Unit,
    onChangeCustomer: () -> Unit,
    onSelectPayment: (String) -> Unit,
    onAddDiscount: () -> Unit,
    onCheckout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Selected loyalty customer indicator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Customer Loyalties", style = MaterialTheme.typography.labelSmall)
                    Text(
                        selectedCustomer?.name ?: "Walk-in Customer (General)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                TextButton(onClick = onChangeCustomer) {
                    Text("Change", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Cart items scroller
        Box(modifier = Modifier.weight(1f)) {
            if (cart.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text("No items added to cart.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().testTag("checkout_cart_scroller_list"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(cart.entries.toList(), key = { it.key.id }) { (product, qty) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "GH₵ ${String.format(Locale.getDefault(), "%,.2f", product.sellPrice)} each",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Interactive increment and decrement controls
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { onUpdateQty(product, qty - 1) },
                                        modifier = Modifier.size(32.dp).testTag("decrease_qty_${product.id}")
                                    ) {
                                        Text("-", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Text(
                                        text = qty.toInt().toString(),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    IconButton(
                                        onClick = { onUpdateQty(product, qty + 1) },
                                        modifier = Modifier.size(32.dp).testTag("increase_qty_${product.id}")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase")
                                    }
                                }

                                // Remove
                                IconButton(onClick = { onRemoveItem(product) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Calculations & Payment options panels
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Payment Method choose
                Text("Select Billing Route", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val methods = listOf("CASH", "MOBILE_MONEY", "CARD")
                    methods.forEach { method ->
                        val selected = paymentMethod == method
                        val mName = when (method) {
                            "CASH" -> "Cash"
                            "MOBILE_MONEY" -> "MoMo"
                            else -> "Bank Card"
                        }
                        val icon = when (method) {
                            "CASH" -> Icons.Default.Star
                            "MOBILE_MONEY" -> Icons.Default.Check
                            else -> Icons.Default.Build
                        }
                        Button(
                            onClick = { onSelectPayment(method) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(44.dp).testTag("payment_option_$method")
                        ) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(mName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Calculations Details
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                    Text("GH₵ " + String.format(Locale.getDefault(), "%,.2f", subtotal), style = MaterialTheme.typography.bodyMedium)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Haggled Discount", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("- GH₵ " + String.format(Locale.getDefault(), "%,.2f", discount), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp).clickable { onAddDiscount() }, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("VAT Tax (${taxRate.toInt()}%)", style = MaterialTheme.typography.bodyMedium)
                    Text("GH₵ " + String.format(Locale.getDefault(), "%,.2f", subtotal * (taxRate / 100.0)), style = MaterialTheme.typography.bodyMedium)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Outlay", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                    Text(
                        "GH₵ " + String.format(Locale.getDefault(), "%,.2f", total),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onCheckout,
                    enabled = cart.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("checkout_confirm_btn")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Complete Invoice Checkout", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

// Gorgeous Printed Printed Receipt Dialog
@Composable
fun CustomerReceiptTicketDialog(
    sale: SaleEntity,
    items: List<SaleItemEntity>,
    products: List<ProductEntity>,
    customer: CustomerEntity?,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(8.dp),
            shape = RoundedCornerShape(8.dp), // flat edges, like true receipt paper
            colors = CardDefaults.cardColors(containerColor = Color.White), // classical white thermal paper
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Receipt Header
                Text(
                    text = "GH POS & RETAILS LTD",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Text(
                    text = "Accra Mall Road, Accra-Ghana",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                )
                Text(
                    text = "+233 (0) 244-123456",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                )
                
                Text(
                    text = "----------------------------------",
                    color = Color.Black,
                    fontFamily = FontFamily.Monospace
                )

                // Invoice metadata
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("INVOICE:", color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text(sale.invoiceNumber, color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("DATE:", color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    Text(format.format(Date(sale.createdAt)), color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("CASHIER:", color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text(sale.cashierId, color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                if (customer != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("CUSTOMER:", color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text(customer.name, color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                Text(
                    text = "----------------------------------",
                    color = Color.Black,
                    fontFamily = FontFamily.Monospace
                )

                // Sales Items column Headers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Item Description", modifier = Modifier.weight(1.2f), color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("Qty", modifier = Modifier.weight(0.3f), color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    Text("Total", modifier = Modifier.weight(0.5f), color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
                }

                // Sales Items row entries list
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items.forEach { item ->
                        val prodName = products.find { it.id == item.productId }?.name ?: "Unknown Product"
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(prodName, modifier = Modifier.weight(1.2f), color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text(item.quantity.toInt().toString(), modifier = Modifier.weight(0.3f), color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
                            Text("GH₵ " + String.format(Locale.getDefault(), "%,.2f", item.subtotal), modifier = Modifier.weight(0.5f), color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.End)
                        }
                    }
                }

                Text(
                    text = "----------------------------------",
                    color = Color.Black,
                    fontFamily = FontFamily.Monospace
                )

                // Summary calculations details
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("SUBTOTAL:", color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("GH₵ " + String.format(Locale.getDefault(), "%,.2f", sale.subtotal), color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                if (sale.discount > 0.0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("DISCOUNT:", color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text("- GH₵ " + String.format(Locale.getDefault(), "%,.2f", sale.discount), color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
                if (sale.tax > 0.0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TAX VAT:", color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text("GH₵ " + String.format(Locale.getDefault(), "%,.2f", sale.tax), color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL AMOUNT PAID:", color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("GH₵ " + String.format(Locale.getDefault(), "%,.2f", sale.total), color = Color.Black, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("PAYMENT METHOD:", color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text(sale.paymentMethod, color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }

                Text(
                    text = "==================================",
                    color = Color.Black,
                    fontFamily = FontFamily.Monospace
                )

                // Footer message
                Text(
                    text = "THANK YOU FOR SHOPPING WITH US!",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Software power by Gh POS Solutions",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontFamily = FontFamily.Monospace)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth().testTag("close_receipt_dialog_btn")
                ) {
                    Text("Start New Checkout")
                }
            }
        }
    }
}
