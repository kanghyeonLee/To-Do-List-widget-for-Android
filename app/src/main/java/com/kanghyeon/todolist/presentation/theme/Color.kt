package com.kanghyeon.todolist.presentation.theme

import androidx.compose.ui.graphics.Color

// ── Primary — 딥 인디고 (Deep Indigo) ─────────────────────────────
// 하늘색 대신 무게감·신뢰감을 주는 인디고 컬러
val AppIndigo            = Color(0xFF4F46E5)   // Primary
val AppIndigoDark        = Color(0xFF3730A3)   // 다크모드 / pressed
val AppIndigoLight       = Color(0xFF818CF8)   // 보조 강조
val AppIndigoContainer   = Color(0xFFEEF2FF)   // Primary Container (아주 연한 인디고)
val OnAppIndigoContainer = Color(0xFF312E81)   // Container 위 텍스트

// ── Surface / Background ───────────────────────────────────────────
val AppSurface     = Color(0xFFFFFFFF)   // 순백
val AppBackground  = Color(0xFFF2F2F7)   // 아주 연한 쿨 그레이
val AppOnSurface   = Color(0xFF111827)   // Near-Black (다크 네이비)
val AppOutline     = Color(0xFFE5E5EA)   // 아주 연한 테두리 선

// ── 서브 텍스트 ────────────────────────────────────────────────────
val AppSubText = Color(0xFF6B7280)   // onSurfaceVariant — 중간 회색, 존재감 낮춤

// ── 우선순위 색상 (Compose에서 사용) ──────────────────────────────
val PriorityHigh   = Color(0xFFEF4444)   // Red 500 — 선명하게 조정
val PriorityMedium = Color(0xFFF59E0B)   // Amber 500
val PriorityLow    = Color(0xFF9CA3AF)   // Gray 400

// ── 우선순위 파스텔 배경 (카드 섹션 배경에 사용) ──────────────────
val PriorityHighContainer    = Color(0x1AEF4444)
val PriorityMediumContainer  = Color(0x1AF59E0B)
val PriorityLowContainer     = Color(0x0F9CA3AF)

// ── 우선순위 강조 텍스트 ──────────────────────────────────────────
val PriorityHighDark   = Color(0xFFDC2626)   // Red 600
val PriorityMediumDark = Color(0xFFD97706)   // Amber 600
val PriorityLowDark    = Color(0xFF6B7280)   // Gray 500

// ── 기한 초과 ─────────────────────────────────────────────────────
val OverdueRed          = Color(0xFFEF4444)
val OverdueRedContainer = Color(0xFFFEF2F2)

// ── 삭제 스와이프 배경 ────────────────────────────────────────────
val SwipeDeleteBackground = Color(0xFFEF4444)
