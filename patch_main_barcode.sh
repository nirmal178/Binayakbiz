#!/bin/bash
sed -i '/composable("items") {/i\        composable("barcode_report") {\n            com.example.ui.screens.BarcodeReportScreen(navController, viewModel)\n        }' app/src/main/java/com/example/MainActivity.kt
