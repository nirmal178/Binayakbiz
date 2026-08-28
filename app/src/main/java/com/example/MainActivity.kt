package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.UserPreferences
import com.example.data.drive.GoogleDriveManager
import com.example.data.firebase.FirebaseSyncManager
import com.example.data.local.AppDatabase
import com.example.data.repository.AppRepository
import com.example.ui.AppViewModel
import com.example.ui.AppViewModelFactory
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GoogleDriveSyncScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    val database = AppDatabase.getDatabase(this)
    val dao = database.appDao()
    val repository = AppRepository(dao)
    val userPrefs = UserPreferences(this)
    val driveManager = GoogleDriveManager(this, dao)
    val firebaseSyncManager = FirebaseSyncManager(this, dao)
    
    setContent {
      val factory = remember { AppViewModelFactory(repository, userPrefs, driveManager, firebaseSyncManager) }
      val viewModel: AppViewModel = viewModel(factory = factory)
      val loggedInCompanyId by viewModel.loggedInCompanyId.collectAsState()
      val isDarkModePref by viewModel.isDarkMode.collectAsState()
      val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
      val activeDarkTheme = isDarkModePref ?: isSystemDark
      
      MyApplicationTheme(darkTheme = activeDarkTheme) {
        if (loggedInCompanyId == null) {
          AuthScreen(viewModel = viewModel)
        } else {
          MainApp(viewModel = viewModel)
        }
      }
    }
  }
}

@Composable
fun MainApp(viewModel: AppViewModel) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "dashboard",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("profile") {
            com.example.ui.screens.ProfileScreen(navController, viewModel)
        }
        composable("dashboard") {
            DashboardScreen(navController, viewModel)
        }
        composable("google_drive_sync") {
            GoogleDriveSyncScreen(navController, viewModel)
        }
        composable("parties") {
            com.example.ui.screens.PartiesScreen(navController, viewModel)
        }
        composable("barcode_report") {
            com.example.ui.screens.BarcodeReportScreen(navController, viewModel)
        }
        composable("inventory") {
            com.example.ui.screens.ItemsScreen(navController, viewModel)
        }
        composable("transactions/{type}") { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "PURCHASE"
            com.example.ui.screens.TransactionsScreen(navController, viewModel, type)
        }
        composable("add_transaction/{type}") { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "PURCHASE"
            com.example.ui.screens.AddTransactionScreen(navController, viewModel, type)
        }
        composable("vouchers/{type}") { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "RECEIPT"
            com.example.ui.screens.VouchersScreen(navController, viewModel, type)
        }
        composable("add_voucher/{type}") { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "RECEIPT"
            com.example.ui.screens.AddVoucherScreen(navController, viewModel, type)
        }
        composable("party_ledger/{partyId}") { backStackEntry ->
            val partyId = backStackEntry.arguments?.getString("partyId") ?: ""
            com.example.ui.screens.PartyLedgerScreen(navController, viewModel, partyId)
        }
        composable("reports") {
            com.example.ui.screens.ReportsScreen(navController, viewModel)
        }
        composable("users") {
            com.example.ui.screens.UserManagementScreen(navController, viewModel)
        }
    }
}
