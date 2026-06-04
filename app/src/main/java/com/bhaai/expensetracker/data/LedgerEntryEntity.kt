package com.bhaai.expensetracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.bhaai.expensetracker.domain.AccountType
import com.bhaai.expensetracker.domain.LedgerEntry

@Entity(
    tableName = "ledger_entries",
    foreignKeys = [
        ForeignKey(
            entity = FinancialEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LedgerEntryEntity(
    @PrimaryKey
    val id: String,
    val eventId: String,
    val accountType: AccountType,
    val amount: Double
) {
    fun toDomain(): LedgerEntry = LedgerEntry(
        id = id,
        eventId = eventId,
        accountType = accountType,
        amount = amount
    )

    companion object {
        fun fromDomain(domain: LedgerEntry): LedgerEntryEntity = LedgerEntryEntity(
            id = domain.id,
            eventId = domain.eventId,
            accountType = domain.accountType,
            amount = domain.amount
        )
    }
}
