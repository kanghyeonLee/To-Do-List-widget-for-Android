package com.kanghyeon.todolist.service

import android.app.NotificationManager
import android.app.Service
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
 * 할 일 목록 상태바 지속 Foreground Service
 *
 * [역할]
 * - Room DB Flow 구독 → 미완료 Task 목록이 바뀔 때마다 상태바 알림을 자동 갱신
 * - 앱이 백그라운드에 있어도 알림이 최신 상태를 유지
 *
 * [잠금화면 알림 (Live Information)]
 * - 마감 알림은 AlarmManager → TodoAlarmReceiver → TodoLiveService 흐름으로 처리
 * - 이 서비스는 상태바 상시 알림(CHANNEL_ID) 만 담당
 */
@AndroidEntryPoint
class TodoForegroundService : Service() {

    @Inject
    lateinit var repository: TaskRepository

    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            NotificationHelper.NOTIFICATION_ID,
            NotificationHelper.buildInitialNotification(this),
        )

        when (intent?.action) {
            TodoActions.ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        observeAndUpdateNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun observeAndUpdateNotification() {
        repository.getLockScreenTasks()
            .onEach { tasks ->
                val notification = NotificationHelper.buildNotification(this, tasks)
                getSystemService(NotificationManager::class.java)
                    .notify(NotificationHelper.NOTIFICATION_ID, notification)
            }
            .catch { e -> e.printStackTrace() }
            .launchIn(serviceScope)
    }

    companion object {
        fun start(context: android.content.Context) {
            val intent = Intent(context, TodoForegroundService::class.java).apply {
                action = TodoActions.ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stop(context: android.content.Context) {
            val intent = Intent(context, TodoForegroundService::class.java).apply {
                action = TodoActions.ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
