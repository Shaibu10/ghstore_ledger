package com.example.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToSales: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.financialStats.collectAsState()
    val rawSales by viewModel.sales.collectAsState()
    val rawExpenses by viewModel.expenses.collectAsState()

    // Interleave recent transactions chronologically
    val recentTransactions = (
        rawSales.map { TransactionItem.SaleTx(it) } +
        rawExpenses.map { TransactionItem.ExpenseTx(it) }
    ).sortedByDescending { it.timestamp }.take(6)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming & Header
        item {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = "Store Ledger",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Live merchant analytical feed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        // --- KEY FINANCIAL BOARD ---
        item {
            FinancialBoard(
                totalSales = stats.totalSales,
                totalExpenses = stats.totalExpenses,
                netProfit = stats.netProfit,
                totalLoansOutstanding = stats.totalLoansOutstanding,
                netRetainedBalance = stats.netRetainedBalance,
                totalUnpaidCreditSales = stats.totalUnpaidCreditSales,
                moneyAtHand = stats.moneyAtHand
            )
        }

        // --- QUICK SHORTCUTS ---
        item {
            Column {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionBtn(
                        title = "New Sale",
                        icon = Icons.Default.TrendingUp,
                        testTag = "action_new_sale",
                        onClick = onNavigateToSales,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionBtn(
                        title = "Log Cost",
                        icon = Icons.Default.Payments,
                        testTag = "action_log_expense",
                        onClick = onNavigateToExpenses,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionBtn(
                        title = "Add Client",
                        icon = Icons.Default.Group,
                        testTag = "action_add_customer",
                        onClick = onNavigateToCustomers,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // --- RATIO CHART INSIGHT ---
        item {
            BusinessPulseCard(sales = stats.totalSales, expenses = stats.totalExpenses)
        }

        // --- RECENT LEDGER ENTRIES ---
        item {
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        if (recentTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recorded transactions yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        } else {
            items(recentTransactions) { tx ->
                TransactionRowItem(tx = tx)
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// UI Sub components

sealed class TransactionItem {
    abstract val timestamp: Long
    abstract val amount: Double
    abstract val desc: String

    data class SaleTx(val sale: com.example.data.Sale) : TransactionItem() {
        override val timestamp = sale.timestamp
        override val amount = sale.amount
        override val desc = "${sale.customerName}: ${sale.description}"
    }

    data class ExpenseTx(val expense: com.example.data.Expense) : TransactionItem() {
        override val timestamp = expense.timestamp
        override val amount = expense.amount
        override val desc = "[${expense.category}] ${expense.description}"
    }
}

@Composable
fun FinancialBoard(
    totalSales: Double,
    totalExpenses: Double,
    netProfit: Double,
    totalLoansOutstanding: Double,
    netRetainedBalance: Double,
    totalUnpaidCreditSales: Double,
    moneyAtHand: Double
) {
    val fmt = java.text.DecimalFormat("GH₵#,##0.00")
    val emerald = MaterialTheme.colorScheme.primary
    val coral = MaterialTheme.colorScheme.tertiary

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // High Contrast Money at Hand Card (Primary Cash Metric)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("money_at_hand_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (moneyAtHand >= 0) emerald.copy(alpha = 0.15f) else coral.copy(alpha = 0.15f)
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                    if (moneyAtHand >= 0) emerald.copy(alpha = 0.5f) else coral.copy(alpha = 0.5f)
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MONEY AT HAND (LIQUID CASH)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (moneyAtHand >= 0) emerald else coral
                    )
                    Text(
                        text = fmt.format(moneyAtHand),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (moneyAtHand >= 0) emerald else coral,
                        modifier = Modifier.testTag("money_at_hand_amount")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Physical cash presently available in the shop.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (moneyAtHand >= 0) emerald.copy(alpha = 0.2f) else coral.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = "Money on Hand Icon",
                        tint = if (moneyAtHand >= 0) emerald else coral,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Net Retained Balance Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("net_profit_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NET RETAINED BALANCE (TOTAL WORTH)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = fmt.format(netRetainedBalance),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("net_profit_amount")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ledger Profit: ${fmt.format(netProfit)} | Active Loans: ${fmt.format(totalLoansOutstanding)} | Unpaid Credits: ${fmt.format(totalUnpaidCreditSales)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Balance health indicator",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Row of mini sales & expenses trackers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("total_sales_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Sales up icon",
                        tint = emerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "TOTAL SALES",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = fmt.format(totalSales),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = emerald
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("total_expenses_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Expenses down icon",
                        tint = coral,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "TOTAL COSTS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = fmt.format(totalExpenses),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = coral
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionBtn(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun BusinessPulseCard(sales: Double, expenses: Double) {
    val percentage = if (sales > 0) ((sales - expenses) / sales).coerceIn(0.0, 1.0) else 0.0
    val retentionPctStr = "${(percentage * 100).toInt()}%"
    val emerald = MaterialTheme.colorScheme.primary
    val borderSlate = MaterialTheme.colorScheme.outline

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("retention_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant Canvas Draw circle
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Gray track outline
                    drawCircle(
                        color = borderSlate.copy(alpha = 0.2f),
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Retention arc
                    drawArc(
                        color = emerald,
                        startAngle = -90f,
                        sweepAngle = (percentage * 360f).toFloat(),
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = retentionPctStr,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Cash Retention Rate",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "You are saving $retentionPctStr of raw inflow. High retention signals stable ledger margins.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun TransactionRowItem(tx: TransactionItem) {
    val fmt = java.text.DecimalFormat("GH₵#,##0.00")
    val df = SimpleDateFormat("MMM d, h:mm a", Locale.US)
    val cardColor = MaterialTheme.colorScheme.surface
    val onBg = MaterialTheme.colorScheme.onBackground

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ledger_item_${tx.timestamp}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
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
                // Circle visual cue indicator
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = when (tx) {
                                is TransactionItem.SaleTx -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                is TransactionItem.ExpenseTx -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (tx) {
                            is TransactionItem.SaleTx -> Icons.Default.TrendingUp
                            is TransactionItem.ExpenseTx -> Icons.Default.ArrowDownward
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = when (tx) {
                            is TransactionItem.SaleTx -> MaterialTheme.colorScheme.primary
                            is TransactionItem.ExpenseTx -> MaterialTheme.colorScheme.tertiary
                        }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = tx.desc,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = onBg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = df.format(Date(tx.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = onBg.copy(alpha = 0.5f)
                    )
                }
            }

            Text(
                text = "${if (tx is TransactionItem.SaleTx) "+" else "-"}${fmt.format(tx.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold,
                color = when (tx) {
                    is TransactionItem.SaleTx -> MaterialTheme.colorScheme.primary
                    is TransactionItem.ExpenseTx -> MaterialTheme.colorScheme.tertiary
                }
            )
        }
    }
}
