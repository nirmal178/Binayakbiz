package com.example.data.drive

import android.content.Context
import android.util.Log
import com.example.data.local.dao.AppDao
import com.example.data.local.entity.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class DriveFileItem(
    val id: String,
    val name: String,
    val createdTime: String?,
    val size: Long? = 0L
)

class GoogleDriveManager(
    private val context: Context,
    private val dao: AppDao
) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val jsonAdapter = moshi.adapter(FullBackupData::class.java).indent("  ")

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Serializes all local Room DB data for the given company into a JSON string.
     */
    suspend fun createBackupPayload(companyId: String): FullBackupData = withContext(Dispatchers.IO) {
        val company = dao.getCompanyFlow(companyId).first()
        val customers = dao.getPartiesByType(companyId, PartyType.CUSTOMER).first()
        val suppliers = dao.getPartiesByType(companyId, PartyType.SUPPLIER).first()
        val allParties = customers + suppliers
        val items = dao.getItems(companyId).first()
        
        val sales = dao.getTransactionsByType(companyId, TransactionType.SALE).first()
        val purchases = dao.getTransactionsByType(companyId, TransactionType.PURCHASE).first()
        val allTransactions = sales + purchases

        val txDtos = allTransactions.map { tx ->
            val txItems = dao.getTransactionItems(tx.id)
            TransactionBackupDto(
                id = tx.id,
                companyId = tx.companyId,
                type = tx.type.name,
                partyId = tx.partyId,
                invoiceNumber = tx.invoiceNumber,
                transactionDate = tx.transactionDate,
                subtotal = tx.subtotal,
                discount = tx.discount,
                vatPercent = tx.vatPercent,
                taxAmount = tx.taxAmount,
                grandTotal = tx.grandTotal,
                paymentMode = tx.paymentMode,
                items = txItems.map { item ->
                    TransactionItemBackupDto(
                        id = item.id,
                        transactionId = item.transactionId,
                        itemId = item.itemId,
                        companyId = item.companyId,
                        quantity = item.quantity,
                        rate = item.rate,
                        amount = item.amount
                    )
                }
            )
        }

        val allVouchers = dao.getAllVouchers(companyId).first()

        FullBackupData(
            version = 1,
            backupTimestamp = System.currentTimeMillis(),
            company = company?.let {
                CompanyBackupDto(
                    id = it.id,
                    panVatNumber = it.panVatNumber,
                    businessName = it.businessName,
                    address = it.address,
                    businessType = it.businessType,
                    ownerName = it.ownerName,
                    phoneNumber = it.phoneNumber,
                    province = it.province,
                    district = it.district,
                    currency = it.currency,
                    fiscalYear = it.fiscalYear
                )
            },
            parties = allParties.map {
                PartyBackupDto(
                    id = it.id,
                    companyId = it.companyId,
                    name = it.name,
                    type = it.type.name,
                    pan = it.pan,
                    contactPhone = it.contactPhone,
                    address = it.address
                )
            },
            items = items.map {
                ItemBackupDto(
                    id = it.id,
                    companyId = it.companyId,
                    itemName = it.itemName,
                    hsCode = it.hsCode,
                    uom = it.uom,
                    purchasePrice = it.purchasePrice,
                    salesPrice = it.salesPrice,
                    stockQuantity = it.stockQuantity,
                    barcode = it.barcode,
                    category = it.category,
                    brand = it.brand,
                    wholesalePrice = it.wholesalePrice,
                    taxRate = it.taxRate,
                    imageUri = it.imageUri,
                    expiryDate = it.expiryDate,
                    batchNumber = it.batchNumber,
                    status = it.status
                )
            },
            transactions = txDtos,
            vouchers = allVouchers.map {
                VoucherBackupDto(
                    id = it.id,
                    companyId = it.companyId,
                    voucherType = it.voucherType.name,
                    partyId = it.partyId,
                    voucherNumber = it.voucherNumber,
                    voucherDate = it.voucherDate,
                    amount = it.amount,
                    paymentMode = it.paymentMode,
                    remarks = it.remarks
                )
            }
        )
    }

    /**
     * Finds or creates the App Data folder on Google Drive.
     */
    suspend fun getOrCreateAppDataFolder(
        accessToken: String,
        folderName: String = "Billing & Inventory Pro AppData"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val query = "mimeType = 'application/vnd.google-apps.folder' and name = '$folderName' and trashed = false"
            val searchUrl = "https://www.googleapis.com/drive/v3/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=files(id,name)"
            
            val searchRequest = Request.Builder()
                .url(searchUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val searchResponse = httpClient.newCall(searchRequest).execute()
            val searchBody = searchResponse.body?.string() ?: ""

            if (searchResponse.isSuccessful) {
                val json = JSONObject(searchBody)
                val files = json.optJSONArray("files")
                if (files != null && files.length() > 0) {
                    val existingFolderId = files.getJSONObject(0).getString("id")
                    return@withContext Result.success(existingFolderId)
                }
            }

            // Create new folder
            val folderMetadata = JSONObject().apply {
                put("name", folderName)
                put("mimeType", "application/vnd.google-apps.folder")
                put("description", "Local data synchronization folder for Billing & Inventory Pro")
            }.toString()

            val createRequest = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files")
                .addHeader("Authorization", "Bearer $accessToken")
                .post(folderMetadata.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                .build()

            val createResponse = httpClient.newCall(createRequest).execute()
            val createBody = createResponse.body?.string() ?: ""

            if (!createResponse.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to create Drive folder (${createResponse.code}): $createBody"))
            }

            val createdJson = JSONObject(createBody)
            val newFolderId = createdJson.getString("id")
            Result.success(newFolderId)
        } catch (e: Exception) {
            Log.e("GoogleDriveManager", "Error in getOrCreateAppDataFolder", e)
            Result.failure(e)
        }
    }

    /**
     * Synchronizes all local data to the Google Drive App Data folder.
     */
    suspend fun syncDataToDrive(
        accessToken: String,
        companyId: String,
        companyName: String,
        folderId: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val backupData = createBackupPayload(companyId)
            val jsonString = jsonAdapter.toJson(backupData)

            val safeName = companyName.replace("[^a-zA-Z0-9]".toRegex(), "_").ifBlank { "Company" }
            val syncFileName = "BillingPro_DataSync_${safeName}.json"
            val timestampFileName = "BillingPro_Backup_${safeName}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"

            // Target folder
            val targetFolderId = folderId ?: getOrCreateAppDataFolder(accessToken).getOrNull()

            // 1. Check if sync file already exists
            var existingFileId: String? = null
            var searchParent = if (targetFolderId != null) " and '$targetFolderId' in parents" else ""
            val query = "name = '$syncFileName' and trashed = false$searchParent"
            val searchUrl = "https://www.googleapis.com/drive/v3/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=files(id,name)"
            
            val searchRequest = Request.Builder()
                .url(searchUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val searchResponse = httpClient.newCall(searchRequest).execute()
            if (searchResponse.isSuccessful) {
                val json = JSONObject(searchResponse.body?.string() ?: "")
                val files = json.optJSONArray("files")
                if (files != null && files.length() > 0) {
                    existingFileId = files.getJSONObject(0).getString("id")
                }
            }

            if (existingFileId != null) {
                // Update existing sync file
                val patchRequest = Request.Builder()
                    .url("https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=media")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .patch(jsonString.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                    .build()
                val patchResponse = httpClient.newCall(patchRequest).execute()
                if (!patchResponse.isSuccessful) {
                    Log.w("GoogleDriveManager", "Patch update failed: ${patchResponse.code}")
                }
            } else {
                // Create initial sync file
                val metadataJson = JSONObject().apply {
                    put("name", syncFileName)
                    put("description", "Latest active data snapshot")
                    put("mimeType", "application/json")
                    if (targetFolderId != null) {
                        put("parents", JSONArray().put(targetFolderId))
                    }
                }.toString()

                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("metadata", null, metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                    .addFormDataPart("file", syncFileName, jsonString.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                    .build()

                val createReq = Request.Builder()
                    .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .post(multipartBody)
                    .build()
                httpClient.newCall(createReq).execute()
            }

            // 2. Also save timestamped snapshot inside the folder
            val snapshotMeta = JSONObject().apply {
                put("name", timestampFileName)
                put("description", "Billing & Inventory Pro Cloud Snapshot")
                put("mimeType", "application/json")
                if (targetFolderId != null) {
                    put("parents", JSONArray().put(targetFolderId))
                }
            }.toString()

            val snapshotBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("metadata", null, snapshotMeta.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                .addFormDataPart("file", timestampFileName, jsonString.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                .build()

            val snapReq = Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                .addHeader("Authorization", "Bearer $accessToken")
                .post(snapshotBody)
                .build()
            val snapResp = httpClient.newCall(snapReq).execute()

            Result.success("Auto-synced with Google Drive successfully! ($syncFileName)")
        } catch (e: Exception) {
            Log.e("GoogleDriveManager", "Error in syncDataToDrive", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads the backup JSON payload to Google Drive using multipart upload.
     */
    suspend fun uploadBackupToDrive(
        accessToken: String,
        companyId: String,
        companyName: String,
        folderId: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val backupData = createBackupPayload(companyId)
            val jsonString = jsonAdapter.toJson(backupData)

            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val safeName = companyName.replace("[^a-zA-Z0-9]".toRegex(), "_")
            val fileName = "BillingPro_Backup_${safeName}_$dateStr.json"

            val targetFolderId = folderId ?: getOrCreateAppDataFolder(accessToken).getOrNull()

            // Metadata JSON
            val metadataJson = JSONObject().apply {
                put("name", fileName)
                put("description", "Billing & Inventory Pro Cloud Backup")
                put("mimeType", "application/json")
                if (targetFolderId != null) {
                    put("parents", JSONArray().put(targetFolderId))
                }
            }.toString()

            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "metadata",
                    null,
                    metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType())
                )
                .addFormDataPart(
                    "file",
                    fileName,
                    jsonString.toRequestBody("application/json; charset=UTF-8".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                .addHeader("Authorization", "Bearer $accessToken")
                .post(multipartBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("GoogleDriveManager", "Upload failed: code=${response.code}, body=$responseBody")
                return@withContext Result.failure(Exception("Google Drive upload failed (${response.code}): $responseBody"))
            }

            val respJson = JSONObject(responseBody)
            val fileId = respJson.optString("id", "")
            Result.success("Backup uploaded successfully to Drive! ($fileName)")
        } catch (e: Exception) {
            Log.e("GoogleDriveManager", "Error uploading to drive", e)
            Result.failure(e)
        }
    }

    /**
     * Lists backup files found on Google Drive.
     */
    suspend fun listBackupsFromDrive(accessToken: String): Result<List<DriveFileItem>> = withContext(Dispatchers.IO) {
        try {
            val query = "trashed = false and mimeType = 'application/json' and name contains 'BillingPro_Backup_'"
            val url = "https://www.googleapis.com/drive/v3/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}&orderBy=createdTime desc&fields=files(id,name,createdTime,size)"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch backups from Drive: ${response.code}"))
            }

            val root = JSONObject(responseBody)
            val filesArray = root.optJSONArray("files") ?: JSONArray()
            val resultList = mutableListOf<DriveFileItem>()

            for (i in 0 until filesArray.length()) {
                val obj = filesArray.getJSONObject(i)
                resultList.add(
                    DriveFileItem(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        createdTime = obj.optString("createdTime", ""),
                        size = obj.optLong("size", 0L)
                    )
                )
            }

            Result.success(resultList)
        } catch (e: Exception) {
            Log.e("GoogleDriveManager", "Error listing backups", e)
            Result.failure(e)
        }
    }

    /**
     * Downloads and restores a backup file from Google Drive into Room SQLite.
     */
    suspend fun restoreBackupFromDrive(
        accessToken: String,
        fileId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to download file from Drive (${response.code})"))
            }

            val jsonString = response.body?.string() ?: return@withContext Result.failure(Exception("Empty backup file received"))
            val backupData = jsonAdapter.fromJson(jsonString) ?: return@withContext Result.failure(Exception("Invalid backup JSON format"))

            restoreDataToDatabase(backupData)
        } catch (e: Exception) {
            Log.e("GoogleDriveManager", "Error restoring backup", e)
            Result.failure(e)
        }
    }

    /**
     * Restores entities directly into Room DB.
     */
    suspend fun restoreDataToDatabase(backup: FullBackupData): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Restore Company if present
            backup.company?.let { comp ->
                val existing = dao.getCompanyByPan(comp.panVatNumber)
                if (existing == null) {
                    dao.insertCompany(
                        CompanyEntity(
                            id = comp.id,
                            panVatNumber = comp.panVatNumber,
                            businessName = comp.businessName,
                            address = comp.address,
                            businessType = comp.businessType,
                            ownerName = comp.ownerName,
                            phoneNumber = comp.phoneNumber,
                            province = comp.province,
                            district = comp.district,
                            currency = comp.currency,
                            fiscalYear = comp.fiscalYear
                        )
                    )
                }
            }

            // Restore Parties
            for (partyDto in backup.parties) {
                val existing = dao.getPartyById(partyDto.id)
                val type = if (partyDto.type == "SUPPLIER") PartyType.SUPPLIER else PartyType.CUSTOMER
                val entity = PartyEntity(
                    id = partyDto.id,
                    companyId = partyDto.companyId,
                    name = partyDto.name,
                    type = type,
                    pan = partyDto.pan,
                    contactPhone = partyDto.contactPhone,
                    address = partyDto.address
                )
                if (existing == null) {
                    dao.insertParty(entity)
                } else {
                    dao.updateParty(entity)
                }
            }

            // Restore Items
            for (itemDto in backup.items) {
                val existing = dao.getItemById(itemDto.id)
                val entity = ItemEntity(
                    id = itemDto.id,
                    companyId = itemDto.companyId,
                    itemName = itemDto.itemName,
                    hsCode = itemDto.hsCode,
                    uom = itemDto.uom,
                    purchasePrice = itemDto.purchasePrice,
                    salesPrice = itemDto.salesPrice,
                    stockQuantity = itemDto.stockQuantity,
                    barcode = itemDto.barcode,
                    category = itemDto.category,
                    brand = itemDto.brand,
                    wholesalePrice = itemDto.wholesalePrice,
                    taxRate = itemDto.taxRate,
                    imageUri = itemDto.imageUri,
                    expiryDate = itemDto.expiryDate,
                    batchNumber = itemDto.batchNumber,
                    status = itemDto.status
                )
                if (existing == null) {
                    dao.insertItem(entity)
                } else {
                    dao.updateItem(entity)
                }
            }

            // Restore Transactions
            for (txDto in backup.transactions) {
                val existing = dao.getTransactionById(txDto.id)
                val txType = if (txDto.type == "PURCHASE") TransactionType.PURCHASE else TransactionType.SALE
                val txEntity = TransactionEntity(
                    id = txDto.id,
                    companyId = txDto.companyId,
                    type = txType,
                    partyId = txDto.partyId,
                    invoiceNumber = txDto.invoiceNumber,
                    transactionDate = txDto.transactionDate,
                    subtotal = txDto.subtotal,
                    discount = txDto.discount,
                    vatPercent = txDto.vatPercent,
                    taxAmount = txDto.taxAmount,
                    grandTotal = txDto.grandTotal,
                    paymentMode = txDto.paymentMode
                )
                val itemEntities = txDto.items.map { itemDto ->
                    TransactionItemEntity(
                        id = itemDto.id,
                        transactionId = itemDto.transactionId,
                        itemId = itemDto.itemId,
                        companyId = itemDto.companyId,
                        quantity = itemDto.quantity,
                        rate = itemDto.rate,
                        amount = itemDto.amount
                    )
                }

                if (existing == null) {
                    dao.insertTransaction(txEntity)
                    dao.insertTransactionItems(itemEntities)
                }
            }

            // Restore Vouchers
            for (vDto in backup.vouchers) {
                val vType = if (vDto.voucherType == "PAYMENT") VoucherType.PAYMENT else VoucherType.RECEIPT
                val vEntity = VoucherEntity(
                    id = vDto.id,
                    companyId = vDto.companyId,
                    voucherType = vType,
                    partyId = vDto.partyId,
                    voucherNumber = vDto.voucherNumber,
                    voucherDate = vDto.voucherDate,
                    amount = vDto.amount,
                    paymentMode = vDto.paymentMode,
                    remarks = vDto.remarks
                )
                try {
                    dao.insertVoucher(vEntity)
                } catch (e: Exception) {
                    dao.updateVoucher(vEntity)
                }
            }

            val summary = "Restored: ${backup.parties.size} Parties, ${backup.items.size} Items, ${backup.transactions.size} Invoices, ${backup.vouchers.size} Vouchers"
            Result.success(summary)
        } catch (e: Exception) {
            Log.e("GoogleDriveManager", "Error in restoreDataToDatabase", e)
            Result.failure(e)
        }
    }

    /**
     * Exports a local JSON backup file to cache directory for sharing/saving.
     */
    suspend fun exportLocalBackupFile(companyId: String, companyName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val backupData = createBackupPayload(companyId)
            val jsonString = jsonAdapter.toJson(backupData)

            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val safeName = companyName.replace("[^a-zA-Z0-9]".toRegex(), "_")
            val fileName = "BillingPro_Backup_${safeName}_$dateStr.json"

            val file = File(context.cacheDir, fileName)
            file.writeText(jsonString)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
