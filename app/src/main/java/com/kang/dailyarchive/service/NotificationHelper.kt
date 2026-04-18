package com.kang.dailyarchive.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.kang.dailyarchive.R
import com.kang.dailyarchive.data.local.entity.Priority
import com.kang.dailyarchive.data.local.entity.TaskEntity
import com.kang.dailyarchive.receiver.TaskActionReceiver
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

private val DUE_TIME_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", Locale.KOREA)

/**
 * 알림 채널 생성 및 알림 빌드 헬퍼
 *
 * [채널 구성]
 * - CHANNEL_ID        : 상태바 상시 알림 (IMPORTANCE_LOW, 소리/진동 없음)
 * - REMINDER_CHANNEL_ID: 마감 사전 알림 (IMPORTANCE_HIGH, Heads-up 팝업)
 *
 * [Android 12+ RemoteViews 제약]
 * - 접힌 상태: 최대 64dp
 * - 펼친 상태: 최대 256dp
 * - DecoratedCustomViewStyle 사용 시 시스템 헤더(아이콘·앱명) + 커스텀 본문 조합
 */
object NotificationHelper {

    const val CHANNEL_ID      = "todo_persistent_channel"
    const val NOTIFICATION_ID = 1001

    const val REMINDER_CHANNEL_ID = "todo_reminder_channel"
    private const val REMINDER_NOTIFICATION_BASE = 10_000

    private const val MAX_VISIBLE_TASKS = 3

    // RemoteViews 슬롯 View ID (notification_todo_expanded.xml 과 1:1 매핑)
    private val SLOT_CONTAINERS  = intArrayOf(R.id.slot_0_container,  R.id.slot_1_container,  R.id.slot_2_container)
    private val SLOT_TITLES      = intArrayOf(R.id.slot_0_title,      R.id.slot_1_title,      R.id.slot_2_title)
    private val SLOT_ACCENTS     = intArrayOf(R.id.slot_0_accent,     R.id.slot_1_accent,     R.id.slot_2_accent)
    private val SLOT_DONE_BUTTONS = intArrayOf(R.id.slot_0_done_btn,  R.id.slot_1_done_btn,   R.id.slot_2_done_btn)

    // ── 우선순위별 색상 (ARGB Int) ────────────────────────────────

    private fun priorityBgColor(priority: Int): Int = when (Priority.from(priority)) {
        Priority.HIGH   -> 0x1AE53935.toInt()
        Priority.MEDIUM -> 0x1AFB8C00.toInt()
        Priority.LOW    -> 0x0F9E9E9E.toInt()
    }

    private fun priorityAccentColor(priority: Int): Int = when (Priority.from(priority)) {
        Priority.HIGH   -> 0xFFE53935.toInt()
        Priority.MEDIUM -> 0xFFFB8C00.toInt()
        Priority.LOW    -> 0xFF9E9E9E.toInt()
    }

    private fun priorityTextColor(priority: Int): Int = when (Priority.from(priority)) {
        Priority.HIGH   -> 0xFFC62828.toInt()
        Priority.MEDIUM -> 0xFFE65100.toInt()
        Priority.LOW    -> 0xFF212121.toInt()
    }

    // ──────────────────────────────────────────────────────
    // 채널 생성
    // ──────────────────────────────────────────────────────

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "할 일 목록",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "잠금화면에서 할 일을 확인하고 체크할 수 있는 알림입니다."
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun createReminderChannel(context: Context) {
        val channel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            "마감 사전 알림",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "마감 시간 전에 할 일을 미리 알려드립니다."
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    // ──────────────────────────────────────────────────────
    // 알림 빌드
    // ──────────────────────────────────────────────────────

    fun buildInitialNotification(context: Context): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_todo_notification)
            .setContentTitle("할 일 불러오는 중…")
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .build()

    fun buildNotification(context: Context, tasks: List<TaskEntity>): Notification {
        val contentIntent = buildContentIntent(context)
        return if (tasks.isEmpty()) {
            buildEmptyNotification(context, contentIntent)
        } else {
            buildTaskNotification(context, tasks, contentIntent)
        }
    }

    fun showReminderNotification(
        context: Context,
        taskId: Long,
        title: String,
        dueDate: Long?,
    ) {
        val contentText = "마감이 다가오고 있습니다. 잊지 말고 완료해 주세요!"

        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.calendar_clock)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(buildLaunchIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 팝업으로 확실히 띄움
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(REMINDER_NOTIFICATION_BASE + taskId.toInt(), notification)
    }

