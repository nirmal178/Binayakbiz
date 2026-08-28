package com.example.data.repository

import com.example.data.local.dao.AppDao
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

class AppRepository(private val dao: AppDao) {
    suspend fun createCompany(company: CompanyEntity) {
        dao.insertCompany(company)
    }
    suspend fun getCompanyByPan(pan: String) = dao.getCompanyByPan(pan)
    suspend fun getCompany(id: String) = dao.getCompanyById(id)
    fun getAllCompanies() = dao.getAllCompanies()
    fun getCompanyFlow(id: String) = dao.getCompanyFlow(id)

    // Branches
    suspend fun insertBranch(branch: BranchEntity) = dao.insertBranch(branch)
    fun getBranches(companyId: String) = dao.getBranches(companyId)
    suspend fun getBranchById(id: String) = dao.getBranchById(id)
    suspend fun getHeadOffice(companyId: String) = dao.getHeadOffice(companyId)

    suspend fun addUser(user: UserEntity) {
        dao.insertUser(user)
    }
    suspend fun updateUser(user: UserEntity) = dao.updateUser(user)
    suspend fun getUserByUsername(companyId: String, username: String) = dao.getUserByUsername(companyId, username)
    fun getUsersByCompany(companyId: String) = dao.getUsersByCompany(companyId)
    suspend fun getFirstUserByCompany(companyId: String) = dao.getFirstUserByCompany(companyId)
    fun getUserById(id: String) = dao.getUserById(id)

    suspend fun addParty(party: PartyEntity) = dao.insertParty(party)
    suspend fun updateParty(party: PartyEntity) = dao.updateParty(party)
    suspend fun deleteParty(partyId: String) = dao.deleteParty(partyId)
    fun getCustomers(companyId: String) = dao.getPartiesByType(companyId, PartyType.CUSTOMER)
    fun getSuppliers(companyId: String) = dao.getPartiesByType(companyId, PartyType.SUPPLIER)
    suspend fun getPartyById(partyId: String) = dao.getPartyById(partyId)

    suspend fun addItem(item: ItemEntity) = dao.insertItem(item)
    suspend fun updateItem(item: ItemEntity) = dao.updateItem(item)
    suspend fun deleteItem(itemId: String) = dao.deleteItem(itemId)
    fun getItems(companyId: String) = dao.getItems(companyId)

    suspend fun saveTransaction(tx: TransactionEntity, items: List<TransactionItemEntity>, isPurchase: Boolean) {
        dao.saveFullTransaction(tx, items, isPurchase)
    }
    fun getSales(companyId: String) = dao.getTransactionsByType(companyId, TransactionType.SALE)
    fun getPurchases(companyId: String) = dao.getTransactionsByType(companyId, TransactionType.PURCHASE)
    suspend fun getTransactionItems(txId: String) = dao.getTransactionItems(txId)
    suspend fun getTransactionById(txId: String) = dao.getTransactionById(txId)
    suspend fun deleteTransaction(txId: String) = dao.deleteTransactionWithStockRevert(txId)

    suspend fun addVoucher(voucher: VoucherEntity) = dao.insertVoucher(voucher)
    suspend fun deleteVoucher(voucherId: String) = dao.deleteVoucher(voucherId)
    fun getReceipts(companyId: String) = dao.getVouchersByType(companyId, VoucherType.RECEIPT)
    fun getPayments(companyId: String) = dao.getVouchersByType(companyId, VoucherType.PAYMENT)
    fun getAllVouchers(companyId: String) = dao.getAllVouchers(companyId)
    fun getVouchersForParty(partyId: String) = dao.getVouchersForParty(partyId)
    fun getTransactionsForParty(partyId: String) = dao.getTransactionsForParty(partyId)
}
