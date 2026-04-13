package com.kanghyeon.todolist.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kanghyeon.todolist.R

// ── Pretendard FontFamily ──────────────────────────────────────────
val PretendardFamily = FontFamily(
    Font(R.font.pretendard_thin,       FontWeight.Thin),
    Font(R.font.pretendard_extralight, FontWeight.ExtraLight),
    Font(R.font.pretendard_light,      FontWeight.Light),
    Font(R.font.pretendard_regular,    FontWeight.Normal),
    Font(R.font.pretendard_medium,     FontWeight.Medium),
    Font(R.font.pretendard_semibold,   FontWeight.SemiBold),
    Font(R.font.pretendard_bold,       FontWeight.Bold),
    Font(R.font.pretendard_extrabold,  FontWeight.ExtraBold),
    Font(R.font.pretendard_black,      FontWeight.Black),
)

// ── Material 3 Typography — 모던 미니멀 자간·굵기 조정 ──────────────
/**
 * [설계 원칙]
 * - 모든 스타일에 Pretendard 적용 → 앱 전체 폰트 통일감
 * - letterSpacing: Display/Title은 음수 자간(-0.02em)으로 압축감 → 모던한 느낌
 * - Body/Label은 0 또는 +0.01em → 가독성 우선
 * - lineHeight: 1.4~1.6 ratio → 한국어 가독성 최적화
 */
val AppTypography = Typography(

    // ── Display ─────────────────────────────────────────────────────
    displayLarge = TextStyle(
        fontFamily    = PretendardFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 57.sp,
        lineHeight    = 64.sp,
        letterSpacing = (-0.02).em,
    ),
    displayMedium = TextStyle(
        fontFamily    = PretendardFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 45.sp,
        lineHeight    = 52.sp,
        letterSpacing = (-0.02).em,
    ),
    displaySmall = TextStyle(
        fontFamily    = PretendardFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 36.sp,
        lineHeight    = 44.sp,
        letterSpacing = (-0.01).em,
    ),

    // ── Headline ─────────────────────────────────────────────────────
    headlineLarge = TextStyle(
        fontFamily    = PretendardFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 32.sp,
        lineHeight    = 40.sp,
        letterSpacing = (-0.01).em,
    ),
    headlineMedium = TextStyle(
        fontFamily    = PretendardFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 28.sp,
        lineHeight    = 36.sp,
        letterSpacing = (-0.01).em,
    ),
    headlineSmall = TextStyle(
        fontFamily    = PretendardFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 24.sp,
        lineHeight    = 32.sp,
        letterSpacing = (-0.005).em,
    ),

    // ── Title ────────────────────────────────────────────────────────
    titleLarge = TextStyle(
        fontFamily    = PretendardFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 22.sp,
        lineHeight    = 28.sp,
        letterSpacing = (-0.005).em,
    ),
    titleMedium = TextStyle(
        fontFamily    = PretendardFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.em,
    ),
    titleSmall = TextStyle(
        fontFamily    = PretendardFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.em,
    ),

    // ── Body ─────────────────────────────────────────────────────────
    bodyLarge = TextStyle(
        fontFamily    = PretendardFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.em,
    ),
    bodyMedium = TextStyle(
        fontFamily    = PretendardFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.em,
    ),
    bodySmall = TextStyle(
        fontFamily    = PretendardFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.01.em,
    ),

    // ── Label ────────────────────────────────────────────────────────
    labelLarge = TextStyle(
        fontFamily    = PretendardFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.em,
    ),
    labelMedium = TextStyle(
        fontFamily    = PretendardFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.01.em,
    ),
    labelSmall = TextStyle(
        fontFamily    = PretendardFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 11.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.01.em,
    ),
)
