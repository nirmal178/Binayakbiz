import re

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
    content = f.read()

old_block = """            val computedEntries = entries.sortedBy { it.date }.map { it.copy() }
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
            val totalCredit = computedEntries.sumOf { it.credit }"""

new_block = """            val sortedEntries = entries.sortedBy { it.date }
            var running = 0.0
            val computedEntries = sortedEntries.map { entry ->
                running += if (partyType == PartyType.CUSTOMER) {
                    (entry.debit - entry.credit) // Positive = Dr (Customer owes us)
                } else {
                    (entry.credit - entry.debit) // Positive = Cr (We owe supplier)
                }
                entry.copy(runningBalance = running)
            }

            val totalDebit = computedEntries.sumOf { it.debit }
            val totalCredit = computedEntries.sumOf { it.credit }"""

if old_block in content:
    content = content.replace(old_block, new_block)
    with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
        f.write(content)
    print("Fixed running balance computation.")
else:
    print("Block not found!")
