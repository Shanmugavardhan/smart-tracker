package com.bhaai.expensetracker.domain

interface ExpenseCategorizationService {
    suspend fun categorizeExpense(text: String): ParsedExpense
}

data class ParsedExpense(
    val transactionType: TransactionType,
    val description: String,
    val category: Category,
    val totalAmount: Double?,
    val effectiveAmount: Double?,
    val originalAmount: Double?,
    val splitCount: Int?,
    val person: String?,
    val people: List<String>,
    val confidence: Double
)
