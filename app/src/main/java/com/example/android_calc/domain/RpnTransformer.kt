package com.example.android_calc.domain

import java.util.ArrayDeque

class RpnTransformer {
    fun transform(tokens: List<Token>): List<Token> {
        val output = mutableListOf<Token>()
        val stack = ArrayDeque<Token>()

        for (token in tokens) {
            when (token) {
                is Token.Number -> output.add(token)

                is Token.Parenthesis -> {
                    if (token.isOpen) {
                        stack.push(token)
                    } else {
                        while (stack.isNotEmpty() && (stack.peek() as? Token.Parenthesis)?.isOpen != true) {
                            output.add(stack.pop())
                        }
                        stack.pop()

                        if (stack.isNotEmpty() && stack.peek() is Token.Operator && (stack.peek() as Token.Operator).isFunction) {
                            output.add(stack.pop())
                        }
                    }
                }

                is Token.Operator -> {
                    if (token.type == Token.Type.FACT) {
                        output.add(token)
                    } else {
                        while (stack.isNotEmpty() && stack.peek() is Token.Operator) {
                            val top = stack.peek() as Token.Operator
                            if (top.priority >= token.priority) {
                                output.add(stack.pop())
                            } else {
                                break
                            }
                        }
                        stack.push(token)
                    }
                }
            }
        }

        while (stack.isNotEmpty()) {
            output.add(stack.pop())
        }

        return output
    }
}