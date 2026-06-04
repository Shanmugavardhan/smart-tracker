package com.bhaai.expensetracker.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ExpenseEntity::class,
        FinancialEventEntity::class,
        LedgerEntryEntity::class,
        PersonEntity::class,
        ObligationEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract val expenseDao: ExpenseDao

    companion object {
        const val DATABASE_NAME = "expense_db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN confidence REAL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN splitCount INTEGER")
                db.execSQL("ALTER TABLE expenses ADD COLUMN originalAmount REAL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create financial_events table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS financial_events (
                        id TEXT NOT NULL PRIMARY KEY,
                        type TEXT NOT NULL,
                        description TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        category TEXT
                    )
                """)

                // Create ledger_entries table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS ledger_entries (
                        id TEXT NOT NULL PRIMARY KEY,
                        eventId TEXT NOT NULL,
                        accountType TEXT NOT NULL,
                        amount REAL NOT NULL,
                        FOREIGN KEY(eventId) REFERENCES financial_events(id) ON DELETE CASCADE
                    )
                """)

                // Create persons table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS persons (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL UNIQUE
                    )
                """)

                // Create obligations table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS obligations (
                        id TEXT NOT NULL PRIMARY KEY,
                        personId TEXT NOT NULL,
                        amountOutstanding REAL NOT NULL,
                        reason TEXT NOT NULL,
                        FOREIGN KEY(personId) REFERENCES persons(id) ON DELETE CASCADE
                    )
                """)

                // Migrate existing expenses to financial_events and ledger_entries
                val cursor = db.query("SELECT * FROM expenses")
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                    val amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"))
                    val description = cursor.getString(cursor.getColumnIndexOrThrow("description"))
                    val category = cursor.getString(cursor.getColumnIndexOrThrow("category"))
                    val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
                    val splitCountIndex = cursor.getColumnIndex("splitCount")
                    val splitCount = if (splitCountIndex != -1 && !cursor.isNull(splitCountIndex)) cursor.getInt(splitCountIndex) else null
                    val originalAmountIndex = cursor.getColumnIndex("originalAmount")
                    val originalAmount = if (originalAmountIndex != -1 && !cursor.isNull(originalAmountIndex)) cursor.getDouble(originalAmountIndex) else null

                    val eventId = "expense_$id"
                    val eventType = if (splitCount != null && splitCount > 1) "SHARED_EXPENSE" else "EXPENSE"

                    // Insert Financial Event
                    db.execSQL(
                        "INSERT OR IGNORE INTO financial_events (id, type, description, timestamp, category) VALUES (?, ?, ?, ?, ?)",
                        arrayOf(eventId, eventType, description, timestamp, category)
                    )

                    // Insert Ledger Entries
                    if (eventType == "SHARED_EXPENSE") {
                        val origAmt = originalAmount ?: amount
                        val effectiveAmt = amount
                        val receivableAmt = origAmt - effectiveAmt

                        db.execSQL(
                            "INSERT OR IGNORE INTO ledger_entries (id, eventId, accountType, amount) VALUES (?, ?, ?, ?)",
                            arrayOf("${eventId}_cash", eventId, "CASH", -origAmt)
                        )
                        db.execSQL(
                            "INSERT OR IGNORE INTO ledger_entries (id, eventId, accountType, amount) VALUES (?, ?, ?, ?)",
                            arrayOf("${eventId}_expense", eventId, "EXPENSE", effectiveAmt)
                        )
                        db.execSQL(
                            "INSERT OR IGNORE INTO ledger_entries (id, eventId, accountType, amount) VALUES (?, ?, ?, ?)",
                            arrayOf("${eventId}_receivable", eventId, "RECEIVABLE", receivableAmt)
                        )

                        // Insert default roommate obligations
                        if (splitCount != null && splitCount > 1) {
                            val roommateCount = splitCount - 1
                            val perRoommateShare = receivableAmt / roommateCount
                            for (i in 1..roommateCount) {
                                val personId = "roommate_$i"
                                val personName = "Roommate $i"
                                db.execSQL(
                                    "INSERT OR IGNORE INTO persons (id, name) VALUES (?, ?)",
                                    arrayOf(personId, personName)
                                )
                                db.execSQL(
                                    "INSERT OR IGNORE INTO obligations (id, personId, amountOutstanding, reason) VALUES (?, ?, ?, ?)",
                                    arrayOf("${eventId}_ob_$i", personId, perRoommateShare, "Split for $description")
                                )
                            }
                        }
                    } else {
                        db.execSQL(
                            "INSERT OR IGNORE INTO ledger_entries (id, eventId, accountType, amount) VALUES (?, ?, ?, ?)",
                            arrayOf("${eventId}_cash", eventId, "CASH", -amount)
                        )
                        db.execSQL(
                            "INSERT OR IGNORE INTO ledger_entries (id, eventId, accountType, amount) VALUES (?, ?, ?, ?)",
                            arrayOf("${eventId}_expense", eventId, "EXPENSE", amount)
                        )
                    }
                }
                cursor.close()
            }
        }
    }
}
