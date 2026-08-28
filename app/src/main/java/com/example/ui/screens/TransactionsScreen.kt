package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    navController: NavController,
    viewModel: AppViewModel,
    transactionTypeStr: String
) {
    val isPurchase = transactionTypeStr == "PURCHASE"
    val transactions by if (isPurchase) viewModel.purchases.collectAsState() else viewModel.sales.collectAsState()
    
    val title = if (isPurchase) "Purchases" else "Sales"
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_transaction/$transactionTypeStr") }) {
                Icon(Icons.Default.Add, contentDescription = "Add $title")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(transactions) { transaction ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Invoice: ${transaction.invoiceNumber}", style = MaterialTheme.typography.titleMedium)
                            Text("Grand Total: \$${transaction.grandTotal}", style = MaterialTheme.typography.bodyLarge)
                            Text("Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(transaction.transactionDate))}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { transactionToDelete = transaction }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Transaction")
                        }
                    }
                }
            }
        }
    }

    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete invoice ${transactionToDelete?.invoiceNumber}? Stock quantities will be reverted. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    transactionToDelete?.let {
                        viewModel.deleteTransaction(it.id) {
                            transactionToDelete = null
                        }
                    }
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
