package com.example.android_calc.presentation

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.android_calc.ui.theme.Android_calcTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Android_calcTheme {
                val viewModel: CalculatorViewModel = viewModel()
                CalculatorScreen(viewModel)
            }
        }
    }
}

@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val exprFontSize = if (isLandscape) 20.sp else (if (state.isFinal) 24.sp else 36.sp)
    val resFontSize = if (isLandscape) 28.sp else (if (state.isFinal) 42.sp else 28.sp)
    val resColor = if (state.isFinal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    val formattedExpression = formatExpression(state.expression)
    val scrollState = rememberScrollState()

    LaunchedEffect(formattedExpression) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(scrollState),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Text(
                            text = formattedExpression,
                            fontSize = exprFontSize,
                            lineHeight = exprFontSize * 1.1,
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Light,
                            modifier = Modifier.fillMaxWidth(),
                            softWrap = false
                        )
                    }

                    Text(
                        text = if (state.result.isEmpty()) "0" else state.result,
                        fontSize = resFontSize,
                        color = resColor,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 4.dp)
                            .fillMaxWidth(),
                        fontWeight = if (state.isFinal) FontWeight.Bold else FontWeight.Normal,
                        softWrap = true
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight()
                ) {
                    CalculatorKeyboard(viewModel)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(scrollState),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Text(
                            text = formattedExpression,
                            fontSize = exprFontSize,
                            lineHeight = exprFontSize * 1.1,
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Light,
                            modifier = Modifier.fillMaxWidth(),
                            softWrap = false
                        )
                    }

                    Text(
                        text = if (state.result.isEmpty()) "0" else state.result,
                        fontSize = resFontSize,
                        color = resColor,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 4.dp)
                            .fillMaxWidth(),
                        fontWeight = if (state.isFinal) FontWeight.Bold else FontWeight.Normal,
                        softWrap = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.weight(2f)) {
                    CalculatorKeyboard(viewModel)
                }
            }
        }
    }
}

@Composable
fun CalculatorKeyboard(viewModel: CalculatorViewModel) {
    val buttons = listOf(
        listOf("sin", "cos", "tan", "√"),
        listOf("lg", "ln", "(", ")"),
        listOf("π", "e", "xʸ", "x!"),
        listOf("AC", "⌫", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "1/x", "=")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier.fillMaxHeight()
    ) {
        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                row.forEach { symbol ->
                    CalcButton(
                        symbol = symbol,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = {
                            when (symbol) {
                                "AC" -> viewModel.onClearClick()
                                "⌫" -> viewModel.onBackspaceClick()
                                "=" -> viewModel.onEqualClick()
                                "." -> viewModel.onDecimalClick()
                                "(", ")" -> viewModel.onBracketClick(symbol)
                                "π", "e" -> viewModel.onConstantClick(symbol)
                                "sin", "cos", "tan", "lg", "ln" -> viewModel.onFunctionClick(symbol)
                                "√" -> viewModel.onFunctionClick("sqrt")
                                "xʸ" -> viewModel.onOperatorClick("^")
                                "x!" -> viewModel.onOperatorClick("!")
                                "1/x" -> viewModel.onOneByXClick()
                                in "0123456789" -> viewModel.onNumberClick(symbol)
                                else -> viewModel.onOperatorClick(symbol)
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun formatExpression(expression: String): String {
    if (expression.isEmpty()) return ""
    val sb = StringBuilder()
    val lineLimit = 15
    val operators = setOf('+', '-', '×', '÷', '^', '*', '/')
    val tokens = mutableListOf<String>()
    var currentToken = ""
    for (char in expression) {
        if (char in operators || char == '(' || char == ')') {
            if (currentToken.isNotEmpty()) tokens.add(currentToken)
            tokens.add(char.toString())
            currentToken = ""
        } else {
            currentToken += char
        }
    }
    if (currentToken.isNotEmpty()) tokens.add(currentToken)
    var currentLineLength = 0
    for (i in tokens.indices) {
        val token = tokens[i]
        if (token.length == 1 && token[0] in operators) {
            val nextNumber = if (i + 1 < tokens.size) tokens[i + 1] else ""
            val totalNeeded = 1 + nextNumber.length
            if (currentLineLength + totalNeeded > lineLimit && currentLineLength > 0) {
                sb.append("\n")
                currentLineLength = 0
            }
        }
        if (token.length > lineLimit) {
            token.chunked(lineLimit).forEachIndexed { index, part ->
                sb.append(part)
                if (index < token.chunked(lineLimit).size - 1) {
                    sb.append("\n")
                    currentLineLength = part.length
                } else {
                    currentLineLength = part.length
                }
            }
        } else {
            sb.append(token)
            currentLineLength += token.length
        }
    }
    return sb.toString()
}

@Composable
fun CalcButton(
    symbol: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val isOrangeText = symbol.lowercase() in listOf("ac", "%", "÷", "×", "-", "+", "=", "⌫")
    val contentColor = when {
        isOrangeText -> MaterialTheme.colorScheme.primary 
        isDark -> Color.Gray 
        else -> Color.Black 
    }

    Button(
        onClick = {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                vibrator.vibrate(100)
            }

            onClick()
        },
        modifier = modifier,
        shape = RoundedCornerShape(0.dp), 
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(0.dp),
        elevation = null
    ) {
        Text(
            text = symbol,
            fontSize = 20.sp,
            fontWeight = if (isOrangeText) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
