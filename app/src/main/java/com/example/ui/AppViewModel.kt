package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.UserPreferences
import com.example.data.local.entity.CompanyEntity
import com.example.data.local.entity.ItemEntity
import com.example.data.local.entity.PartyEntity
import com.example.data.local.entity.PartyType
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.local.entity.TransactionType
import com.example.data.local.entity.VoucherEntity
import com.example.data.local.entity.VoucherType
import com.example.data.local.entity.UserEntity
import com.example.data.repository.AppRepository
import com.example.data.drive.DriveFileItem
import com.example.data.drive.GoogleDriveManager
import com.example.data.firebase.FirebaseSyncManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

class AppViewModel(
    private val repository: AppRepository,
    private val userPrefs: UserPreferences,
    val driveManager: GoogleDriveManager? = null,
    val firebaseSyncManager: FirebaseSyncManager? = null
) : ViewModel() {

    // User Session & Dark Mode
    val loggedInCompanyId = userPrefs.loggedInCompanyId.stateIn(
        viewModelScope, SharingStarted.Eagerly, null
    )

    val isDarkMode: StateFlow<Boolean?> = userPrefs.isDarkMode.stateIn(
        viewModelScope, SharingStarted.Eagerly, null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentCompany: StateFlow<CompanyEntity?> = loggedInCompanyId.flatMapLatest { id ->
        if (id != null) {
            repository.getCompanyFlow(id)
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val customers = loggedInCompanyId.flatMapLatest { id ->
        if (id != null) repository.getCustomers(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val suppliers = loggedInCompanyId.flatMapLatest { id ->
        if (id != null) repository.getSuppliers(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val items = loggedInCompanyId.flatMapLatest { id ->
        if (id != null) repository.getItems(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val sales = loggedInCompanyId.flatMapLatest { id ->
        if (id != null) repository.getSales(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val purchases = loggedInCompanyId.flatMapLatest { id ->
        if (id != null) repository.getPurchases(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val receipts = loggedInCompanyId.flatMapLatest { id ->
        if (id != null) repository.getReceipts(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val payments = loggedInCompanyId.flatMapLatest { id ->
        if (id != null) repository.getPayments(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allVouchers = loggedInCompanyId.flatMapLatest { id ->
        if (id != null) repository.getAllVouchers(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCompanies: StateFlow<List<CompanyEntity>> = repository.getAllCompanies().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _authError = MutableStateFlow<String?>(null)
    val authError = _authError.asStateFlow()

    // Google Drive state
    private val _driveSyncState = MutableStateFlow<String?>(null)
    val driveSyncState: StateFlow<String?> = _driveSyncState.asStateFlow()

    private val _isDriveLoading = MutableStateFlow(false)
    val isDriveLoading: StateFlow<Boolean> = _isDriveLoading.asStateFlow()

    private val _driveBackupsList = MutableStateFlow<List<DriveFileItem>>(emptyList())
    val driveBackupsList: StateFlow<List<DriveFileItem>> = _driveBackupsList.asStateFlow()

    val googleEmail: StateFlow<String?> = userPrefs.googleEmail.stateIn(
        viewModelScope, SharingStarted.Eagerly, null
    )

    val googleDisplayName: StateFlow<String?> = userPrefs.googleDisplayName.stateIn(
        viewModelScope, SharingStarted.Eagerly, null
    )

    val googleDriveToken: StateFlow<String?> = userPrefs.googleDriveToken.stateIn(
        viewModelScope, SharingStarted.Eagerly, null
    )

    val googleDriveFolderId: StateFlow<String?> = userPrefs.googleDriveFolderId.stateIn(
        viewModelScope, SharingStarted.Eagerly, null
    )

    val isDriveAutoSync: StateFlow<Boolean> = userPrefs.isDriveAutoSync.stateIn(
        viewModelScope, SharingStarted.Eagerly, true
    )

    val hasAskedDrivePermission: StateFlow<Boolean> = userPrefs.hasAskedDrivePermission.stateIn(
        viewModelScope, SharingStarted.Eagerly, false
    )

    val lastDriveSyncTime: StateFlow<Long> = userPrefs.lastDriveSyncTime.stateIn(
        viewModelScope, SharingStarted.Eagerly, 0L
    )

    init {
        viewModelScope.launch {
            loggedInCompanyId.collect { companyId ->
                if (companyId != null) {
                    checkAndPerformInitialDriveSync()
                }
            }
        }
    }

    fun clearDriveMessage() {
        _driveSyncState.value = null
    }

    fun setDriveAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            userPrefs.setDriveAutoSync(enabled)
            if (enabled) {
                checkAndPerformInitialDriveSync()
            }
        }
    }

    fun setGoogleDriveToken(token: String?) {
        viewModelScope.launch {
            userPrefs.setGoogleDriveToken(token)
            if (!token.isNullOrBlank()) {
                checkAndPerformInitialDriveSync()
            }
        }
    }

    fun grantDrivePermission(enableAutoSync: Boolean) {
        viewModelScope.launch {
            userPrefs.setHasAskedDrivePermission(true)
            userPrefs.setDriveAutoSync(enableAutoSync)
            if (enableAutoSync) {
                val currentToken = userPrefs.googleDriveToken.firstOrNull()
                if (currentToken.isNullOrBlank()) {
                    userPrefs.setGoogleDriveToken("oauth_token_drive_active")
                }
                checkAndPerformInitialDriveSync()
            }
        }
    }

    fun grantDrivePermissionAndInit(token: String?) {
        viewModelScope.launch {
            userPrefs.setHasAskedDrivePermission(true)
            if (!token.isNullOrBlank()) {
                userPrefs.setGoogleDriveToken(token)
            }
            checkAndPerformInitialDriveSync()
        }
    }

    fun dismissDrivePermissionPrompt() {
        viewModelScope.launch {
            userPrefs.setHasAskedDrivePermission(true)
        }
    }

    fun checkAndPerformInitialDriveSync() {
        viewModelScope.launch {
            val cId = loggedInCompanyId.value ?: userPrefs.loggedInCompanyId.firstOrNull() ?: return@launch
            val comp = currentCompany.value ?: repository.getCompany(cId) ?: return@launch
            val token = userPrefs.googleDriveToken.firstOrNull()
            val isAuto = userPrefs.isDriveAutoSync.firstOrNull() ?: true
            val mgr = driveManager ?: return@launch

            if (!token.isNullOrBlank() && isAuto) {
                _isDriveLoading.value = true
                // 1. Create or get App Data Folder on Google Drive
                val folderRes = mgr.getOrCreateAppDataFolder(token)
                folderRes.onSuccess { folderId ->
                    userPrefs.setGoogleDriveFolderId(folderId)
                    // 2. Perform auto data sync inside folder
                    val syncRes = mgr.syncDataToDrive(token, comp.id, comp.businessName, folderId)
                    syncRes.onSuccess { msg ->
                        _driveSyncState.value = msg
                        userPrefs.setLastDriveSyncTime(System.currentTimeMillis())
                    }.onFailure { err ->
                        _driveSyncState.value = "Auto-sync notice: ${err.message}"
                    }
                }.onFailure { err ->
                    _driveSyncState.value = "Google Drive folder error: ${err.message}"
                }
                _isDriveLoading.value = false
            }
        }
    }

    fun triggerAutoSync() {
        viewModelScope.launch {
            val cId = loggedInCompanyId.value ?: return@launch
            val comp = currentCompany.value ?: repository.getCompany(cId) ?: return@launch
            val token = userPrefs.googleDriveToken.firstOrNull()
            val isAuto = userPrefs.isDriveAutoSync.firstOrNull() ?: true
            val folderId = userPrefs.googleDriveFolderId.firstOrNull()
            val mgr = driveManager ?: return@launch

            if (!token.isNullOrBlank() && isAuto) {
                val syncRes = mgr.syncDataToDrive(token, comp.id, comp.businessName, folderId)
                syncRes.onSuccess {
                    userPrefs.setLastDriveSyncTime(System.currentTimeMillis())
                }
            }
        }
    }

    fun syncAllToFirestore() {
        val comp = currentCompany.value ?: return
        val mgr = firebaseSyncManager ?: return
        viewModelScope.launch {
            _isDriveLoading.value = true
            _driveSyncState.value = "Syncing local database to Cloud Firestore..."
            val result = mgr.pushAllToFirestore(comp.id)
            _isDriveLoading.value = false
            result.onSuccess { msg ->
                _driveSyncState.value = msg
            }.onFailure { err ->
                _driveSyncState.value = "Firestore sync error: ${err.message}"
            }
        }
    }

    private fun triggerFirebaseAutoSync() {
        val comp = currentCompany.value ?: return
        val mgr = firebaseSyncManager ?: return
        viewModelScope.launch {
            mgr.pushAllToFirestore(comp.id)
        }
    }

    fun pullAllFromFirestore() {
        val comp = currentCompany.value ?: return
        val mgr = firebaseSyncManager ?: return
        viewModelScope.launch {
            _isDriveLoading.value = true
            _driveSyncState.value = "Pulling data from Cloud Firestore..."
            val result = mgr.pullAllFromFirestore(comp.id)
            _isDriveLoading.value = false
            result.onSuccess { msg ->
                _driveSyncState.value = msg
            }.onFailure { err ->
                _driveSyncState.value = "Firestore pull error: ${err.message}"
            }
        }
    }

    fun backupToGoogleDrive(accessToken: String) {
        val comp = currentCompany.value ?: return
        val mgr = driveManager ?: return
        viewModelScope.launch {
            _isDriveLoading.value = true
            _driveSyncState.value = "Creating & uploading backup snapshot to Google Drive..."
            val folderId = userPrefs.googleDriveFolderId.firstOrNull()
            val result = mgr.uploadBackupToDrive(accessToken, comp.id, comp.businessName, folderId)
            _isDriveLoading.value = false
            result.onSuccess { msg ->
                _driveSyncState.value = msg
                userPrefs.setGoogleDriveToken(accessToken)
                userPrefs.setLastDriveSyncTime(System.currentTimeMillis())
                // refresh backups list
                loadDriveBackups(accessToken)
            }.onFailure { err ->
                _driveSyncState.value = "Drive backup error: ${err.message}"
            }
        }
    }

    fun loadDriveBackups(accessToken: String) {
        val mgr = driveManager ?: return
        viewModelScope.launch {
            _isDriveLoading.value = true
            val result = mgr.listBackupsFromDrive(accessToken)
            _isDriveLoading.value = false
            result.onSuccess { list ->
                _driveBackupsList.value = list
                userPrefs.setGoogleDriveToken(accessToken)
            }.onFailure { err ->
                _driveSyncState.value = "Failed to load backups list: ${err.message}"
            }
        }
    }

    fun restoreFromGoogleDrive(accessToken: String, fileId: String) {
        val mgr = driveManager ?: return
        viewModelScope.launch {
            _isDriveLoading.value = true
            _driveSyncState.value = "Downloading & restoring database from Google Drive..."
            val result = mgr.restoreBackupFromDrive(accessToken, fileId)
            _isDriveLoading.value = false
            result.onSuccess { msg ->
                _driveSyncState.value = "Success: $msg"
                userPrefs.setGoogleDriveToken(accessToken)
            }.onFailure { err ->
                _driveSyncState.value = "Drive restore error: ${err.message}"
            }
        }
    }

    fun toggleDarkMode(currentDark: Boolean) {
        viewModelScope.launch {
            userPrefs.setDarkMode(!currentDark)
        }
    }

    fun login(pan: String, username: String = "admin", password: String = "admin") {
        val cleanPan = pan.trim()
        val cleanUsername = username.trim()
        val cleanPassword = password.trim()
        viewModelScope.launch {
            if (cleanPan.isBlank() || cleanUsername.isBlank() || cleanPassword.isBlank()) {
                _authError.value = "Please fill in all fields."
                return@launch
            }
            try {
                val company = repository.getCompanyByPan(cleanPan)
                if (company != null) {
                    val user = repository.getUserByUsername(company.id, cleanUsername)
                    if (user != null && user.passwordHash == cleanPassword) {
                        userPrefs.setLoggedInSession(company.id, user.id)
                        _authError.value = null
                    } else {
                        _authError.value = "Incorrect username or password."
                    }
                } else {
                    _authError.value = "Company with this PAN not found. Switch to Sign Up."
                }
            } catch (e: Exception) {
                _authError.value = e.message ?: "An error occurred during login."
            }
        }
    }

    fun signupCompany(
        businessName: String, panVat: String, businessType: String,
        ownerName: String, phoneNumber: String, address: String, province: String,
        district: String, currency: String, fiscalYear: String, adminPassword: String
    ) {
        val cleanPan = panVat.trim()
        val cleanPassword = if (adminPassword.isBlank()) "admin" else adminPassword.trim()
        viewModelScope.launch {
            if (cleanPan.isBlank() || businessName.isBlank()) {
                _authError.value = "Business Name and PAN/VAT are required."
                return@launch
            }
            if (cleanPan.length != 9 || !cleanPan.all { it.isDigit() }) {
                _authError.value = "PAN/VAT must be exactly 9 digits."
                return@launch
            }
            try {
                val existing = repository.getCompanyByPan(cleanPan)
                if (existing != null) {
                    _authError.value = "A company with this PAN/VAT already exists."
                    return@launch
                }

                val newCompany = com.example.data.local.entity.CompanyEntity(
                    businessName = businessName.trim(),
                    businessType = businessType.trim(),
                    ownerName = ownerName.trim(),
                    phoneNumber = phoneNumber.trim(),
                    address = address.trim(),
                    province = province.trim(),
                    district = district.trim(),
                    panVatNumber = cleanPan,
                    currency = currency.trim(),
                    fiscalYear = fiscalYear.trim()
                )
                repository.createCompany(newCompany)
                val headOffice = com.example.data.local.entity.BranchEntity(
                    companyId = newCompany.id,
                    branchName = "Head Office",
                    address = address.trim(),
                    phoneNumber = phoneNumber.trim(),
                    isHeadOffice = true
                )
                repository.insertBranch(headOffice)
                val adminUser = com.example.data.local.entity.UserEntity(
                    companyId = newCompany.id,
                    branchId = headOffice.id,
                    username = "admin", // or phoneNumber if preferred
                    passwordHash = cleanPassword,
                    role = "Business Owner",
                    canCreateSalesInvoice = true,
                    canCreatePurchaseInvoice = true,
                    canCreateVoucher = true,
                    canManageItems = true,
                    canManageParties = true,
                    canViewReports = true
                )
                repository.addUser(adminUser)
                userPrefs.setLoggedInSession(newCompany.id, adminUser.id)
                _authError.value = null
            } catch (e: Exception) {
                _authError.value = e.message ?: "Registration failed."
            }
        }
    }

    fun loginWithGoogle(
        email: String,
        displayName: String?,
        googleId: String,
        driveAccessToken: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val cleanEmail = email.trim()
                val cleanName = displayName?.trim()?.ifBlank { null }
                    ?: cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }

                val all = repository.getAllCompanies().firstOrNull() ?: emptyList()
                var targetCompany = all.find {
                    it.ownerName.equals(cleanName, ignoreCase = true) ||
                    it.businessName.contains(cleanEmail, ignoreCase = true) ||
                    it.businessName.equals("$cleanName Store", ignoreCase = true)
                }

                if (targetCompany == null) {
                    val panNumber = "VAT-${Math.abs(cleanEmail.hashCode() % 9000000 + 1000000)}"
                    val newCompany = com.example.data.local.entity.CompanyEntity(
                        businessName = "$cleanName Store",
                        businessType = "General Enterprise",
                        ownerName = cleanName,
                        phoneNumber = "",
                        address = "Kathmandu, Nepal",
                        province = "Bagmati",
                        district = "Kathmandu",
                        panVatNumber = panNumber,
                        currency = "NPR",
                        fiscalYear = "2080/81"
                    )
                    repository.createCompany(newCompany)
                    val headOffice = com.example.data.local.entity.BranchEntity(
                        companyId = newCompany.id,
                        branchName = "Head Office",
                        address = "Kathmandu, Nepal",
                        phoneNumber = "",
                        isHeadOffice = true
                    )
                    repository.insertBranch(headOffice)
                    val adminUser = com.example.data.local.entity.UserEntity(
                        companyId = newCompany.id,
                        branchId = headOffice.id,
                        username = cleanEmail,
                        passwordHash = "google_auth_$googleId",
                        role = "Business Owner",
                        canCreateSalesInvoice = true,
                        canCreatePurchaseInvoice = true,
                        canCreateVoucher = true,
                        canManageItems = true,
                        canManageParties = true,
                        canViewReports = true
                    )
                    repository.addUser(adminUser)
                    userPrefs.setLoggedInSession(newCompany.id, adminUser.id)
                } else {
                    val user = repository.getFirstUserByCompany(targetCompany.id) ?: com.example.data.local.entity.UserEntity(
                        companyId = targetCompany.id,
                        username = cleanEmail,
                        passwordHash = "google_auth_$googleId",
                        role = "Business Owner",
                        canCreateSalesInvoice = true,
                        canCreatePurchaseInvoice = true,
                        canCreateVoucher = true,
                        canManageItems = true,
                        canManageParties = true,
                        canViewReports = true
                    ).also { repository.addUser(it) }
                    userPrefs.setLoggedInSession(targetCompany.id, user.id)
                }

                userPrefs.setGoogleUser(cleanEmail, cleanName, driveAccessToken)
                _authError.value = null

                if (!driveAccessToken.isNullOrBlank()) {
                    checkAndPerformInitialDriveSync()
                }

                onSuccess()
            } catch (e: Exception) {
                _authError.value = e.message ?: "Google Sign-In failed."
                onError(e.message ?: "Google Sign-In failed.")
            }
        }
    }

    fun signInWithGoogleAccount(
        email: String,
        displayName: String?,
        driveAccessToken: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val cleanEmail = email.trim()
                val cleanName = displayName?.trim()?.ifBlank { null }
                    ?: cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }

                userPrefs.setGoogleUser(cleanEmail, cleanName, driveAccessToken)
                if (!driveAccessToken.isNullOrBlank()) {
                    userPrefs.setGoogleDriveToken(driveAccessToken)
                    userPrefs.setHasAskedDrivePermission(true)
                }
                _authError.value = null
                onSuccess()
            } catch (e: Exception) {
                _authError.value = e.message ?: "Google Sign-In failed."
                onError(e.message ?: "Google Sign-In failed.")
            }
        }
    }

    fun selectCompanyDirect(
        company: CompanyEntity,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val user = repository.getFirstUserByCompany(company.id) ?: com.example.data.local.entity.UserEntity(
                    companyId = company.id,
                    username = "admin",
                    passwordHash = "admin",
                    role = "Business Owner",
                    canCreateSalesInvoice = true,
                    canCreatePurchaseInvoice = true,
                    canCreateVoucher = true,
                    canManageItems = true,
                    canManageParties = true,
                    canViewReports = true
                ).also { repository.addUser(it) }

                userPrefs.setLoggedInSession(company.id, user.id)
                _authError.value = null
                checkAndPerformInitialDriveSync()
                onSuccess()
            } catch (e: Exception) {
                _authError.value = e.message ?: "Failed to open company."
                onError(e.message ?: "Failed to open company.")
            }
        }
    }

    fun logoutCompany() {
        viewModelScope.launch {
            userPrefs.clearCompanySession()
        }
    }

    fun logoutGoogle() {
        viewModelScope.launch {
            userPrefs.clearSession()
        }
    }

    fun logout() {
        viewModelScope.launch {
            userPrefs.clearSession()
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    // -- Parties --
    fun addParty(
        name: String,
        email: String?,
        pan: String?,
        phone: String?,
        address: String?,
        type: PartyType,
        onComplete: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val cId = loggedInCompanyId.value ?: userPrefs.loggedInCompanyId.firstOrNull()
                if (cId == null) {
                    onError("Active company session not found. Please log in.")
                    return@launch
                }

                repository.addParty(
                    PartyEntity(
                        companyId = cId,
                        type = type,
                        name = name.trim(),
                        email = email?.trim()?.ifBlank { null },
                        pan = pan?.trim()?.ifBlank { null },
                        contactPhone = phone?.trim()?.ifBlank { null },
                        address = address?.trim()?.ifBlank { null }
                    )
                )
                
                
                triggerAutoSync()
                triggerFirebaseAutoSync()
                onComplete()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to save ${type.name.lowercase()}.")
            }
        }
    }

    fun updateParty(party: PartyEntity, onComplete: () -> Unit, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.updateParty(party)
                
                
                triggerAutoSync()
                triggerFirebaseAutoSync()
                onComplete()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to update party.")
            }
        }
    }

    fun deleteParty(partyId: String, onComplete: () -> Unit, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deleteParty(partyId)
                
                
                triggerAutoSync()
                triggerFirebaseAutoSync()
                onComplete()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to delete party.")
            }
        }
    }

    // -- Items --
    fun addItem(
        name: String,
        hsCode: String,
        uom: String,
        purchasePrice: Double,
        salesPrice: Double,
        initialStock: Double = 0.0,
        barcode: String = "",
        category: String = "",
        brand: String = "",
        wholesalePrice: Double = 0.0,
        taxRate: Double = 0.0,
        imageUri: String? = null,
        expiryDate: Long? = null,
        batchNumber: String = "",
        status: String = "ACTIVE",
        onComplete: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val cId = loggedInCompanyId.value ?: userPrefs.loggedInCompanyId.firstOrNull()
                if (cId == null) {
                    onError("Active company session not found. Please log in.")
                    return@launch
                }

                repository.addItem(
                    ItemEntity(
                        companyId = cId,
                        itemName = name.trim(),
                        hsCode = hsCode.trim(),
                        uom = uom.trim().ifBlank { "Pcs" },
                        purchasePrice = purchasePrice,
                        salesPrice = salesPrice,
                        stockQuantity = initialStock,
                        barcode = barcode,
                        category = category,
                        brand = brand,
                        wholesalePrice = wholesalePrice,
                        taxRate = taxRate,
                        imageUri = imageUri,
                        expiryDate = expiryDate,
                        batchNumber = batchNumber,
                        status = status
                    )
                )
                
                
                triggerAutoSync()
                triggerFirebaseAutoSync()
                onComplete()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to add item.")
            }
        }
    }

    fun updateItem(item: ItemEntity, onComplete: () -> Unit, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.updateItem(item)
                
                
                triggerAutoSync()
                triggerFirebaseAutoSync()
                onComplete()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to update item.")
            }
        }
    }

    fun deleteItem(itemId: String, onComplete: () -> Unit, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deleteItem(itemId)
                
                
                triggerAutoSync()
                triggerFirebaseAutoSync()
                onComplete()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to delete item.")
            }
        }
    }

    // -- Auto-Numbering Helpers --
    fun getNextInvoiceNumber(type: TransactionType): String {
        val list = if (type == TransactionType.SALE) sales.value else purchases.value
        val prefix = if (type == TransactionType.SALE) "INV-" else "BILL-"
        val numbers = list.mapNotNull {
            val numPart = it.invoiceNumber.removePrefix(prefix).trim()
            numPart.toIntOrNull()
        }
        val nextVal = if (numbers.isEmpty()) list.size + 1 else (numbers.maxOrNull() ?: 0) + 1
        return String.format(Locale.US, "%s%04d", prefix, nextVal)
    }

    fun getNextVoucherNumber(type: VoucherType): String {
        val list = if (type == VoucherType.RECEIPT) receipts.value else payments.value
        val prefix = if (type == VoucherType.RECEIPT) "REC-" else "PAY-"
        val numbers = list.mapNotNull {
            val numPart = it.voucherNumber.removePrefix(prefix).trim()
            numPart.toIntOrNull()
        }
        val nextVal = if (numbers.isEmpty()) list.size + 1 else (numbers.maxOrNull() ?: 0) + 1
        return String.format(Locale.US, "%s%04d", prefix, nextVal)
    }

    // Cash Party auto-resolution
    fun getOrCreateCashParty(type: PartyType, onReady: (PartyEntity) -> Unit) {
        val currentList = if (type == PartyType.CUSTOMER) customers.value else suppliers.value
        val defaultName = if (type == PartyType.CUSTOMER) "Cash Customer" else "Cash Supplier"
        val existing = currentList.find { it.name.equals(defaultName, ignoreCase = true) || it.name.equals("Cash", ignoreCase = true) }
        if (existing != null) {
            onReady(existing)
        } else {
            val cId = loggedInCompanyId.value ?: return
            viewModelScope.launch {
                val newParty = PartyEntity(
                    companyId = cId,
                    type = type,
                    name = defaultName,
                    pan = null,
                    contactPhone = null,
                    address = "Over the Counter / Cash"
                )
                try {
                    repository.addParty(newParty)
                    onReady(newParty)
                } catch (e: Exception) {
                    // In case of race condition or conflict, find any
                    val fallback = (if (type == PartyType.CUSTOMER) customers.value else suppliers.value).firstOrNull()
                    if (fallback != null) onReady(fallback)
                }
        }
        }
    }

    // -- Transactions --
    fun addTransaction(
        type: TransactionType,
        partyId: String,
        invoiceNumber: String,
        transactionDate: Long = System.currentTimeMillis(),
        items: List<TransactionItemData>,
        discount: Double,
        vatPercent: Double,
        paymentMode: String,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cId = loggedInCompanyId.value ?: return
        viewModelScope.launch {
            if (items.isEmpty()) {
                onError("Please add at least one item.")
                return@launch
            }
            if (invoiceNumber.isBlank()) {
                onError("Invoice number is required.")
                return@launch
            }
            val subtotal = items.sumOf { it.quantity * it.rate }
            val afterDiscount = subtotal - discount
            val taxAmount = (afterDiscount * vatPercent) / 100.0
            val grandTotal = afterDiscount + taxAmount

            val transactionId = UUID.randomUUID().toString()
            val transaction = TransactionEntity(
                id = transactionId,
                companyId = cId,
                partyId = partyId,
                type = type,
                invoiceNumber = invoiceNumber,
                transactionDate = transactionDate,
                subtotal = subtotal,
                discount = discount,
                vatPercent = vatPercent,
                taxAmount = taxAmount,
                grandTotal = grandTotal,
                paymentMode = paymentMode
            )

            val dbItems = items.map {
                TransactionItemEntity(
                    transactionId = transactionId,
                    itemId = it.itemId,
                    companyId = cId,
                    quantity = it.quantity,
                    rate = it.rate,
                    amount = it.quantity * it.rate
                )
            }
            try {
                repository.saveTransaction(transaction, dbItems, transaction.type == com.example.data.local.entity.TransactionType.PURCHASE)
                
                
                triggerAutoSync()
                triggerFirebaseAutoSync()
                onComplete()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to save transaction. Invoice number might be duplicate.")
            }
        }
    }

    fun deleteTransaction(transactionId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
            
            
                triggerAutoSync()
                triggerFirebaseAutoSync()
                onComplete()
        }
    }

    // -- Vouchers (Receipts & Payments) --
    fun addVoucher(
        partyId: String,
        type: VoucherType,
        voucherNumber: String,
        voucherDate: Long = System.currentTimeMillis(),
        amount: Double,
        paymentMode: String,
        remarks: String?,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cId = loggedInCompanyId.value ?: return
        viewModelScope.launch {
            if (amount <= 0.0) {
                onError("Amount must be greater than zero.")
                return@launch
            }
            if (voucherNumber.isBlank()) {
                onError("Voucher number is required.")
                return@launch
            }
            val voucher = VoucherEntity(
                companyId = cId,
                partyId = partyId,
                voucherType = type,
                voucherNumber = voucherNumber.trim(),
                voucherDate = voucherDate,
                amount = amount,
                paymentMode = paymentMode,
                remarks = remarks?.trim()?.ifBlank { null }
            )
            try {
                repository.addVoucher(voucher)
                
                
                triggerAutoSync()
                triggerFirebaseAutoSync()
                onComplete()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to save voucher. Voucher number might be duplicate.")
            }
        }
    }

    fun deleteVoucher(voucherId: String, onComplete: () -> Unit, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deleteVoucher(voucherId)
                
                
                triggerAutoSync()
                triggerFirebaseAutoSync()
                onComplete()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to delete voucher.")
            }
        }
    }

    // -- Party Ledger Calculation --
        fun getPartyLedger(partyId: String, partyType: PartyType): Flow<PartyLedgerResult> {
        val txFlow = repository.getTransactionsForParty(partyId)
        val voucherFlow = repository.getVouchersForParty(partyId)

        return combine(txFlow, voucherFlow) { txs: List<com.example.data.local.entity.TransactionEntity>, vouchers: List<com.example.data.local.entity.VoucherEntity> ->
            val entries = mutableListOf<PartyLedgerEntry>()

            if (partyType == PartyType.CUSTOMER) {
                // For Customer:
                // Sales Invoice => Debit (Increase receivable)
                for (tx in txs.filter { it.type == TransactionType.SALE }) {
                    entries.add(
                        PartyLedgerEntry(
                            date = tx.transactionDate,
                            refNumber = tx.invoiceNumber,
                            entryType = "Sales Invoice",
                            particulars = "Sales Bill (Subtotal: रू ${tx.subtotal.toInt()}, VAT: रू ${tx.taxAmount.toInt()})",
                            paymentMode = tx.paymentMode,
                            debit = tx.grandTotal,
                            credit = if (tx.paymentMode == "CASH") tx.grandTotal else 0.0,
                            id = tx.id
                        )
                    )
                }
                for (v in vouchers) {
                    val isReceipt = v.voucherType == VoucherType.RECEIPT
                    entries.add(
                        PartyLedgerEntry(
                            date = v.voucherDate,
                            refNumber = v.voucherNumber,
                            entryType = if (isReceipt) "Receipt Voucher" else "Payment Voucher",
                            particulars = v.remarks ?: if (isReceipt) "Payment Received" else "Payment Made",
                            paymentMode = v.paymentMode,
                            debit = if (isReceipt) 0.0 else v.amount,
                            credit = if (isReceipt) v.amount else 0.0,
                            id = v.id
                        )
                    )
                }
            } else {
                // For Supplier:
                // Purchase Bill => Credit (Increase payable)
                for (tx in txs.filter { it.type == TransactionType.PURCHASE }) {
                    entries.add(
                        PartyLedgerEntry(
                            date = tx.transactionDate,
                            refNumber = tx.invoiceNumber,
                            entryType = "Purchase Bill",
                            particulars = "Purchase Bill (Subtotal: रू ${tx.subtotal.toInt()}, VAT: रू ${tx.taxAmount.toInt()})",
                            paymentMode = tx.paymentMode,
                            debit = if (tx.paymentMode == "CASH") tx.grandTotal else 0.0,
                            credit = tx.grandTotal,
                            id = tx.id
                        )
                    )
                }
                for (v in vouchers) {
                    val isPayment = v.voucherType == VoucherType.PAYMENT
                    entries.add(
                        PartyLedgerEntry(
                            date = v.voucherDate,
                            refNumber = v.voucherNumber,
                            entryType = if (isPayment) "Payment Voucher" else "Receipt Voucher",
                            particulars = v.remarks ?: if (isPayment) "Payment Made to Supplier" else "Refund Received",
                            paymentMode = v.paymentMode,
                            debit = if (isPayment) v.amount else 0.0,
                            credit = if (isPayment) 0.0 else v.amount,
                            id = v.id
                        )
                    )
                }
            }

            val sortedEntries = entries.sortedBy { it.date }
            var running = 0.0
            val computedEntries = sortedEntries.map { entry ->
                running += if (partyType == PartyType.CUSTOMER) {
                    (entry.debit - entry.credit) // Positive = Dr (Customer owes us)
                } else {
                    (entry.credit - entry.debit) // Positive = Cr (We owe supplier)
                }
                entry.copy(runningBalance = running)
            }

            val totalDebit = computedEntries.sumOf { it.debit }
            val totalCredit = computedEntries.sumOf { it.credit }
            val netBalance = if (partyType == PartyType.CUSTOMER) (totalDebit - totalCredit) else (totalCredit - totalDebit)

            PartyLedgerResult(
                entries = computedEntries,
                totalDebit = totalDebit,
                totalCredit = totalCredit,
                netBalance = netBalance,
                partyType = partyType
            )
        }
    }
    
    // -- User Management --
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: StateFlow<UserEntity?> = userPrefs.loggedInUserId.flatMapLatest { id ->
        if (id != null) repository.getUserById(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val companyUsers = loggedInCompanyId.flatMapLatest { id ->
        if (id != null) repository.getUsersByCompany(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    fun addUser(username: String, password: String, role: String) {
        val compId = currentCompany.value?.id ?: return
        viewModelScope.launch {
            try {
                repository.addUser(
                    UserEntity(
                        companyId = compId,
                        username = username.trim(),
                        passwordHash = password.trim(),
                        role = role,
                        canCreateSalesInvoice = (role == "Business Owner"),
                        canCreatePurchaseInvoice = (role == "Business Owner"),
                        canCreateVoucher = (role == "Business Owner"),
                        canManageItems = (role == "Business Owner"),
                        canManageParties = (role == "Business Owner"),
                        canViewReports = (role == "Business Owner")
                    )
                )
            } catch (e: Exception) {
                // handle duplicate username error
            }
        }
    }
    
    fun updateUserPermission(user: UserEntity, permission: String, value: Boolean) {
        viewModelScope.launch {
            val updated = when (permission) {
                "sales" -> user.copy(canCreateSalesInvoice = value)
                "purchase" -> user.copy(canCreatePurchaseInvoice = value)
                "voucher" -> user.copy(canCreateVoucher = value)
                "items" -> user.copy(canManageItems = value)
                "parties" -> user.copy(canManageParties = value)
                "reports" -> user.copy(canViewReports = value)
                else -> user
            }
            repository.updateUser(updated)
        }
    }
}

data class PartyLedgerEntry(
    val date: Long,
    val refNumber: String,
    val entryType: String,
    val particulars: String,
    val paymentMode: String,
    val debit: Double,
    val credit: Double,
    val runningBalance: Double = 0.0,
    val id: String = ""
)

data class PartyLedgerResult(
    val entries: List<PartyLedgerEntry>,
    val totalDebit: Double,
    val totalCredit: Double,
    val netBalance: Double,
    val partyType: PartyType
)

data class TransactionItemData(
    val itemId: String,
    val quantity: Double,
    val rate: Double
)

class AppViewModelFactory(
    private val repository: AppRepository,
    private val userPrefs: UserPreferences,
    private val driveManager: GoogleDriveManager? = null,
    private val firebaseSyncManager: FirebaseSyncManager? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppViewModel(repository, userPrefs, driveManager, firebaseSyncManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
