package com.bhaai.expensetracker.data

import androidx.room.TypeConverter
import com.bhaai.expensetracker.domain.Category
import com.bhaai.expensetracker.domain.ExpenseStatus

class Converters {
    @TypeConverter
    fun fromCategory(category: Category?): String? {
        return category?.name
    }

    @TypeConverter
    fun toCategory(name: String?): Category? {
        return name?.let { Category.valueOf(it) }
    }

    @TypeConverter
    fun fromExpenseStatus(status: ExpenseStatus): String {
        return status.name
    }

    @TypeConverter
    fun toExpenseStatus(name: String): ExpenseStatus {
        return ExpenseStatus.valueOf(name)
    }
}
