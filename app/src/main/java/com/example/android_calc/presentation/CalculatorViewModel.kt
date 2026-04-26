package com.example.android_calc.presentation

import androidx.lifecycle.ViewModel
import com.example.android_calc.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.DecimalFormat
import java.util.Locale

class CalculatorViewModel : ViewModel() {

    private val tokenizer = Tokenizer()
    private val transformer = RpnTransformer()
    private val calculator = RpnCalculator()

    private val _state = MutableStateFlow(CalculatorState())
    val state = _state.asStateFlow()

    private val operators = setOf('+', '-', '×', '÷', '%', '^')
    private val constants = setOf('π', 'e')
    private val functions = listOf("sin(", "cos(", "tan(", "lg(", "ln(", "√(", "arcsin(", "arccos(", "arctan(")

    private fun needsInternalMultiply(expr: String): Boolean {
        if (expr.isEmpty()) return false
        val last = expr.last()
        return last.isDigit() || last == ')' || last in constants || last == '!'
    }

    fun onNumberClick(number: String) {
        if (_state.value.isFinal) onClearClick()
        val expr = _state.value.expression
        
        // Исправление: если последним был факториал, добавляем знак умножения перед цифрой
        val prefix = if (expr.isNotEmpty() && (expr.last() == ')' || expr.last() in constants || expr.last() == '!')) "×" else ""
        val newExpr = expr + prefix + number

        val delimiters = (operators + '(' + ')').toCharArray()
        val lastPart = newExpr.split(*delimiters).last()
        
        if (lastPart.length <= 15) {
            _state.update { it.copy(expression = newExpr) }
            liveCalculate()
        }
    }

    fun onOperatorClick(op: String) {
        prepareForChaining()
        val expr = _state.value.expression
        if (expr == "Can't divide by zero" || expr == "Error") return

        if (op == "%") {
            if (expr.isNotEmpty() && (expr.last().isDigit() || expr.last() == ')' || expr.last() in constants)) {
                _state.update { it.copy(expression = expr + "/100") }
                liveCalculate()
                return
            }
        }
        if (expr.isNotEmpty() && expr.last() in operators) {
            if (expr.length > 1 && expr[expr.length - 2] == '(' && expr.last() == '-') {
                //skip
            } else {
                _state.update { it.copy(expression = expr.dropLast(1) + op) }
            }
        } else if (expr.isEmpty()) {
            if (op == "-") _state.update { it.copy(expression = "-") }
        } else if (expr.last() == '(') {
            if (op == "-") _state.update { it.copy(expression = expr + "-") }
        } else {
            _state.update { it.copy(expression = expr + op) }
        }
        liveCalculate()
    }

    fun onDecimalClick() {
        if (_state.value.isFinal) onClearClick()
        val expr = _state.value.expression

        if (expr.isEmpty() || expr.last() in operators || expr.last() == '(') {
            _state.update { it.copy(expression = expr + "0.") }
        } else if (expr.last() == ')' || expr.last() == '!') {
            // Исправление: при нажатии точки после закрывающей скобки или факториала добавляем умножение
            _state.update { it.copy(expression = expr + "×0.") }
        } else {
            val delimiters = (operators + '(' + ')').toCharArray()
            val lastPart = expr.split(*delimiters).last()
            if (!lastPart.contains(".")) {
                _state.update { it.copy(expression = expr + ".") }
            }
        }
        liveCalculate()
    }

    fun onFunctionClick(func: String) {
        if (_state.value.isFinal) prepareForChaining()
        val expr = _state.value.expression
        val prefix = if (needsInternalMultiply(expr)) "×" else ""
        val displayFunc = if (func == "sqrt") "√" else func
        _state.update { it.copy(expression = expr + prefix + "$displayFunc(") }
        liveCalculate()
    }

