package com.bhaai.expensetracker.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bhaai.expensetracker.domain.Category
import com.bhaai.expensetracker.domain.ExpenseStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineVerificationTest {

    @Test
    fun verifyExpensePersistsAcrossAppRestarts() = runBlocking {
        // "App Session 1" - User adds expense
        var database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ExpenseDatabase::class.java
        ).allowMainThreadQueries().build()
        var dao = database.expenseDao

        val expense = ExpenseEntity(
            amount = 350.0,
            description = "Offline Biryani",
            category = Category.OTHER,
            subcategory = null,
            status = ExpenseStatus.PENDING_CATEGORIZATION,
            timestamp = 1000L
        )

        dao.insertExpense(expense)
        var list = dao.getAllExpenses().first()
        assertEquals(1, list.size)
        assertEquals("Offline Biryani", list[0].description)

        // Close DB, simulating App Kill
        database.close()

        // "App Session 2" - User reopens app (Since it's inMemory, we cannot actually restart.
        // We will simulate the behavior using a persistent file DB instead for this specific test.)

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.deleteDatabase("test_offline_db")

        var realDatabase = Room.databaseBuilder(
            context,
            ExpenseDatabase::class.java,
            "test_offline_db"
        ).allowMainThreadQueries().build()
        var realDao = realDatabase.expenseDao

        realDao.insertExpense(expense)
        realDatabase.close()

        // Reopen real DB
        val restartedDatabase = Room.databaseBuilder(
            context,
            ExpenseDatabase::class.java,
            "test_offline_db"
        ).allowMainThreadQueries().build()
        val restartedDao = restartedDatabase.expenseDao

        val restartedList = restartedDao.getAllExpenses().first()
        assertEquals(1, restartedList.size)
        assertEquals("Offline Biryani", restartedList[0].description)
        assertEquals(ExpenseStatus.PENDING_CATEGORIZATION, restartedList[0].status)

        restartedDatabase.close()
        context.deleteDatabase("test_offline_db")
    }
}
