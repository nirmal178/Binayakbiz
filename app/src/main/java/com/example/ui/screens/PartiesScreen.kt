package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.local.entity.PartyEntity
import com.example.data.local.entity.PartyType
import com.example.ui.AppViewModel
import com.example.ui.components.AddressPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartiesScreen(navController: NavController, viewModel: AppViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Customers", "Suppliers")
    
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var partyToEdit by remember { mutableStateOf<PartyEntity?>(null) }
    var partyToDelete by remember { mutableStateOf<PartyEntity?>(null) }
    var statusSnackbarMessage by remember { mutableStateOf<String?>(null) }

    val currentList = if (selectedTab == 0) customers else suppliers
    val currentType = if (selectedTab == 0) PartyType.CUSTOMER else PartyType.SUPPLIER
    val currentTypeName = if (selectedTab == 0) "Customer" else "Supplier"

    val filteredParties = remember(currentList, searchQuery) {
        if (searchQuery.isBlank()) currentList
        else currentList.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            (it.pan != null && it.pan.contains(searchQuery, ignoreCase = true)) ||
            (it.contactPhone != null && it.contactPhone.contains(searchQuery, ignoreCase = true)) ||
            (it.address != null && it.address.contains(searchQuery, ignoreCase = true))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parties & Contacts") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add $currentTypeName")
            }
        },
        snackbarHost = {
            if (statusSnackbarMessage != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { statusSnackbarMessage = null }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(statusSnackbarMessage!!)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    val count = if (index == 0) customers.size else suppliers.size
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text("$title ($count)") }
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by name, PAN, phone, address...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true
            )
            
            if (filteredParties.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "No $currentTypeName registered yet" else "No $currentTypeName matches '$searchQuery'",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { showAddDialog = true }) {
                                Text("Add $currentTypeName")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredParties, key = { it.id }) { party ->
                        PartyCard(
                            party = party,
                            onEdit = { partyToEdit = party },
                            onDelete = { partyToDelete = party },
                            onViewLedger = { navController.navigate("party_ledger/${party.id}") }
                        )
                    }
                }
            }
        }
    }
    
    // Add / Edit Dialog
    if (showAddDialog || partyToEdit != null) {
        val isEditing = partyToEdit != null
        var name by remember { mutableStateOf(partyToEdit?.name ?: "") }
        var email by remember { mutableStateOf(partyToEdit?.email ?: "") }
        var pan by remember { mutableStateOf(partyToEdit?.pan ?: "") }
        var phone by remember { mutableStateOf(partyToEdit?.contactPhone ?: "") }
        var address by remember { mutableStateOf(partyToEdit?.address ?: "") }
        var dialogError by remember { mutableStateOf<String?>(null) }
        
        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false
                partyToEdit = null 
            },
            title = { Text(if (isEditing) "Edit $currentTypeName" else "Add New $currentTypeName") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (dialogError != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = dialogError!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; dialogError = null },
                        label = { Text("Party Name *") },
                        placeholder = { Text("e.g. Acme Traders, Ram Shrestha") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = pan,
                        onValueChange = { pan = it },
                        label = { Text("PAN Number (Optional, 9 Digits)") },
                        placeholder = { Text("e.g. 123456789") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address (Optional)") },
                        placeholder = { Text("e.g. info@acme.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Contact Phone (Optional)") },
                        placeholder = { Text("e.g. 9801234567") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Text(
                        text = "Address (District, Municipality, Ward)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AddressPicker(
                        selectedAddress = address,
                        onAddressChange = { address = it }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.trim().isBlank()) {
                        dialogError = "Please enter party name."
                        return@Button
                    }
                    val cleanPan = pan.trim().filter { it.isDigit() }
                    if (cleanPan.isNotEmpty() && cleanPan.length != 9) {
                        dialogError = "PAN must be exactly 9 digits if provided."
                        return@Button
                    }

                    if (isEditing) {
                        viewModel.updateParty(
                            party = partyToEdit!!.copy(
                                name = name.trim(),
                                email = email.trim().ifBlank { null },
                                pan = cleanPan.ifBlank { null },
                                contactPhone = phone.trim().ifBlank { null },
                                address = address.trim().ifBlank { null }
                            ),
                            onComplete = {
                                partyToEdit = null
                                statusSnackbarMessage = "$currentTypeName updated successfully."
                            },
                            onError = { msg -> dialogError = msg }
                        )
                    } else {
                        viewModel.addParty(
                            name = name,
                            email = email.trim().ifBlank { null },
                            pan = cleanPan.ifBlank { null },
                            phone = phone.trim().ifBlank { null },
                            address = address.trim().ifBlank { null },
                            type = currentType,
                            onComplete = {
                                showAddDialog = false
                                statusSnackbarMessage = "$currentTypeName registered successfully."
                            },
                            onError = { msg -> dialogError = msg }
                        )
                    }
                }) {
                    Text(if (isEditing) "Save Changes" else "Add $currentTypeName")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddDialog = false
                    partyToEdit = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (partyToDelete != null) {
        AlertDialog(
            onDismissRequest = { partyToDelete = null },
            title = { Text("Delete $currentTypeName") },
            text = { Text("Are you sure you want to delete '${partyToDelete?.name}'? This record will be permanently removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        partyToDelete?.let { party ->
                            viewModel.deleteParty(
                                partyId = party.id,
                                onComplete = {
                                    partyToDelete = null
                                    statusSnackbarMessage = "'${party.name}' deleted."
                                },
                                onError = { msg ->
                                    statusSnackbarMessage = msg
                                    partyToDelete = null
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { partyToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PartyCard(
    party: PartyEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewLedger: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = party.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!party.pan.isNullOrBlank()) {
                        Text(
                            text = "PAN: ${party.pan}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Party",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Party",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (!party.contactPhone.isNullOrBlank() || !party.address.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!party.contactPhone.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = party.contactPhone,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    if (!party.address.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = party.address,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onViewLedger,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("View Party Statement / Ledger", fontSize = 12.sp)
            }
        }
    }
}
