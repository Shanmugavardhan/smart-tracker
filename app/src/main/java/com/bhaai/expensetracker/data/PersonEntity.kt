package com.bhaai.expensetracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bhaai.expensetracker.domain.Person

@Entity(
    tableName = "persons",
    indices = [Index(value = ["name"], unique = true)]
)
data class PersonEntity(
    @PrimaryKey
    val id: String,
    val name: String
) {
    fun toDomain(): Person = Person(
        id = id,
        name = name
    )

    companion object {
        fun fromDomain(domain: Person): PersonEntity = PersonEntity(
            id = domain.id,
            name = domain.name
        )
    }
}
