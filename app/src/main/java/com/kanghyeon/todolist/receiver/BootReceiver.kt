package com.kanghyeon.todolist.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kanghyeon.todolist.service.TodoForegroundService

/**
 * 기기 부팅 완료 시 TodoForegroundService를 자동으로 재시작하는 BroadcastReceiver.
 *
 * ── 수신 인텐트 ────────────────────────────────────────────────────
 * • BOOT_COMPLETED
 *   기기가 완전히 부팅되고 사용자가 화면을 잠금 해제한 이후에 발생.
 *   일반적인 부팅 완료 신호로, 자격 증명 암호화(CE) 스토리지에 접근 가능.
 *
 * • LOCKED_BOOT_COMPLETED  (Android 7.0+ / API 24+)
 *   사용자가 잠금 해제하기 전 직접 부팅(Direct Boot) 단계에서 발생.
 *   이 시점에는 기기 암호화(DE) 스토리지만 접근 가능 — Room DB(CE 스토리지)는
 *   잠금 해제 전까지 사용 불가하므로, 서비스를 시작해도 DB 읽기는 잠금 해제 후
 *   Flow가 방출될 때까지 대기하게 됨. 실질적인 알림 갱신은 BOOT_COMPLETED 이후.
 *
 * ── 방어 코드 ─────────────────────────────────────────────────────
 * startForegroundService() 는 백그라운드 제한 정책에 따라 일부 기기/OS 버전에서
 * IllegalStateException 을 던질 수 있다. try-catch 로 감싸 크래시를 방지한다.
 *
 * ── Manifest 등록 필수 항목 ────────────────────────────────────────
 * • <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
 * • <receiver android:exported="true"> + intent-filter 선언
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            ACTION_LOCKED_BOOT_COMPLETED -> {
                Log.d(TAG, "부팅 완료 감지 (action=${intent.action}), 서비스 시작 시도")
                startServiceSafely(context)
            }
            else -> Unit // 관심 없는 인텐트 무시
        }
    }

    private fun startServiceSafely(context: Context) {
        try {
            TodoForegroundService.start(context)
            Log.d(TAG, "TodoForegroundService 시작 성공")
        } catch (e: IllegalStateException) {
            // Android 8.0+: 백그라운드에서 ForegroundService 시작 불가 시 발생
            // 일부 OEM(삼성, 샤오미 등) 배터리 절약 정책으로도 발생 가능
            Log.e(TAG, "서비스 시작 실패 — 백그라운드 제한 가능성: ${e.message}")
        } catch (e: SecurityException) {
            // 권한 누락 또는 OEM 커스텀 제한
            Log.e(TAG, "서비스 시작 실패 — 권한 오류: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
        private const val ACTION_LOCKED_BOOT_COMPLETED =
            "android.intent.action.LOCKED_BOOT_COMPLETED"
    }
}
