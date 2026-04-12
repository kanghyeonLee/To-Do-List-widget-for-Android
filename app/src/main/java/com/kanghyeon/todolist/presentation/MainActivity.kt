package com.kanghyeon.todolist.presentation

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kanghyeon.todolist.R
import com.kanghyeon.todolist.presentation.screen.MainScreen
import com.kanghyeon.todolist.presentation.theme.TodoTheme
import com.kanghyeon.todolist.service.TodoForegroundService
import dagger.hilt.android.AndroidEntryPoint

/**
 * 앱의 단일 진입 Activity
 *
 * ── 처리 순서 (onCreate) ──────────────────────────────────────────
 * 1. enableEdgeToEdge()         — 시스템 바 영역까지 Compose가 그리도록 확장
 * 2. 알림 권한 요청             — Android 13+ POST_NOTIFICATIONS 런타임 권한
 * 3. Compose setContent 진입
 *    └─ 배터리 최적화 예외 다이얼로그 (LaunchedEffect + repeatOnLifecycle)
 *    └─ MainScreen
 *
 * ── 배터리 최적화 예외 처리 (Doze 방어) ─────────────────────────────
 * Android의 Doze 모드는 화면 꺼짐 + 충전 미연결 상태에서 네트워크·알람·서비스를
 * 주기적으로 차단한다. Foreground Service는 Doze에서도 살아남지만,
 * 일부 제조사(삼성 One UI, MIUI 등) 커스텀 배터리 정책은 이를 무시하고 강제 종료한다.
 *
 * 해결책: 사용자가 직접 '배터리 사용량 최적화 무시' 목록에 앱을 추가하도록 안내.
 *
 * ── 재확인 로직 (onResume 기반) ──────────────────────────────────
 * 사용자가 시스템 설정에서 돌아왔을 때 예외 등록 여부를 다시 확인하기 위해
 * LaunchedEffect + repeatOnLifecycle(RESUMED)을 사용한다.
 * RESUMED 진입마다 isIgnoringBatteryOptimizations() 를 재확인 → 이미 허용됐으면
 * 다이얼로그를 자동으로 숨긴다.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    // ── 알림 권한 런처 ────────────────────────────────────────────
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            Log.d(TAG, "POST_NOTIFICATIONS 결과: isGranted=$isGranted")
            startTodoService()
        }

    // ─────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        setContent {
            TodoTheme {
                // ── 배터리 최적화 다이얼로그 상태 ──────────────
                // remember 초기값: 앱 시작 시점에 이미 예외 처리됐으면 false
                var showBatteryDialog by remember {
                    mutableStateOf(shouldRequestBatteryOptimization())
                }

                // ── RESUMED 때마다 재확인 ─────────────────────
                // 사용자가 설정 화면에서 돌아오면 RESUMED가 다시 발생하고
                // isIgnoringBatteryOptimizations()를 재평가해 다이얼로그를 자동 숨김
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                LaunchedEffect(lifecycleOwner) {
                    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                        showBatteryDialog = shouldRequestBatteryOptimization()
                    }
                }

                // ── 다이얼로그 표시 ───────────────────────────
                if (showBatteryDialog) {
                    BatteryOptimizationDialog(
                        onConfirm = {
                            requestBatteryOptimizationExemption()
                            showBatteryDialog = false   // 설정 앱으로 넘어가므로 일단 숨김
                                                       // onResume 재확인에서 실제 결과 반영
                        },
                        onDismiss = {
                            showBatteryDialog = false   // 사용자가 "나중에" 선택
                        },
                    )
                }

                MainScreen()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 알림 권한
    // ─────────────────────────────────────────────────────────────

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

            if (granted) {
                startTodoService()
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            startTodoService()
        }
    }

    private fun startTodoService() {
        TodoForegroundService.start(this)
    }

    // ─────────────────────────────────────────────────────────────
    // 배터리 최적화 예외 처리
    // ─────────────────────────────────────────────────────────────

    /**
     * 앱이 배터리 최적화 예외 목록에 없으면 true를 반환한다.
     * API 23(Marshmallow) 미만에서는 Doze 모드 자체가 없으므로 항상 false.
     */
    private fun shouldRequestBatteryOptimization(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        val pm = getSystemService(PowerManager::class.java) ?: return false
        return !pm.isIgnoringBatteryOptimizations(packageName)
    }

    /**
     * 시스템 다이얼로그를 직접 띄워 배터리 최적화 예외를 요청한다.
     *
     * ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS:
     *   "앱 이름이(가) 항상 백그라운드에서 실행되도록 허용하시겠습니까?" 시스템 팝업.
     *   data=Uri("package:패키지명") 이 필수.
     *
     * Fallback (ActivityNotFoundException):
     *   일부 기기(특히 중국산 OEM)에서는 이 인텐트를 처리하는 액티비티가 없을 수 있다.
     *   이때 배터리 최적화 전체 목록 화면(ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)으로
     *   이동해 수동 설정을 유도한다.
     *
     * [Google Play 정책 주의]
     * REQUEST_IGNORE_BATTERY_OPTIMIZATIONS 는 제한된 권한(restricted permission)이다.
     * Play Store에 배포 시 '배터리 최적화가 앱 핵심 기능에 반드시 필요한 이유'를
     * 선언(declaration)으로 제출해야 한다. (내부/엔터프라이즈 배포는 무관)
     */
    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        val directIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

        try {
            startActivity(directIntent)
            Log.d(TAG, "배터리 최적화 예외 요청 다이얼로그 표시")
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "직접 요청 불가, 설정 목록으로 이동: ${e.message}")
            try {
                startActivity(fallbackIntent)
            } catch (e2: ActivityNotFoundException) {
                Log.e(TAG, "배터리 설정 화면도 열 수 없음: ${e2.message}")
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────
// 배터리 최적화 예외 요청 다이얼로그
// ──────────────────────────────────────────────────────────────────

/**
 * Doze 모드에 의해 서비스가 중단될 수 있음을 설명하고 설정 이동을 유도하는 AlertDialog.
 *
 * [UX 설계 원칙]
 * • 첫 실행 시 한 번만 표시 (onResume 재확인 후 이미 허용됐으면 자동 숨김)
 * • "나중에" 버튼으로 강제 없이 거절 가능 — 권한 강요 UX 금지
 * • 설정으로 이동 후 돌아오면 자동으로 다이얼로그 재평가
 */
@Composable
private fun BatteryOptimizationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.BatteryAlert,
                contentDescription = null,
            )
        },
        title = {
            Text(text = "배터리 최적화 해제 필요")
        },
        text = {
            Text(
                text = "안드로이드의 Doze 모드나 제조사 배터리 정책으로 인해 " +
                       "잠금화면 알림이 중단될 수 있습니다.\n\n" +
                       "배터리 최적화 예외 목록에 이 앱을 추가하면 " +
                       "재부팅 후에도 알림이 안정적으로 유지됩니다.",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "지금 설정")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "나중에")
            }
        },
    )
}
