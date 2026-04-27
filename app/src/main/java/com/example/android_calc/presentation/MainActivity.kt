package com.example.android_calc.presentation

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
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
import androidx.core.app.NotificationCompat
import androidx.core.view.WindowCompat
import com.example.android_calc.data.HistoryItem
import com.example.android_calc.data.SecurityManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        fetchFcmToken()

        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

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

    fun fetchFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_DEBUG", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result

            Log.d("FCM_DEBUG", "Current FCM Token: $token")
        }
    }

    fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Auth error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Auth failed", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verify Identity")
            .setSubtitle("Authenticate using your device security to reset Pass Key")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
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
    val context = LocalContext.current as MainActivity
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var showMenu by remember { mutableStateOf(false) }
    var changePassKeyStep by remember { mutableStateOf(0) }
    var confirmedOldKey by remember { mutableStateOf("") }
    var localAuthError by remember { mutableStateOf<String?>(null) }

    if (state.isSetupNeeded) {
        AuthDialog(
            title = "Setup Pass Key",
            buttonText = "Set Pass Key",
            error = state.authError,
            onConfirm = { viewModel.onSetupPassKey(it) },
            showForgot = false
        )
    } else if (state.isAuthNeeded) {
        AuthDialog(
            title = "Enter Pass Key",
            buttonText = "Login",
            error = state.authError,
            onConfirm = { viewModel.onAuthInput(it) },
            showForgot = true,
            onForgot = {
                context.showBiometricPrompt {
                    viewModel.resetPassKey()
                }
            }
        )
    }

    if (changePassKeyStep == 1) {
        AuthDialog(
            title = "Confirm Old Pass Key",
            buttonText = "Next",
            error = localAuthError,
            onConfirm = { input ->
                val securityManager = SecurityManager(context)
                if (input == securityManager.getPassKey()) {
                    confirmedOldKey = input
                    localAuthError = null
                    changePassKeyStep = 2
                } else {
                    localAuthError = "Incorrect Old Pass Key"
                }
            },
            showForgot = true,
            onForgot = {
                context.showBiometricPrompt {
                    changePassKeyStep = 0
                    viewModel.resetPassKey()
                }
            },
            onDismiss = { changePassKeyStep = 0; localAuthError = null }
        )
    } else if (changePassKeyStep == 2) {
        AuthDialog(
            title = "Enter New Pass Key",
            buttonText = "Change",
            error = state.authError,
            onConfirm = { newKey ->
                viewModel.onChangePassKey(confirmedOldKey, newKey)
                if (state.authError?.contains("successfully") == true) {
                    changePassKeyStep = 0
                    confirmedOldKey = ""
                }
            },
            showForgot = false,
            onDismiss = { changePassKeyStep = 0 }
        )
    }

    Scaffold(
        topBar = {
            if (!state.isHistoryVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
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
                        DropdownMenuItem(
                            text = { Text("Change Pass Key") },
                            onClick = {
                                showMenu = false
                                changePassKeyStep = 1
                            }
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (state.isHistoryVisible) PaddingValues(0.dp) else paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            if (state.isHistoryVisible) {
                HistoryView(
                    history = state.history,
                    onBack = { viewModel.toggleHistory(false) }
                )
            } else {
                val systemInsets = WindowInsets.systemBars.asPaddingValues()
                val displayCutoutPadding = WindowInsets.displayCutout.asPaddingValues()
                
                val exprFontSize = if (isLandscape) 15.sp else (if (state.isFinal) 19.sp else 31.sp)
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
                            .padding(systemInsets)
                            .padding(displayCutoutPadding)
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
                            .padding(systemInsets)
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
fun AuthDialog(
    title: String,
    buttonText: String,
    error: String?,
    onConfirm: (String) -> Unit,
    showForgot: Boolean = false,
    onForgot: () -> Unit = {},
    onDismiss: (() -> Unit)? = null
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { onDismiss?.invoke() },
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Pass Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (error != null) {
                    Text(error, color = if (error.contains("success")) Color.Green else Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
                if (showForgot) {
                    TextButton(
                        onClick = onForgot,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Forgot Pass Key?", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }) {
                Text(buttonText)
            }
        },
        dismissButton = if (onDismiss != null) {
            {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        } else null
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryView(history: List<HistoryItem>, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {
            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No history yet")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(history) { item ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(item.timestamp))
                            Text(date, fontSize = 12.sp, color = Color.Gray)
                            Text(item.expression, fontSize = 18.sp)
                            Text(item.result, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
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
    val lineLimit = 20
    val operators = setOf('+', '-', '×', '÷', '^', '*', '/')
    val tokens = mutableListOf<String>()
    
    var i = 0
    while (i < expression.length) {
        val char = expression[i]
        when {
            char in operators || char == '(' || char == ')' -> {
                tokens.add(char.toString())
                i++
            }
            char.isLetter() || char == '√' -> {
                val start = i
                while (i < expression.length && (expression[i].isLetter() || expression[i] == '(' || expression[i] == '√')) {
                    val current = expression[i]
                    i++
                    if (current == '(') break
                }
                tokens.add(expression.substring(start, i))
            }
            else -> {
                val start = i
                while (i < expression.length && (expression[i].isDigit() || expression[i] == '.' || expression[i] == '!')) {
                    i++
                }
                tokens.add(expression.substring(start, i))
            }
        }
    }

    var currentLineLength = 0
    for (token in tokens) {
        val tokenLength = token.length
        
        if (currentLineLength + tokenLength > lineLimit && currentLineLength > 0) {
            sb.append("\n")
            currentLineLength = 0
        }

        if (tokenLength > lineLimit) {
            token.chunked(lineLimit).forEachIndexed { index, part ->
                sb.append(part)
                if (index < (tokenLength - 1) / lineLimit) {
                    sb.append("\n")
                    currentLineLength = 0
                } else {
                    currentLineLength = part.length
                }
            }
        } else {
            sb.append(token)
            currentLineLength += tokenLength
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
    
    val isOperatorSymbol = symbol in listOf("÷", "×", "-", "+", "=")
    val fontSize = if (isOperatorSymbol) 26.sp else 20.sp

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
            fontSize = fontSize,
            fontWeight = if (isOrangeText) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
