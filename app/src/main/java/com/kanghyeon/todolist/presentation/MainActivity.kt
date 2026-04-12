package com.kanghyeon.todolist.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.kanghyeon.todolist.presentation.screen.MainScreen
import com.kanghyeon.todolist.presentation.theme.TodoTheme
import com.kanghyeon.todolist.service.TodoForegroundService
import dagger.hilt.android.AndroidEntryPoint

/**
 * 앱의 단일 진입 Activity
 *
 * [역할]
 * 1. Hilt 진입점 (@AndroidEntryPoint)
 * 2. Android 13+ POST_NOTIFICATIONS 런타임 권한 요청
 * 3. TodoForegroundService 시작 (이미 실행 중이면 OS가 중복 시작을 무시)
 * 4. Compose 콘텐츠 설정
 *
 * [Edge-to-Edge]
 * enableEdgeToEdge()를 호출해 시스템 바 영역까지 앱이 그려지도록 설정.
 * Scaffold의 contentWindowInsets가 자동으로 패딩을 보정한다.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // ── 알림 권한 요청 런처 (Android 13+ / API 33) ─────────────
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // 권한 허용 여부와 관계없이 서비스는 시작
            // (권한 거부 시 알림이 표시되지 않지만 데이터는 정상 유지됨)
            startTodoService()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        setContent {
            TodoTheme {
                MainScreen()
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // 알림 권한
    // ──────────────────────────────────────────────────────────

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                // 이미 권한이 있으면 바로 서비스 시작
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED -> startTodoService()

                // 권한 요청 (시스템 다이얼로그 표시)
                else -> notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        } else {
            // API 33 미만: 권한 불필요, 바로 시작
            startTodoService()
        }
    }

    private fun startTodoService() {
        TodoForegroundService.start(this)
    }
}
