package com.example.android_calc.presentation

data class CalculatorState(
    val expression: String = "",
    val result: String = "",
    val isFinal: Boolean = false
)