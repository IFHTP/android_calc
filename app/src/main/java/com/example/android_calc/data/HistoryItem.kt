package com.example.android_calc.data

data class HistoryItem(
    val expression: String = "",
    val result: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {}