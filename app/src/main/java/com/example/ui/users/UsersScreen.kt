package com.example.ui.users

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.UserEntity
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val users by viewModel.users.collectAsState()
    val loggedInUsername by viewModel.loggedInUsername.collectAsState()
    val loggedInUserRole by viewModel.loggedInUserRole.collectAsState()

    // Permissions active states for feedback
    val canLogProducts by viewModel.canLogProducts.collectAsState()
    val canProcessPurchases by viewModel.canProcessPurchases.collectAsState()
    val canAddClients by viewModel.canAddClients.collectAsState()
    val canManageExpenses by viewModel.canManageExpenses.collectAsState()
    val canViewReports by viewModel.canViewReports.collectAsState()

    var showAddUserDialog by remember { mutableStateOf(false) }
    var selectedUserForEdit by remember { mutableStateOf<UserEntity?>(null) }

    // Add user form state
    var newUserName by remember { mutableStateOf("") }
    var newUserUsername by remember { mutableStateOf("") }
    var newUserPassword by remember { mutableStateOf("") }
    var newUserRole by remember { mutableStateOf("USER") } // "ADMINISTRATOR" or "USER"
    var newCanLogProducts by remember { mutableStateOf(true) }
    var newCanProcessPurchases by remember { mutableStateOf(true) }
    var newCanAddClients by remember { mutableStateOf(true) }
    var newCanManageExpenses by remember { mutableStateOf(true) }
    var newCanViewReports by remember { mutableStateOf(true) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Profile & Access Control", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.logout()
                            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("action_logout")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Log out",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        var selectedSubTab by remember { mutableStateOf("profiles") } // "profiles", "activity_logs", "wifi_sync", "store_info"
        var logSearchQuery by remember { mutableStateOf("") }
        var selectedActionFilter by remember { mutableStateOf("ALL") } // "ALL", "AUTH", "SALES", "CLIENTS", "EXPENSES", "ACCESS", "CATALOG"
        
        val activityLogs by viewModel.activityLogs.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // SUB TABS SELECTOR Container
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .height(38.dp)
                        .background(
                            color = if (selectedSubTab == "profiles") MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedSubTab = "profiles" }
                        .testTag("tab_profiles_access"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Profiles",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedSubTab == "profiles") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(0.8f)
                        .height(38.dp)
                        .background(
                            color = if (selectedSubTab == "activity_logs") MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedSubTab = "activity_logs" }
                        .testTag("tab_activity_logs"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Logs",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedSubTab == "activity_logs") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .height(38.dp)
                        .background(
                            color = if (selectedSubTab == "wifi_sync") MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedSubTab = "wifi_sync" }
                        .testTag("tab_wifi_sync"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Wi-Fi Sync",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedSubTab == "wifi_sync") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .height(38.dp)
                        .background(
                            color = if (selectedSubTab == "store_info") MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedSubTab = "store_info" }
                        .testTag("tab_store_info"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Store Info",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedSubTab == "store_info") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (selectedSubTab == "profiles") {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
            // CURRENT USER CARD SUMMARY
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Column {
                                Text(
                                    text = if (loggedInUsername.isNotBlank()) loggedInUsername.uppercase() else "STAFF ACCOUNT",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Role: ${loggedInUserRole.uppercase()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Your Configured POS Permissions:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val myPermissions = listOf(
                            Triple("Add Products Catalog", canLogProducts, Icons.Default.GridView),
                            Triple("Process Client Sales", canProcessPurchases, Icons.Default.TrendingUp),
                            Triple("Manage Customer Directories", canAddClients, Icons.Default.Group),
                            Triple("Log Business Expenses", canManageExpenses, Icons.Default.Payments),
                            Triple("View Financial Reports", canViewReports, Icons.Default.Analytics)
                        )

                        myPermissions.forEach { (name, allowed, icon) ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (allowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (allowed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    fontWeight = if (allowed) FontWeight.Medium else FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (allowed) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Active",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Restricted",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // USER MANAGEMENT CAPABILITIES FOR ADMIN
            if (loggedInUserRole.uppercase() == "ADMINISTRATOR" || loggedInUsername == "shaibu5278@gmail.com") {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Manage Staff Credentials (${users.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        Button(
                            onClick = {
                                newUserName = ""
                                newUserUsername = ""
                                newUserPassword = ""
                                newUserRole = "USER"
                                newCanLogProducts = true
                                newCanProcessPurchases = true
                                newCanAddClients = true
                                newCanManageExpenses = true
                                newCanViewReports = true
                                showAddUserDialog = true
                            },
                            modifier = Modifier.testTag("button_add_user_dialog"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New User", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                if (users.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No registered users found. Click \"New User\" above to bootstrap accounts.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(users.filter { it.username != loggedInUsername }) { user ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedUserForEdit = user },
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = user.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = "Username: ${user.username} • Role: ${user.role}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.deleteUser(user.id)
                                            Toast.makeText(context, "${user.name} removed.", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete user account",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    "Features Enabled:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val actList = listOf(
                                        "Catalog" to user.canLogProducts,
                                        "Sales" to user.canProcessPurchases,
                                        "Clients" to user.canAddClients,
                                        "Ledger" to user.canManageExpenses,
                                        "Reports" to user.canViewReports
                                    )

                                    actList.forEach { (name, isAllowed) ->
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(name) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = if (isAllowed) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                                                labelColor = if (isAllowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                            ),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isAllowed) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Non admin view message
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Role-Based Restraints",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Your account is registered as a user. Management capabilities like adding staff, deleting registers, or altering permission values require Administrator authorizations.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
        } else if (selectedSubTab == "activity_logs") {
            // Activity Logs View
            val filteredLogs = remember(activityLogs, logSearchQuery, selectedActionFilter) {
                activityLogs.filter { log ->
                    val matchesSearch = log.description.contains(logSearchQuery, ignoreCase = true) ||
                            log.username.contains(logSearchQuery, ignoreCase = true)
                    val matchesFilter = selectedActionFilter == "ALL" || log.actionType == selectedActionFilter
                    matchesSearch && matchesFilter
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Search field
                OutlinedTextField(
                    value = logSearchQuery,
                    onValueChange = { logSearchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("log_search_query"),
                    placeholder = { Text("Search system logs...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (logSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { logSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Category filter chips
                val filterOptions = listOf("ALL", "AUTH", "CATALOG", "SALES", "CLIENTS", "EXPENSES", "ACCESS")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterOptions.forEach { opt ->
                        val isSelected = selectedActionFilter == opt
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedActionFilter = opt },
                            label = { Text(opt) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                // Headers & clear logs action for ADMINISTRATOR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "System Logs (${filteredLogs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (loggedInUserRole.uppercase() == "ADMINISTRATOR" || loggedInUsername == "shaibu5278@gmail.com") {
                        if (activityLogs.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    viewModel.clearActivityLogs()
                                    Toast.makeText(context, "Activity trace records cleared successfully.", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.testTag("clear_logs_button")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Logs")
                            }
                        }
                    }
                }

                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = if (activityLogs.isEmpty()) "No activity registered yet" else "No matching logs found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (activityLogs.isEmpty()) "Logs will be recorded as actions are carried on." else "Try adjusting search strings or filters.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    val sFormatter = remember { java.text.SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", java.util.Locale.getDefault()) }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredLogs) { log ->
                            val logIcon = when (log.actionType) {
                                "AUTH" -> Icons.Default.Lock
                                "CATALOG" -> Icons.Default.ShoppingCart
                                "SALES" -> Icons.Default.TrendingUp
                                "CLIENTS" -> Icons.Default.Group
                                "EXPENSES" -> Icons.Default.Payments
                                "ACCESS" -> Icons.Default.Settings
                                else -> Icons.Default.Info
                            }

                            val iconColor = when (log.actionType) {
                                "AUTH" -> MaterialTheme.colorScheme.error
                                "CATALOG" -> MaterialTheme.colorScheme.primary
                                "SALES" -> MaterialTheme.colorScheme.primary
                                "CLIENTS" -> MaterialTheme.colorScheme.secondary
                                "EXPENSES" -> MaterialTheme.colorScheme.tertiary
                                "ACCESS" -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.outline
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("log_item_${log.id}"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(iconColor.copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = logIcon,
                                            contentDescription = null,
                                            tint = iconColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = log.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text(log.username, style = MaterialTheme.typography.labelSmall) },
                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                                    labelColor = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                            
                                            Text(
                                                text = sFormatter.format(java.util.Date(log.timestamp)),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedSubTab == "wifi_sync") {
            WifiSyncTerminalTab(viewModel = viewModel)
        } else if (selectedSubTab == "store_info") {
            StoreInfoSettingsTab(viewModel = viewModel)
        }
        }
        }

    // --- ADD DIALOG FORM ---
    if (showAddUserDialog) {
        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUserName.isBlank() || newUserUsername.isBlank() || newUserPassword.isBlank()) {
                            Toast.makeText(context, "All registration fields are required.", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.createUser(
                                name = newUserName,
                                usernameLog = newUserUsername,
                                passwordLog = newUserPassword,
                                role = newUserRole,
                                canLog = newCanLogProducts,
                                canProcess = newCanProcessPurchases,
                                canAdd = newCanAddClients,
                                canExpenses = newCanManageExpenses,
                                canReports = newCanViewReports
                            )
                            showAddUserDialog = false
                            Toast.makeText(context, "Successfully created POS account for $newUserName", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("dialog_confirm_add_user")
                ) {
                    Text("Register Staff")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddUserDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Register New POS User") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = newUserName,
                        onValueChange = { newUserName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newUserUsername,
                        onValueChange = { newUserUsername = it },
                        label = { Text("Username / Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newUserPassword,
                        onValueChange = { newUserPassword = it },
                        label = { Text("Set Local Security Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Role switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Account Authorization Level:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = newUserRole == "USER",
                                onClick = { newUserRole = "USER" },
                                label = { Text("User (Standard)") }
                            )
                            FilterChip(
                                selected = newUserRole == "ADMINISTRATOR",
                                onClick = { newUserRole = "ADMINISTRATOR" },
                                label = { Text("Admin") }
                            )
                        }
                    }

                    Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    Text("Features Authorized per Account:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = newCanLogProducts, onCheckedChange = { newCanLogProducts = it })
                        Text("Can Record New Products (Catalog)", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = newCanProcessPurchases, onCheckedChange = { newCanProcessPurchases = it })
                        Text("Can Record Transactions (Sales)", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = newCanAddClients, onCheckedChange = { newCanAddClients = it })
                        Text("Can Register Clients (Directory)", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = newCanManageExpenses, onCheckedChange = { newCanManageExpenses = it })
                        Text("Can Track Business Expenses", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = newCanViewReports, onCheckedChange = { newCanViewReports = it })
                        Text("Can View Financial Reports / Analytics", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        )
    }

    // --- QUICK EDIT DIALOG FOR USER PERMISSIONS ---
    if (selectedUserForEdit != null) {
        val editing = selectedUserForEdit!!
        var editRole by remember(editing) { mutableStateOf(editing.role) }
        var editLogProd by remember(editing) { mutableStateOf(editing.canLogProducts) }
        var editProcSale by remember(editing) { mutableStateOf(editing.canProcessPurchases) }
        var editAddCli by remember(editing) { mutableStateOf(editing.canAddClients) }
        var editExp by remember(editing) { mutableStateOf(editing.canManageExpenses) }
        var editRep by remember(editing) { mutableStateOf(editing.canViewReports) }

        AlertDialog(
            onDismissRequest = { selectedUserForEdit = null },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateUserPermissions(
                            userId = editing.id,
                            name = editing.name,
                            usernameLog = editing.username,
                            passwordHash = editing.passwordHash,
                            role = editRole,
                            canLog = editLogProd,
                            canProcess = editProcSale,
                            canAdd = editAddCli,
                            canExpenses = editExp,
                            canReports = editRep
                        )
                        selectedUserForEdit = null
                        Toast.makeText(context, "Permissions updated for ${editing.name}", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Apply Authorization Changes")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedUserForEdit = null }) {
                    Text("Cancel")
                }
            },
            title = { Text("Permissions & Role: ${editing.name}") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Role Level:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = editRole == "USER",
                                onClick = { editRole = "USER" },
                                label = { Text("User") }
                            )
                            FilterChip(
                                selected = editRole == "ADMINISTRATOR",
                                onClick = { editRole = "ADMINISTRATOR" },
                                label = { Text("Admin") }
                            )
                        }
                    }

                    Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    Text("Active Authorized Features:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = editLogProd, onCheckedChange = { editLogProd = it })
                        Text("Can Record New Products (Catalog)", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = editProcSale, onCheckedChange = { editProcSale = it })
                        Text("Can Record Transactions (Sales)", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = editAddCli, onCheckedChange = { editAddCli = it })
                        Text("Can Register Clients (Directory)", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = editExp, onCheckedChange = { editExp = it })
                        Text("Can Track Business Expenses", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = editRep, onCheckedChange = { editRep = it })
                        Text("Can View Financial Reports / Analytics", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        )
    }
}

@Composable
fun WifiSyncTerminalTab(viewModel: com.example.ui.MainViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val syncMode by viewModel.syncMode.collectAsState()
    val clientHostIp by viewModel.clientHostIp.collectAsState()
    val clientHostPort by viewModel.clientHostPort.collectAsState()
    val serverPort by viewModel.serverPort.collectAsState()
    val serverRunning by viewModel.serverRunning.collectAsState()
    val clientConnected by viewModel.clientConnected.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()

    var inputHostIp by remember { mutableStateOf(clientHostIp) }
    var inputHostPort by remember { mutableStateOf(clientHostPort.toString()) }
    var inputServerPort by remember { mutableStateOf(serverPort.toString()) }
    var isPulling by remember { mutableStateOf(false) }

    val currentLocalIp = remember { com.example.util.WifiSyncManager.getLocalIpAddress() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Mode Selector Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Operational Network Role",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Configure if this device acts as a Standalone register, a central Server Host, or a Satellite Terminal Client.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Modes list
                    val modes = listOf(
                        Triple("STANDALONE", "Standalone", "Local offline storage mode."),
                        Triple("HOST", "Host Server", "Share database over Wi-Fi with terminals."),
                        Triple("CLIENT", "Client Terminal", "Live sync and commit records to host.")
                    )

                    modes.forEach { modeInfo ->
                        val isSelected = syncMode == modeInfo.first
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val portHost = inputHostPort.toIntOrNull() ?: 8080
                                    val portServ = inputServerPort.toIntOrNull() ?: 8080
                                    viewModel.updateSyncSettings(modeInfo.first, inputHostIp, portHost, portServ)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    val portHost = inputHostPort.toIntOrNull() ?: 8080
                                    val portServ = inputServerPort.toIntOrNull() ?: 8080
                                    viewModel.updateSyncSettings(modeInfo.first, inputHostIp, portHost, portServ)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = modeInfo.second,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = modeInfo.third,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Details block based on mode
        if (syncMode == "STANDALONE") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                "Local Mode Active",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Ideal for single-device businesses. No local network setup is required.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (syncMode == "HOST") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Host Server Parameters",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Current Assigned IP Address:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = currentLocalIp,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = inputServerPort,
                            onValueChange = {
                                inputServerPort = it
                                val portVal = it.toIntOrNull() ?: 8080
                                viewModel.updateSyncSettings("HOST", inputHostIp, inputHostPort.toIntOrNull() ?: 8080, portVal)
                            },
                            label = { Text("Hosting Server Port") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        color = if (serverRunning) androidx.compose.ui.graphics.Color.Green else androidx.compose.ui.graphics.Color.Red,
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = if (serverRunning) "Server Running (Master Database Node)" else "Server Stopped",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (syncError.isNotBlank()) {
                            Text(
                                text = "Startup Failure: $syncError",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.startHostServer() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Start Server")
                            }
                            OutlinedButton(
                                onClick = { viewModel.stopHostServer() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Stop Server")
                            }
                        }
                    }
                }
            }
        }

        if (syncMode == "CLIENT") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Link to Host Server",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = inputHostIp,
                            onValueChange = {
                                inputHostIp = it
                                val portHost = inputHostPort.toIntOrNull() ?: 8080
                                viewModel.updateSyncSettings("CLIENT", it, portHost, inputServerPort.toIntOrNull() ?: 8080)
                            },
                            label = { Text("Host Server IP Address") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = inputHostPort,
                            onValueChange = {
                                inputHostPort = it
                                val portHost = it.toIntOrNull() ?: 8080
                                viewModel.updateSyncSettings("CLIENT", inputHostIp, portHost, inputServerPort.toIntOrNull() ?: 8080)
                            },
                            label = { Text("Host Server Port") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Connection status indicators
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        color = if (clientConnected) androidx.compose.ui.graphics.Color.Green else androidx.compose.ui.graphics.Color.Gray,
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = if (clientConnected) "Connected to Host" else "Not Connected / Offline",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (lastSyncTime > 0) {
                            val formatter = remember { java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()) }
                            Text(
                                text = "Last successful database sync: ${formatter.format(java.util.Date(lastSyncTime))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        if (syncError.isNotBlank()) {
                            Text(
                                text = syncError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isPulling) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Downloading Database...", style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.testClientConnection(
                                            onSuccess = {
                                                Toast.makeText(context, "Ping host successful. Connection verified!", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, "Ping failure: $err", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Ping Host")
                                }

                                Button(
                                    onClick = {
                                        isPulling = true
                                        viewModel.pullAllDataFromHost(
                                            onSuccess = {
                                                isPulling = false
                                                Toast.makeText(context, "Complete database pulling & sync finished!", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { err ->
                                                isPulling = false
                                                Toast.makeText(context, "Full download failure: $err", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Pull Sync")
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
fun StoreInfoSettingsTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    
    val storeNameState by viewModel.storeName.collectAsState()
    val storePhoneState by viewModel.storePhone.collectAsState()
    val storeLocationState by viewModel.storeLocation.collectAsState()
    val storeFooterState by viewModel.storeFooter.collectAsState()
    val storeTaxIdState by viewModel.storeTaxId.collectAsState()

    var name by remember(storeNameState) { mutableStateOf(storeNameState) }
    var phone by remember(storePhoneState) { mutableStateOf(storePhoneState) }
    var location by remember(storeLocationState) { mutableStateOf(storeLocationState) }
    var footer by remember(storeFooterState) { mutableStateOf(storeFooterState) }
    var taxId by remember(storeTaxIdState) { mutableStateOf(storeTaxIdState) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Store Info Decoration",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Receipt & Store Customization",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Configure details to display at the top and bottom of your printed receipts and reports.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Store Identity",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Store/Business Name") },
                        modifier = Modifier.fillMaxWidth().testTag("store_name_input"),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number(s)") },
                        placeholder = { Text("e.g. +233 (0) 244-112233") },
                        modifier = Modifier.fillMaxWidth().testTag("store_phone_input"),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location/Address") },
                        placeholder = { Text("e.g. Accra Mall Road, Accra-Ghana") },
                        modifier = Modifier.fillMaxWidth().testTag("store_location_input"),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = taxId,
                        onValueChange = { taxId = it },
                        label = { Text("Business TIN / TAX ID (Optional)") },
                        placeholder = { Text("e.g. C000312456X") },
                        modifier = Modifier.fillMaxWidth().testTag("store_taxid_input"),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Receipt Customization",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = footer,
                        onValueChange = { footer = it },
                        label = { Text("Receipt Footer Note") },
                        placeholder = { Text("THANK YOU FOR YOUR PATRONAGE!") },
                        modifier = Modifier.fillMaxWidth().testTag("store_footer_input"),
                        maxLines = 3,
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "Store name cannot be empty!", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.updateStoreInfo(
                            name = name.trim(),
                            phone = phone.trim(),
                            location = location.trim(),
                            footer = footer.trim(),
                            taxId = taxId.trim()
                        )
                        Toast.makeText(context, "Store configuration updated successfully!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_store_info_btn")
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Store Information & Settings", fontWeight = FontWeight.Bold)
            }
        }
    }
}

