package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.local.dao.AppDao
import com.example.data.local.entity.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class FirebaseSyncManager(
    private val context: Context,
    private val dao: AppDao
) {
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    /**
     * Ensures an active Firebase session (e.g., anonymous auth if not signed in)
     */
    suspend fun ensureAuth(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                Result.success(currentUser.uid)
            } else {
                val authResult = auth.signInAnonymously().await()
                val uid = authResult.user?.uid ?: "anonymous"
                Result.success(uid)
            }
        } catch (e: Exception) {
            Log.w("FirebaseSyncManager", "Anonymous auth failed or not enabled (proceeding with direct Firestore): ${e.message}")
            Result.success("direct_access")
        }
    }

    /**
     * Push all local Room DB data to Firestore in the cloud.
     */
    suspend fun pushAllToFirestore(companyId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            ensureAuth()
            val companyRef = firestore.collection("companies").document(companyId)

            // 1. Sync Company Info
            val company = dao.getCompanyFlow(companyId).first()
            if (company != null) {
                val compMap = hashMapOf(
                    "id" to company.id,
                    "panVatNumber" to company.panVatNumber,
                    "businessName" to company.businessName,
                    "businessType" to company.businessType,
                    "ownerName" to company.ownerName,
                    "phoneNumber" to company.phoneNumber,
                    "province" to company.province,
                    "district" to company.district,
                    "currency" to company.currency,
                    "fiscalYear" to company.fiscalYear,
                    "email" to company.email,
                    "address" to company.address,
                    "lastSynced" to System.currentTimeMillis()
                )
                companyRef.set(compMap, SetOptions.merge()).await()
            }

            // 2. Sync Parties
            val customers = dao.getPartiesByType(companyId, PartyType.CUSTOMER).first()
            val suppliers = dao.getPartiesByType(companyId, PartyType.SUPPLIER).first()
            val allParties = customers + suppliers

            val partiesBatch = firestore.batch()
            for (party in allParties) {
                val partyDoc = companyRef.collection("parties").document(party.id)
                val partyMap = hashMapOf(
                    "id" to party.id,
                    "companyId" to party.companyId,
                    "name" to party.name,
                    "email" to party.email,
                    "type" to party.type.name,
                    "pan" to party.pan,
                    "contactPhone" to party.contactPhone,
                    "address" to party.address
                )
                partiesBatch.set(partyDoc, partyMap, SetOptions.merge())
            }
            if (allParties.isNotEmpty()) {
                partiesBatch.commit().await()
            }

            // 3. Sync Inventory Items
            val items = dao.getItems(companyId).first()
            val itemsBatch = firestore.batch()
            for (item in items) {
                val itemDoc = companyRef.collection("items").document(item.id)
                val itemMap = hashMapOf(
                    "id" to item.id,
                    "companyId" to item.companyId,
                    "itemName" to item.itemName,
                    "hsCode" to item.hsCode,
                    "uom" to item.uom,
                    "purchasePrice" to item.purchasePrice,
                    "salesPrice" to item.salesPrice,
                    "stockQuantity" to item.stockQuantity
                )
                itemsBatch.set(itemDoc, itemMap, SetOptions.merge())
            }
            if (items.isNotEmpty()) {
                itemsBatch.commit().await()
            }

            // 4. Sync Transactions & Line Items
            val sales = dao.getTransactionsByType(companyId, TransactionType.SALE).first()
            val purchases = dao.getTransactionsByType(companyId, TransactionType.PURCHASE).first()
            val allTx = sales + purchases

            for (tx in allTx) {
                val txDoc = companyRef.collection("transactions").document(tx.id)
                val txItems = dao.getTransactionItems(tx.id)
                val txItemsList = txItems.map {
                    hashMapOf(
                        "id" to it.id,
                        "transactionId" to it.transactionId,
                        "itemId" to it.itemId,
                        "companyId" to it.companyId,
                        "quantity" to it.quantity,
                        "rate" to it.rate,
                        "amount" to it.amount
                    )
                }
                val txMap = hashMapOf(
                    "id" to tx.id,
                    "companyId" to tx.companyId,
                    "type" to tx.type.name,
                    "partyId" to tx.partyId,
                    "invoiceNumber" to tx.invoiceNumber,
                    "transactionDate" to tx.transactionDate,
                    "subtotal" to tx.subtotal,
                    "discount" to tx.discount,
                    "vatPercent" to tx.vatPercent,
                    "taxAmount" to tx.taxAmount,
                    "grandTotal" to tx.grandTotal,
                    "paymentMode" to tx.paymentMode,
                    "items" to txItemsList
                )
                txDoc.set(txMap, SetOptions.merge()).await()
            }

            // 5. Sync Vouchers
            val allVouchers = dao.getAllVouchers(companyId).first()
            val vouchersBatch = firestore.batch()
            for (voucher in allVouchers) {
                val vDoc = companyRef.collection("vouchers").document(voucher.id)
                val vMap = hashMapOf(
                    "id" to voucher.id,
                    "companyId" to voucher.companyId,
                    "voucherType" to voucher.voucherType.name,
                    "partyId" to voucher.partyId,
                    "voucherNumber" to voucher.voucherNumber,
                    "voucherDate" to voucher.voucherDate,
                    "amount" to voucher.amount,
                    "paymentMode" to voucher.paymentMode,
                    "remarks" to voucher.remarks
                )
                vouchersBatch.set(vDoc, vMap, SetOptions.merge())
            }
            if (allVouchers.isNotEmpty()) {
                vouchersBatch.commit().await()
            }

            val summary = "Successfully synced to Firestore: ${allParties.size} Parties, ${items.size} Items, ${allTx.size} Invoices, ${allVouchers.size} Vouchers"
            Log.d("FirebaseSyncManager", summary)
            Result.success(summary)
        } catch (e: Exception) {
            Log.e("FirebaseSyncManager", "Failed to sync to Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Pull all data from Firestore down into local Room DB.
     */
    suspend fun pullAllFromFirestore(companyId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            ensureAuth()
            val companyRef = firestore.collection("companies").document(companyId)

            // 1. Pull Company
            val compSnap = companyRef.get().await()
            if (compSnap.exists()) {
                val pan = compSnap.getString("panVatNumber") ?: ""
                val name = compSnap.getString("businessName") ?: ""
                val email = compSnap.getString("email")
                val addr = compSnap.getString("address")
                val businessType = compSnap.getString("businessType") ?: "Retail"
                val ownerName = compSnap.getString("ownerName") ?: ""
                val phoneNumber = compSnap.getString("phoneNumber") ?: ""
                val province = compSnap.getString("province") ?: ""
                val district = compSnap.getString("district") ?: ""
                val currency = compSnap.getString("currency") ?: "NPR"
                val fiscalYear = compSnap.getString("fiscalYear") ?: "2080/81"
                val existing = dao.getCompanyByPan(pan)
                if (existing == null && pan.isNotBlank()) {
                    dao.insertCompany(
                        CompanyEntity(
                            id = companyId,
                            panVatNumber = pan,
                            businessName = name,
                            businessType = businessType,
                            ownerName = ownerName,
                            phoneNumber = phoneNumber,
                            province = province,
                            district = district,
                            currency = currency,
                            fiscalYear = fiscalYear,
                            email = email,
                            address = addr,
                        )
                    )
                }
            }

            // 2. Pull Parties
            val partiesSnap = companyRef.collection("parties").get().await()
            var partyCount = 0
            for (doc in partiesSnap.documents) {
                val id = doc.getString("id") ?: doc.id
                val name = doc.getString("name") ?: continue
                val typeStr = doc.getString("type") ?: "CUSTOMER"
                val type = if (typeStr == "SUPPLIER") PartyType.SUPPLIER else PartyType.CUSTOMER
                val pan = doc.getString("pan")
                val email = doc.getString("email")
                val phone = doc.getString("contactPhone")
                val addr = doc.getString("address")

                val entity = PartyEntity(
                    id = id,
                    companyId = companyId,
                    name = name,
                    email = email,
                    type = type,
                    pan = pan,
                    contactPhone = phone,
                    address = addr
                )
                val existing = dao.getPartyById(id)
                if (existing == null) {
                    dao.insertParty(entity)
                } else {
                    dao.updateParty(entity)
                }
                partyCount++
            }

            // 3. Pull Items
            val itemsSnap = companyRef.collection("items").get().await()
            var itemCount = 0
            for (doc in itemsSnap.documents) {
                val id = doc.getString("id") ?: doc.id
                val itemName = doc.getString("itemName") ?: continue
                val hsCode = doc.getString("hsCode") ?: ""
                val uom = doc.getString("uom") ?: "Pcs"
                val purchasePrice = doc.getDouble("purchasePrice") ?: 0.0
                val salesPrice = doc.getDouble("salesPrice") ?: 0.0
                val stockQuantity = doc.getDouble("stockQuantity") ?: 0.0
                val barcode = doc.getString("barcode") ?: ""
                val category = doc.getString("category") ?: ""
                val brand = doc.getString("brand") ?: ""
                val wholesalePrice = doc.getDouble("wholesalePrice") ?: 0.0
                val taxRate = doc.getDouble("taxRate") ?: 0.0
                val imageUri = doc.getString("imageUri")
                val expiryDate = doc.getLong("expiryDate")
                val batchNumber = doc.getString("batchNumber") ?: ""
                val status = doc.getString("status") ?: "ACTIVE"

                val entity = ItemEntity(
                    id = id,
                    companyId = companyId,
                    itemName = itemName,
                    hsCode = hsCode,
                    uom = uom,
                    purchasePrice = purchasePrice,
                    salesPrice = salesPrice,
                    stockQuantity = stockQuantity,
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
                val existing = dao.getItemById(id)
                if (existing == null) {
                    dao.insertItem(entity)
                } else {
                    dao.updateItem(entity)
                }
                itemCount++
            }

            // 4. Pull Transactions
            val txSnap = companyRef.collection("transactions").get().await()
            var txCount = 0
            for (doc in txSnap.documents) {
                val id = doc.getString("id") ?: doc.id
                val typeStr = doc.getString("type") ?: "SALE"
                val txType = if (typeStr == "PURCHASE") TransactionType.PURCHASE else TransactionType.SALE
                val partyId = doc.getString("partyId") ?: ""
                val invoiceNumber = doc.getString("invoiceNumber") ?: ""
                val transactionDate = doc.getLong("transactionDate") ?: System.currentTimeMillis()
                val subtotal = doc.getDouble("subtotal") ?: 0.0
                val discount = doc.getDouble("discount") ?: 0.0
                val vatPercent = doc.getDouble("vatPercent") ?: 13.0
                val taxAmount = doc.getDouble("taxAmount") ?: 0.0
                val grandTotal = doc.getDouble("grandTotal") ?: 0.0
                val paymentMode = doc.getString("paymentMode") ?: "CASH"

                val txEntity = TransactionEntity(
                    id = id,
                    companyId = companyId,
                    type = txType,
                    partyId = partyId,
                    invoiceNumber = invoiceNumber,
                    transactionDate = transactionDate,
                    subtotal = subtotal,
                    discount = discount,
                    vatPercent = vatPercent,
                    taxAmount = taxAmount,
                    grandTotal = grandTotal,
                    paymentMode = paymentMode
                )

                @Suppress("UNCHECKED_CAST")
                val itemsListRaw = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
                val itemEntities = itemsListRaw.map { raw ->
                    TransactionItemEntity(
                        id = raw["id"] as? String ?: UUID.randomUUID().toString(),
                        transactionId = id,
                        itemId = raw["itemId"] as? String ?: "",
                        companyId = companyId,
                        quantity = (raw["quantity"] as? Number)?.toDouble() ?: 1.0,
                        rate = (raw["rate"] as? Number)?.toDouble() ?: 0.0,
                        amount = (raw["amount"] as? Number)?.toDouble() ?: 0.0
                    )
                }

                val existing = dao.getTransactionById(id)
                if (existing == null) {
                    dao.insertTransaction(txEntity)
                    dao.insertTransactionItems(itemEntities)
                }
                txCount++
            }

            // 5. Pull Vouchers
            val vouchersSnap = companyRef.collection("vouchers").get().await()
            var vCount = 0
            for (doc in vouchersSnap.documents) {
                val id = doc.getString("id") ?: doc.id
                val typeStr = doc.getString("voucherType") ?: "RECEIPT"
                val vType = if (typeStr == "PAYMENT") VoucherType.PAYMENT else VoucherType.RECEIPT
                val partyId = doc.getString("partyId") ?: ""
                val vNum = doc.getString("voucherNumber") ?: ""
                val vDate = doc.getLong("voucherDate") ?: System.currentTimeMillis()
                val amount = doc.getDouble("amount") ?: 0.0
                val pMode = doc.getString("paymentMode") ?: "CASH"
                val remarks = doc.getString("remarks")

                val vEntity = VoucherEntity(
                    id = id,
                    companyId = companyId,
                    voucherType = vType,
                    partyId = partyId,
                    voucherNumber = vNum,
                    voucherDate = vDate,
                    amount = amount,
                    paymentMode = pMode,
                    remarks = remarks
                )
                try {
                    dao.insertVoucher(vEntity)
                } catch (e: Exception) {
                    dao.updateVoucher(vEntity)
                }
                vCount++
            }

            val summary = "Retrieved from Firestore: $partyCount Parties, $itemCount Items, $txCount Invoices, $vCount Vouchers"
            Result.success(summary)
        } catch (e: Exception) {
            Log.e("FirebaseSyncManager", "Failed to pull from Firestore", e)
            Result.failure(e)
        }
    }
}
