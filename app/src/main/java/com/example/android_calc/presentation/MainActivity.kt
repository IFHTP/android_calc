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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import androidx.core.graphics.toColorInt

import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.activity.enableEdgeToEdge
import androidx.core.app.NotificationCompat
import com.example.android_calc.data.HistoryItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.app.ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        }

        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val colorHex = remoteConfig.getString("status_bar_color")
                applyStatusBarColor(colorHex)
            }
        }
        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                if (configUpdate.updatedKeys.contains("status_bar_color")) {
                    remoteConfig.activate().addOnCompleteListener {
                        applyStatusBarColor(remoteConfig.getString("status_bar_color"))
                        sendLocalNotification(remoteConfig.getString("status_bar_color"))
                    }
                }
            }
            override fun onError(error: FirebaseRemoteConfigException) {
                android.util.Log.e("Firebase", "Update error", error)
            }
        })

        setContent {
            Android_calcTheme {
                val viewModel: CalculatorViewModel = viewModel()
                CalculatorScreen(viewModel)
            }
        }
    }

    private fun applyStatusBarColor(colorHex: String) {
        if (colorHex.isEmpty()) return
        try {
            val colorInt = colorHex.toColorInt()
            window.statusBarColor = colorInt

            val isLightColor = androidx.core.graphics.ColorUtils.calculateLuminance(colorInt) > 0.5
            val view = window.decorView
            androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = isLightColor
            }
        } catch (e: Exception) {
        }
    }

    private fun sendLocalNotification(newColor: String) {
        val channelId = "color_updates"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Updates", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Bar color changed to: $newColor")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        manager.notify(1, notification)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("History") },
                            onClick = {
                                showMenu = false
                                viewModel.toggleHistory(true)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            if (state.isHistoryVisible) {
                HistoryView(
                    history = state.history,
                    onBack = { viewModel.toggleHistory(false) }
                )
            } else {
                val exprFontSize = if (isLandscape) 20.sp else (if (state.isFinal) 24.sp else 36.sp)
                val resFontSize = if (isLandscape) 28.sp else (if (state.isFinal) 42.sp else 28.sp)
                val resColor = if (state.isFinal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                val formattedExpression = formatExpression(state.expression)
                val scrollState = rememberScrollState()

                LaunchedEffect(formattedExpression) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }

                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
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
    }
}

@Composable
fun HistoryView(history: List<HistoryItem>, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("History", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onBack) {
                Text("Back")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No history yet")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(item.timestamp))
                            Text(date, fontSize = 12.sp, color = Color.Gray)
                            Text(item.expression, fontSize = 18.sp)
                            Text(item.result, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
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
