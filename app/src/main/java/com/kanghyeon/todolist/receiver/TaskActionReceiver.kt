package com.kanghyeon.todolist.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kanghyeon.todolist.data.repository.TaskRepository
import com.kanghyeon.todolist.service.TodoActions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 알림 버튼(완료/삭제) 클릭을 처리하는 BroadcastReceiver.
 *
 * [goAsync() 사용 이유]
 * BroadcastReceiver.onReceive()는 메인 스레드에서 실행되며
 * 약 10초의 타임아웃 내에 반환해야 한다.
 * 하지만 Repository의 suspend 함수는 IO 작업이 포함되어 있으므로
 * goAsync()로 비동기 처리를 허용한 뒤 완료 시 finish()를 호출한다.
 *
 * [대안: WorkManager]
 * 작업이 길거나 재시도가 필요한 경우 WorkManager를 쓰는 것이 더 안전하다.
 * 이 앱에서는 단순 DB 업데이트이므로 goAsync()로 충분하다.
 *
 * [Hilt in BroadcastReceiver]
 * @AndroidEntryPoint 선언 시 onReceive() 진입 전에 @Inject 필드가 주입된다.
 */
@AndroidEntryPoint
class TaskActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: TaskRepository

    /**
     * 장시간 실행되는 suspend 작업을 위한 코루틴 스코프.
     * 각 onReceive() 호출마다 독립적인 Job이 생성되며,
     * pendingResult.finish() 호출 후 GC에 의해 정리된다.
     */
    private val receiverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(TodoActions.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        // goAsync(): 시스템에게 "아직 처리 중"임을 알림 → 타임아웃 연장
        val pendingResult = goAsync()

        receiverScope.launch {
            try {
                when (intent.action) {
                    TodoActions.ACTION_MARK_DONE -> repository.toggleDone(taskId, isDone = true)
                    TodoActions.ACTION_DELETE    -> repository.deleteById(taskId)
                }
            } finally {
                // 반드시 호출해야 시스템이 BroadcastReceiver를 정상 종료로 처리
                pendingResult.finish()
            }
        }
        // Room Flow를 구독 중인 TodoForegroundService가 DB 변경을 감지해
        // 알림을 자동으로 갱신함 — 여기서 직접 알림을 수정할 필요 없음
    }
}