    // ──────────────────────────────────────────────────────
    // 내부 헬퍼
    // ──────────────────────────────────────────────────────
    private fun getDDayLabel(dueDateMillis: Long?): String {
        if (dueDateMillis == null) return ""
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

    private fun buildEmptyNotification(
        context: Context,
        contentIntent: PendingIntent,
    ): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.calendar_clock)
            .setContentTitle("마감 임박 할 일 없음!") 
            .setContentText("현재 등록된 D-Day 일정이 없습니다.") 
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .build()

    private fun buildTaskNotification(
        context: Context,
        tasks: List<TaskEntity>,
        contentIntent: PendingIntent,
    ): Notification {
        val sorted         = tasks.sortedBy { it.dueDate }
        val visibleTasks   = sorted.take(MAX_VISIBLE_TASKS)
        val remainingCount = sorted.size - visibleTasks.size

        val pillView       = buildPillRemoteViews(context, sorted.first())
        val expandedView = buildExpandedRemoteViews(context, visibleTasks, remainingCount)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.calendar_clock)
            .setContentTitle("D-Day 마감 ${sorted.size}개")
            .setContentText("[${getDDayLabel(sorted.first().dueDate)}] ${sorted.first().title}") 
            .setContentIntent(contentIntent)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(pillView)
            .setCustomBigContentView(expandedView)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun buildPillRemoteViews(context: Context, task: TaskEntity): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.notification_todo)

        views.setInt(R.id.pill_priority_bar, "setBackgroundColor", priorityAccentColor(task.priority))

        val dueTimeText = getDDayLabel(task.dueDate).ifEmpty { "기한 없음" }
        views.setTextViewText(R.id.pill_due_time, dueTimeText)
        views.setTextViewText(R.id.pill_title, task.title)
        views.setTextColor(R.id.pill_title, priorityTextColor(task.priority))

        val doneIntent = buildMarkDoneIntent(context, task.id, requestCode = 99)
        views.setOnClickPendingIntent(R.id.pill_done_btn, doneIntent)

        return views
    }

    private fun buildExpandedRemoteViews(
        context: Context,
        tasks: List<TaskEntity>,
        remainingCount: Int,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.notification_todo_expanded)

        for (i in 0 until MAX_VISIBLE_TASKS) {
            val task = tasks.getOrNull(i)
            if (task == null) {
                views.setViewVisibility(SLOT_CONTAINERS[i], android.view.View.GONE)
            } else {
                views.setViewVisibility(SLOT_CONTAINERS[i], android.view.View.VISIBLE)
                views.setInt(SLOT_CONTAINERS[i], "setBackgroundColor", priorityBgColor(task.priority))
                views.setInt(SLOT_ACCENTS[i],    "setBackgroundColor", priorityAccentColor(task.priority))
                
                val dDayLabel = getDDayLabel(task.dueDate)
                val displayTitle = if (dDayLabel.isNotEmpty()) "[$dDayLabel] ${task.title}" else task.title
                views.setTextViewText(SLOT_TITLES[i], displayTitle)
                
                views.setTextColor(SLOT_TITLES[i], priorityTextColor(task.priority))
                val doneIntent = buildMarkDoneIntent(context, task.id, requestCode = i)
                views.setOnClickPendingIntent(SLOT_DONE_BUTTONS[i], doneIntent)
            }
        }

        if (remainingCount > 0) {
            views.setViewVisibility(R.id.tv_more_count, android.view.View.VISIBLE)
            views.setTextViewText(R.id.tv_more_count, "외 $remainingCount 개 더…")
        } else {
            views.setViewVisibility(R.id.tv_more_count, android.view.View.GONE)
        }

        return views
    }

    private fun buildContentIntent(context: Context): PendingIntent = buildLaunchIntent(context)

    private fun buildLaunchIntent(context: Context): PendingIntent {
        val intent = context.packageManager
            .getLaunchIntentForPackage(context.packageName) ?: Intent()
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun buildMarkDoneIntent(
        context: Context,
        taskId: Long,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(context, TaskActionReceiver::class.java).apply {
            action = TodoActions.ACTION_MARK_DONE
            putExtra(TodoActions.EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
