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
                            if (b == 0.0) Double.NaN else a / b
                        }
                        Token.Type.POW -> {
                            val b = stack.pop()
                            val a = stack.pop()
                            a.pow(b)
                        }

                        Token.Type.SIN -> sin(stack.pop())
                        Token.Type.COS -> cos(stack.pop())
                        Token.Type.TAN -> {
                            val x = stack.pop()
                            val mod = (x - PI / 2.0) % PI
                            if (abs(mod) < 1e-10 || abs(mod - PI) < 1e-10) Double.NaN else tan(x)
                        }
                        Token.Type.SQRT -> {
                            val x = stack.pop()
                            if (x < 0) Double.NaN else sqrt(x)
                        }
                        Token.Type.LN -> {
                            val x = stack.pop()
                            if (x <= 0) Double.NaN else ln(x)
                        }
                        Token.Type.LG -> {
                            val x = stack.pop()
                            if (x <= 0) Double.NaN else log10(x)
                        }
                        Token.Type.INV -> {
                            val x = stack.pop()
                            if (x == 0.0) Double.NaN else 1.0 / x
                        }
                        Token.Type.FACT -> factorial(stack.pop())
                        
                        Token.Type.ARCSIN -> {
                            val x = stack.pop()
                            if (x < -1.0 || x > 1.0) Double.NaN else asin(x)
                        }
                        Token.Type.ARCCOS -> {
                            val x = stack.pop()
                            if (x < -1.0 || x > 1.0) Double.NaN else acos(x)
                        }
                        Token.Type.ARCTAN -> atan(stack.pop())

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
        if (n < 0 || n > 20) return Double.NaN
        if (n == 0.0) return 1.0
        var res = 1.0
        for (i in 1..n.toInt()) {
            res *= i
        }
        return res
    }
}