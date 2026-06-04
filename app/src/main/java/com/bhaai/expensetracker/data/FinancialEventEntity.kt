package com.bhaai.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bhaai.expensetracker.domain.Category
import com.bhaai.expensetracker.domain.FinancialEvent
import com.bhaai.expensetracker.domain.TransactionType

@Entity(tableName = "financial_events")
data class FinancialEventEntity(
    @PrimaryKey
    val id: String,
    val type: TransactionType,
    val description: String,
    val timestamp: Long,
    val category: Category?
) {
    fun toDomain(): FinancialEvent = FinancialEvent(
        id = id,
        type = type,
        description = description,
        timestamp = timestamp,
        category = category
    )

    companion object {
        fun fromDomain(domain: FinancialEvent): FinancialEventEntity = FinancialEventEntity(
            id = domain.id,
            type = domain.type,
            description = domain.description,
            timestamp = domain.timestamp,
            category = domain.category
        )
    }
}
