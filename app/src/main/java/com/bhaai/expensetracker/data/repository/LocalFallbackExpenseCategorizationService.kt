package com.bhaai.expensetracker.data.repository

import com.bhaai.expensetracker.domain.Category
import com.bhaai.expensetracker.domain.ExpenseCategorizationService
import com.bhaai.expensetracker.domain.ParsedExpense
import com.bhaai.expensetracker.domain.TransactionType
import java.util.Locale
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalFallbackExpenseCategorizationService @Inject constructor() : ExpenseCategorizationService {
    override suspend fun categorizeExpense(text: String): ParsedExpense {
        val lowerText = text.lowercase(Locale.ROOT)

        var transactionType = TransactionType.EXPENSE
        var description = text.trim()
        var totalAmount: Double? = null
        var effectiveAmount: Double? = null
        var originalAmount: Double? = null
        var splitCount: Int? = null
        var person: String? = null
        var people = emptyList<String>()

        // 1. Detect Rent split (e.g., "Paid rent 10000 roommates paid 2500 each")
        val rentSplitPattern = Pattern.compile("paid\\s+rent\\s+([0-9]+(?:\\.[0-9]+)?)\\s*(?:k)?\\s+roommates\\s+paid\\s+([0-9]+(?:\\.[0-9]+)?)\\s*(?:k)?\\s+each")
        val rentSplitMatcher = rentSplitPattern.matcher(lowerText)
        if (rentSplitMatcher.find()) {
            val total = rentSplitMatcher.group(1)?.toDoubleOrNull() ?: 10000.0
            val perRoommate = rentSplitMatcher.group(2)?.toDoubleOrNull() ?: 2500.0
            transactionType = TransactionType.SHARED_EXPENSE
            description = "Rent"
            totalAmount = total
            effectiveAmount = total - (3 * perRoommate) // default to 4 people split
            originalAmount = total
            splitCount = (total / perRoommate).toInt()
            people = List(splitCount - 1) { "Roommate " + ('A' + it) }
        }
        // 2. Detect General Shared Expense Split (e.g., "Groceries 3369 split among 4")
        else {
            val splitPattern = Pattern.compile("(.+?)\\s+([0-9]+(?:\\.[0-9]+)?)\\s*(k)?\\s*[,\\s]*(?:split|shared)\\s+(?:it\\s+)?(?:for|among|between|by)?\\s*([0-9]+)")
            val splitMatcher = splitPattern.matcher(lowerText)
            if (splitMatcher.find()) {
                val itemDesc = splitMatcher.group(1)?.trim() ?: "Shared Expense"
                val numVal = splitMatcher.group(2)?.toDoubleOrNull() ?: 0.0
                val multiplierStr = splitMatcher.group(3)
                val multiplier = when (multiplierStr) {
                    "k" -> 1000.0
                    else -> 1.0
                }
                val total = numVal * multiplier
                val count = splitMatcher.group(4)?.toIntOrNull() ?: 1

                transactionType = TransactionType.SHARED_EXPENSE
                description = itemDesc.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                totalAmount = total
                splitCount = count
                effectiveAmount = total / count
                originalAmount = total
                people = List(count - 1) { "Roommate " + ('A' + it) }
            }
            // 3. Detect Loan Given (e.g., "Gave Arun 3500 loan")
            else {
                val loanGivenPattern = Pattern.compile("gave\\s+([a-zA-Z]+)\\s+([0-9]+(?:\\.[0-9]+)?)\\s*(k)?\\s+loan")
                val loanGivenMatcher = loanGivenPattern.matcher(lowerText)
                if (loanGivenMatcher.find()) {
                    val name = loanGivenMatcher.group(1)?.trim() ?: "Arun"
                    val numVal = loanGivenMatcher.group(2)?.toDoubleOrNull() ?: 0.0
                    val multiplierStr = loanGivenMatcher.group(3)
                    val multiplier = when (multiplierStr) {
                        "k" -> 1000.0
                        else -> 1.0
                    }
                    val total = numVal * multiplier

                    transactionType = TransactionType.LOAN_GIVEN
                    person = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                    description = "Loan to $person"
                    totalAmount = total
                    effectiveAmount = total
                }
                // 4. Detect Loan Repayment (e.g., "Arun returned 1000")
                else {
                    val loanRepayPattern = Pattern.compile("([a-zA-Z]+)\\s+returned\\s+([0-9]+(?:\\.[0-9]+)?)\\s*(k)?")
                    val loanRepayMatcher = loanRepayPattern.matcher(lowerText)
                    if (loanRepayMatcher.find()) {
                        val name = loanRepayMatcher.group(1)?.trim() ?: "Arun"
                        val numVal = loanRepayMatcher.group(2)?.toDoubleOrNull() ?: 0.0
                        val multiplierStr = loanRepayMatcher.group(3)
                        val multiplier = when (multiplierStr) {
                            "k" -> 1000.0
                            else -> 1.0
                        }
                        val total = numVal * multiplier

                        transactionType = TransactionType.LOAN_REPAYMENT
                        person = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                        description = "Loan repayment from $person"
                        totalAmount = total
                        effectiveAmount = total
                    }
                    // 5. Detect Reimbursement Received (e.g., "Received 451 split from roommate")
                    else {
                        val reimbursementPattern = Pattern.compile("received\\s+([0-9]+(?:\\.[0-9]+)?)\\s*(k)?\\s+split")
                        val reimbursementMatcher = reimbursementPattern.matcher(lowerText)
                        if (reimbursementMatcher.find()) {
                            val numVal = reimbursementMatcher.group(1)?.toDoubleOrNull() ?: 0.0
                            val multiplierStr = reimbursementMatcher.group(2)
                            val multiplier = when (multiplierStr) {
                                "k" -> 1000.0
                                else -> 1.0
                            }
                            val total = numVal * multiplier

                            transactionType = TransactionType.REIMBURSEMENT_RECEIVED
                            description = "Split reimbursement"
                            totalAmount = total
                            effectiveAmount = total
                        }
                        // 6. Plain Expense (e.g., "Had biriyani for 500", "Ate dosa at A2B 120")
                        else {
                            val amountPattern = Pattern.compile("\\b(rs\\.?|inr|₹|for|spent|of|price)?\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(k|lakh|lac|crore)?\\b")
                            val matcher = amountPattern.matcher(lowerText)
                            var parsedAmount = 0.0
                            var foundAmount = false
                            while (matcher.find()) {
                                val numVal = matcher.group(2)?.toDoubleOrNull()
                                val multiplierStr = matcher.group(3)
                                if (numVal != null) {
                                    val multiplier = when (multiplierStr) {
                                        "k" -> 1000.0
                                        "lakh", "lac" -> 100000.0
                                        "crore" -> 10000000.0
                                        else -> 1.0
                                    }
                                    val calculated = numVal * multiplier
                                    if (calculated > parsedAmount || !foundAmount) {
                                        parsedAmount = calculated
                                        foundAmount = true
                                    }
                                }
                            }

                            if (foundAmount) {
                                totalAmount = parsedAmount
                                effectiveAmount = parsedAmount

                                // Clean description
                                var cleanDesc = text.trim()
                                val removePattern = Pattern.compile("(?i)\\b(?:for|spent|of)?\\s*(?:rs\\.?|inr|₹)?\\s*[0-9]+(?:\\.[0-9]+)?\\s*(?:k|lakh|lac|crore)?\\b")
                                cleanDesc = removePattern.matcher(cleanDesc).replaceAll("").trim()
                                cleanDesc = cleanDesc.replace("\\s+".toRegex(), " ")
                                description = cleanDesc.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                            }
                        }
                    }
                }
            }
        }

        // Extract Category
        var category = Category.OTHER
        val foodKeywords = listOf("biryani", "biriyani", "food", "dinner", "lunch", "breakfast", "burger", "pizza", "coffee", "restaurant", "tea", "chai", "eat", "eating", "hotel", "swiggy", "zomato", "cafe", "grocery", "groceries", "milk", "vegetables", "fruit", "fruits", "dosa", "ate", "a2b", "rice")
        val travelKeywords = listOf("uber", "ola", "cab", "travel", "auto", "train", "flight", "bus", "petrol", "fuel", "diesel", "ride", "metro", "transport", "ticket")
        val housingKeywords = listOf("rent", "room", "flat", "hostel", "pg", "maintenance", "house", "apartment")
        val utilitiesKeywords = listOf("electricity", "water", "wifi", "internet", "bill", "recharge", "gas", "tv", "dth", "broadband", "phone bill")
        val shoppingKeywords = listOf("fridge", "refrigerator", "shopping", "clothes", "shoes", "shirt", "pant", "amazon", "flipkart", "myntra", "t-shirt", "watch", "bag", "mall", "appliance", "appliances", "stove", "induction", "purchased", "purchase", "utensils", "cooker")
        val healthcareKeywords = listOf("doctor", "medicine", "hospital", "clinic", "health", "pharmacy", "medical", "dentist", "tablet", "syrup")
        val educationKeywords = listOf("school", "college", "book", "fees", "course", "udemy", "stationery", "pen", "notebook", "exam")
        val entertainmentKeywords = listOf("movie", "theater", "netflix", "game", "spotify", "concert", "party", "pub", "bar", "club", "drinks", "fun", "show")
        val investmentKeywords = listOf("invest", "stock", "mutual fund", "gold", "share", "sip", "fd", "crypto", "bitcoin")

        val matchesKeyword = { keywords: List<String> ->
            keywords.any { keyword ->
                val regex = Regex("\\b${Regex.escape(keyword)}\\b")
                regex.containsMatchIn(lowerText)
            }
        }

        when {
            matchesKeyword(foodKeywords) -> category = Category.FOOD
            matchesKeyword(travelKeywords) -> category = Category.TRAVEL
            matchesKeyword(housingKeywords) -> category = Category.HOUSING
            matchesKeyword(utilitiesKeywords) -> category = Category.UTILITIES
            matchesKeyword(shoppingKeywords) -> category = Category.SHOPPING
            matchesKeyword(healthcareKeywords) -> category = Category.HEALTHCARE
            matchesKeyword(educationKeywords) -> category = Category.EDUCATION
            matchesKeyword(entertainmentKeywords) -> category = Category.ENTERTAINMENT
            matchesKeyword(investmentKeywords) -> category = Category.INVESTMENT
        }

        // Rejection rule: If no amount was found and category is OTHER, throw exception
        if (totalAmount == null && category == Category.OTHER) {
            throw IllegalArgumentException("Invalid expense text: $text")
        }

        val confidence = if (totalAmount != null && category != Category.OTHER) 0.95 else if (totalAmount != null) 0.6 else 0.1

        return ParsedExpense(
            transactionType = transactionType,
            description = description,
            category = category,
            totalAmount = totalAmount,
            effectiveAmount = effectiveAmount,
            originalAmount = originalAmount,
            splitCount = splitCount,
            person = person,
            people = people,
            confidence = confidence
        )
    }
}
