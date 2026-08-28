package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.local.entity.PartyType
import com.example.data.local.entity.VoucherType
import com.example.ui.AppViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVoucherScreen(
    navController: NavController,
    viewModel: AppViewModel,
    voucherTypeStr: String,
    preselectedPartyId: String? = null
) {
    val isReceipt = voucherTypeStr == "RECEIPT"
    val type = if (isReceipt) VoucherType.RECEIPT else VoucherType.PAYMENT
    val partyType = if (isReceipt) PartyType.CUSTOMER else PartyType.SUPPLIER

    val parties by if (isReceipt) viewModel.customers.collectAsState() else viewModel.suppliers.collectAsState()
    val receipts by viewModel.receipts.collectAsState()
    val payments by viewModel.payments.collectAsState()

    var voucherNumber by remember { mutableStateOf("") }
    var voucherDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedPartyId by remember { mutableStateOf(preselectedPartyId) }
    var amount by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("CASH") }
    val paymentModes = listOf("CASH", "BANK / CHEQUE", "ESEWA / FONEPAY", "OTHER")
    var remarks by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val currencyFormat = remember { java.text.NumberFormat.getNumberInstance(java.util.Locale.US) }
    
    val currentLedger by remember(selectedPartyId) {
        if (selectedPartyId != null) viewModel.getPartyLedger(selectedPartyId!!, partyType)
        else kotlinx.coroutines.flow.flowOf(null)
    }.collectAsState(initial = null)

    // Auto-generate voucher number
    LaunchedEffect(type, receipts, payments) {
        if (voucherNumber.isBlank()) {
            voucherNumber = viewModel.getNextVoucherNumber(type)
        }
    }

    // Auto-set preselected party if provided
    LaunchedEffect(preselectedPartyId, parties) {
        if (selectedPartyId == null && preselectedPartyId != null) {
            selectedPartyId = preselectedPartyId
        }
    }

    val title = if (isReceipt) "Receipt from Customer" else "Payment to Supplier"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Banner
            Surface(
                color = if (isReceipt) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isReceipt) Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.CallMade,
                        contentDescription = null,
                        tint = if (isReceipt) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = if (isReceipt) "Money Receipt Entry" else "Payment Voucher Entry",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isReceipt) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = if (isReceipt) "Record cash/bank received from a customer against sales dues." else "Record payment made to a supplier against purchases.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isReceipt) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Voucher Number & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = voucherNumber,
                    onValueChange = { voucherNumber = it },
                    label = { Text("Voucher No *") },
                    modifier = Modifier.weight(1.1f),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            voucherNumber = viewModel.getNextVoucherNumber(type)
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Auto Generate")
                        }
                    }
                )

                val cal = Calendar.getInstance().apply { timeInMillis = voucherDate }
                val datePickerDialog = DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val picked = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        }
                        voucherDate = picked.timeInMillis
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                )

                OutlinedTextField(
                    value = dateFormat.format(Date(voucherDate)),
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

            // Party Selection Dropdown
            var partyExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = partyExpanded,
                onExpandedChange = { partyExpanded = !partyExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                val selectedParty = parties.find { it.id == selectedPartyId }
                OutlinedTextField(
                    value = selectedParty?.name ?: "Select ${if (isReceipt) "Customer" else "Supplier"} *",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (isReceipt) "Received From (Customer) *" else "Paid To (Supplier) *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = partyExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = partyExpanded,
                    onDismissRequest = { partyExpanded = false }
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
                                partyExpanded = false
                            }
                        )
                    }
                }
            }
            
            if (currentLedger != null && selectedPartyId != null) {
                val bal = currentLedger!!.netBalance
                val balStr = if (bal >= 0) "Dr. ${currencyFormat.format(bal)}" else "Cr. ${currencyFormat.format(-bal)}"
                Text(
                    text = "Current Balance: $balStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }

            // Amount Field
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount (रू) *") },
                placeholder = { Text("e.g. 5000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Text("रू", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                }
            )

            // Payment Mode Selector
            Text(
                "Payment Mode",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                paymentModes.forEach { mode ->
                    val isSelected = paymentMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { paymentMode = mode },
                        label = { Text(mode, fontSize = 12.sp) }
                    )
                }
            }

            // Remarks / Particulars
            OutlinedTextField(
                value = remarks,
                onValueChange = { remarks = it },
                label = { Text("Narration / Remarks / Cheque No.") },
                placeholder = { Text("e.g. Received via Bank cheque #99482, full settlement") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3
            )

            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    if (selectedPartyId == null) {
                        error = "Please select a ${if (isReceipt) "customer" else "supplier"}."
                        return@Button
                    }
                    val parsedAmt = amount.toDoubleOrNull()
                    if (parsedAmt == null || parsedAmt <= 0) {
                        error = "Please enter a valid positive amount."
                        return@Button
                    }
                    if (voucherNumber.isBlank()) {
                        error = "Voucher number is required."
                        return@Button
                    }

                    viewModel.addVoucher(
                        partyId = selectedPartyId!!,
                        type = type,
                        voucherNumber = voucherNumber,
                        voucherDate = voucherDate,
                        amount = parsedAmt,
                        paymentMode = paymentMode,
                        remarks = remarks,
                        onComplete = { showSuccessDialog = true },
                        onError = { error = it }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = if (isReceipt) "Save Receipt Voucher" else "Save Payment Voucher",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { navController.navigateUp() },
            title = { Text(if (isReceipt) "Receipt Recorded" else "Payment Recorded") },
            text = { Text("Voucher $voucherNumber of रू $amount has been saved to the party ledger.") },
            confirmButton = {
                Button(onClick = { navController.navigateUp() }) {
                    Text("Done")
                }
            }
        )
    }
}
