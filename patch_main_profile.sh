#!/bin/bash
sed -i '/composable("dashboard") {/i\        composable("profile") {\n            com.example.ui.screens.ProfileScreen(navController, viewModel)\n        }' app/src/main/java/com/example/MainActivity.kt
