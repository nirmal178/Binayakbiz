package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.data.local.entity.UserEntity
import com.example.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    navController: NavController,
    viewModel: AppViewModel
) {
    val users by viewModel.companyUsers.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Management") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add User")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Manage Users & Permissions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            items(users) { user ->
                UserCard(user = user, onTogglePermission = { perm, value ->
                    viewModel.updateUserPermission(user, perm, value)
                })
            }
        }
    }

    if (showDialog) {
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var role by remember { mutableStateOf("Cashier") }
        val roles = listOf("Manager", "Cashier", "Inventory Staff", "Accountant")
        
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add New User") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username / Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Role", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    roles.forEach { r ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = role == r, onClick = { role = r })
                            Text(r, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addUser(username, password, role)
                    showDialog = false
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun UserCard(user: UserEntity, onTogglePermission: (String, Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "${user.username} (${user.role})", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (user.role != "Business Owner") {
                PermissionSwitch(
                    label = "Can Create Sales Invoice",
                    checked = user.canCreateSalesInvoice,
                    onCheckedChange = { onTogglePermission("sales", it) }
                )
                PermissionSwitch(
                    label = "Can Create Purchase Invoice",
                    checked = user.canCreatePurchaseInvoice,
                    onCheckedChange = { onTogglePermission("purchase", it) }
                )
                PermissionSwitch(
                    label = "Can Manage Vouchers",
                    checked = user.canCreateVoucher,
                    onCheckedChange = { onTogglePermission("voucher", it) }
                )
                PermissionSwitch(
                    label = "Can Manage Items",
                    checked = user.canManageItems,
                    onCheckedChange = { onTogglePermission("items", it) }
                )
                PermissionSwitch(
                    label = "Can Manage Parties",
                    checked = user.canManageParties,
                    onCheckedChange = { onTogglePermission("parties", it) }
                )
                PermissionSwitch(
                    label = "Can View Reports",
                    checked = user.canViewReports,
                    onCheckedChange = { onTogglePermission("reports", it) }
                )
            } else {
                Text("Business Owners have all permissions.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun PermissionSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
