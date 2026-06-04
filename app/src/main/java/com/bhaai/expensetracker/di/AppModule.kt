package com.bhaai.expensetracker.di

import android.app.Application
import androidx.room.Room
import com.bhaai.expensetracker.data.ExpenseDatabase
import com.bhaai.expensetracker.data.ExpenseRepositoryImpl
import com.bhaai.expensetracker.data.NetworkMonitor
import com.bhaai.expensetracker.data.api.GeminiApi
import com.bhaai.expensetracker.data.repository.ExpenseCategorizationServiceImpl
import com.bhaai.expensetracker.data.repository.GeminiExpenseCategorizationService
import com.bhaai.expensetracker.data.repository.LocalFallbackExpenseCategorizationService
import com.bhaai.expensetracker.domain.ExpenseCategorizationService
import com.bhaai.expensetracker.domain.ExpenseRepository
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
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
        ).addMigrations(
            ExpenseDatabase.MIGRATION_1_2,
            ExpenseDatabase.MIGRATION_2_3,
            ExpenseDatabase.MIGRATION_3_4
        )
            .build()
    }

    @Provides
    @Singleton
    fun provideExpenseRepository(db: ExpenseDatabase): ExpenseRepository {
        return ExpenseRepositoryImpl(db.expenseDao)
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideGeminiApi(okHttpClient: OkHttpClient, moshi: Moshi): GeminiApi {
        return Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(app: Application): NetworkMonitor {
        return NetworkMonitor(app)
    }

    @Provides
    @Singleton
    @javax.inject.Named("gemini_api_key")
    fun provideGeminiApiKey(): String {
        return com.bhaai.expensetracker.BuildConfig.GEMINI_API_KEY
    }

    @Provides
    @Singleton
    fun provideExpenseCategorizationService(
        geminiService: GeminiExpenseCategorizationService,
        localFallbackService: LocalFallbackExpenseCategorizationService,
        networkMonitor: NetworkMonitor
    ): ExpenseCategorizationService {
        return ExpenseCategorizationServiceImpl(
            geminiService = geminiService,
            localFallbackService = localFallbackService,
            networkMonitor = networkMonitor
        )
    }
}
