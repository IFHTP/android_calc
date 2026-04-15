package com.example.android_calc.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// ТЕМНАЯ ТЕМА (Основная для калькулятора)
private val DarkColorScheme = darkColorScheme(
    primary = OrangePrimary,        // Оранжевые кнопки
    onPrimary = WhiteText,          // Белый текст на оранжевом

    secondary = ActionGray,         // Кнопки AC, ⌫
    onSecondary = BlackText,        // Черный текст на серых кнопках

    surface = DarkGraySurface,      // Кнопки цифр
    onSurface = WhiteText,          // Белый текст цифр

    background = DeepBlack,         // Весь фон экрана
    onBackground = WhiteText        // Цвет текста в поле ввода
)

// СВЕТЛАЯ ТЕМА
private val LightColorScheme = lightColorScheme(
    primary = OrangePrimary,        // Оранжевый остается для акцентов
    onPrimary = WhiteText,

    secondary = ActionGray,         // Спецкнопки
    onSecondary = WhiteText,

    surface = LightGraySurface,     // Светлые кнопки цифр
    onSurface = BlackText,          // Черный текст цифр

    background = OffWhiteBackground, // Светлый фон
    onBackground = BlackText         // Черный текст в поле ввода
)

@Composable
fun Android_calcTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Выключаем динамические цвета, чтобы сохранить наш оранжевый дизайн
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Если dynamicColor = true, Android перекрасит всё под обои телефона
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}