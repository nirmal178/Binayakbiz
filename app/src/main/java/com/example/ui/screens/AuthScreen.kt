package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.example.R
import com.example.data.local.entity.CompanyEntity
import com.example.ui.AppViewModel
import com.example.ui.components.AddressPicker
import kotlinx.coroutines.launch

enum class CompanyAuthMode { SELECT_COMPANY, CREATE_COMPANY, PAN_LOGIN, FORGOT_PASSWORD, OTP }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val credentialManager = remember { CredentialManager.create(context) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val googleEmail by viewModel.googleEmail.collectAsState()
    val googleDisplayName by viewModel.googleDisplayName.collectAsState()
    val allCompanies by viewModel.allCompanies.collectAsState()
    val authError by viewModel.authError.collectAsState()

    // Google Sign-In Dialog State
    var showGoogleDialog by remember { mutableStateOf(false) }
    var googleEmailInput by remember { mutableStateOf("") }
    var googleNameInput by remember { mutableStateOf("") }
    var isGoogleSigningIn by remember { mutableStateOf(false) }

    // Mode when logged in with Google
    var companyAuthMode by remember { mutableStateOf(CompanyAuthMode.SELECT_COMPANY) }

    // Login State (PAN Login)
    var loginPan by remember { mutableStateOf("") }
    var loginUser by remember { mutableStateOf("admin") }
    var loginPass by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Signup State (Company Onboarding)
    var businessName by remember { mutableStateOf("") }
    var businessType by remember { mutableStateOf("Retail") }
    var ownerName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var panVat by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("NPR") }
    var fiscalYear by remember { mutableStateOf("2080/81") }
    var adminPassword by remember { mutableStateOf("") }

    // Set default owner name once Google user is signed in
    LaunchedEffect(googleDisplayName, googleEmail) {
        if (ownerName.isBlank()) {
            ownerName = googleDisplayName ?: googleEmail?.substringBefore("@") ?: ""
        }
        if (googleEmailInput.isBlank() && !googleEmail.isNullOrBlank()) {
            googleEmailInput = googleEmail ?: ""
        }
    }

    // Google Sign-In Dialog
    if (showGoogleDialog) {
        AlertDialog(
            onDismissRequest = { if (!isGoogleSigningIn) showGoogleDialog = false },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Sign in with Google",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Connect your Google account to manage your businesses and invoices.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = googleEmailInput,
                        onValueChange = { googleEmailInput = it },
                        label = { Text("Google Email Account") },
                        placeholder = { Text("e.g. yourname@gmail.com") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = googleNameInput,
                        onValueChange = { googleNameInput = it },
                        label = { Text("Owner / Business Name") },
                        placeholder = { Text("e.g. Lal Prasad Poudel") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val email = googleEmailInput.trim().ifBlank { "lpdpoudel@gmail.com" }
                        val name = googleNameInput.trim().ifBlank { email.substringBefore("@").replaceFirstChar { it.uppercase() } }
                        isGoogleSigningIn = true
                        viewModel.loginWithGoogle(
                            email = email,
                            displayName = name,
                            googleId = email,
                            driveAccessToken = null,
                            onSuccess = {
                                isGoogleSigningIn = false
                                showGoogleDialog = false
                            },
                            onError = {
                                isGoogleSigningIn = false
                            }
                        )
                    },
                    enabled = !isGoogleSigningIn
                ) {
                    if (isGoogleSigningIn) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Continue with Google")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showGoogleDialog = false },
                    enabled = !isGoogleSigningIn
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            // STEP 1: If NOT logged in with Google yet -> SHOW GOOGLE LOGIN ONLY
            if (googleEmail.isNullOrBlank() && companyAuthMode == CompanyAuthMode.SELECT_COMPANY) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // App Logo & Header
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = "App Logo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "BillingPro ERP",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Inventory, Accounting & Invoicing",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Informational Banner
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Google Cloud & Drive Sync",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                Text(
                                    text = "To create a new company or log in to your existing business accounts, please start by continuing with your Google account.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 17.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SuggestionChip(
                                        onClick = { },
                                        label = { Text("Drive Backup", fontSize = 10.sp) },
                                        icon = { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                    )
                                    SuggestionChip(
                                        onClick = { },
                                        label = { Text("Multi-Company", fontSize = 10.sp) },
                                        icon = { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                    )
                                }
                            }
                        }

                        if (authError != null) {
                            Text(
                                text = authError!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Google Sign In CTA Button
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        isGoogleSigningIn = true
                                        val webClientId = context.getString(R.string.default_web_client_id)
                                        
                                        val googleIdOption = GetSignInWithGoogleOption.Builder(webClientId)
                                            .build()
                                            
                                        val request = GetCredentialRequest.Builder()
                                            .addCredentialOption(googleIdOption)
                                            .build()
                                            
                                        val result = credentialManager.getCredential(
                                            request = request,
                                            context = context
                                        )
                                        
                                        val credential = result.credential
                                        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                            val email = googleIdTokenCredential.id
                                            val name = googleIdTokenCredential.displayName
                                            
                                            viewModel.loginWithGoogle(
                                                email = email,
                                                displayName = name,
                                                googleId = email,
                                                driveAccessToken = null, // Defer to explicit permission step
                                                onSuccess = { isGoogleSigningIn = false },
                                                onError = { isGoogleSigningIn = false }
                                            )
                                        } else {
                                            isGoogleSigningIn = false
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        android.widget.Toast.makeText(context, "Google Sign-In Error: ${e.message}. Using fallback.", android.widget.Toast.LENGTH_LONG).show()
                                        showGoogleDialog = true
                                        isGoogleSigningIn = false
                                    }
                                }
                            },
                            enabled = !isGoogleSigningIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isGoogleSigningIn) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "Google Logo",
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                Text(
                                    text = if (isGoogleSigningIn) "Signing in..." else "Continue with Google",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        OutlinedButton(
                            onClick = {
                                viewModel.clearAuthError()
                                companyAuthMode = CompanyAuthMode.PAN_LOGIN
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Login by PAN / VAT Number")
                        }

                        Text(
                            text = "Company login and creation will be unlocked immediately after Google authentication, or you can log in with a PAN directly.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // STEP 2: Logged in with Google -> SHOW COMPANY LOGIN AND CREATION
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (!googleEmail.isNullOrBlank()) {
                            // Google Account Connected Header Card
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.AccountCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = googleDisplayName ?: "Google User",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = googleEmail ?: "",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    TextButton(
                                        onClick = { viewModel.logoutGoogle() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Switch", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Navigation Header / Mode Switcher
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (companyAuthMode) {
                                CompanyAuthMode.SELECT_COMPANY -> {
                                    Text(
                                        text = "Select or Create Company",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                CompanyAuthMode.CREATE_COMPANY -> {
                                    Text(
                                        text = "Create New Company",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                CompanyAuthMode.PAN_LOGIN -> {
                                    Text(
                                        text = "Login with PAN/VAT",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                CompanyAuthMode.FORGOT_PASSWORD -> {
                                    Text(
                                        text = "Reset Password",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                CompanyAuthMode.OTP -> {
                                    Text(
                                        text = "OTP Login",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (companyAuthMode != CompanyAuthMode.SELECT_COMPANY) {
                                TextButton(onClick = { 
                                    viewModel.clearAuthError()
                                    companyAuthMode = CompanyAuthMode.SELECT_COMPANY 
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Back", fontSize = 12.sp)
                                }
                            }
                        }

                        if (authError != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = authError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }

                        // Animated Content for Modes
                        AnimatedContent(
                            targetState = companyAuthMode,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "company_mode_anim"
                        ) { mode ->
                            when (mode) {
                                CompanyAuthMode.SELECT_COMPANY -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        if (allCompanies.isNotEmpty()) {
                                            Text(
                                                text = "Your Available Companies (${allCompanies.size})",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )

                                            allCompanies.forEach { comp ->
                                                CompanyCardItem(
                                                    company = comp,
                                                    onOpen = {
                                                        viewModel.selectCompanyDirect(comp)
                                                    }
                                                )
                                            }

                                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                        } else {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(16.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Storefront,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                    Text(
                                                        text = "No company found yet",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                    Text(
                                                        text = "Create your business profile to start generating invoices and managing items.",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }

                                        // Action Buttons
                                        Button(
                                            onClick = {
                                                viewModel.clearAuthError()
                                                companyAuthMode = CompanyAuthMode.CREATE_COMPANY
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Create New Company")
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                viewModel.clearAuthError()
                                                companyAuthMode = CompanyAuthMode.PAN_LOGIN
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(46.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Login by PAN / VAT Number")
                                        }
                                    }
                                }

                                CompanyAuthMode.PAN_LOGIN -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = loginPan,
                                            onValueChange = { loginPan = it },
                                            label = { Text("Company PAN/VAT Number") },
                                            placeholder = { Text("e.g. 123456789") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        OutlinedTextField(
                                            value = loginUser,
                                            onValueChange = { loginUser = it },
                                            label = { Text("Username / Admin") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        OutlinedTextField(
                                            value = loginPass,
                                            onValueChange = { loginPass = it },
                                            label = { Text("Password") },
                                            singleLine = true,
                                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                            trailingIcon = {
                                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                                    Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            TextButton(onClick = { companyAuthMode = CompanyAuthMode.OTP }) {
                                                Text("Login with OTP", fontSize = 12.sp)
                                            }
                                            TextButton(onClick = { companyAuthMode = CompanyAuthMode.FORGOT_PASSWORD }) {
                                                Text("Forgot Password?", fontSize = 12.sp)
                                            }
                                        }

                                        Button(
                                            onClick = { viewModel.login(loginPan, loginUser, loginPass) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Log in to Company")
                                        }
                                    }
                                }

                                CompanyAuthMode.CREATE_COMPANY -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = businessName,
                                            onValueChange = { businessName = it },
                                            label = { Text("Business / Company Name *") },
                                            placeholder = { Text("e.g. Kathmandu Trading Pvt. Ltd.") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        OutlinedTextField(
                                            value = panVat,
                                            onValueChange = { panVat = it },
                                            label = { Text("PAN/VAT Number *") },
                                            placeholder = { Text("e.g. 601234567") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        OutlinedTextField(
                                            value = businessType,
                                            onValueChange = { businessType = it },
                                            label = { Text("Business Type (e.g. Retail, Wholesale)") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        OutlinedTextField(
                                            value = ownerName,
                                            onValueChange = { ownerName = it },
                                            label = { Text("Owner / Proprietor Name") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        OutlinedTextField(
                                            value = phoneNumber,
                                            onValueChange = { phoneNumber = it },
                                            label = { Text("Contact Phone Number") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Text(
                                            text = "Business Address (District, Municipality)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        AddressPicker(
                                            selectedAddress = address,
                                            onAddressChange = { address = it }
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = currency,
                                                onValueChange = { currency = it },
                                                label = { Text("Currency") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = fiscalYear,
                                                onValueChange = { fiscalYear = it },
                                                label = { Text("Fiscal Year") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        OutlinedTextField(
                                            value = adminPassword,
                                            onValueChange = { adminPassword = it },
                                            label = { Text("Admin Password (default: admin)") },
                                            visualTransformation = PasswordVisualTransformation(),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Button(
                                            onClick = {
                                                viewModel.signupCompany(
                                                    businessName = businessName,
                                                    panVat = panVat,
                                                    businessType = businessType,
                                                    ownerName = ownerName,
                                                    phoneNumber = phoneNumber,
                                                    address = address,
                                                    province = province,
                                                    district = district,
                                                    currency = currency,
                                                    fiscalYear = fiscalYear,
                                                    adminPassword = adminPassword
                                                )
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Complete Setup & Open")
                                        }
                                    }
                                }

                                CompanyAuthMode.FORGOT_PASSWORD -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "Enter your registered phone or email to receive password reset assistance.",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        var resetInput by remember { mutableStateOf("") }
                                        OutlinedTextField(
                                            value = resetInput,
                                            onValueChange = { resetInput = it },
                                            label = { Text("Phone / Email") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Button(
                                            onClick = { companyAuthMode = CompanyAuthMode.PAN_LOGIN },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Send Reset Code")
                                        }
                                    }
                                }

                                CompanyAuthMode.OTP -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "Enter your phone number to receive a one-time login OTP.",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        var phoneInput by remember { mutableStateOf("") }
                                        OutlinedTextField(
                                            value = phoneInput,
                                            onValueChange = { phoneInput = it },
                                            label = { Text("Phone Number") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Button(
                                            onClick = { companyAuthMode = CompanyAuthMode.PAN_LOGIN },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Send OTP")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompanyCardItem(
    company: CompanyEntity,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = company.businessName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "PAN: ${company.panVatNumber}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    if (!company.address.isNullOrBlank()) {
                        Text(
                            text = "• ${company.address}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            FilledTonalButton(
                onClick = onOpen,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Open", fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        }
    }
}
