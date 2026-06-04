package com.bhaai.expensetracker.data.repository

import com.bhaai.expensetracker.data.NetworkMonitor
import com.bhaai.expensetracker.domain.ExpenseCategorizationService
import com.bhaai.expensetracker.domain.ParsedExpense
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseCategorizationServiceImpl @Inject constructor(
    private val geminiService: GeminiExpenseCategorizationService,
    private val localFallbackService: LocalFallbackExpenseCategorizationService,
    private val networkMonitor: NetworkMonitor
) : ExpenseCategorizationService {

    override suspend fun categorizeExpense(text: String): ParsedExpense {
        if (!networkMonitor.isNetworkAvailable()) {
            return localFallbackService.categorizeExpense(text)
        }
        return try {
            geminiService.categorizeExpense(text)
        } catch (e: Exception) {
            // Fallback to local parsing on network failure or rate limit
            localFallbackService.categorizeExpense(text)
        }
    }
}
