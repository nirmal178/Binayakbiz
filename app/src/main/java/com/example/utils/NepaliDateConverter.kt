package com.example.utils

import java.util.Calendar
import java.util.Date

object NepaliDateConverter {
    // Rough approximation for display purposes
    // BS is approximately 56 years, 8 months, and 15 days ahead of AD.
    fun getBsDate(adDateMillis: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = adDateMillis
        
        var bsYear = cal.get(Calendar.YEAR) + 56
        var bsMonth = cal.get(Calendar.MONTH) + 1 + 8 // 1-indexed AD month
        var bsDay = cal.get(Calendar.DAY_OF_MONTH) + 15
        
        if (bsDay > 30) {
            bsDay -= 30
            bsMonth += 1
        }
        
        if (bsMonth > 12) {
            bsMonth -= 12
            bsYear += 1
        }
        
        return "$bsYear-${bsMonth.toString().padStart(2, '0')}-${bsDay.toString().padStart(2, '0')}"
    }
}
