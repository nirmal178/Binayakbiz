#!/bin/bash
awk '
/for \(v in vouchers.filter \{ it.voucherType == VoucherType.RECEIPT \}\)/ {
    print "                for (v in vouchers) {"
    print "                    val isReceipt = v.voucherType == VoucherType.RECEIPT"
    print "                    entries.add("
    print "                        PartyLedgerEntry("
    print "                            date = v.voucherDate,"
    print "                            refNumber = v.voucherNumber,"
    print "                            entryType = if (isReceipt) \"Receipt Voucher\" else \"Payment Voucher\","
    print "                            particulars = v.remarks ?: if (isReceipt) \"Payment Received\" else \"Payment Made\","
    print "                            paymentMode = v.paymentMode,"
    print "                            debit = if (isReceipt) 0.0 else v.amount,"
    print "                            credit = if (isReceipt) v.amount else 0.0,"
    print "                            id = v.id"
    print "                        )"
    print "                    )"
    skip = 13
    next
}
/for \(v in vouchers.filter \{ it.voucherType == VoucherType.PAYMENT \}\)/ {
    print "                for (v in vouchers) {"
    print "                    val isPayment = v.voucherType == VoucherType.PAYMENT"
    print "                    entries.add("
    print "                        PartyLedgerEntry("
    print "                            date = v.voucherDate,"
    print "                            refNumber = v.voucherNumber,"
    print "                            entryType = if (isPayment) \"Payment Voucher\" else \"Receipt Voucher\","
    print "                            particulars = v.remarks ?: if (isPayment) \"Payment Made to Supplier\" else \"Refund Received\","
    print "                            paymentMode = v.paymentMode,"
    print "                            debit = if (isPayment) v.amount else 0.0,"
    print "                            credit = if (isPayment) 0.0 else v.amount,"
    print "                            id = v.id"
    print "                        )"
    print "                    )"
    skip = 13
    next
}
skip > 0 {
    skip--
    next
}
{ print }
' app/src/main/java/com/example/ui/AppViewModel.kt > temp.kt && mv temp.kt app/src/main/java/com/example/ui/AppViewModel.kt
