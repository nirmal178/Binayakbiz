package com.example.ui.screens

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.local.entity.ItemEntity
import com.example.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeReportScreen(navController: NavController, viewModel: AppViewModel) {
    val items by viewModel.items.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Barcode Report") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { printBarcodes(context, items) }) {
                        Icon(Icons.Default.Print, contentDescription = "Print / Export PDF")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items.filter { it.barcode.isNotBlank() }) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        Text(item.itemName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Category: ${item.category} | Brand: ${item.brand}", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Fake barcode visualization for UI
                        // Using a monospace font with some vertical pipe characters to simulate a barcode in UI
                        Text(
                            text = "||| |||| | || || | |||| ||",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                        Text(item.barcode, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            if (items.none { it.barcode.isNotBlank() }) {
                item {
                    Text("No items with barcodes found. Please generate barcodes for items first.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun printBarcodes(context: Context, items: List<ItemEntity>) {
    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val printAdapter = view.createPrintDocumentAdapter("Barcode_Report")
            val jobName = "Items Barcode Report"
            printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
        }
    }
    
    val htmlContent = buildString {
        append("<html><head><style>")
        append("body { font-family: sans-serif; text-align: center; }")
        append(".barcode-container { border: 1px solid #ccc; margin: 16px; padding: 16px; display: inline-block; width: 40%; }")
        append(".barcode-visual { font-family: monospace; font-size: 40px; font-weight: bold; letter-spacing: 2px; }")
        append("</style></head><body>")
        append("<h2>Item Barcode Report</h2>")
        
        items.filter { it.barcode.isNotBlank() }.forEach { item ->
            append("<div class='barcode-container'>")
            append("<strong>${item.itemName}</strong><br/>")
            append("<small>${item.category} - ${item.brand}</small><br/>")
            append("<div class='barcode-visual'>||||| | || || || |</div>")
            append("<div>${item.barcode}</div>")
            append("</div>")
        }
        
        append("</body></html>")
    }
    
    webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
}
