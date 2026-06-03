package com.bhaai.expensetracker.di

import android.app.Application
import androidx.room.Room
import com.bhaai.expensetracker.data.ExpenseDatabase
import com.bhaai.expensetracker.data.ExpenseRepositoryImpl
import com.bhaai.expensetracker.domain.ExpenseRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideExpenseDatabase(app: Application): ExpenseDatabase {
        return Room.databaseBuilder(
            app,
            ExpenseDatabase::class.java,
            ExpenseDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideExpenseRepository(db: ExpenseDatabase): ExpenseRepository {
        return ExpenseRepositoryImpl(db.expenseDao)
    }
}