    fun onBracketClick(bracket: String) {
        if (_state.value.isFinal) prepareForChaining()
        val expr = _state.value.expression
        if (bracket == "(") {
            val prefix = if (needsInternalMultiply(expr)) "×" else ""
            _state.update { it.copy(expression = expr + prefix + "(") }
        } else {
            val openCount = expr.count { it == '(' }
            val closeCount = expr.count { it == ')' }
            if (openCount > closeCount && expr.isNotEmpty() && expr.last() !in operators && expr.last() != '(') {
                _state.update { it.copy(expression = expr + ")") }
            }
        }
        liveCalculate()
    }

    fun onConstantClick(c: String) {
        if (_state.value.isFinal) onClearClick()
        val expr = _state.value.expression
        val prefix = if (needsInternalMultiply(expr)) "×" else ""
        _state.update { it.copy(expression = expr + prefix + c) }
        liveCalculate()
    }

    fun onOneByXClick() {
        if (_state.value.expression.isEmpty()) return
        prepareForChaining()
        _state.update { it.copy(expression = it.expression + "^(-1)") }
        liveCalculate()
    }

    private fun liveCalculate() {
        val expr = _state.value.expression
        if (expr.isEmpty()) {
            _state.update { it.copy(result = "") }
            return
        }
        try {
            var tempExpr = expr
            val diff = tempExpr.count { it == '(' } - tempExpr.count { it == ')' }
            if (diff > 0) repeat(diff) { tempExpr += ")" }

            if (isDividingByZero(tempExpr)) {
                _state.update { it.copy(result = "Can't divide by zero") }
                return
            }

            val tokens = tokenizer.tokenize(tempExpr)
            val rpn = transformer.transform(tokens)
            val res = calculator.calculate(rpn)
            
            if (res.isInfinite()) {
                 _state.update { it.copy(result = "=∞") }
            } else if (res.isNaN()) {
                 _state.update { it.copy(result = "Error") }
            } else {
                _state.update { it.copy(result = "=" + formatValue(res)) }
            }
        } catch (e: Exception) {
        }
    }

    private fun isDividingByZero(expr: String): Boolean {
        val regex = Regex("/0(?![0-9.])")
        return regex.containsMatchIn(expr)
    }

    fun onEqualClick() {
        val currentResult = _state.value.result
        if (currentResult == "Can't divide by zero" || currentResult == "Error") {
            _state.update { it.copy(isFinal = true) }
            return
        }
        if (_state.value.expression.isEmpty()) return
        liveCalculate()
        _state.update { it.copy(isFinal = true) }
    }

    private fun prepareForChaining() {
        if (_state.value.isFinal) {
            val lastRes = _state.value.result.replace("=", "")
            if (lastRes == "Can't divide by zero" || lastRes == "Error" || lastRes == "∞") {
                onClearClick()
            } else {
                _state.update { it.copy(expression = lastRes, result = "", isFinal = false) }
            }
        }
    }

    private fun formatValue(d: Double): String {
        return when {
            d.isInfinite() -> "∞"
            d.isNaN() -> "Error"
            d >= 1_000_000_000.0 || d <= -1_000_000_000.0 -> {
                DecimalFormat("0.#######E0").apply {
                    val symbols = decimalFormatSymbols
                    symbols.exponentSeparator = "e"
                    decimalFormatSymbols = symbols
                }.format(d).replace(",", ".")
            }
            d % 1.0 == 0.0 -> d.toLong().toString()
            else -> {
                val formatted = String.format(Locale.US, "%.7f", d).trimEnd('0').trimEnd('.')
                formatted
            }
        }
    }

    fun onClearClick() { _state.update { CalculatorState() } }

    fun onBackspaceClick() {
        val currentExpr = _state.value.expression
        if (currentExpr.isEmpty()) return

        var found = false
        for (f in functions) {
            if (currentExpr.endsWith(f)) {
                _state.update { it.copy(expression = currentExpr.dropLast(f.length), isFinal = false) }
                found = true
                break
            }
        }

        if (!found) {
            _state.update { it.copy(expression = currentExpr.dropLast(1), isFinal = false) }
        }
        
        liveCalculate()
    }
}
