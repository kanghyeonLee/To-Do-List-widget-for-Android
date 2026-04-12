package com.kanghyeon.todolist.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary          = TodoGreen,
    onPrimary        = androidx.compose.ui.graphics.Color.White,
    primaryContainer = TodoGreenContainer,
    onPrimaryContainer = OnTodoGreenContainer,
    secondary        = TodoGreenLight,
    surface          = TodoSurface,
    background       = TodoBackground,
)

private val DarkColorScheme = darkColorScheme(
    primary          = TodoGreenLight,
    onPrimary        = TodoGreenDark,
    primaryContainer = TodoGreenDark,
    onPrimaryContainer = TodoGreenContainer,
    secondary        = TodoGreen,
)

/**
 * 앱 전체에 적용되는 Material3 테마
 *
 * - Android 12+(API 31) 이상 기기에서는 Dynamic Color(월페이퍼 기반)를 사용
 * - 하위 기기는 TodoGreen 시드 색상 기반 커스텀 팔레트 적용
 * - 상태바 색상을 투명으로 처리(edge-to-edge)
 */
@Composable
fun TodoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    // Edge-to-Edge: 상태바 아이콘 색상을 테마에 맞춰 조정
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
