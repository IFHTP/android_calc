package com.example.android_calc.presentation

import com.example.android_calc.data.HistoryItem

data class CalculationState(
    val expression: String = "",
    val result: String = "",
    val isFinal: Boolean = false,
    val history: List<HistoryItem> = emptyList(),
    val isHistoryVisible: Boolean = false
)
