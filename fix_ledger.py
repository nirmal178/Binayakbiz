import re

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
    content = f.read()

# Find the start of the function
start_idx = content.find("fun getPartyLedger(partyId: String, partyType: PartyType): Flow<PartyLedgerResult> {")
end_idx = content.find("fun addUser(username: String, password: String, role: String) {")

if start_idx != -1 and end_idx != -1:
    # Just replace this block with the correct one
    new_func = """    fun getPartyLedger(partyId: String, partyType: PartyType): Flow<PartyLedgerResult> {
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

            val computedEntries = entries.sortedBy { it.date }.map { it.copy() }
            var running = 0.0
            computedEntries.forEach { entry ->
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
    
    """
    
    # We replace from start_idx to end_idx
    content = content[:start_idx] + new_func + content[end_idx:]
    
    with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
        f.write(content)
