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
