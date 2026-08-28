package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ui.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(navController: NavController, viewModel: AppViewModel) {
    val sales by viewModel.sales.collectAsState()
    val purchases by viewModel.purchases.collectAsState()
    val items by viewModel.items.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val currentCompany by viewModel.currentCompany.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Sales", "Purchases", "Items", "Parties")

    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val partyMap = remember(customers, suppliers) { (customers + suppliers).associateBy { it.id } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val csv = generateCsv(selectedTab, sales, purchases, items, customers + suppliers, dateFormat)
                        shareCsv(context, csv)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Export CSV")
                    }
                    IconButton(onClick = {
                        val html = generateHtml(selectedTab, sales, purchases, items, customers + suppliers, dateFormat, currentCompany)
                        printHtml(context, html)
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Print PDF")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 8.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Tabular View
            Column(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> {
                        // Sales
                        TableRow(listOf("Date", "Invoice #", "Party", "Amount", "Mode"), true)
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(sales) { s ->
                                TableRow(listOf(dateFormat.format(Date(s.transactionDate)), s.invoiceNumber, partyMap[s.partyId]?.name ?: "Unknown", "Rs. ${s.grandTotal}", s.paymentMode))
                            }
                        }
                    }
                    1 -> {
                        // Purchases
                        TableRow(listOf("Date", "Invoice #", "Party", "Amount", "Mode"), true)
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(purchases) { p ->
                                TableRow(listOf(dateFormat.format(Date(p.transactionDate)), p.invoiceNumber, partyMap[p.partyId]?.name ?: "Unknown", "Rs. ${p.grandTotal}", p.paymentMode))
                            }
                        }
                    }
                    2 -> {
                        // Items
                        TableRow(listOf("Item Name", "Stock", "Cost", "Price", "Value"), true)
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(items) { i ->
                                TableRow(listOf(i.itemName, "${i.stockQuantity} ${i.uom}", "Rs. ${i.purchasePrice}", "Rs. ${i.salesPrice}", "Rs. ${i.stockQuantity * i.purchasePrice}"))
                            }
                        }
                    }
                    3 -> {
                        // Parties
                        TableRow(listOf("Type", "Name", "Phone", "PAN", "Balance"), true)
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(customers + suppliers) { p ->
                                PartyReportRow(p, viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PartyReportRow(party: com.example.data.local.entity.PartyEntity, viewModel: AppViewModel) {
    val ledgerResult by viewModel.getPartyLedger(party.id, party.type).collectAsState(initial = null)
    val balance = ledgerResult?.netBalance ?: 0.0
    
    val format = java.text.NumberFormat.getNumberInstance(Locale.US)
    val balanceStr = if (balance != 0.0) "Rs. ${format.format(balance)}" else "Rs. 0"
    
    TableRow(listOf(party.type.name.take(4), party.name, party.contactPhone ?: "", party.pan ?: "", balanceStr))
}

@Composable
fun TableRow(cells: List<String>, isHeader: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        cells.forEach { cell ->
            Text(
                text = cell,
                modifier = Modifier.weight(1f).padding(end = 4.dp),
                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
    HorizontalDivider()
}

fun generateCsv(tab: Int, sales: List<com.example.data.local.entity.TransactionEntity>, purchases: List<com.example.data.local.entity.TransactionEntity>, items: List<com.example.data.local.entity.ItemEntity>, parties: List<com.example.data.local.entity.PartyEntity>, df: SimpleDateFormat): String {
    val sb = java.lang.StringBuilder()
    val partyMap = parties.associateBy { it.id }
    when (tab) {
        0 -> {
            sb.append("Date,Invoice #,Party,Amount,Mode\n")
            sales.forEach { s ->
                val pName = partyMap[s.partyId]?.name ?: "Unknown"
                sb.append("${df.format(Date(s.transactionDate))},${s.invoiceNumber},${pName},${s.grandTotal},${s.paymentMode}\n")
            }
        }
        1 -> {
            sb.append("Date,Invoice #,Party,Amount,Mode\n")
            purchases.forEach { p ->
                val pName = partyMap[p.partyId]?.name ?: "Unknown"
                sb.append("${df.format(Date(p.transactionDate))},${p.invoiceNumber},${pName},${p.grandTotal},${p.paymentMode}\n")
            }
        }
        2 -> {
            sb.append("Item Name,Stock,Cost,Price,Value\n")
            items.forEach { i ->
                sb.append("${i.itemName},${i.stockQuantity},${i.purchasePrice},${i.salesPrice},${i.stockQuantity * i.purchasePrice}\n")
            }
        }
        3 -> {
            sb.append("Type,Name,Phone,PAN,Address\n")
            parties.forEach { p ->
                sb.append("${p.type.name},${p.name},${p.contactPhone ?: ""},${p.pan ?: ""},${p.address ?: ""}\n")
            }
        }
    }
    return sb.toString()
}

fun shareCsv(context: Context, csv: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_TEXT, csv)
        putExtra(Intent.EXTRA_SUBJECT, "Exported Report")
    }
    context.startActivity(Intent.createChooser(intent, "Share CSV via"))
}

fun generateHtml(tab: Int, sales: List<com.example.data.local.entity.TransactionEntity>, purchases: List<com.example.data.local.entity.TransactionEntity>, items: List<com.example.data.local.entity.ItemEntity>, parties: List<com.example.data.local.entity.PartyEntity>, df: SimpleDateFormat, company: com.example.data.local.entity.CompanyEntity?): String {
    val sb = java.lang.StringBuilder()
    val partyMap = parties.associateBy { it.id }
    sb.append("<html><head><style>table { width: 100%; border-collapse: collapse; } th, td { border: 1px solid black; padding: 8px; text-align: left; } th { background-color: #f2f2f2; } .header { text-align: center; margin-bottom: 20px; }</style></head><body>")
    
    if (company != null) {
        sb.append("<div class='header'>")
        sb.append("<h2>${company.businessName}</h2>")
        if (!company.address.isNullOrBlank()) sb.append("<p>${company.address}</p>")
        if (!company.panVatNumber.isNullOrBlank()) sb.append("<p>PAN: ${company.panVatNumber}</p>")
        sb.append("</div>")
    }

    val reportTitle = when (tab) {
        0 -> "Sales Report"
        1 -> "Purchases Report"
        2 -> "Items Report"
        3 -> "Parties Report"
        else -> "Report"
    }

    sb.append("<h3>$reportTitle</h3><table>")
    when (tab) {
        0 -> {
            sb.append("<tr><th>Date</th><th>Invoice #</th><th>Party</th><th>Amount</th><th>Mode</th></tr>")
            sales.forEach { s ->
                val pName = partyMap[s.partyId]?.name ?: "Unknown"
                sb.append("<tr><td>${df.format(Date(s.transactionDate))}</td><td>${s.invoiceNumber}</td><td>${pName}</td><td>${s.grandTotal}</td><td>${s.paymentMode}</td></tr>")
            }
        }
        1 -> {
            sb.append("<tr><th>Date</th><th>Invoice #</th><th>Party</th><th>Amount</th><th>Mode</th></tr>")
            purchases.forEach { p ->
                val pName = partyMap[p.partyId]?.name ?: "Unknown"
                sb.append("<tr><td>${df.format(Date(p.transactionDate))}</td><td>${p.invoiceNumber}</td><td>${pName}</td><td>${p.grandTotal}</td><td>${p.paymentMode}</td></tr>")
            }
        }
        2 -> {
            sb.append("<tr><th>Item Name</th><th>Stock</th><th>Cost</th><th>Price</th><th>Value</th></tr>")
            items.forEach { i ->
                sb.append("<tr><td>${i.itemName}</td><td>${i.stockQuantity}</td><td>${i.purchasePrice}</td><td>${i.salesPrice}</td><td>${i.stockQuantity * i.purchasePrice}</td></tr>")
            }
        }
        3 -> {
            sb.append("<tr><th>Type</th><th>Name</th><th>Phone</th><th>PAN</th><th>Address</th></tr>")
            parties.forEach { p ->
                sb.append("<tr><td>${p.type.name}</td><td>${p.name}</td><td>${p.contactPhone ?: ""}</td><td>${p.pan ?: ""}</td><td>${p.address ?: ""}</td></tr>")
            }
        }
    }
    sb.append("</table></body></html>")
    return sb.toString()
}

fun printHtml(context: Context, html: String) {
    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val jobName = "Report_Print"
            val printAdapter = view.createPrintDocumentAdapter(jobName)
            printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
}
