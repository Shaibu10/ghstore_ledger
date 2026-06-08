package com.example.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.clickable
import android.content.Intent
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sales by viewModel.sales.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val loans by viewModel.loans.collectAsState()

    var selectedTab by remember { mutableStateOf("trends") } // "trends", "cashflow", "backup"

    // Dynamic cash calculations
    val totalSales = sales.sumOf { it.amount }
    val totalExpenses = expenses.sumOf { it.amount }

    // Grouping expenses by category for the donut chart
    val expenseCategories = expenses.groupBy { it.category }
        .mapValues { (_, list) -> list.sumOf { it.amount } }

    val totalCategorizedExpenses = expenseCategories.values.sum()

    val fmt = java.text.DecimalFormat("GH₵#,##0.00")

    // Calculate aggregated daily sales and expenses for the bar chart
    val cal = Calendar.getInstance()
    val dailyData = (0..5).map { offset ->
        cal.timeInMillis = System.currentTimeMillis()
        cal.add(Calendar.DAY_OF_YEAR, -offset)
        val dayKey = cal.get(Calendar.DAY_OF_YEAR)
        val salesSum = sales.filter {
            val sCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            sCal.get(Calendar.DAY_OF_YEAR) == dayKey
        }.sumOf { it.amount }

        val expenseSum = expenses.filter {
            val eCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            eCal.get(Calendar.DAY_OF_YEAR) == dayKey
        }.sumOf { it.amount }

        val label = when (offset) {
            0 -> "Today"
            1 -> "Yest."
            else -> {
                val dayOfWeek = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US) ?: ""
                dayOfWeek
            }
        }
        DailyLedgerPoint(label = label, sales = salesSum, expenses = expenseSum)
    }.reversed() // Put in ascending historical order

    // --- Dynamic Cash Flow Metrics ---
    val cashSales = sales.filter { !it.isCredit }
    val totalCashSalesAmount = cashSales.sumOf { it.amount }

    val creditSalesPaid = sales.filter { it.isCredit && it.creditPaid }
    val totalCreditSalesPaidAmount = creditSalesPaid.sumOf { it.amount }

    val totalLoanRepaymentsAmount = loans.sumOf { it.repaidAmount }

    val totalInflowAmount = totalCashSalesAmount + totalCreditSalesPaidAmount + totalLoanRepaymentsAmount

    val totalExpensesAmount = expenses.sumOf { it.amount }
    val totalLoansIssuedAmount = loans.sumOf { it.amount }

    val totalOutflowAmount = totalExpensesAmount + totalLoansIssuedAmount

    val netCashFlowAmount = totalInflowAmount - totalOutflowAmount

    // Cash Movement Ledger Chronology
    val movements = remember(sales, expenses, loans) {
        val list = mutableListOf<CashMovement>()
        
        // Add Cash Sales
        sales.filter { !it.isCredit }.forEach { s ->
            list.add(
                CashMovement(
                    id = "sale_${s.id}",
                    type = "Cash Sale",
                    description = s.description.ifBlank { "Walk-in sale" },
                    amount = s.amount,
                    timestamp = s.timestamp,
                    isInflow = true
                )
            )
        }
        
        // Add Paid Credit Sales
        sales.filter { it.isCredit && it.creditPaid }.forEach { s ->
            list.add(
                CashMovement(
                    id = "credit_${s.id}",
                    type = "Credit Recovered",
                    description = "Paid credit: ${s.description.ifBlank { "Sale" }} by ${s.customerName}",
                    amount = s.amount,
                    timestamp = s.timestamp,
                    isInflow = true
                )
            )
        }
        
        // Add Loan Repayments
        loans.filter { l -> l.repaidAmount > 0 }.forEach { l ->
            list.add(
                CashMovement(
                    id = "loan_repay_${l.id}",
                    type = "Loan Repayment",
                    description = "Received from ${l.customerName}",
                    amount = l.repaidAmount,
                    timestamp = l.timestamp,
                    isInflow = true
                )
            )
        }
        
        // Add Expenses
        expenses.forEach { e ->
            list.add(
                CashMovement(
                    id = "expense_${e.id}",
                    type = "Expense Paid",
                    description = "[${e.category}] ${e.description}",
                    amount = e.amount,
                    timestamp = e.timestamp,
                    isInflow = false
                )
            )
        }
        
        // Add Loan Issuing
        loans.forEach { l ->
            list.add(
                CashMovement(
                    id = "loan_issue_${l.id}",
                    type = "Loan Granted",
                    description = "Granted cash to ${l.customerName}",
                    amount = l.amount,
                    timestamp = l.timestamp,
                    isInflow = false
                )
            )
        }
        
        list.sortedByDescending { it.timestamp }
    }

    // Share Text Report String Creator and Launcher (SAF Create Documents)
    val createReportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let {
            try {
                val reportStr = generateCashFlowReportString(sales, expenses, loans, movements)
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(reportStr.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Cash Flow Report printed successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error printing report: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    var showPreviewDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("reports_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = "GH Shop Accounts",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Explore real-time revenue cycles, cash retention, and admin tools.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Modern Tab-Selector Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(4.dp)
                ) {
                    // Trends Tab Clicker
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .background(
                                color = if (selectedTab == "trends") MaterialTheme.colorScheme.surface else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedTab = "trends" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Trends",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == "trends") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Cash Flow Tab Clicker
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(38.dp)
                            .background(
                                color = if (selectedTab == "cashflow") MaterialTheme.colorScheme.surface else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedTab = "cashflow" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cash Flow",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == "cashflow") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Maintenance Tab Clicker
                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .height(38.dp)
                            .background(
                                color = if (selectedTab == "backup") MaterialTheme.colorScheme.surface else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedTab = "backup" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Maintenance",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == "backup") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // --- TRENDS TAB ---
        if (selectedTab == "trends") {
            // --- SIDE BY SIDE HISTORICAL COLUMN REVENUE VS COST CHART ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("historical_bar_chart_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Ledger Trends (Last 6 Days)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Daily comparison of incoming sales vs outgoing expenses",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Draw the canvas double bar chart
                        SideBySideBarChart(points = dailyData)

                        // Legend markers
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sales (Inflows)", style = MaterialTheme.typography.labelMedium)
                            }
                            Spacer(modifier = Modifier.width(20.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Expenses (Costs)", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // --- EXPENSE CATEGORY DONUT CHART ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cost_distribution_chart_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Outlay Category Distribution",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Distribution breakout of accumulated operating expenditures",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        if (totalCategorizedExpenses == 0.0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No costs logged to breakdown.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Donut Canvas representation
                                Box(
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .aspectRatio(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CostDonutChart(categories = expenseCategories, total = totalCategorizedExpenses)
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Colored text grid listing categories
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val colorsMap = getCategoryColorMap()
                                    expenseCategories.forEach { (cat, amt) ->
                                        val percent = (amt / totalCategorizedExpenses * 100).toInt()
                                        val color = colorsMap[cat] ?: Color.Gray
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(color, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = cat,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${fmt.format(amt)} ($percent%)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- BUSINESS ADVICE CARD ---
            item {
                val recommendation = getBusinessRecommendation(salesSum = totalSales, expenseSum = totalExpenses, categoriesMap = expenseCategories)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_recommendation_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TipsAndUpdates,
                                contentDescription = "Tips and update recommendations icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Ledger Recommendation",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = recommendation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // --- CASH FLOW STATEMENT TAB ---
        if (selectedTab == "cashflow") {
            // General Statement and printing triggers
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cash_flow_statement_summary_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Physical Cash Flow Audit",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Reconciled",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Track the exact movement of actual cash at hand. Reconcile point-of-sale cash flows, recovering debts, and general outlays.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                        )

                        // Split Grid of Inflows vs Outflows
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // CASH INFLOW
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "⚡ CASH INFLOWS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF10B981) // bright green
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = fmt.format(totalInflowAmount),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "+ Cash Sales: ${fmt.format(totalCashSalesAmount)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "+ Credit Recv: ${fmt.format(totalCreditSalesPaidAmount)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "+ Loan Recv: ${fmt.format(totalLoanRepaymentsAmount)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            // CASH OUTFLOW
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "💸 CASH OUTFLOWS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = fmt.format(totalOutflowAmount),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "- Costs Paid: ${fmt.format(totalExpensesAmount)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "- Loans Issued: ${fmt.format(totalLoansIssuedAmount)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(14.dp)) // padding aligner
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // NET CASH RECONCILIATION RESULT CARD
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "NET RETAINED LIQUID CASH",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = fmt.format(netCashFlowAmount),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = "Money reconciled icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Trigger printable preview dialog
                        Button(
                            onClick = { showPreviewDialog = true },
                            modifier = Modifier.fillMaxWidth().testTag("show_cashflow_preview_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Reports Share")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Preview & Print Report", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // TRANSACTIONS LOG CHRONOLOGY HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Cash Movements Log",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${movements.size} events logged",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }

            // Render matching chronological ledger entries
            if (movements.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No cash flow events logged in database history.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(movements.size) { index ->
                    val movement = movements[index]
                    val isPos = movement.isInflow
                    val indicatorColor = if (isPos) Color(0xFF10B981) else MaterialTheme.colorScheme.tertiary
                    val timeStr = java.text.SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(java.util.Date(movement.timestamp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(indicatorColor.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPos) Icons.Default.TrendingUp else Icons.Default.Info,
                                        contentDescription = "flow",
                                        tint = indicatorColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = movement.type,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = movement.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = timeStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                            }
                            Text(
                                text = (if (isPos) "+ " else "- ") + fmt.format(movement.amount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = indicatorColor
                            )
                        }
                    }
                }
            }
        }

        // --- DATABASE MAINTENANCE TAB ---
        if (selectedTab == "backup") {
            item {
                BackupAndRestoreSection(viewModel = viewModel)
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // --- PREVIEW DIALOG TO COPY/PRINT/SHARE ---
    if (showPreviewDialog) {
        val reportString = remember(sales, expenses, loans, movements) {
            generateCashFlowReportString(sales, expenses, loans, movements)
        }

        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Copy Report Option
                    OutlinedButton(
                        onClick = {
                            try {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Cash Flow Statement", reportString)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Statement copied to clipboard!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Copy Text", style = MaterialTheme.typography.labelMedium)
                    }

                    // Native Share chopper (highly recommended for printing on Android)
                    Button(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, reportString)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Print/Share Ledger Report")
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Share / Send", style = MaterialTheme.typography.labelMedium)
                    }
                }
            },
            dismissButton = {
                // File-System Download alternative (real offline printing)
                OutlinedButton(
                    onClick = {
                        val fn = "ledger_cashflow_statement_${System.currentTimeMillis()}.txt"
                        createReportLauncher.launch(fn)
                        showPreviewDialog = false
                    },
                    modifier = Modifier.padding(top = 10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save to File", style = MaterialTheme.typography.labelMedium)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = "Inflow Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Print Cash Flow Statement", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Official cash reconciliation receipt sheet:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    // Monospace preview container
                    OutlinedTextField(
                        value = reportString,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp, 
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            lineHeight = 14.sp
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Text(
                        text = "Tip: Choose 'Share / Send' to directly send this report to a connected office printer or communication platform.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun BackupAndRestoreSection(
    viewModel: MainViewModel
) {
    val context = LocalContext.current

    // Launcher for exporting backup to a JSON file (Create Document SAF)
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            try {
                val jsonString = viewModel.generateBackupString()
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(jsonString.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Backup file saved successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving backup: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Selected file details
    var loadedFileName by remember { mutableStateOf<String?>(null) }
    var loadedFileContent by remember { mutableStateOf<String?>(null) }
    var showLoadedDialog by remember { mutableStateOf(false) }
    var showConfirmEraseDialog by remember { mutableStateOf(false) }

    // Launcher for selecting/restoring a backup JSON file (Open Document SAF)
    val selectBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val content = stream.bufferedReader(Charsets.UTF_8).use { br -> br.readText() }
                    
                    // Validate JSON signature
                    val tempRoot = org.json.JSONObject(content)
                    if (tempRoot.has("backupVersion")) {
                        loadedFileContent = content
                        loadedFileName = getFileName(context, uri) ?: "ledger_backup.json"
                        showLoadedDialog = true
                    } else {
                        Toast.makeText(context, "Invalid backup signature: backupVersion property missing.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load backup: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("backup_restore_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "System Backup & Restoration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Secure local backup file management. Save ledger records directly into a downloaded JSON file, or restore matching indices from an existing JSON backup document.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val fileName = "ledger_backup_${System.currentTimeMillis()}.json"
                        createBackupLauncher.launch(fileName)
                    },
                    modifier = Modifier.weight(1f).testTag("action_export_backup_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Export Backup Icon",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export file", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = {
                        selectBackupLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                    },
                    modifier = Modifier.weight(1f).testTag("action_import_backup_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Import Backup Icon",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore file", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    // --- BACKUP FILE LOADED DIALOG ---
    if (showLoadedDialog && loadedFileContent != null) {
        val stats = remember(loadedFileContent) { getBackupSummaryStats(loadedFileContent ?: "") }
        val dateStr = remember(loadedFileContent) {
            try {
                val root = org.json.JSONObject(loadedFileContent ?: "")
                val ts = root.optLong("timestamp", 0L)
                if (ts > 0L) {
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(ts))
                } else "N/A"
            } catch (e: Exception) {
                "N/A"
            }
        }

        AlertDialog(
            onDismissRequest = { showLoadedDialog = false },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val content = loadedFileContent
                            if (content != null) {
                                viewModel.restoreBackup(
                                    jsonStr = content,
                                    overwrite = false,
                                    onSuccess = {
                                        showLoadedDialog = false
                                        Toast.makeText(context, "Ledger Merged Successfully!", Toast.LENGTH_LONG).show()
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Merge / Upsert", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = {
                            showConfirmEraseDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Erase & Restore", style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLoadedDialog = false },
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    Text("Cancel")
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("System Backup Loaded", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "File: ${loadedFileName ?: "Selected JSON"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Backup Timestamp: $dateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    Text(
                        text = "Records summarized inside this backup state:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp).fillMaxWidth()) {
                            Text("• Customers: ${stats["customers"] ?: 0}", style = MaterialTheme.typography.bodySmall)
                            Text("• Sales Transactions: ${stats["sales"] ?: 0}", style = MaterialTheme.typography.bodySmall)
                            Text("• Expenses Logs: ${stats["expenses"] ?: 0}", style = MaterialTheme.typography.bodySmall)
                            Text("• Catalog Products: ${stats["products"] ?: 0}", style = MaterialTheme.typography.bodySmall)
                            Text("• Credit Loans: ${stats["loans"] ?: 0}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text(
                        text = "Choose 'Merge / Upsert' to integrate matching records alongside current data. Choose 'Erase & Restore' to rewrite and wipe all local databases first.",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // --- ERASE WARNING ALERT CONFIRMATION DIALOG ---
    if (showConfirmEraseDialog && loadedFileContent != null) {
        AlertDialog(
            onDismissRequest = { showConfirmEraseDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmEraseDialog = false
                        val content = loadedFileContent
                        if (content != null) {
                            viewModel.restoreBackup(
                                jsonStr = content,
                                overwrite = true,
                                onSuccess = {
                                    showLoadedDialog = false
                                    Toast.makeText(context, "Wiped and Complete File Restore Successful!", Toast.LENGTH_LONG).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, "Parse Failed: $err", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Proceed & Erase All")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmEraseDialog = false }) {
                    Text("Go Back")
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning Indicator",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Destructive Overwrite Action", fontWeight = FontWeight.Black)
                }
            },
            text = {
                Text(
                    text = "WARNING: You have selected the Erase & Restore option. This operation is fully irreversible. It will wipe all local data tables, databases, loans logs, products catalog, and transactions permanently, substituting everything with the active contents of the backup JSON file.\n\nAre you absolutely sure you wish to proceed?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

private fun getFileName(context: android.content.Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } catch (e: Exception) {
            // Ignored, fallback below
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

private fun getBackupSummaryStats(jsonStr: String): Map<String, Int> {
    val stats = mutableMapOf<String, Int>()
    try {
        val root = org.json.JSONObject(jsonStr)
        stats["customers"] = root.optJSONArray("customers")?.length() ?: 0
        stats["sales"] = root.optJSONArray("sales")?.length() ?: 0
        stats["expenses"] = root.optJSONArray("expenses")?.length() ?: 0
        stats["products"] = root.optJSONArray("products")?.length() ?: 0
        stats["loans"] = root.optJSONArray("loans")?.length() ?: 0
    } catch (e: Exception) {
        // Ignored
    }
    return stats
}

data class CashMovement(
    val id: String,
    val type: String,
    val description: String,
    val amount: Double,
    val timestamp: Long,
    val isInflow: Boolean
)

fun generateCashFlowReportString(
    sales: List<com.example.data.Sale>,
    expenses: List<com.example.data.Expense>,
    loans: List<com.example.data.Loan>,
    movements: List<CashMovement>
): String {
    val fmt = java.text.DecimalFormat("GH₵#,##0.00")
    val df = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
    val now = df.format(java.util.Date())
    
    val totalCashSales = sales.filter { !it.isCredit }.sumOf { it.amount }
    val totalCreditSalesPaid = sales.filter { it.isCredit && it.creditPaid }.sumOf { it.amount }
    val totalLoanRepaid = loans.sumOf { it.repaidAmount }
    val totalInflow = totalCashSales + totalCreditSalesPaid + totalLoanRepaid
    
    val totalExpenses = expenses.sumOf { it.amount }
    val totalLoansIssued = loans.sumOf { it.amount }
    val totalOutflow = totalExpenses + totalLoansIssued
    val netCashFlow = totalInflow - totalOutflow
    val totalUnpaidCredit = sales.sumOf { if (it.isCredit && !it.creditPaid) it.amount else 0.0 }
    val totalLoansOutstanding = loans.sumOf { if (!it.isRepaid) it.amount - it.repaidAmount else 0.0 }
    
    val sb = StringBuilder()
    sb.append("====================================================\n")
    sb.append("               GH SHOP LEDGER REPORT                \n")
    sb.append("====================================================\n")
    sb.append("Statement Run:  $now\n")
    sb.append("Currency:       GHS (GH₵)\n")
    sb.append("----------------------------------------------------\n\n")
    
    sb.append("1. CASH RECEIPTS (INFLOWS)\n")
    sb.append("   + Direct Cash Sales:        ${fmt.format(totalCashSales)}\n")
    sb.append("   + Recovered Credit Sales:   ${fmt.format(totalCreditSalesPaid)}\n")
    sb.append("   + Customer Loan Repayments: ${fmt.format(totalLoanRepaid)}\n")
    sb.append("   -------------------------------------------------\n")
    sb.append("   TOTAL INFLOWS:              ${fmt.format(totalInflow)}\n\n")
    
    sb.append("2. CASH PAYMENTS (OUTFLOWS)\n")
    sb.append("   - Business Expenses Paid:   ${fmt.format(totalExpenses)}\n")
    sb.append("   - Customer Loans Issued:    ${fmt.format(totalLoansIssued)}\n")
    sb.append("   -------------------------------------------------\n")
    sb.append("   TOTAL OUTFLOWS:             ${fmt.format(totalOutflow)}\n\n")
    
    sb.append("3. CASH FLOW RECONCILIATION\n")
    sb.append("   Net Cash Flow:              ${fmt.format(netCashFlow)}\n")
    sb.append("   Current Cash at Hand:       ${fmt.format(netCashFlow)}\n")
    sb.append("   -------------------------------------------------\n")
    sb.append("   Outstanding Active Debtors:  ${fmt.format(totalLoansOutstanding)}\n")
    sb.append("   Unpaid Store Credits:       ${fmt.format(totalUnpaidCredit)}\n")
    sb.append("====================================================\n")
    sb.append("            RECENT RECORDED MOVEMENTS CHRONOLOGY     \n")
    sb.append("====================================================\n")
    
    if (movements.isEmpty()) {
        sb.append("   No cash transactions logged in historical memory.\n")
    } else {
        movements.take(30).forEach { m ->
            val date = df.format(java.util.Date(m.timestamp))
            val prefix = if (m.isInflow) "  (+) " else "  (-) "
            val spaceText = m.type.padEnd(20, ' ')
            sb.append("$prefix$date - $spaceText ${fmt.format(m.amount)} (${m.description})\n")
        }
    }
    
    sb.append("\n====================================================\n")
    sb.append("        End of Report - Issued by GH Shop Ledger.     \n")
    sb.append("               Ensure business integrity.           \n")
    sb.append("====================================================\n")
    
    return sb.toString()
}

data class DailyLedgerPoint(
    val label: String,
    val sales: Double,
    val expenses: Double
)

@Composable
fun SideBySideBarChart(points: List<DailyLedgerPoint>) {
    val emerald = MaterialTheme.colorScheme.primary
    val coral = MaterialTheme.colorScheme.tertiary
    val borderSlate = MaterialTheme.colorScheme.outline

    val maxVal = (points.maxOfOrNull { maxOf(it.sales, it.expenses) } ?: 100.0).coerceAtLeast(100.0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(top = 10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val itemCount = points.size

            val barGroupWidth = width / itemCount
            val individualBarWidth = (barGroupWidth * 0.28).toFloat()

            // Draw clean background horizontal divider rules (grid)
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = height * i / gridLines
                drawLine(
                    color = borderSlate.copy(alpha = 0.15f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            points.forEachIndexed { idx, pt ->
                val xGroupCenter = barGroupWidth * idx + barGroupWidth / 2

                // Redefine relative heights mapped to max limit
                val salesHeight = (pt.sales / maxVal * (height - 30.dp.toPx())).toFloat()
                val expensesHeight = (pt.expenses / maxVal * (height - 30.dp.toPx())).toFloat()

                // Sales individual bar offset coordinates
                val salesL = xGroupCenter - individualBarWidth - 2.dp.toPx()
                val salesT = height - 20.dp.toPx() - salesHeight

                // Draw Sales Pillar Graph
                if (salesHeight > 0) {
                    drawRoundRect(
                        color = emerald,
                        topLeft = Offset(salesL, salesT),
                        size = Size(individualBarWidth, salesHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }

                // Expenses individual bar offset coordinates
                val expL = xGroupCenter + 2.dp.toPx()
                val expT = height - 20.dp.toPx() - expensesHeight

                // Draw Expenses Pillar Graph
                if (expensesHeight > 0) {
                    drawRoundRect(
                        color = coral,
                        topLeft = Offset(expL, expT),
                        size = Size(individualBarWidth, expensesHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }
        }

        // Overlay text labels under the bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            points.forEach { pt ->
                Text(
                    text = pt.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.width(48.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CostDonutChart(categories: Map<String, Double>, total: Double) {
    val colorMapping = getCategoryColorMap()

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val diameter = minOf(width, height)
        val outerRadius = diameter / 2 - 8.dp.toPx()

        var currentAngle = -90f

        categories.forEach { (cat, amt) ->
            val sweepAngle = (amt / total * 360f).toFloat()
            val color = colorMapping[cat] ?: Color.Gray

            // Draw stylized arcs forming the donut ring
            drawArc(
                color = color,
                startAngle = currentAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset((width - diameter) / 2 + 8.dp.toPx(), (height - diameter) / 2 + 8.dp.toPx()),
                size = Size(diameter - 16.dp.toPx(), diameter - 16.dp.toPx()),
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            )

            currentAngle += sweepAngle
        }
    }
}

fun getCategoryColorMap(): Map<String, Color> {
    return mapOf(
        "Inventory" to Color(0xFF10B981), // Emerald
        "Salary" to Color(0xFF3B82F6),    // Blue
        "Rent" to Color(0xFFF59E0B),      // Amber
        "Utilities" to Color(0xFF8B5CF6), // Purple
        "Marketing" to Color(0xFFEC4899), // Pink
        "Other" to Color(0xFF64748B)      // Slate Slate
    )
}

fun getBusinessRecommendation(
    salesSum: Double,
    expenseSum: Double,
    categoriesMap: Map<String, Double>
): String {
    if (salesSum == 0.0 && expenseSum == 0.0) {
        return "Complete logging more sales activity to unleash custom business intelligence algorithms."
    }

    val retentionRate = if (salesSum > 0) (salesSum - expenseSum) / salesSum else -1.0
    val maxExpenseCat = categoriesMap.maxByOrNull { it.value }

    return when {
        retentionRate < 0.0 -> {
            val f = java.text.DecimalFormat("GH₵#,##0.00")
            "CRITICAL: Operating outlays are exceeding revenues. " +
            (maxExpenseCat?.let { "Focus on cutting cost allocation in '${it.key}' which consumes ${f.format(it.value)}." } ?: "Minimize utilities and operational waste immediately.")
        }
        retentionRate < 0.20 -> {
            "ALERT: Low Cash Retention (${(retentionRate*100).toInt()}%). Your margins are thin. " +
            (maxExpenseCat?.let { "Restructuring pricing schemas or reducing '${it.key}' expenses would boost net cash yields." } ?: "Audit running bills.")
        }
        else -> {
            "STABLE: Healthy savings margins (${(retentionRate*100).toInt()}%). This signals high-performing retail operations. " +
            (maxExpenseCat?.let { "Consider re-investing cash into high-yield inventory lines or targeted ads to fuel growth." } ?: "Excellent fiscal health.")
        }
    }
}
