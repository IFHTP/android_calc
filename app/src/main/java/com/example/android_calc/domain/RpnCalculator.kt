package com.example.android_calc.domain

import java.util.ArrayDeque
import kotlin.math.*

class RpnCalculator {
    fun calculate(tokens: List<Token>): Double {
        val stack = ArrayDeque<Double>()

        for (token in tokens) {
            when (token) {
                is Token.Number -> stack.push(token.value)
                is Token.Operator -> {
                    val result = when (token.type) {
                        Token.Type.PLUS -> stack.pop() + stack.pop()
                        Token.Type.MULTIPLY -> stack.pop() * stack.pop()
                        Token.Type.MINUS -> {
                            val b = stack.pop()
                            val a = stack.pop()
                            a - b
                        }
                        Token.Type.DIVIDE -> {
                            val b = stack.pop()
                            val a = stack.pop()
                            a / b
                        }
                        Token.Type.POW -> {
                            val b = stack.pop()
                            val a = stack.pop()
                            a.pow(b)
                        }

                        Token.Type.SIN -> sin(stack.pop())
                        Token.Type.COS -> cos(stack.pop())
                        Token.Type.TAN -> tan(stack.pop())
                        Token.Type.SQRT -> sqrt(stack.pop())
                        Token.Type.LN -> ln(stack.pop())
                        Token.Type.LG -> log10(stack.pop())
                        Token.Type.INV -> 1.0 / stack.pop()
                        Token.Type.FACT -> factorial(stack.pop())

                        else -> throw IllegalArgumentException("Unknown operator: ${token.type}")
                    }
                    stack.push(result)
                }
                else -> {}
            }
        }
        return stack.pop()
    }

    private fun factorial(n: Double): Double {
        if (n < 0) return Double.NaN
        if (n == 0.0) return 1.0
        var res = 1.0
        for (i in 1..n.toInt()) {
            res *= i
        }
        return res
    }
}