package com.bhaai.expensetracker.domain

data class Expense(
    val id: Long = 0,
    val amount: Double,
    val description: String,
    val category: Category?,
    val subcategory: String? = null,
    val status: ExpenseStatus,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ExpenseStatus {
    PENDING_CATEGORIZATION,
    CATEGORIZED
}

enum class Category {
    FOOD,
    TRAVEL,
    HOUSING,
    UTILITIES,
    SHOPPING,
    HEALTHCARE,
    EDUCATION,
    ENTERTAINMENT,
    INVESTMENT,
    OTHER
}
