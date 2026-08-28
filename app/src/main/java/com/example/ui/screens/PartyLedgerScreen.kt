package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.local.entity.CompanyEntity
import com.example.data.local.entity.PartyEntity
import com.example.data.local.entity.PartyType
import com.example.ui.AppViewModel
import com.example.ui.PartyLedgerEntry
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyLedgerScreen(
    navController: NavController,
    viewModel: AppViewModel,
    partyId: String
) {
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()

    val party = remember(partyId, customers, suppliers) {
        customers.find { it.id == partyId } ?: suppliers.find { it.id == partyId }
    }

    if (party == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Party Ledger") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Party not found.")
            }
        }
        return
    }

    PartyLedgerContent(navController = navController, viewModel = viewModel, party = party)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyLedgerContent(
    navController: NavController,
    viewModel: AppViewModel,
    party: PartyEntity
) {
    val ledgerResult by viewModel.getPartyLedger(party.id, party.type).collectAsState(initial = null)
    val isCustomer = party.type == PartyType.CUSTOMER

    var searchQuery by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    var actionMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(party.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (isCustomer) "Customer Statement / Ledger" else "Supplier Statement / Ledger",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val context = LocalContext.current
                    val currentCompany by viewModel.currentCompany.collectAsState()
                    
                    IconButton(onClick = {
                        val html = generateLedgerHtml(party, ledgerResult, dateFormat, currencyFormat, currentCompany)
                        printHtml(context, html)
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Print PDF")
                    }
                    IconButton(onClick = {
                        val route = if (isCustomer) "add_voucher/RECEIPT" else "add_voucher/PAYMENT"
                        navController.navigate(route)
                    }) {
                        Icon(Icons.Default.AddCard, contentDescription = "Add Voucher")
                    }
                }
            )
        },
        snackbarHost = {
            if (actionMessage != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { actionMessage = null }) { Text("Dismiss") }
                    }
                ) {
                    Text(actionMessage!!)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Header Summary Card (Debit / Credit / Balance)
            val totalDebit = ledgerResult?.totalDebit ?: 0.0
            val totalCredit = ledgerResult?.totalCredit ?: 0.0
            val balance = ledgerResult?.netBalance ?: 0.0

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Party Info Column & Balance Badge
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (!party.pan.isNullOrBlank()) {
                            Text("PAN: ${party.pan}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (!party.contactPhone.isNullOrBlank()) {
                            Text("Phone: ${party.contactPhone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (!party.address.isNullOrBlank()) {
                            Text("Address: ${party.address}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Net Balance Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (balance > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isCustomer) "NET RECEIVABLE (DR)" else "NET PAYABLE (CR)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (balance > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "रू ${currencyFormat.format(balance)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (balance > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Totals Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                if (isCustomer) "Total Billed (Sales / Dr)" else "Total Paid (Payments / Dr)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "रू ${currencyFormat.format(totalDebit)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                if (isCustomer) "Total Received (Receipts / Cr)" else "Total Billed (Purchases / Cr)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "रू ${currencyFormat.format(totalCredit)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Quick Actions: Record Receipt / Payment or Print
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val route = if (isCustomer) "add_voucher/RECEIPT" else "add_voucher/PAYMENT"
                        navController.navigate(route)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (isCustomer) Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.CallMade,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isCustomer) "+ New Receipt" else "+ New Payment", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        actionMessage = "Exporting Ledger statement for ${party.name}..."
                    },
                    modifier = Modifier.weight(0.8f)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Print / PDF", fontSize = 12.sp)
                }
            }

            // Ledger Entries Section
            val allEntries = ledgerResult?.entries ?: emptyList()
            val filteredEntries = remember(allEntries, searchQuery) {
                if (searchQuery.isBlank()) allEntries
                else allEntries.filter {
                    it.refNumber.contains(searchQuery, ignoreCase = true) ||
                    it.particulars.contains(searchQuery, ignoreCase = true) ||
                    it.paymentMode.contains(searchQuery, ignoreCase = true)
                }
            }

            // Search Filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter entries by voucher/bill no or remarks...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                singleLine = true
            )

            if (filteredEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No transactions recorded for ${party.name} yet.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredEntries, key = { it.id + it.refNumber + it.date }) { entry ->
                        LedgerRowCard(entry = entry, dateFormat = dateFormat, currencyFormat = currencyFormat, isCustomer = isCustomer)
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerRowCard(
    entry: PartyLedgerEntry,
    dateFormat: SimpleDateFormat,
    currencyFormat: NumberFormat,
    isCustomer: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Type Badge, Ref No, Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (entry.entryType) {
                            "Sales Invoice" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            "Receipt Voucher" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                            "Purchase Bill" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        }
                    ) {
                        Text(
                            text = entry.entryType,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = entry.refNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = dateFormat.format(Date(entry.date)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Particulars and Payment Mode
            Text(
                text = entry.particulars,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Mode: ${entry.paymentMode}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))

            // Dr, Cr and Running Balance Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (entry.debit > 0) {
                        Column {
                            Text("DEBIT (DR)", fontSize = 10.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            Text("रू ${currencyFormat.format(entry.debit)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (entry.credit > 0) {
                        Column {
                            Text("CREDIT (CR)", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text("रू ${currencyFormat.format(entry.credit)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("RUNNING BAL", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text(
                        "रू ${currencyFormat.format(entry.runningBalance)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (entry.runningBalance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

fun generateLedgerHtml(
    party: PartyEntity,
    result: com.example.ui.PartyLedgerResult?,
    dateFormat: SimpleDateFormat,
    currencyFormat: NumberFormat,
    company: CompanyEntity?
): String {
    val ledgerEntries = result?.entries ?: emptyList()
    val isCustomer = party.type == PartyType.CUSTOMER
    val partyType = if (isCustomer) "Customer" else "Supplier"
    
    val html = StringBuilder()
    html.append("<html><head><style>")
    html.append("body { font-family: sans-serif; padding: 20px; }")
    html.append("h1 { text-align: center; }")
    html.append("table { width: 100%; border-collapse: collapse; margin-top: 20px; }")
    html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }")
    html.append("th { background-color: #f2f2f2; }")
    html.append(".right { text-align: right; }")
    html.append("</style></head><body>")
    
    if (company != null) {
        html.append("<h1>${company.businessName}</h1>")
        html.append("<p style='text-align:center;'>${company.address} | PAN: ${company.panVatNumber}</p>")
    }
    html.append("<h2>Party Ledger / Statement</h2>")
    html.append("<p><strong>$partyType Name:</strong> ${party.name}</p>")
    html.append("<p><strong>Address:</strong> ${party.address}</p>")
    html.append("<p><strong>Contact:</strong> ${party.contactPhone}</p>")
    html.append("<p><strong>Generated On:</strong> ${dateFormat.format(Date())}</p>")
    
    html.append("<table>")
    html.append("<tr><th>Date</th><th>Type</th><th>Particulars</th><th class='right'>Debit</th><th class='right'>Credit</th><th class='right'>Balance</th></tr>")
    
    for (entry in ledgerEntries) {
        val debitStr = if (entry.debit > 0) currencyFormat.format(entry.debit) else "-"
        val creditStr = if (entry.credit > 0) currencyFormat.format(entry.credit) else "-"
        html.append("<tr>")
        html.append("<td>${dateFormat.format(Date(entry.date))}</td>")
        html.append("<td>${entry.entryType}</td>")
        html.append("<td>${entry.particulars}</td>")
        html.append("<td class='right'>$debitStr</td>")
        html.append("<td class='right'>$creditStr</td>")
        html.append("<td class='right'>${currencyFormat.format(entry.runningBalance)}</td>")
        html.append("</tr>")
    }
    html.append("</table>")
    html.append("</body></html>")
    return html.toString()
}
