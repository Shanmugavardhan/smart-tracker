package com.bhaai.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bhaai.expensetracker.domain.Category
import com.bhaai.expensetracker.domain.ExpenseStatus

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val description: String,
    val category: Category?,
    val subcategory: String?,
    val status: ExpenseStatus,
    val timestamp: Long
)
