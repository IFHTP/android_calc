package com.example.android_calc.domain

sealed class Token {
    data class Number(
        val value: Double
    ) : Token()
    enum class Type {
        PLUS, MINUS, MULTIPLY, DIVIDE,
        POW,
        SQRT,
        FACT,
        INV,
        SIN, COS, TAN,
        ARCSIN, ARCCOS, ARCTAN,
        LG, LN
    }
    data class Operator(
        val type: Type,
        val priority: Int,
        val isFunction: Boolean
    ) : Token()
    data class Parenthesis(
        val isOpen: Boolean
    ) : Token()
}