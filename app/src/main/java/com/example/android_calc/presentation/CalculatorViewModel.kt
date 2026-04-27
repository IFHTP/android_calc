package com.example.android_calc.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.android_calc.domain.*
import com.example.android_calc.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.DecimalFormat
import java.util.Locale

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HistoryRepository()
    private val securityManager = SecurityManager(application)
    private val tokenizer = Tokenizer()
    private val transformer = RpnTransformer()
    private val calculator = RpnCalculator()

    private val _state = MutableStateFlow(CalculationState(
        isAuthNeeded = securityManager.hasPassKey(),
        isSetupNeeded = !securityManager.hasPassKey()
    ))
    val state = _state.asStateFlow()

    private val operators = setOf('+', '-', '×', '÷', '%', '^')
    private val constants = setOf('π', 'e')
    private val functions = listOf("sin(", "cos(", "tan(", "lg(", "ln(", "√(", "arcsin(", "arccos(", "arctan(")

    fun onAuthInput(passKey: String) {
        val savedKey = securityManager.getPassKey()
        if (passKey == savedKey) {
            _state.update { it.copy(isAuthNeeded = false, authError = null) }
        } else {
            _state.update { it.copy(authError = "Incorrect Pass Key") }
        }
    }

    fun onSetupPassKey(passKey: String) {
        if (passKey.length >= 4) {
            securityManager.savePassKey(passKey)
            _state.update { it.copy(isSetupNeeded = false, isAuthNeeded = false, authError = null) }
        } else {
            _state.update { it.copy(authError = "Pass Key must be at least 4 digits") }
        }
    }

    fun onChangePassKey(oldKey: String, newKey: String) {
        val savedKey = securityManager.getPassKey()
        if (oldKey == savedKey) {
            if (newKey.length >= 4) {
                securityManager.savePassKey(newKey)
                _state.update { it.copy(authError = "Pass Key changed successfully") }
            } else {
                _state.update { it.copy(authError = "New Pass Key too short") }
            }
        } else {
            _state.update { it.copy(authError = "Old Pass Key incorrect") }
        }
    }

    fun resetPassKey() {
        securityManager.clearPassKey()
        _state.update { it.copy(isSetupNeeded = true, isAuthNeeded = false, authError = null) }
    }

    private fun needsInternalMultiply(expr: String): Boolean {
        if (expr.isEmpty()) return false
        val last = expr.last()
        return last.isDigit() || last == ')' || last in constants || last == '!'
    }

    fun onNumberClick(number: String) {
        if (_state.value.isFinal) onClearClick()
        val expr = _state.value.expression
        
        val prefix = if (expr.isNotEmpty() && (expr.last() == ')' || expr.last() in constants || expr.last() == '!')) "×" else ""
        val newExpr = expr + prefix + number

        val delimiters = (operators + '(' + ')').toCharArray()
        val lastPart = newExpr.split(*delimiters).last()
        
        val parts = lastPart.split('.')
        val wholePart = parts[0]
        val fractionalPart = if (parts.size > 1) parts[1] else ""
        
        if (wholePart.length <= 15 && (wholePart.length + fractionalPart.length) <= 19) {
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
            _state.update { it.copy(expression = expr + "×0.") }
        } else {
            val delimiters = (operators + '(' + ')').toCharArray()
            val lastPart = expr.split(*delimiters).last()
            if (!lastPart.contains(".")) {
                if (lastPart.length < 19) {
                    _state.update { it.copy(expression = expr + ".") }
                }
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
        val expr = _state.value.expression
        val currentResult = _state.value.result
        if (currentResult == "Can't divide by zero" || currentResult == "Error") {
            _state.update { it.copy(isFinal = true) }
            return
        }
        if (_state.value.expression.isEmpty()) return
        liveCalculate()
        val result = _state.value.result
        _state.update { it.copy(isFinal = true) }
        viewModelScope.launch {
            repository.saveCalculation(expr, result)
        }
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
        val absD = abs(d)
        if (d.isInfinite()) return "∞"
        if (d.isNaN()) return "Error"
        if (d == 0.0) return "0"

        if (absD >= 1e14 || (absD > 0 && absD < 1e-7)) {
            val df = DecimalFormat("0.#######E0").apply {
                val symbols = decimalFormatSymbols
                symbols.exponentSeparator = "e"
                decimalFormatSymbols = symbols
            }
            return df.format(d).replace(",", ".").lowercase()
        }

        if (d % 1.0 == 0.0) return d.toLong().toString()

        val wholePart = floor(absD).toLong()
        val wholePartLength = if (wholePart == 0L) 1 else log10(wholePart.toDouble()).toInt() + 1
        val maxFractionalDigits = (14 - wholePartLength).coerceIn(0, 7)

        return if (maxFractionalDigits <= 0) {
            wholePart.toString()
        } else {
            val pattern = "#." + "#".repeat(maxFractionalDigits)
            val df = DecimalFormat(pattern, java.text.DecimalFormatSymbols(Locale.US))
            df.format(d)
        }
    }

    fun onClearClick() { _state.update { it.copy(expression = "", result = "", isFinal = false) } }

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

    fun toggleHistory(visible: Boolean) {
        _state.update { it.copy(isHistoryVisible = visible) }
        if (visible) {
            loadHistory()
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val history = repository.getHistory()
            _state.update { it.copy(history = history) }
        }
    }
}
