package com.kanghyeon.todolist.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kanghyeon.todolist.service.NotificationHelper

/**
 * AlarmManager가 발화하면 호출되는 BroadcastReceiver.
 *
 * AlarmScheduler가 setExactAndAllowWhileIdle()로 예약한 인텐트를 수신하여
 * 사전 알림(Heads-up) 푸시 알림을 표시한다.
 *
 * [AndroidManifest 등록 필요]
 *   <receiver android:name=".receiver.TodoAlarmReceiver" android:exported="false" />
 */
class TodoAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMINDER) return

        val taskId  = intent.getLongExtra(EXTRA_TASK_ID, -1L).takeIf { it != -1L } ?: return
        val title   = intent.getStringExtra(EXTRA_TASK_TITLE) ?: return
        val dueDate = intent.getLongExtra(EXTRA_DUE_DATE, -1L).takeIf { it != -1L }

        NotificationHelper.showReminderNotification(
            context  = context,
            taskId   = taskId,
            title    = title,
            dueDate  = dueDate,
        )
    }

    companion object {
        const val ACTION_REMINDER    = "com.kanghyeon.todolist.ACTION_REMINDER"
        const val EXTRA_TASK_ID      = "extra_task_id"
        const val EXTRA_TASK_TITLE   = "extra_task_title"
        const val EXTRA_DUE_DATE     = "extra_due_date"
    }
}
