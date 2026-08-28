#!/bin/bash
sed -i 's/\.companyName/.businessName/g' app/src/main/java/com/example/ui/AppViewModel.kt
sed -i 's/repository\.insertUser/repository.addUser/g' app/src/main/java/com/example/ui/AppViewModel.kt
sed -i 's/repository\.getPartyTransactions/repository.getTransactionsForParty/g' app/src/main/java/com/example/ui/AppViewModel.kt
sed -i 's/repository\.getPartyVouchers/repository.getVouchersForParty/g' app/src/main/java/com/example/ui/AppViewModel.kt
