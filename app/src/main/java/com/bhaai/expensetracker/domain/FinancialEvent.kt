package com.bhaai.expensetracker.domain

data class FinancialEvent(
    val id: String,
    val type: TransactionType,
    val description: String,
    val timestamp: Long,
    val category: Category? = null
)
