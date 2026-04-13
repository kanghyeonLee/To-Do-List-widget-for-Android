package com.kanghyeon.todolist.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kanghyeon.todolist.data.local.entity.Priority
import com.kanghyeon.todolist.service.NotificationHelper

/**
 * AlarmManager 발화 → 상단바 마감 알림 표시.
 */
class TodoAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMINDER) return

        val taskId  = intent.getLongExtra(EXTRA_TASK_ID, -1L).takeIf { it >= 0 } ?: return
        val title   = intent.getStringExtra(EXTRA_TASK_TITLE) ?: return
        val dueDate = intent.getLongExtra(EXTRA_DUE_DATE, -1L).takeIf { it >= 0 }

        NotificationHelper.showReminderNotification(context, taskId, title, dueDate)
    }

    companion object {
        const val ACTION_REMINDER  = "com.kanghyeon.todolist.ACTION_REMINDER"
        const val EXTRA_TASK_ID    = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_DUE_DATE   = "extra_due_date"
        const val EXTRA_PRIORITY   = "extra_priority"
    }
}
