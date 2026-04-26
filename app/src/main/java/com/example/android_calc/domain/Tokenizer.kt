package com.example.android_calc.domain

class Tokenizer {
    fun tokenize(expression: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while(i < expression.length){
            val char = expression[i]
            when{
                char.isWhitespace() -> i++
                char.isDigit() || char == '.' -> {
                    val sb = StringBuilder()
                    while (i < expression.length && (expression[i].isDigit() || expression[i] == '.' || expression[i].lowercaseChar() == 'e')) {
                        if (expression[i].lowercaseChar() == 'e') {
                            sb.append(expression[i++])
                            if (i < expression.length && (expression[i] == '+' || expression[i] == '-')) {
                                sb.append(expression[i++])
                            }
                        } else {
                            sb.append(expression[i++])
                        }
                    }
                    val numStr = sb.toString()
                    try {
                        tokens.add(Token.Number(numStr.toDouble()))
                    } catch (e: Exception) {
                    }
                }
                char.isLetter() || char == 'π' -> {
                    val sb = StringBuilder()
                    while (i < expression.length && (expression[i].isLetter() || expression[i] == 'π')) {
                        sb.append(expression[i++])
                    }
                    val word = sb.toString().lowercase()
                    val token = when (word) {
                        "pi", "π" -> Token.Number(Math.PI)
                        "e" -> Token.Number(Math.E)
                        "sin" -> Token.Operator(Token.Type.SIN, 4, true)
                        "cos" -> Token.Operator(Token.Type.COS, 4, true)
                        "tan" -> Token.Operator(Token.Type.TAN, 4, true)
                        "arcsin" -> Token.Operator(Token.Type.ARCSIN, 4, true)
                        "arccos" -> Token.Operator(Token.Type.ARCCOS, 4, true)
                        "arctan" -> Token.Operator(Token.Type.ARCTAN, 4, true)
                        "sqrt", "√" -> Token.Operator(Token.Type.SQRT, 4, true)
                        "ln" -> Token.Operator(Token.Type.LN, 4, true)
                        "lg" -> Token.Operator(Token.Type.LG, 4, true)
                        "fact" -> Token.Operator(Token.Type.FACT, priority = 5, false)
                        "inv" -> Token.Operator(Token.Type.INV, priority = 4, false)
                        else -> throw IllegalArgumentException("Unknown function: $word")
                    }
                    tokens.add(token)
                }
                char == '√' -> {
                    tokens.add(Token.Operator(Token.Type.SQRT, 4, true))
                    i++
                }
                char == '+' -> {
                    tokens.add(Token.Operator(Token.Type.PLUS, 1, false))
                    i++
                }
                char == '-' -> {
                    if (tokens.isEmpty() || (tokens.last() is Token.Parenthesis && (tokens.last() as Token.Parenthesis).isOpen)) {
                        tokens.add(Token.Number(-1.0))
                        tokens.add(Token.Operator(Token.Type.MULTIPLY, 2, false))
                    } else {
                        tokens.add(Token.Operator(Token.Type.MINUS, 1, false))
                    }
                    i++
                }
                char == '×' -> {
                    tokens.add(Token.Operator(Token.Type.MULTIPLY, 2, false))
                    i++
                }
                char == '÷' -> {
                    tokens.add(Token.Operator(Token.Type.DIVIDE, 2, false))
                    i++
                }
                char == '^' -> {
                    tokens.add(Token.Operator(Token.Type.POW, 3, false))
                    i++
                }
                char == '!' -> {
                    tokens.add(Token.Operator(Token.Type.FACT, 5, false))
                    i++
                }
                char == '(' -> {
                    tokens.add(Token.Parenthesis(true))
                    i++
                }
                char == ')' -> {
                    tokens.add(Token.Parenthesis(false))
                    i++
                }
                else -> i++
            }
        }
        return tokens.toList()
    }
}