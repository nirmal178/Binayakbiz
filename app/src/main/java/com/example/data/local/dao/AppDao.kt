package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.CompanyEntity
import com.example.data.local.entity.BranchEntity
import com.example.data.local.entity.ItemEntity
import com.example.data.local.entity.PartyEntity
import com.example.data.local.entity.PartyType
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.local.entity.TransactionType
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Company
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCompany(company: CompanyEntity)

    @Query("SELECT * FROM companies WHERE panVatNumber = :pan LIMIT 1")
    suspend fun getCompanyByPan(pan: String): CompanyEntity?

    @Query("SELECT * FROM companies WHERE id = :id LIMIT 1")
    fun getCompanyFlow(id: String): Flow<CompanyEntity?>

    @Query("SELECT * FROM companies WHERE id = :id LIMIT 1")
    suspend fun getCompanyById(id: String): CompanyEntity?

    @Query("SELECT * FROM companies")
    fun getAllCompanies(): Flow<List<CompanyEntity>>
 
    // Branches
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranch(branch: BranchEntity)
    @Query("SELECT * FROM branches WHERE companyId = :companyId")
    fun getBranches(companyId: String): Flow<List<BranchEntity>>
    @Query("SELECT * FROM branches WHERE id = :id LIMIT 1")
    suspend fun getBranchById(id: String): BranchEntity?
    @Query("SELECT * FROM branches WHERE companyId = :companyId AND isHeadOffice = 1 LIMIT 1")
    suspend fun getHeadOffice(companyId: String): BranchEntity?
    
    // User
    @Insert
    suspend fun insertUser(user: UserEntity)
    
    @Update
    suspend fun updateUser(user: UserEntity)
    
    @Query("SELECT * FROM users WHERE companyId = :companyId AND username = :username LIMIT 1")
    suspend fun getUserByUsername(companyId: String, username: String): UserEntity?
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserById(id: String): Flow<UserEntity?>
    
    @Query("SELECT * FROM users WHERE companyId = :companyId ORDER BY role ASC, username ASC")
    fun getUsersByCompany(companyId: String): Flow<List<UserEntity>>
    
    @Query("SELECT * FROM users WHERE companyId = :companyId ORDER BY role ASC, username ASC LIMIT 1")
    suspend fun getFirstUserByCompany(companyId: String): UserEntity?

    // Party
    @Insert
    suspend fun insertParty(party: PartyEntity)

    @Update
    suspend fun updateParty(party: PartyEntity)

    @Query("DELETE FROM parties WHERE id = :partyId")
    suspend fun deleteParty(partyId: String)

    @Query("SELECT * FROM parties WHERE companyId = :companyId AND type = :type ORDER BY name ASC")
    fun getPartiesByType(companyId: String, type: PartyType): Flow<List<PartyEntity>>

    @Query("SELECT * FROM parties WHERE id = :partyId LIMIT 1")
    suspend fun getPartyById(partyId: String): PartyEntity?

    // Item
    @Insert
    suspend fun insertItem(item: ItemEntity)

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Query("DELETE FROM items WHERE id = :itemId")
    suspend fun deleteItem(itemId: String)

    @Query("SELECT * FROM items WHERE companyId = :companyId ORDER BY itemName ASC")
    fun getItems(companyId: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :itemId LIMIT 1")
    suspend fun getItemById(itemId: String): ItemEntity?

    // Transactions
    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert
    suspend fun insertTransactionItems(items: List<TransactionItemEntity>)

    @Transaction
    suspend fun saveFullTransaction(
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>,
        isPurchase: Boolean
    ) {
        insertTransaction(transaction)
        insertTransactionItems(items)
        // Update stock
        for (item in items) {
            val dbItem = getItemById(item.itemId)
            if (dbItem != null) {
                val newStock = if (isPurchase) {
                    dbItem.stockQuantity + item.quantity
                } else {
                    dbItem.stockQuantity - item.quantity
                }
                updateItem(dbItem.copy(stockQuantity = newStock))
            }
        }
    }

    @Query("SELECT * FROM transactions WHERE companyId = :companyId AND type = :type ORDER BY transactionDate DESC")
    fun getTransactionsByType(companyId: String, type: TransactionType): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun getTransactionItems(transactionId: String): List<TransactionItemEntity>
    
    @Query("SELECT * FROM transactions WHERE id = :transactionId LIMIT 1")
    suspend fun getTransactionById(transactionId: String): TransactionEntity?

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransaction(transactionId: String)
    
    @Transaction
    suspend fun deleteTransactionWithStockRevert(transactionId: String) {
        val transaction = getTransactionById(transactionId) ?: return
        val items = getTransactionItems(transactionId)
        val isPurchase = transaction.type == TransactionType.PURCHASE
        for (item in items) {
            val dbItem = getItemById(item.itemId)
            if (dbItem != null) {
                val newStock = if (isPurchase) {
                    dbItem.stockQuantity - item.quantity // revert purchase
                } else {
                    dbItem.stockQuantity + item.quantity // revert sale
                }
                updateItem(dbItem.copy(stockQuantity = newStock))
            }
        }
        deleteTransaction(transactionId)
    }

    // Vouchers (Receipts & Payments)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVoucher(voucher: com.example.data.local.entity.VoucherEntity)

    @Update
    suspend fun updateVoucher(voucher: com.example.data.local.entity.VoucherEntity)

    @Query("DELETE FROM vouchers WHERE id = :voucherId")
    suspend fun deleteVoucher(voucherId: String)

    @Query("SELECT * FROM vouchers WHERE companyId = :companyId AND voucherType = :type ORDER BY voucherDate DESC")
    fun getVouchersByType(companyId: String, type: com.example.data.local.entity.VoucherType): Flow<List<com.example.data.local.entity.VoucherEntity>>

    @Query("SELECT * FROM vouchers WHERE companyId = :companyId ORDER BY voucherDate DESC")
    fun getAllVouchers(companyId: String): Flow<List<com.example.data.local.entity.VoucherEntity>>

    @Query("SELECT * FROM vouchers WHERE partyId = :partyId ORDER BY voucherDate ASC")
    fun getVouchersForParty(partyId: String): Flow<List<com.example.data.local.entity.VoucherEntity>>

    @Query("SELECT * FROM transactions WHERE partyId = :partyId ORDER BY transactionDate ASC")
    fun getTransactionsForParty(partyId: String): Flow<List<TransactionEntity>>
}
