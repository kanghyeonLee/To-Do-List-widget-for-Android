package com.kanghyeon.todolist.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kanghyeon.todolist.service.TodoForegroundService

/**
 * 기기 부팅 완료 시 Foreground Service를 자동으로 시작하는 BroadcastReceiver.
 *
 * [수신하는 인텐트]
 * - BOOT_COMPLETED       : 기기 완전 부팅 후 발생
 * - LOCKED_BOOT_COMPLETED: 직접 부팅(Direct Boot) 모드 지원 (Android 7.0+)
 *   - 기기가 잠긴 상태에서도 앱이 알림을 띄울 수 있게 하려면
 *     directBootAware="true" 와 함께 사용
 *
 * [Hilt 미사용 이유]
 * BootReceiver는 @Inject 없이 단순히 서비스를 시작하는 역할만 하므로
 * @AndroidEntryPoint 없이 일반 BroadcastReceiver로 충분하다.
 * (Repository를 직접 호출할 필요가 없음)
 *
 * [Manifest 등록 필수]
 * <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
 * <receiver android:exported="true"> 와 intent-filter를 반드시 선언해야 한다.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.LOCKED_BOOT_COMPLETED"
        ) {
            TodoForegroundService.start(context)
        }
    }
}
