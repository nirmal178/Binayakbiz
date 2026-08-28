package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.local.entity.ItemEntity
import com.example.data.local.entity.PartyType
import com.example.data.local.entity.TransactionType
import com.example.ui.AppViewModel
import com.example.ui.TransactionItemData
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navController: NavController,
    viewModel: AppViewModel,
    transactionTypeStr: String
) {
    val isPurchase = transactionTypeStr == "PURCHASE"
    val type = if (isPurchase) TransactionType.PURCHASE else TransactionType.SALE
    
    val parties by if (isPurchase) viewModel.suppliers.collectAsState() else viewModel.customers.collectAsState()
    val items by viewModel.items.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val purchases by viewModel.purchases.collectAsState()
    
    var invoiceNumber by remember { mutableStateOf("") }
    var transactionDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedPartyId by remember { mutableStateOf<String?>(null) }
    
    var transactionItems by remember { mutableStateOf(listOf<TransactionItemData>()) }
    var discount by remember { mutableStateOf("0.0") }
    var vatPercent by remember { mutableStateOf("13.0") }
    var paymentMode by remember { mutableStateOf("CASH") }
    var error by remember { mutableStateOf<String?>(null) }
    
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    val currentCompany by viewModel.currentCompany.collectAsState()

    // Auto-generate invoice number on screen load
    LaunchedEffect(type, sales, purchases) {
        if (invoiceNumber.isBlank()) {
            invoiceNumber = viewModel.getNextInvoiceNumber(type)
        }
    }

    // Auto-select Cash Customer / Cash Supplier if mode is CASH
    LaunchedEffect(paymentMode, parties) {
        if (paymentMode == "CASH" && selectedPartyId == null) {
            val partyType = if (isPurchase) PartyType.SUPPLIER else PartyType.CUSTOMER
            viewModel.getOrCreateCashParty(partyType) { cashParty ->
                selectedPartyId = cashParty.id
            }
        }
    }

    // Subtotal, VAT, Grand total calculation
    val subtotal = remember(transactionItems) {
        transactionItems.sumOf { it.quantity * it.rate }
    }
    val discountVal = discount.toDoubleOrNull() ?: 0.0
    val taxableAmount = maxOf(0.0, subtotal - discountVal)
    val vatVal = vatPercent.toDoubleOrNull() ?: 13.0
    val vatAmount = (taxableAmount * vatVal) / 100.0
    val grandTotal = taxableAmount + vatAmount

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isPurchase) "New Purchase Bill" else "New Sales Invoice",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Top Section: Invoice Number & Date Picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = invoiceNumber,
                    onValueChange = { invoiceNumber = it },
                    label = { Text(if (isPurchase) "Bill / Inv No *" else "Invoice No *") },
                    modifier = Modifier.weight(1.1f),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            invoiceNumber = viewModel.getNextInvoiceNumber(type)
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Auto Generate")
                        }
                    }
                )

                // Date Picker Field
                val cal = Calendar.getInstance().apply { timeInMillis = transactionDate }
                val datePickerDialog = DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val picked = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        }
                        transactionDate = picked.timeInMillis
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                )

                OutlinedTextField(
                    value = dateFormat.format(Date(transactionDate)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    modifier = Modifier
                        .weight(0.9f)
                        .clickable { datePickerDialog.show() },
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Payment Mode (Cash vs Credit)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Payment Mode:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    paymentMode = "CASH"
                                    val partyType = if (isPurchase) PartyType.SUPPLIER else PartyType.CUSTOMER
                                    viewModel.getOrCreateCashParty(partyType) { cashParty ->
                                        selectedPartyId = cashParty.id
                                    }
                                }
                                .padding(end = 12.dp)
                        ) {
                            RadioButton(
                                selected = paymentMode == "CASH",
                                onClick = {
                                    paymentMode = "CASH"
                                    val partyType = if (isPurchase) PartyType.SUPPLIER else PartyType.CUSTOMER
                                    viewModel.getOrCreateCashParty(partyType) { cashParty ->
                                        selectedPartyId = cashParty.id
                                    }
                                }
                            )
                            Text("Cash", fontSize = 13.sp)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                paymentMode = "CREDIT"
                                // If was on cash customer, clear so user explicitly picks customer/supplier
                                val cashParty = parties.find { it.name.contains("Cash", ignoreCase = true) }
                                if (selectedPartyId == cashParty?.id) {
                                    selectedPartyId = null
                                }
                            }
                        ) {
                            RadioButton(
                                selected = paymentMode == "CREDIT",
                                onClick = {
                                    paymentMode = "CREDIT"
                                    val cashParty = parties.find { it.name.contains("Cash", ignoreCase = true) }
                                    if (selectedPartyId == cashParty?.id) {
                                        selectedPartyId = null
                                    }
                                }
                            )
                            Text("Credit", fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Party Dropdown (Customer / Supplier)
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                val selectedParty = parties.find { it.id == selectedPartyId }
                val displayText = when {
                    selectedParty != null -> selectedParty.name + if (!selectedParty.pan.isNullOrBlank()) " (PAN: ${selectedParty.pan})" else ""
                    paymentMode == "CASH" -> if (isPurchase) "Cash Supplier (Over the Counter)" else "Cash Customer (Walk-in)"
                    else -> "Select ${if (isPurchase) "Supplier *" else "Customer *"}"
                }

                OutlinedTextField(
                    value = displayText,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (isPurchase) "Supplier Name *" else "Customer Name *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    parties.forEach { party ->
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(party.name, fontWeight = FontWeight.Bold)
                                    if (!party.pan.isNullOrBlank()) {
                                        Text("PAN: ${party.pan}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                            onClick = {
                                selectedPartyId = party.id
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Items Table Header & Add Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Particulars & Items (${transactionItems.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { showAddItemDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Item", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Item rows list
            if (transactionItems.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("No items added yet. Click 'Add Item' to insert line items.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(transactionItems) { tItem ->
                        val itemDetail = items.find { it.id == tItem.itemId }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        itemDetail?.itemName ?: "Unknown Item",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "${tItem.quantity} ${itemDetail?.uom ?: "Pcs"} × रू ${tItem.rate}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "रू ${NumberFormat.getNumberInstance(Locale.US).format(tItem.quantity * tItem.rate)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = { transactionItems = transactionItems.filter { it != tItem } }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Discount & VAT Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = discount,
                    onValueChange = { discount = it },
                    label = { Text("Discount (रू)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = vatPercent,
                    onValueChange = { vatPercent = it },
                    label = { Text("VAT %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Grand Total Calculation Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Subtotal: रू ${NumberFormat.getNumberInstance(Locale.US).format(subtotal)} | VAT: रू ${NumberFormat.getNumberInstance(Locale.US).format(vatAmount)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            "GRAND TOTAL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        "रू ${NumberFormat.getNumberInstance(Locale.US).format(grandTotal)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (error != null) {
                Text(
                    error!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    if (selectedPartyId == null) {
                        // If cash mode, auto create/select cash party
                        val partyType = if (isPurchase) PartyType.SUPPLIER else PartyType.CUSTOMER
                        viewModel.getOrCreateCashParty(partyType) { cashParty ->
                            selectedPartyId = cashParty.id
                            saveTransaction(viewModel, type, cashParty.id, invoiceNumber, transactionDate, transactionItems, discountVal, vatVal, paymentMode, { showSuccessDialog = true }, { error = it })
                        }
                        return@Button
                    }
                    saveTransaction(viewModel, type, selectedPartyId!!, invoiceNumber, transactionDate, transactionItems, discountVal, vatVal, paymentMode, { showSuccessDialog = true }, { error = it })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    if (isPurchase) "Save Purchase Bill" else "Save Sales Invoice",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { navController.navigateUp() },
            title = { Text("Invoice Saved") },
            text = { Text("The invoice $invoiceNumber has been recorded successfully.") },
            confirmButton = {
                TextButton(onClick = { navController.navigateUp() }) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        val partyName = parties.find { it.id == selectedPartyId }?.name ?: "Unknown"
                        val html = generateInvoiceHtml(isPurchase, invoiceNumber, dateFormat.format(Date(transactionDate)), partyName, transactionItems, subtotal, discountVal, taxableAmount, vatAmount, grandTotal, paymentMode, currentCompany, items)
                        printHtml(context, html)
                    }) { Text("Print / PDF") }
                }
            }
        )
    }

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
    
    if (showAddItemDialog) {
        var selectedItem by remember { mutableStateOf<ItemEntity?>(null) }
        var quantity by remember { mutableStateOf("1") }
        var rate by remember { mutableStateOf("") }
        var dialogError by remember { mutableStateOf<String?>(null) }
        
        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("Add Item to Invoice") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedItem?.itemName ?: "Select Item *",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            items.forEach { item ->
                                DropdownMenuItem(
                                    text = { 
                                        Column {
                                            Text(item.itemName, fontWeight = FontWeight.Bold)
                                            Text(
                                                "Stock: ${item.stockQuantity} ${item.uom} | Price: रू ${if (isPurchase) item.purchasePrice else item.salesPrice}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedItem = item
                                        rate = if (isPurchase) item.purchasePrice.toString() else item.salesPrice.toString()
                                        expanded = false
                                        dialogError = null
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it; dialogError = null },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = rate,
                        onValueChange = { rate = it; dialogError = null },
                        label = { Text("Rate per Unit (रू)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    if (dialogError != null) {
                        Text(
                            text = dialogError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val q = quantity.toDoubleOrNull()
                    val r = rate.toDoubleOrNull()
                    if (selectedItem == null) {
                        dialogError = "Please select an item."
                    } else if (q == null || q <= 0) {
                        dialogError = "Please enter a valid quantity."
                    } else if (r == null || r < 0) {
                        dialogError = "Please enter a valid rate."
                    } else {
                        transactionItems = transactionItems + TransactionItemData(selectedItem!!.id, q, r)
                        showAddItemDialog = false
                    }
                }) { Text("Add", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) { Text("Cancel") }
            }
        )
    }
}

private fun saveTransaction(
    viewModel: AppViewModel,
    type: TransactionType,
    partyId: String,
    invoiceNumber: String,
    transactionDate: Long,
    items: List<TransactionItemData>,
    discount: Double,
    vatPercent: Double,
    paymentMode: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    viewModel.addTransaction(
        type = type,
        partyId = partyId,
        invoiceNumber = invoiceNumber,
        transactionDate = transactionDate,
        items = items,
        discount = discount,
        vatPercent = vatPercent,
        paymentMode = paymentMode,
        onComplete = onSuccess,
        onError = onError
    )
}

fun generateInvoiceHtml(
    isPurchase: Boolean,
    invoiceNumber: String,
    dateStr: String,
    partyName: String,
    items: List<TransactionItemData>,
    subtotal: Double,
    discount: Double,
    taxableAmount: Double,
    vatAmount: Double,
    grandTotal: Double,
    paymentMode: String,
    company: com.example.data.local.entity.CompanyEntity?,
    allItems: List<ItemEntity>
): String {
    val sb = StringBuilder()
    sb.append("<html><head><style>table { width: 100%; border-collapse: collapse; } th, td { border: 1px solid black; padding: 8px; text-align: left; } th { background-color: #f2f2f2; } .header { text-align: center; margin-bottom: 20px; }</style></head><body>")
    
    if (company != null) {
        sb.append("<div class='header'>")
        sb.append("<h2>${company.businessName}</h2>")
        if (!company.address.isNullOrBlank()) sb.append("<p>${company.address}</p>")
        if (!company.panVatNumber.isNullOrBlank()) sb.append("<p>PAN: ${company.panVatNumber}</p>")
        sb.append("</div>")
    }

    sb.append("<h3>${if (isPurchase) "Purchase Bill" else "Sales Invoice"}</h3>")
    sb.append("<p><b>Invoice #:</b> $invoiceNumber &nbsp;&nbsp;&nbsp; <b>Date:</b> $dateStr</p>")
    sb.append("<p><b>Party:</b> $partyName &nbsp;&nbsp;&nbsp; <b>Payment:</b> $paymentMode</p>")
    
    sb.append("<table><tr><th>Item</th><th>Qty</th><th>Rate</th><th>Total</th></tr>")
    val itemMap = allItems.associateBy { it.id }
    items.forEach { item ->
        val itemName = itemMap[item.itemId]?.itemName ?: "Unknown Item"
        sb.append("<tr><td>$itemName</td><td>${item.quantity}</td><td>${item.rate}</td><td>${item.quantity * item.rate}</td></tr>")
    }
    sb.append("</table>")
    
    sb.append("<br/><table>")
    sb.append("<tr><td><b>Subtotal:</b></td><td>$subtotal</td></tr>")
    sb.append("<tr><td><b>Discount:</b></td><td>$discount</td></tr>")
    sb.append("<tr><td><b>Taxable:</b></td><td>$taxableAmount</td></tr>")
    sb.append("<tr><td><b>VAT:</b></td><td>$vatAmount</td></tr>")
    sb.append("<tr><td><b>Grand Total:</b></td><td>$grandTotal</td></tr>")
    sb.append("</table>")
    
    sb.append("</body></html>")
    return sb.toString()
}
