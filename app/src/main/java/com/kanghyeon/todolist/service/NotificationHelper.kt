package com.kanghyeon.todolist.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.kanghyeon.todolist.R
import com.kanghyeon.todolist.data.local.entity.Priority
import com.kanghyeon.todolist.data.local.entity.TaskEntity
import com.kanghyeon.todolist.receiver.TaskActionReceiver
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 알림 채널 생성 및 알림 빌드 헬퍼
 *
 * [알림 채널 설계]
 * - IMPORTANCE_LOW: 소리/진동 없이 상태바에 표시 (지속 알림에 적합)
 * - VISIBILITY_PUBLIC: 잠금화면에 내용 전체 노출
 * - setShowBadge(false): 런처 아이콘 뱃지 비표시 (할 일 앱에서는 불필요)
 *
 * [Android 12+ RemoteViews 제약]
 * - 접힌 상태: 최대 64dp
 * - 펼친 상태: 최대 256dp
 * - DecoratedCustomViewStyle 사용 시 시스템 템플릿 안에 커스텀 뷰가 삽입됨
 */
object NotificationHelper {

    const val CHANNEL_ID          = "todo_persistent_channel"
    const val NOTIFICATION_ID     = 1001

    const val REMINDER_CHANNEL_ID = "todo_reminder_channel"
    // 알람 알림 ID = 10000 + taskId.toInt() (지속 알림과 충돌 방지)
    private const val REMINDER_NOTIFICATION_BASE = 10_000

    // 알림에 표시할 최대 Task 개수
    private const val MAX_VISIBLE_TASKS = 3

    // RemoteViews에서 사용하는 각 슬롯의 View ID 목록
    // (notification_todo_expanded.xml 의 ID와 1:1 매핑)
    private val SLOT_CONTAINERS = intArrayOf(
        R.id.slot_0_container,
        R.id.slot_1_container,
        R.id.slot_2_container,
    )
    private val SLOT_TITLES = intArrayOf(
        R.id.slot_0_title,
        R.id.slot_1_title,
        R.id.slot_2_title,
    )
    private val SLOT_ACCENTS = intArrayOf(
        R.id.slot_0_accent,
        R.id.slot_1_accent,
        R.id.slot_2_accent,
    )
    private val SLOT_DONE_BUTTONS = intArrayOf(
        R.id.slot_0_done_btn,
        R.id.slot_1_done_btn,
        R.id.slot_2_done_btn,
    )

    // ── 우선순위별 색상 (ARGB Int) ────────────────────────────────
    // RemoteViews는 Compose/Theme 색상을 사용할 수 없으므로 상수로 정의.

    /** 슬롯 컨테이너 파스텔 배경색 (alpha ~10%) */
    private fun priorityBgColor(priority: Int): Int = when (Priority.from(priority)) {
        Priority.HIGH   -> 0x1AE53935.toInt()   // Red α≈10%
        Priority.MEDIUM -> 0x1AFB8C00.toInt()   // Orange α≈10%
        Priority.LOW    -> 0x0F9E9E9E.toInt()   // Grey α≈6%
    }

    /** 좌측 액센트 바 색상 (불투명) */
    private fun priorityAccentColor(priority: Int): Int = when (Priority.from(priority)) {
        Priority.HIGH   -> 0xFFE53935.toInt()
        Priority.MEDIUM -> 0xFFFB8C00.toInt()
        Priority.LOW    -> 0xFF9E9E9E.toInt()
    }

    /**
     * 제목 텍스트 색상 — 파스텔 배경 위에서 가독성 확보
     * 우선순위별 진한 색상 또는 near-black 사용
     */
    private fun priorityTextColor(priority: Int): Int = when (Priority.from(priority)) {
        Priority.HIGH   -> 0xFFC62828.toInt()   // Red 800
        Priority.MEDIUM -> 0xFFE65100.toInt()   // Orange 900
        Priority.LOW    -> 0xFF212121.toInt()   // Grey 900 (near-black)
    }

    // ──────────────────────────────────────────────────────
    // 채널 생성
    // ──────────────────────────────────────────────────────

