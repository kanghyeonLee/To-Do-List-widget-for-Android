package com.kanghyeon.todolist.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.kanghyeon.todolist.data.repository.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * 할 일 목록 잠금화면 알림 Foreground Service
 *
 * [생명주기 설계]
 * ┌──────────────────────────────────────────────────────┐
 * │  App 시작 / 부팅                                      │
 * │    → startForegroundService() (BootReceiver)         │
 * │      → onCreate() → startForeground() (즉시 필수!)   │
 * │        → 할 일 Flow 수집 시작                         │
 * │          → 데이터 변경 시 알림 자동 갱신               │
 * │                                                      │
 * │  시스템에 의한 강제 종료 (메모리 부족 등)               │
 * │    → START_STICKY → 시스템이 서비스 재시작             │
 * │      → onStartCommand(intent=null) 처리 필요         │
 * │                                                      │
 * │  사용자 Force Stop → 재시작 없음 (OS 정책)             │
 * └──────────────────────────────────────────────────────┘
 *
 * [Android 14 (API 34) 주의사항]
 * - foregroundServiceType="dataSync"를 Manifest에 선언해야 한다.
 * - FOREGROUND_SERVICE_DATA_SYNC 권한도 함께 선언 필요.
 *
 * [Hilt in Service]
 * - @AndroidEntryPoint 선언 시 Hilt가 super.onCreate() 이후 @Inject 필드를 주입.
 * - onCreate()에서 @Inject 필드를 사용하려면 super.onCreate() 이후에 접근해야 한다.
 */
@AndroidEntryPoint
class TodoForegroundService : Service() {

    @Inject
    lateinit var repository: TaskRepository

    /**
     * 서비스 전용 코루틴 스코프.
     * SupervisorJob: 자식 코루틴 하나가 실패해도 나머지에 영향 없음.
     * onDestroy()에서 cancel() 호출 → 메모리 누수 방지.
     */
    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    // ──────────────────────────────────────────────────────
    // 생명주기
    // ──────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        // Hilt @Inject 필드는 super.onCreate() 이후에만 유효
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // ① startForeground()는 onStartCommand() 진입 후 5초 내 반드시 호출해야 함
        //    (미호출 시 ANR + ForegroundServiceDidNotStartInTimeException)
        startForeground(
            NotificationHelper.NOTIFICATION_ID,
            NotificationHelper.buildInitialNotification(this),
        )

        // ② 인텐트 액션 처리 (서비스 재시작 시 intent=null 가능)
        when (intent?.action) {
            TodoActions.ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        // ③ DB Flow 구독 → 변경 시 알림 자동 갱신
        observeAndUpdateNotification()

        // START_STICKY: 시스템이 서비스를 강제 종료했을 때 자동 재시작
        // (intent=null로 재시작되므로 ②에서 null 처리 필수)
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    /** Bound Service 미사용 → null 반환 */
    override fun onBind(intent: Intent?): IBinder? = null

    // ──────────────────────────────────────────────────────
    // 핵심: Flow 구독 → 알림 갱신
    // ──────────────────────────────────────────────────────

    /**
     * getLockScreenTasks()는 Room Flow이므로 DB가 바뀔 때마다 자동 방출.
     * 새 목록이 들어올 때마다 NotificationManager를 통해 알림을 갱신한다.
     *
     * catch: DB 오류가 발생해도 서비스가 죽지 않도록 예외를 잡아 처리.
     * launchIn(serviceScope): collect()와 동일하지만 반환값으로 Job을 받아 취소 가능.
     */
    private fun observeAndUpdateNotification() {
        repository.getLockScreenTasks()
            .onEach { tasks ->
                val notification = NotificationHelper.buildNotification(this, tasks)
                // notify()로 갱신 시 기존 알림을 교체 (같은 ID 사용)
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NotificationHelper.NOTIFICATION_ID, notification)
            }
            .catch { e ->
                // 오류 발생 시 에러 알림으로 교체 (서비스 자체는 유지)
                e.printStackTrace()
            }
            .launchIn(serviceScope)
    }

    // ──────────────────────────────────────────────────────
    // 컴패니언: 서비스 시작/종료 헬퍼
    // ──────────────────────────────────────────────────────

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, TodoForegroundService::class.java).apply {
                action = TodoActions.ACTION_START
            }
            // API 26+: 백그라운드에서 startService() 불가 → startForegroundService() 사용
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TodoForegroundService::class.java).apply {
                action = TodoActions.ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
