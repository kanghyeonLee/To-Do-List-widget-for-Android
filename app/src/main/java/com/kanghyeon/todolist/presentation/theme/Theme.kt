package com.kanghyeon.todolist.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// ── 라이트 컬러 스킴 ─────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary             = AppIndigo,           // #4F46E5
    onPrimary           = Color.White,
    primaryContainer    = AppIndigoContainer,  // #EEF2FF
    onPrimaryContainer  = OnAppIndigoContainer,
    secondary           = AppIndigoLight,      // #818CF8
    onSecondary         = Color.White,
    surface             = AppSurface,          // #FFFFFF
    onSurface           = AppOnSurface,        // #111827 near-black
    background          = AppBackground,       // #F2F2F7 쿨 그레이
    onBackground        = AppOnSurface,
    outline             = AppOutline,          // #E5E5EA
    surfaceVariant      = Color(0xFFF9FAFB),   // 아주 연한 회색 (카드 배경 변형)
    onSurfaceVariant    = AppSubText,          // #6B7280 중간 회색
    error               = Color(0xFFEF4444),
    onError             = Color.White,
)

// ── 다크 컬러 스킴 ───────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary             = AppIndigoLight,
    onPrimary           = AppIndigoDark,
    primaryContainer    = AppIndigoDark,
    onPrimaryContainer  = AppIndigoContainer,
    secondary           = AppIndigo,
    surface             = Color(0xFF1C1C1E),
    onSurface           = Color(0xFFF9FAFB),
    background          = Color(0xFF000000),
    onBackground        = Color(0xFFF9FAFB),
    outline             = Color(0xFF374151),
    surfaceVariant      = Color(0xFF1F2937),
    onSurfaceVariant    = Color(0xFF9CA3AF),
    error               = Color(0xFFF87171),
)

// ── Shape 시스템 ─────────────────────────────────────────────────
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(20.dp),   // 카드·시트 기본
    extraLarge = RoundedCornerShape(24.dp),
)

/**
 * 앱 전체 테마
 *
 * [디자인 원칙]
 * - Primary: Deep Indigo (#4F46E5) — 무게감·신뢰감
 * - 배경: #F2F2F7 쿨 그레이, 카드: #FFFFFF 순백
 * - OnSurfaceVariant: #6B7280 — 서브텍스트 존재감 최소화
 * - Elevation 대신 미세한 outline 또는 shadow-free flat 디자인
 * - Pretendard + 강한 Typography 계층으로 프리미엄감 확보
 */
@Composable
fun TodoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        shapes      = AppShapes,
        content     = content,
    )
}