    /**
     * 지속 알림 채널 생성 (API 26+)
     * 이미 존재하면 시스템이 무시하므로 중복 호출 안전.
     * Application.onCreate()에서 한 번만 호출하면 된다.
     */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "할 일 목록",                          // 설정 화면에 표시될 채널 이름
            NotificationManager.IMPORTANCE_LOW,    // 소리/진동 없음
        ).apply {
            description = "잠금화면에서 할 일을 확인하고 체크할 수 있는 알림입니다."
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    /**
     * 알림 알람 채널 생성 (IMPORTANCE_HIGH → Heads-up 팝업)
     * Application.onCreate()에서 한 번만 호출하면 된다.
     */
    fun createReminderChannel(context: Context) {
        val channel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            "마감 사전 알림",
            NotificationManager.IMPORTANCE_HIGH,   // Heads-up 팝업
        ).apply {
            description = "마감 시간 전에 할 일을 미리 알려드립니다."
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    /**
     * 마감 사전 알림 표시.
     * TodoAlarmReceiver.onReceive()에서 호출된다.
     *
     * @param taskId  알림 ID 생성에 사용 (REMINDER_NOTIFICATION_BASE + taskId)
     * @param title   할 일 제목
     * @param dueDate 마감 epoch ms (포맷팅용, null 허용)
     */
    fun showReminderNotification(
        context: Context,
        taskId: Long,
        title: String,
        dueDate: Long?,
    ) {
        val contentText = if (dueDate != null) {
            val time = Instant.ofEpochMilli(dueDate)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("M월 d일 HH:mm", Locale.KOREA))
            "마감: $time"
        } else {
            "마감이 다가왔습니다."
        }

        val contentIntent = buildLaunchIntent(context)

        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_todo_notification)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(REMINDER_NOTIFICATION_BASE + taskId.toInt(), notification)
    }

    // ──────────────────────────────────────────────────────
    // 알림 빌드
    // ──────────────────────────────────────────────────────

    /**
     * 앱 최초 실행 / 서비스 재시작 직후 DB 로딩 전에 표시할 임시 알림.
     * startForeground()는 onCreate() 직후 즉시 호출해야 하므로 필요.
     */
    fun buildInitialNotification(context: Context): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_todo_notification)
            .setContentTitle("할 일 불러오는 중…")
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .build()

    /**
     * 할 일 목록으로 알림을 빌드한다.
     *
     * @param tasks getLockScreenTasks()에서 받은 미완료 Task 목록
     */
    fun buildNotification(context: Context, tasks: List<TaskEntity>): Notification {
        // ── 탭 시 앱 메인 화면으로 이동하는 PendingIntent ──
        val contentIntent = buildContentIntent(context)

        return if (tasks.isEmpty()) {
            buildEmptyNotification(context, contentIntent)
        } else {
            buildTaskNotification(context, tasks, contentIntent)
        }
    }

    // ──────────────────────────────────────────────────────
    // 내부 헬퍼
    // ──────────────────────────────────────────────────────

    private fun buildEmptyNotification(
        context: Context,
        contentIntent: PendingIntent,
    ): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_todo_notification)
            .setContentTitle("모든 할 일 완료!")
            .setContentText("새로운 할 일을 추가해 보세요.")
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
        val visibleTasks = tasks.take(MAX_VISIBLE_TASKS)
        val remainingCount = tasks.size - visibleTasks.size

        val expandedView = buildExpandedRemoteViews(context, visibleTasks, remainingCount)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_todo_notification)
            .setContentTitle("할 일 ${tasks.size}개")
            .setContentText(tasks.first().title)       // 접혔을 때 첫 항목 표시
            .setContentIntent(contentIntent)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomBigContentView(expandedView)     // 펼쳤을 때 커스텀 뷰
            .setOngoing(true)                          // 스와이프로 제거 불가
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)  // 잠금화면 전체 표시
            .setSilent(true)                           // 갱신 시 소리/진동 없음
            .setOnlyAlertOnce(true)                    // 최초 표시 시에만 알림음
            .build()
    }

    /**
     * 펼친 상태의 커스텀 RemoteViews를 빌드한다.
     *
     * [슬롯 방식 선택 이유]
     * ListView / RecyclerView는 RemoteViews에서 setRemoteAdapter()를 써야 하고
     * 알림 내부에서는 어댑터 갱신 타이밍이 복잡해진다.
     * 최대 3개 고정 슬롯을 VISIBLE/GONE 토글하는 방식이 더 안정적이다.
     */
    private fun buildExpandedRemoteViews(
        context: Context,
        tasks: List<TaskEntity>,
        remainingCount: Int,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.notification_todo_expanded)

        // 슬롯 3개를 순서대로 채우거나 숨김
        for (i in 0 until MAX_VISIBLE_TASKS) {
            val task = tasks.getOrNull(i)

            if (task == null) {
                views.setViewVisibility(SLOT_CONTAINERS[i], android.view.View.GONE)
            } else {
                views.setViewVisibility(SLOT_CONTAINERS[i], android.view.View.VISIBLE)

                // ── 우선순위별 색상 적용 ──────────────────────
                // 파스텔 배경 (컨테이너)
                views.setInt(
                    SLOT_CONTAINERS[i],
                    "setBackgroundColor",
                    priorityBgColor(task.priority),
                )
                // 좌측 액센트 바
                views.setInt(
                    SLOT_ACCENTS[i],
                    "setBackgroundColor",
                    priorityAccentColor(task.priority),
                )
                // 제목 텍스트 (파스텔 배경 위 가독성 확보)
                views.setTextViewText(SLOT_TITLES[i], task.title)
                views.setTextColor(SLOT_TITLES[i], priorityTextColor(task.priority))

                // 완료 버튼 PendingIntent
                val doneIntent = buildMarkDoneIntent(context, task.id, requestCode = i)
                views.setOnClickPendingIntent(SLOT_DONE_BUTTONS[i], doneIntent)
            }
        }

        // "외 N개" 텍스트
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
        // MainActivity로 이동 (아직 생성 전이면 패키지 내 임시 인텐트 사용)
        val intent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?: Intent()
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * "완료" 버튼용 PendingIntent
     *
     * requestCode를 슬롯 인덱스(0~2)로 사용.
     * 같은 requestCode + 같은 action이면 시스템이 PendingIntent를 재사용하므로
     * FLAG_UPDATE_CURRENT로 extras(taskId)를 갱신한다.
     */
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
