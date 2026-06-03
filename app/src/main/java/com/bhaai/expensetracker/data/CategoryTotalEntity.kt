package com.bhaai.expensetracker.data

import androidx.room.ColumnInfo
import com.bhaai.expensetracker.domain.Category

data class CategoryTotalEntity(
    val category: Category?,
    @ColumnInfo(name = "totalAmount")
    val totalAmount: Double
)
