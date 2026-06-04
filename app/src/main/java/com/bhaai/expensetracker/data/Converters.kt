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

    @TypeConverter
    fun fromTransactionType(type: com.bhaai.expensetracker.domain.TransactionType): String {
        return type.name
    }

    @TypeConverter
    fun toTransactionType(name: String): com.bhaai.expensetracker.domain.TransactionType {
        return com.bhaai.expensetracker.domain.TransactionType.valueOf(name)
    }

    @TypeConverter
    fun fromAccountType(type: com.bhaai.expensetracker.domain.AccountType): String {
        return type.name
    }

    @TypeConverter
    fun toAccountType(name: String): com.bhaai.expensetracker.domain.AccountType {
        return com.bhaai.expensetracker.domain.AccountType.valueOf(name)
    }
}
