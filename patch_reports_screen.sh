#!/bin/bash
cat << 'INNER_EOF' > app/src/main/java/com/example/ui/screens/ReportsScreen.kt
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

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Sales", "Purchases", "Items", "Parties")

    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

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
                        val html = generateHtml(selectedTab, sales, purchases, items, customers + suppliers, dateFormat)
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
            val scrollState = rememberScrollState()
            Column(modifier = Modifier.fillMaxSize().horizontalScroll(scrollState)) {
                when (selectedTab) {
                    0 -> {
                        // Sales
                        TableRow(listOf("Date", "Invoice #", "Party", "Amount", "Mode"), true)
                        LazyColumn {
                            items(sales) { s ->
                                TableRow(listOf(dateFormat.format(Date(s.transactionDate)), s.invoiceNumber, s.partyName, "Rs. ${s.grandTotal}", s.paymentMode))
                            }
                        }
                    }
                    1 -> {
                        // Purchases
                        TableRow(listOf("Date", "Invoice #", "Party", "Amount", "Mode"), true)
                        LazyColumn {
                            items(purchases) { p ->
                                TableRow(listOf(dateFormat.format(Date(p.transactionDate)), p.invoiceNumber, p.partyName, "Rs. ${p.grandTotal}", p.paymentMode))
                            }
                        }
                    }
                    2 -> {
                        // Items
                        TableRow(listOf("Item Name", "Stock", "Cost", "Price", "Value"), true)
                        LazyColumn {
                            items(items) { i ->
                                TableRow(listOf(i.itemName, "${i.stockQuantity} ${i.uom}", "Rs. ${i.purchasePrice}", "Rs. ${i.salesPrice}", "Rs. ${i.stockQuantity * i.purchasePrice}"))
                            }
                        }
                    }
                    3 -> {
                        // Parties
                        TableRow(listOf("Type", "Name", "Phone", "PAN", "Address"), true)
                        LazyColumn {
                            items(customers + suppliers) { p ->
                                TableRow(listOf(p.type.name, p.name, p.contactPhone ?: "", p.pan ?: "", p.address ?: ""))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TableRow(cells: List<String>, isHeader: Boolean = false) {
    Row(modifier = Modifier.width(600.dp).padding(horizontal = 16.dp, vertical = 8.dp)) {
        cells.forEach { cell ->
            Text(
                text = cell,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
    HorizontalDivider()
}

fun generateCsv(tab: Int, sales: List<Any>, purchases: List<Any>, items: List<Any>, parties: List<Any>, df: SimpleDateFormat): String {
    val sb = java.lang.StringBuilder()
    when (tab) {
        0 -> {
            sb.append("Date,Invoice #,Party,Amount,Mode\n")
            (sales as List<com.example.data.local.entity.TransactionEntity>).forEach { s ->
                sb.append("${df.format(Date(s.transactionDate))},${s.invoiceNumber},${s.partyName},${s.grandTotal},${s.paymentMode}\n")
            }
        }
        1 -> {
            sb.append("Date,Invoice #,Party,Amount,Mode\n")
            (purchases as List<com.example.data.local.entity.TransactionEntity>).forEach { p ->
                sb.append("${df.format(Date(p.transactionDate))},${p.invoiceNumber},${p.partyName},${p.grandTotal},${p.paymentMode}\n")
            }
        }
        2 -> {
            sb.append("Item Name,Stock,Cost,Price,Value\n")
            (items as List<com.example.data.local.entity.ItemEntity>).forEach { i ->
                sb.append("${i.itemName},${i.stockQuantity},${i.purchasePrice},${i.salesPrice},${i.stockQuantity * i.purchasePrice}\n")
            }
        }
        3 -> {
            sb.append("Type,Name,Phone,PAN,Address\n")
            (parties as List<com.example.data.local.entity.PartyEntity>).forEach { p ->
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

fun generateHtml(tab: Int, sales: List<Any>, purchases: List<Any>, items: List<Any>, parties: List<Any>, df: SimpleDateFormat): String {
    val sb = java.lang.StringBuilder()
    sb.append("<html><head><style>table { width: 100%; border-collapse: collapse; } th, td { border: 1px solid black; padding: 8px; text-align: left; } th { background-color: #f2f2f2; }</style></head><body>")
    sb.append("<h2>Report</h2><table>")
    when (tab) {
        0 -> {
            sb.append("<tr><th>Date</th><th>Invoice #</th><th>Party</th><th>Amount</th><th>Mode</th></tr>")
            (sales as List<com.example.data.local.entity.TransactionEntity>).forEach { s ->
                sb.append("<tr><td>${df.format(Date(s.transactionDate))}</td><td>${s.invoiceNumber}</td><td>${s.partyName}</td><td>${s.grandTotal}</td><td>${s.paymentMode}</td></tr>")
            }
        }
        1 -> {
            sb.append("<tr><th>Date</th><th>Invoice #</th><th>Party</th><th>Amount</th><th>Mode</th></tr>")
            (purchases as List<com.example.data.local.entity.TransactionEntity>).forEach { p ->
                sb.append("<tr><td>${df.format(Date(p.transactionDate))}</td><td>${p.invoiceNumber}</td><td>${p.partyName}</td><td>${p.grandTotal}</td><td>${p.paymentMode}</td></tr>")
            }
        }
        2 -> {
            sb.append("<tr><th>Item Name</th><th>Stock</th><th>Cost</th><th>Price</th><th>Value</th></tr>")
            (items as List<com.example.data.local.entity.ItemEntity>).forEach { i ->
                sb.append("<tr><td>${i.itemName}</td><td>${i.stockQuantity}</td><td>${i.purchasePrice}</td><td>${i.salesPrice}</td><td>${i.stockQuantity * i.purchasePrice}</td></tr>")
            }
        }
        3 -> {
            sb.append("<tr><th>Type</th><th>Name</th><th>Phone</th><th>PAN</th><th>Address</th></tr>")
            (parties as List<com.example.data.local.entity.PartyEntity>).forEach { p ->
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
INNER_EOF
