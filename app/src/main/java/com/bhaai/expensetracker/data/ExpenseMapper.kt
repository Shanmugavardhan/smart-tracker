package com.bhaai.expensetracker.data

import com.bhaai.expensetracker.domain.Expense

fun ExpenseEntity.toDomain(): Expense {
    return Expense(
        id = id,
        amount = amount,
        description = description,
        category = category,
        subcategory = subcategory,
        status = status,
        confidence = confidence,
        timestamp = timestamp,
        splitCount = splitCount,
        originalAmount = originalAmount
    )
}

fun Expense.toEntity(): ExpenseEntity {
    return ExpenseEntity(
        id = id,
        amount = amount,
        description = description,
        category = category,
        subcategory = subcategory,
        status = status,
        confidence = confidence,
        timestamp = timestamp,
        splitCount = splitCount,
        originalAmount = originalAmount
    )
}
