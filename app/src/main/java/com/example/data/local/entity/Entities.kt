package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "companies",
    indices = [Index(value = ["panVatNumber"], unique = true)]
)
data class CompanyEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val businessName: String,
    val businessType: String = "Retail",
    val ownerName: String = "",
    val phoneNumber: String = "",
    val email: String? = null,
    val address: String?,
    val province: String = "",
    val district: String = "",
    val panVatNumber: String,
    val businessLogo: String? = null,
    val currency: String = "NPR",
    val fiscalYear: String = "2080/81",
    val barcode: String = "",
    val category: String = "",
    val brand: String = "",
    val wholesalePrice: Double = 0.0,
    val taxRate: Double = 0.0,
    val imageUri: String? = null,
    val expiryDate: Long? = null,
    val batchNumber: String = "",
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "branches",
    foreignKeys = [
        ForeignKey(
            entity = CompanyEntity::class,
            parentColumns = ["id"],
            childColumns = ["companyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["companyId"])]
)
data class BranchEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val companyId: String,
    val branchName: String,
    val address: String,
    val phoneNumber: String = "",
    val isHeadOffice: Boolean = false,
    val barcode: String = "",
    val category: String = "",
    val brand: String = "",
    val wholesalePrice: Double = 0.0,
    val taxRate: Double = 0.0,
    val imageUri: String? = null,
    val expiryDate: Long? = null,
    val batchNumber: String = "",
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "users",
    foreignKeys = [
        ForeignKey(
            entity = CompanyEntity::class,
            parentColumns = ["id"],
            childColumns = ["companyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["companyId", "username"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val companyId: String,
    val branchId: String? = null,
    val username: String, // Treat as email or phone based on input
    val passwordHash: String,
    val role: String = "Cashier", // Business Owner, Manager, Cashier, Inventory Staff, Accountant
    val canCreateSalesInvoice: Boolean = false,
    val canCreatePurchaseInvoice: Boolean = false,
    val canCreateVoucher: Boolean = false,
    val canManageItems: Boolean = false,
    val canManageParties: Boolean = false,
    val canViewReports: Boolean = false,
    val barcode: String = "",
    val category: String = "",
    val brand: String = "",
    val wholesalePrice: Double = 0.0,
    val taxRate: Double = 0.0,
    val imageUri: String? = null,
    val expiryDate: Long? = null,
    val batchNumber: String = "",
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis()
)

enum class PartyType { CUSTOMER, SUPPLIER }

@Entity(
    tableName = "parties",
    foreignKeys = [
        ForeignKey(
            entity = CompanyEntity::class,
            parentColumns = ["id"],
            childColumns = ["companyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["companyId"])]
)
data class PartyEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val companyId: String,
    val type: PartyType,
    val name: String,
    val email: String? = null,
    val pan: String?,
    val contactPhone: String?,
    val address: String?,
    val barcode: String = "",
    val category: String = "",
    val brand: String = "",
    val wholesalePrice: Double = 0.0,
    val taxRate: Double = 0.0,
    val imageUri: String? = null,
    val expiryDate: Long? = null,
    val batchNumber: String = "",
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = CompanyEntity::class,
            parentColumns = ["id"],
            childColumns = ["companyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["companyId"])]
)
data class ItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val companyId: String,
    val itemName: String,
    val hsCode: String,
    val uom: String,
    val purchasePrice: Double = 0.0,
    val salesPrice: Double = 0.0,
    val stockQuantity: Double = 0.0,
    val barcode: String = "",
    val category: String = "",
    val brand: String = "",
    val wholesalePrice: Double = 0.0,
    val taxRate: Double = 0.0,
    val imageUri: String? = null,
    val expiryDate: Long? = null,
    val batchNumber: String = "",
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis()
)

enum class TransactionType { PURCHASE, SALE }
enum class VoucherType { RECEIPT, PAYMENT }

@Entity(
    tableName = "vouchers",
    foreignKeys = [
        ForeignKey(
            entity = CompanyEntity::class,
            parentColumns = ["id"],
            childColumns = ["companyId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PartyEntity::class,
            parentColumns = ["id"],
            childColumns = ["partyId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["companyId", "voucherType", "voucherNumber"], unique = true),
        Index(value = ["partyId"])
    ]
)
data class VoucherEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val companyId: String,
    val partyId: String,
    val voucherType: VoucherType, // RECEIPT (from Customer), PAYMENT (to Supplier)
    val voucherNumber: String,
    val voucherDate: Long = System.currentTimeMillis(),
    val amount: Double = 0.0,
    val paymentMode: String = "CASH", // CASH, BANK / CHEQUE, DIGITAL (eSewa/FonePay)
    val remarks: String? = null,
    val barcode: String = "",
    val category: String = "",
    val brand: String = "",
    val wholesalePrice: Double = 0.0,
    val taxRate: Double = 0.0,
    val imageUri: String? = null,
    val expiryDate: Long? = null,
    val batchNumber: String = "",
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CompanyEntity::class,
            parentColumns = ["id"],
            childColumns = ["companyId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PartyEntity::class,
            parentColumns = ["id"],
            childColumns = ["partyId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["companyId", "type", "invoiceNumber"], unique = true),
        Index(value = ["partyId"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val companyId: String,
    val branchId: String? = null,
    val partyId: String,
    val type: TransactionType,
    val invoiceNumber: String,
    val transactionDate: Long = System.currentTimeMillis(),
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val vatPercent: Double = 13.0,
    val taxAmount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val paymentMode: String = "CASH",
    val barcode: String = "",
    val category: String = "",
    val brand: String = "",
    val wholesalePrice: Double = 0.0,
    val taxRate: Double = 0.0,
    val imageUri: String? = null,
    val expiryDate: Long? = null,
    val batchNumber: String = "",
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transaction_items",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CompanyEntity::class,
            parentColumns = ["id"],
            childColumns = ["companyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transactionId"]),
        Index(value = ["itemId"]),
        Index(value = ["companyId"])
    ]
)
data class TransactionItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val transactionId: String,
    val itemId: String,
    val companyId: String,
    val quantity: Double,
    val rate: Double,
    val amount: Double
)
