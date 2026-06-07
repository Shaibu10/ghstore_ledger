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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
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
    val sales by viewModel.sales.collectAsState()
    val expenses by viewModel.expenses.collectAsState()

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
                    text = "Financial Reports",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Comprehensive analytics drawn directly from the ledger",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

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

        // --- LEDGER BACKUP & RESTORE TOOL ---
        item {
            BackupAndRestoreSection(viewModel = viewModel)
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun BackupAndRestoreSection(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    val customers by viewModel.customers.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val products by viewModel.products.collectAsState()
    val loans by viewModel.loans.collectAsState()

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var pasteValue by remember { mutableStateOf("") }
    
    var showConfirmEraseDialog by remember { mutableStateOf(false) }

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
                text = "Backup & System Maintenance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Export database records as a secure JSON string configuration to save, share, or restore matching indices from file copy.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showExportDialog = true },
                    modifier = Modifier.weight(1f).testTag("action_export_backup_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Export Backup Icon",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = { 
                        pasteValue = ""
                        showImportDialog = true 
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
                    Text("Restore", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    // --- EXPORT POPUP CONFIRMATION & TEXT ---
    if (showExportDialog) {
        val backupString = remember { viewModel.generateBackupString() }
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(backupString))
                        Toast.makeText(context, "Backup JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Code")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExportDialog = false }) {
                    Text("Close")
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("System Backup Created", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Data state summary to be backed up:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp).fillMaxWidth()) {
                            Text("• Register: ${customers.size} Customers", style = MaterialTheme.typography.bodySmall)
                            Text("• Checkout: ${sales.size} Transaction Sales", style = MaterialTheme.typography.bodySmall)
                            Text("• Operating: ${expenses.size} Expenses Logs", style = MaterialTheme.typography.bodySmall)
                            Text("• Products: ${products.size} Catalog Items", style = MaterialTheme.typography.bodySmall)
                            Text("• Credit: ${loans.size} Loans Records", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text(
                        text = "Save the printed transaction token below to your notes or file system. Copy it anytime to restore exactly this state.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    OutlinedTextField(
                        value = backupString,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // --- IMPORT RESTORE SETUP POPUP DIALOG ---
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            confirmButton = {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (pasteValue.isBlank()) {
                                    Toast.makeText(context, "Please paste valid JSON!", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.restoreBackup(
                                        jsonStr = pasteValue,
                                        overwrite = false,
                                        onSuccess = {
                                            showImportDialog = false
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
                                if (pasteValue.isBlank()) {
                                    Toast.makeText(context, "Please paste valid JSON!", Toast.LENGTH_SHORT).show()
                                } else {
                                    showConfirmEraseDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Erase & Restore", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showImportDialog = false },
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
                    Text("Restore System Ledger", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Paste your backup configuration JSON script inside the box below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = pasteValue,
                            onValueChange = { pasteValue = it },
                            placeholder = { Text("Paste JSON here...", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)) },
                            modifier = Modifier.fillMaxWidth().height(140.dp).testTag("backup_import_paste_input"),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            shape = RoundedCornerShape(8.dp)
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.getText()?.text?.let {
                                    pasteValue = it
                                    Toast.makeText(context, "Pasted from Clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste from clipboard", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text(
                        text = "Choose 'Merge / Upsert' to integrate matching records gently. Choose 'Erase & Restore' to rewrite and override all indices.",
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
    if (showConfirmEraseDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmEraseDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmEraseDialog = false
                        viewModel.restoreBackup(
                            jsonStr = pasteValue,
                            overwrite = true,
                            onSuccess = {
                                showImportDialog = false
                                Toast.makeText(context, "Total Erase & Complete Restore Successful!", Toast.LENGTH_LONG).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, "Parse Failed: $err", Toast.LENGTH_LONG).show()
                            }
                        )
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
                    text = "WARNING: You have selected an Erase & Restore option. This operation is fully irreversible. It will wipe all local data tables, databases, loans logs, products catalog, and transactions permanently, substituting everything with the active copy of the JSON configuration.\n\nAre you absolutely sure you wish to proceed?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
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
