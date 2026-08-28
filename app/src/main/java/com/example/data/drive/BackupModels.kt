package com.example.data.drive

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CompanyBackupDto(
    val id: String,
    val panVatNumber: String,
    val businessName: String,
    val businessType: String = "Retail",
    val ownerName: String = "",
    val phoneNumber: String = "",
    val address: String?,
    val province: String = "",
    val district: String = "",
    val currency: String = "NPR",
    val fiscalYear: String = "2080/81"
)

@JsonClass(generateAdapter = true)
data class PartyBackupDto(
    val id: String,
    val companyId: String,
    val name: String,
    val type: String, // CUSTOMER, SUPPLIER
    val pan: String?,
    val contactPhone: String?,
    val address: String?
)

@JsonClass(generateAdapter = true)
data class ItemBackupDto(
    val id: String,
    val companyId: String,
    val itemName: String,
    val hsCode: String,
    val uom: String,
    val purchasePrice: Double,
    val salesPrice: Double,
    val stockQuantity: Double,
    val barcode: String = "",
    val category: String = "",
    val brand: String = "",
    val wholesalePrice: Double = 0.0,
    val taxRate: Double = 0.0,
    val imageUri: String? = null,
    val expiryDate: Long? = null,
    val batchNumber: String = "",
    val status: String = "ACTIVE"
)

@JsonClass(generateAdapter = true)
data class TransactionItemBackupDto(
    val id: String,
    val transactionId: String,
    val itemId: String,
    val companyId: String,
    val quantity: Double,
    val rate: Double,
    val amount: Double
)

@JsonClass(generateAdapter = true)
data class TransactionBackupDto(
    val id: String,
    val companyId: String,
    val type: String, // SALE, PURCHASE
    val partyId: String,
    val invoiceNumber: String,
    val transactionDate: Long,
    val subtotal: Double,
    val discount: Double,
    val vatPercent: Double = 13.0,
    val taxAmount: Double,
    val grandTotal: Double,
    val paymentMode: String,
    val items: List<TransactionItemBackupDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class VoucherBackupDto(
    val id: String,
    val companyId: String,
    val voucherType: String, // RECEIPT, PAYMENT
    val partyId: String,
    val voucherNumber: String,
    val voucherDate: Long,
    val amount: Double,
    val paymentMode: String,
    val remarks: String?
)

@JsonClass(generateAdapter = true)
data class FullBackupData(
    val version: Int = 1,
    val backupTimestamp: Long = System.currentTimeMillis(),
    val appName: String = "Billing & Inventory Pro",
    val company: CompanyBackupDto?,
    val parties: List<PartyBackupDto> = emptyList(),
    val items: List<ItemBackupDto> = emptyList(),
    val transactions: List<TransactionBackupDto> = emptyList(),
    val vouchers: List<VoucherBackupDto> = emptyList()
)
