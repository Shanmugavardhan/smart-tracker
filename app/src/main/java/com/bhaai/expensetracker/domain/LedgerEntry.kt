package com.bhaai.expensetracker.domain

data class LedgerEntry(
    val id: String,
    val eventId: String,
    val accountType: AccountType,
    val amount: Double
)
