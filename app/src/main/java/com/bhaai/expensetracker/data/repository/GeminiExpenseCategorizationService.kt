package com.bhaai.expensetracker.data.repository

import com.bhaai.expensetracker.data.api.GeminiApi
import com.bhaai.expensetracker.data.api.GeminiRequest
import com.bhaai.expensetracker.data.api.Content
import com.bhaai.expensetracker.data.api.Part
import com.bhaai.expensetracker.data.api.GenerationConfig
import com.bhaai.expensetracker.data.api.ResponseSchema
import com.bhaai.expensetracker.data.api.SchemaProperty
import com.bhaai.expensetracker.data.api.GeminiParsedResult
import com.bhaai.expensetracker.domain.Category
import com.bhaai.expensetracker.domain.ExpenseCategorizationService
import com.bhaai.expensetracker.domain.ParsedExpense
import com.bhaai.expensetracker.domain.TransactionType
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiExpenseCategorizationService @Inject constructor(
    private val api: GeminiApi,
    private val moshi: Moshi,
    @javax.inject.Named("gemini_api_key") private val apiKey: String
) : ExpenseCategorizationService {

    override suspend fun categorizeExpense(text: String): ParsedExpense {
        if (apiKey.isBlank()) {
            throw IllegalStateException("Gemini API key is not configured")
        }

        val prompt = """
            Extract the financial event details from the following user text: "$text".

            Strictly follow these rules:
            1. Parse monetary values with notations: '15k' is 15000, '2.5k' is 2500, 'lakh/lac' is 100000, 'crore' is 10000000.
            2. Detect the transactionType:
               - EXPENSE: personal spending outflow (e.g. "Had biriyani for 500").
               - SHARED_EXPENSE: shared outflow (e.g. "Bought fridge worth 15k split among 4" or "Paid rent 10000 roommates paid 2500 each").
               - LOAN_GIVEN: lending cash to someone (e.g. "Gave Arun 3500 loan").
               - LOAN_RECEIVED: borrowing cash from someone.
               - LOAN_REPAYMENT: receiving cash back from someone you loaned (e.g. "Arun returned 1000").
               - REIMBURSEMENT_RECEIVED: receiving cash for a shared expense split (e.g. "Received 451 split from roommate").
               - INCOME: money inflow from work/gifts.
               - TRANSFER: moving cash between your own accounts.
            3. Detect the category matching one of: ${Category.entries.joinToString { it.name }}.
            4. Calculate the totalAmount (total outflow/inflow), originalAmount (amount before split), and effectiveAmount (user's personal share).
            5. Extract the primary person name involved (if any) as 'person'.
            6. Extract other people involved in splits as 'people'.
            7. Set a confidence score between 0.0 and 1.0.

            Examples:
            - Input: "Bought fridge worth 15k split among 4"
              Output: { "transactionType": "SHARED_EXPENSE", "description": "Fridge", "category": "SHOPPING", "totalAmount": 15000, "effectiveAmount": 3750, "originalAmount": 15000, "splitCount": 4, "confidence": 0.95 }
            - Input: "Gave Arun 3500 loan"
              Output: { "transactionType": "LOAN_GIVEN", "person": "Arun", "totalAmount": 3500, "effectiveAmount": 3500, "confidence": 0.95 }
            - Input: "Received 451 split from roommate"
              Output: { "transactionType": "REIMBURSEMENT_RECEIVED", "totalAmount": 451, "confidence": 0.95 }
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(text = prompt))
                )
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                responseSchema = ResponseSchema(
                    type = "OBJECT",
                    properties = mapOf(
                        "transactionType" to SchemaProperty(
                            type = "STRING",
                            enum = TransactionType.entries.map { it.name }
                        ),
                        "category" to SchemaProperty(
                            type = "STRING",
                            enum = Category.entries.map { it.name }
                        ),
                        "description" to SchemaProperty(type = "STRING"),
                        "totalAmount" to SchemaProperty(type = "NUMBER"),
                        "effectiveAmount" to SchemaProperty(type = "NUMBER"),
                        "originalAmount" to SchemaProperty(type = "NUMBER"),
                        "splitCount" to SchemaProperty(type = "INTEGER"),
                        "person" to SchemaProperty(type = "STRING"),
                        "confidence" to SchemaProperty(type = "NUMBER")
                    ),
                    required = listOf("transactionType", "totalAmount", "effectiveAmount", "confidence")
                )
            )
        )

        val response = api.generateContent(apiKey = apiKey, request = request)
        val textResult = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw Exception("Empty response from Gemini API")

        val adapter = moshi.adapter(GeminiParsedResult::class.java)
        val parsed = adapter.fromJson(textResult) ?: throw Exception("Failed to parse JSON response: $textResult")

        val matchedCategory = try {
            Category.valueOf(parsed.category?.uppercase() ?: "OTHER")
        } catch (e: Exception) {
            Category.OTHER
        }

        val matchedTransactionType = try {
            TransactionType.valueOf(parsed.transactionType.uppercase())
        } catch (e: Exception) {
            TransactionType.EXPENSE
        }

        return ParsedExpense(
            transactionType = matchedTransactionType,
            description = parsed.description ?: "",
            category = matchedCategory,
            totalAmount = parsed.totalAmount,
            effectiveAmount = parsed.effectiveAmount,
            originalAmount = parsed.originalAmount,
            splitCount = parsed.splitCount,
            person = parsed.person,
            people = parsed.people ?: emptyList(),
            confidence = parsed.confidence ?: 0.5
        )
    }
}
