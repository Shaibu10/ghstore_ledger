package com.example.ui.customers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Customer
import com.example.data.Loan
import com.example.ui.MainViewModel

@Composable
fun CustomerManagementScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val customers by viewModel.customers.collectAsState()
    val loans by viewModel.loans.collectAsState()
    val stats by viewModel.financialStats.collectAsState()
    val context = LocalContext.current

    var selectedSubTab by remember { mutableStateOf("directory") } // "directory" or "loans"
    var searchQuery by remember { mutableStateOf("") }
    
    // Dialog control
    var showAddDialog by remember { mutableStateOf(false) }
    var showIssueLoanDialog by remember { mutableStateOf(false) }
    var showRepayDialog by remember { mutableStateOf(false) }

    // Add Customer State
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    // Issue Loan State
    var selectedCustomerForLoan by remember { mutableStateOf<Customer?>(null) }
    var loanAmountString by remember { mutableStateOf("") }
    var loanInterestString by remember { mutableStateOf("0") }
    var loanDaysString by remember { mutableStateOf("30") }

    // Repay Loan State
    var selectedLoanForRepay by remember { mutableStateOf<Loan?>(null) }
    var repayAmountString by remember { mutableStateOf("") }

    val filteredCustomers = customers.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.email.contains(searchQuery, ignoreCase = true) ||
        it.phone.contains(searchQuery, ignoreCase = true)
    }

    val filteredLoans = loans.filter {
        it.customerName.contains(searchQuery, ignoreCase = true)
    }

    val df = remember { java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US) }
    val currencyFmt = remember { java.text.DecimalFormat("GH₵#,##0.00") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("customer_management_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header Title block
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = if (selectedSubTab == "directory") "Customer Directory" else "Credit & Loans Ledger",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (selectedSubTab == "directory") "Manage customer accounts & communications" else "Issue & track repayments (Cash at Hand: ${currencyFmt.format(stats.moneyAtHand)})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            // Sub tabs Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .background(
                            color = if (selectedSubTab == "directory") MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { 
                            selectedSubTab = "directory"
                            searchQuery = "" // clear search on shift
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Directory",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedSubTab == "directory") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .background(
                            color = if (selectedSubTab == "loans") MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { 
                            selectedSubTab = "loans"
                            searchQuery = "" // clear search on shift
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Loans Ledger",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedSubTab == "loans") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        val activeLoansCount = loans.count { !it.isRepaid }
                        if (activeLoansCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.error, CircleShape)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = activeLoansCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onError,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Central search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("customer_search_input"),
                placeholder = { Text(if (selectedSubTab == "directory") "Search by name, email, phone..." else "Search loans by customer name...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (selectedSubTab == "directory") {
                // DIRECTORY SUBTAB
                if (filteredCustomers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isEmpty()) "No customers saved yet" else "No matching customers",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "Tap the plus button below to register a customer",
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
                        items(filteredCustomers) { cust ->
                            CustomerCardWithLoanOption(
                                customer = cust,
                                onDelete = {
                                    viewModel.deleteCustomer(cust.id)
                                    Toast.makeText(context, "${cust.name} deleted", Toast.LENGTH_SHORT).show()
                                },
                                onCall = {
                                    triggerDialIntent(context, cust.phone)
                                },
                                onEmail = {
                                    triggerEmailIntent(context, cust.email)
                                },
                                onGiveLoan = {
                                    selectedCustomerForLoan = cust
                                    loanAmountString = ""
                                    loanInterestString = "5.0"
                                    loanDaysString = "30"
                                    showIssueLoanDialog = true
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp)) // Clearance
                        }
                    }
                }
            } else {
                // LOANS SUBTAB
                val totalLoansGiven = loans.sumOf { it.amount }
                val totalOutstanding = stats.totalLoansOutstanding
                val totalRepayments = loans.sumOf { it.repaidAmount }

                // Mini stats panel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Disbursed Loans", style = MaterialTheme.typography.labelSmall)
                            Text(currencyFmt.format(totalLoansGiven), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1.5f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Outstanding Active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            Text(currencyFmt.format(totalOutstanding), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Total Repaid", style = MaterialTheme.typography.labelSmall)
                            Text(currencyFmt.format(totalRepayments), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black)
                        }
                    }
                }

                if (filteredLoans.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isEmpty()) "No customer loan records" else "No matching loan records",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "To issue a loan, switch to Directory, click the Give Loan button next to a customer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredLoans) { loan ->
                            LoanCardItem(
                                loan = loan,
                                onRepayClick = {
                                    selectedLoanForRepay = loan
                                    repayAmountString = ""
                                    showRepayDialog = true
                                },
                                onDelete = {
                                    viewModel.deleteLoan(loan.id)
                                    Toast.makeText(context, "Loan entry deleted", Toast.LENGTH_SHORT).show()
                                },
                                currencyFmt = currencyFmt,
                                df = df
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                if (selectedSubTab == "directory") {
                    name = ""
                    email = ""
                    phone = ""
                    showAddDialog = true
                } else {
                    // Open Give Loan dialog directly, requiring selection
                    selectedCustomerForLoan = null
                    loanAmountString = ""
                    loanInterestString = "5.0"
                    loanDaysString = "30"
                    showIssueLoanDialog = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_add_customer"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Icon")
        }

        // --- REGISTER CUSTOMER ALERT DIALOG ---
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                confirmButton = {
                    Button(
                        modifier = Modifier.testTag("dialog_confirm_add_customer"),
                        onClick = {
                            if (name.isBlank()) {
                                Toast.makeText(context, "Name cannot be blank", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.addCustomer(name, email, phone)
                                showAddDialog = false
                                Toast.makeText(context, "Added $name successfully", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Add Customer")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showAddDialog = false }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Register Customer", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Customer Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_customer_name"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_customer_email"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_customer_phone"),
                            singleLine = true
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // --- ISSUE CUSTOMER LOAN DIALOG ---
        if (showIssueLoanDialog) {
            var selectedCust by remember { mutableStateOf(selectedCustomerForLoan) }
            val balance = stats.moneyAtHand

            AlertDialog(
                onDismissRequest = { showIssueLoanDialog = false },
                confirmButton = {
                    val amountVal = loanAmountString.toDoubleOrNull() ?: 0.0
                    val interestVal = loanInterestString.toDoubleOrNull() ?: 0.0
                    val daysVal = loanDaysString.toLongOrNull() ?: 30L

                    Button(
                        onClick = {
                            if (selectedCust == null) {
                                Toast.makeText(context, "Please select/identify a customer", Toast.LENGTH_LONG).show()
                            } else if (amountVal <= 0.0) {
                                Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_LONG).show()
                            } else if (amountVal > balance) {
                                Toast.makeText(context, "Insufficient physical Cash at Hand to grant this loan!", Toast.LENGTH_LONG).show()
                            } else {
                                val dueTime = System.currentTimeMillis() + (daysVal * 24L * 3600L * 1000L)
                                viewModel.giveLoan(
                                    customerId = selectedCust!!.id,
                                    customerName = selectedCust!!.name,
                                    amount = amountVal,
                                    interestRate = interestVal,
                                    dueDate = dueTime
                                )
                                showIssueLoanDialog = false
                                Toast.makeText(context, "Loan of ${currencyFmt.format(amountVal)} issued to ${selectedCust!!.name}!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Give Loan")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showIssueLoanDialog = false }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Issue Capital Loan", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Net Retained Balance: ${currencyFmt.format(balance)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (balance > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )

                        if (selectedCust != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                  ) {
                                    Column {
                                        Text("Recipient", style = MaterialTheme.typography.labelSmall)
                                        Text(selectedCust!!.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    }
                                    if (selectedCustomerForLoan == null) {
                                        // let them change it
                                        Text(
                                            "Change",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable { selectedCust = null }
                                        )
                                    }
                                }
                            }
                        } else {
                            // customer list view
                            Text("Select Customer for Loan:", style = MaterialTheme.typography.labelMedium)
                            if (customers.isEmpty()) {
                                Text("No customers registered yet. Please register a customer first.", color = MaterialTheme.colorScheme.error)
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(customers) { c ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { selectedCust = c }
                                                .background(if (selectedCust?.id == c.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else androidx.compose.ui.graphics.Color.Transparent)
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(c.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = loanAmountString,
                            onValueChange = { loanAmountString = it },
                            label = { Text("Loan Principal (GH₵)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = (loanAmountString.toDoubleOrNull() ?: 0.0) > balance
                        )
                        if ((loanAmountString.toDoubleOrNull() ?: 0.0) > balance) {
                            Text(
                                text = "⚠️ Amount exceeds available net retained balance!",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = loanInterestString,
                                onValueChange = { loanInterestString = it },
                                label = { Text("Interest Rate (%)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = loanDaysString,
                                onValueChange = { loanDaysString = it },
                                label = { Text("Term (Days)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // --- REPAY LOAN DIALOG ---
        if (showRepayDialog && selectedLoanForRepay != null) {
            val loan = selectedLoanForRepay!!
            val principalWithInterest = loan.amount * (1 + loan.interestRate / 100.0)
            val leftToPay = principalWithInterest - loan.repaidAmount

            AlertDialog(
                onDismissRequest = { showRepayDialog = false },
                confirmButton = {
                    Button(
                        onClick = {
                            val repayVal = repayAmountString.toDoubleOrNull() ?: 0.0
                            if (repayVal <= 0.0) {
                                Toast.makeText(context, "Please enter a valid payment sum.", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.recordLoanRepayment(loan, repayVal)
                                showRepayDialog = false
                                Toast.makeText(context, "Repayment of ${currencyFmt.format(repayVal)} recorded!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Record Repayment")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showRepayDialog = false }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Record Repayment", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Recipient: ${loan.customerName}", fontWeight = FontWeight.SemiBold)
                        Text("Total Amount Due: ${currencyFmt.format(principalWithInterest)} (${loan.interestRate}% Int.)", style = MaterialTheme.typography.bodyMedium)
                        Text("Already Repaid: ${currencyFmt.format(loan.repaidAmount)}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Remaining Balance: ${currencyFmt.format(leftToPay)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        OutlinedTextField(
                            value = repayAmountString,
                            onValueChange = { repayAmountString = it },
                            label = { Text("Repayment Amount Paid (GH₵)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
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
fun CustomerCard(
    customer: Customer,
    onDelete: () -> Unit,
    onCall: () -> Unit,
    onEmail: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_card_${customer.id}"),
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
                // Circle icon mockup
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = customer.name.firstOrNull()?.uppercase() ?: "C",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = customer.email.ifBlank { "No email registered" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = customer.phone.ifBlank { "No phone registered" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Quick Actions: Email, Call, and Delete
            Row {
                if (customer.email.isNotBlank()) {
                    IconButton(
                        onClick = onEmail,
                        modifier = Modifier.testTag("email_customer_${customer.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email customer",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (customer.phone.isNotBlank()) {
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier.testTag("call_customer_${customer.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Call customer",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_customer_${customer.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete customer",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerCardWithLoanOption(
    customer: Customer,
    onDelete: () -> Unit,
    onCall: () -> Unit,
    onEmail: () -> Unit,
    onGiveLoan: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_card_${customer.id}"),
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
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = customer.name.firstOrNull()?.uppercase() ?: "C",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = customer.phone.ifBlank { "No phone registered" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onGiveLoan,
                    modifier = Modifier.testTag("give_loan_customer_${customer.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = "Give Loan to customer",
                        tint = androidx.compose.ui.graphics.Color(0xFF2E7D32),
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (customer.email.isNotBlank()) {
                    IconButton(
                        onClick = onEmail,
                        modifier = Modifier.testTag("email_customer_${customer.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email customer",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (customer.phone.isNotBlank()) {
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier.testTag("call_customer_${customer.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Call customer",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_customer_${customer.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete customer",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LoanCardItem(
    loan: Loan,
    onRepayClick: () -> Unit,
    onDelete: () -> Unit,
    currencyFmt: java.text.DecimalFormat,
    df: java.text.SimpleDateFormat
) {
    val totalWithInterest = loan.amount * (1 + loan.interestRate / 100.0)
    val remaining = totalWithInterest - loan.repaidAmount

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("loan_card_${loan.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (loan.isRepaid) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (loan.isRepaid) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f) else androidx.compose.ui.graphics.Color(0xFFFFF3E0),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (loan.isRepaid) "✓" else "L",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (loan.isRepaid) MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color(0xFFE65100)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = loan.customerName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Due on ${df.format(java.util.Date(loan.dueDate))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Loan entry",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Issued: ${currencyFmt.format(loan.amount)} " + if (loan.interestRate > 0) "(+${loan.interestRate}%)" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Total Repaid: ${currencyFmt.format(loan.repaidAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (!loan.isRepaid) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Outstanding: ${currencyFmt.format(remaining)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (loan.isRepaid) {
                    Box(
                        modifier = Modifier
                            .background(androidx.compose.ui.graphics.Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "FULLY REPAID",
                            style = MaterialTheme.typography.labelSmall,
                            color = androidx.compose.ui.graphics.Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = onRepayClick,
                        modifier = Modifier.testTag("repay_loan_button_${loan.id}"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Record Pay", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

// Intents trigger helpers

fun triggerDialIntent(context: Context, phoneNumber: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${phoneNumber.trim()}")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot dial. Action unsupported.", Toast.LENGTH_SHORT).show()
    }
}

fun triggerEmailIntent(context: Context, emailAddress: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${emailAddress.trim()}")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No email client configured.", Toast.LENGTH_SHORT).show()
    }
}
