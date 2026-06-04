package com.bhaai.expensetracker.domain

data class Obligation(
    val id: String,
    val personId: String,
    val amountOutstanding: Double,
    val reason: String
)
