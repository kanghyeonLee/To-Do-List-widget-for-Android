package com.kang.dailyarchive.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kang.dailyarchive.service.NotificationHelper
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * AlarmManager 발화 → 상단바 마감(D-Day) 알림 표시.
 */
class TodoAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMINDER) return

        val taskId  = intent.getLongExtra(EXTRA_TASK_ID, -1L).takeIf { it >= 0 } ?: return
        val rawTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: return
        val dueDate = intent.getLongExtra(EXTRA_DUE_DATE, -1L).takeIf { it >= 0 }

        // ViewModel에서 걸렀지만, 혹시 모를 일반 할 일(dueDate 없음) 방어 코드
        if (dueDate == null) return

        // 1. D-Day 라벨 계산 (예: D-3, D-Day, D+1)
        val dDayLabel = getDDayString(dueDate)
        
        // 2. 알림 제목 조립 (예: "[D-3] 정보보안기사 실기 접수")
        val notificationTitle = "[$dDayLabel] $rawTitle"

        // 3. 조립된 제목을 헬퍼로 전달
        // (NotificationHelper 내부에서는 전달받은 title을 setContentTitle 에 그대로 넣으면 됩니다)
        NotificationHelper.showReminderNotification(context, taskId, notificationTitle, dueDate)
    }

    /**
     * dueDate 밀리초를 기준으로 오늘 날짜와 비교하여 D-Day 문자열을 반환합니다.
     */
    private fun getDDayString(dueDateMillis: Long): String {
        val targetDate = Instant.ofEpochMilli(dueDateMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val today = LocalDate.now(ZoneId.systemDefault())

        val daysDiff = ChronoUnit.DAYS.between(today, targetDate)

        return when {
            daysDiff == 0L -> "D-Day"
            daysDiff > 0 -> "D-$daysDiff"
            else -> "D+${-daysDiff}"
        }
    }

    companion object {
        const val ACTION_REMINDER  = "com.kang.dailyarchive.ACTION_REMINDER"
        const val EXTRA_TASK_ID    = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_DUE_DATE   = "extra_due_date"
        const val EXTRA_PRIORITY   = "extra_priority"
    }
}