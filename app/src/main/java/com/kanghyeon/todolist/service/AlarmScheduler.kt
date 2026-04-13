package com.kanghyeon.todolist.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.kanghyeon.todolist.data.local.entity.TaskEntity
import com.kanghyeon.todolist.receiver.TodoAlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AlarmManager 기반 알림 예약/취소 헬퍼
 *
 * [설계 결정]
 * - setExactAndAllowWhileIdle(): Doze 모드에서도 정시에 발화
 * - API 31+ canScheduleExactAlarms() 체크: 권한 없으면 예약 생략
 * - requestCode = taskId.toInt(): 동일 Task의 알람을 PendingIntent로 식별
 */
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /**
     * 알람 예약.
     * 다음 조건 중 하나라도 해당하면 예약하지 않는다:
     * - task.dueDate == null
     * - task.reminderMinutes == null
     * - task.isDone == true
     * - triggerMs <= now (이미 지난 시각)
     * - API 31+ 이고 SCHEDULE_EXACT_ALARM 권한 없음
     */
    fun schedule(task: TaskEntity) {
        val dueDate = task.dueDate ?: return
        val minutes = task.reminderMinutes ?: return
        if (task.isDone) return

        if (dueDate < System.currentTimeMillis() + 5 * 60_000L) return

        val triggerMs = dueDate - minutes * 60_000L
        if (triggerMs <= System.currentTimeMillis()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) return
        }

        val intent = buildIntent(task.id, task.title, dueDate, task.priority)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerMs,
            intent,
        )
    }

    /**
     * 알람 취소.
     * 저장/수정/삭제/완료 토글 시 기존 예약을 먼저 취소하고 필요 시 재예약.
     */
    fun cancel(taskId: Long) {
        // 인텐트 내용보다 requestCode와 action이 같으면 시스템이 같은 PI로 인식하므로
        // extras가 달라도 FLAG_NO_CREATE로 찾아서 cancel 가능.
        val intent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            Intent(context, TodoAlarmReceiver::class.java).apply {
                action = TodoAlarmReceiver.ACTION_REMINDER
            },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(intent)
        intent.cancel()
    }

    // ──────────────────────────────────────────────────────
    // 내부 헬퍼
    // ──────────────────────────────────────────────────────

    private fun buildIntent(taskId: Long, title: String, dueDate: Long, priority: Int): PendingIntent {
        val intent = Intent(context, TodoAlarmReceiver::class.java).apply {
            action = TodoAlarmReceiver.ACTION_REMINDER
            putExtra(TodoAlarmReceiver.EXTRA_TASK_ID, taskId)
            putExtra(TodoAlarmReceiver.EXTRA_TASK_TITLE, title)
            putExtra(TodoAlarmReceiver.EXTRA_DUE_DATE, dueDate)
            putExtra(TodoAlarmReceiver.EXTRA_PRIORITY, priority)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
