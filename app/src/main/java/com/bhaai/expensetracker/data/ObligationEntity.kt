package com.bhaai.expensetracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.bhaai.expensetracker.domain.Obligation

@Entity(
    tableName = "obligations",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ObligationEntity(
    @PrimaryKey
    val id: String,
    val personId: String,
    val amountOutstanding: Double,
    val reason: String
) {
    fun toDomain(): Obligation = Obligation(
        id = id,
        personId = personId,
        amountOutstanding = amountOutstanding,
        reason = reason
    )

    companion object {
        fun fromDomain(domain: Obligation): ObligationEntity = ObligationEntity(
            id = domain.id,
            personId = domain.personId,
            amountOutstanding = domain.amountOutstanding,
            reason = domain.reason
        )
    }
}
