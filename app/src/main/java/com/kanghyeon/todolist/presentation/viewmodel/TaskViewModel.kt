package com.kanghyeon.todolist.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanghyeon.todolist.data.local.entity.TaskEntity
import com.kanghyeon.todolist.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ──────────────────────────────────────────────────────────
// UI State
// ──────────────────────────────────────────────────────────

/**
 * 화면 전체 상태를 표현하는 단일 불변 데이터 클래스.
 *
 * [왜 단일 UiState인가?]
 * - 여러 개의 StateFlow를 두면 UI가 각각 구독해야 하고,
 *   상태 간 조합 타이밍이 달라져 깜박임(race condition)이 발생할 수 있다.
 * - 하나의 StateFlow로 묶으면 UI는 단 하나의 collect()만 유지하면 된다.
 */
data class TaskUiState(
    val todayTasks: List<TaskEntity> = emptyList(),
    val overdueTasks: List<TaskEntity> = emptyList(),
    val allTasks: List<TaskEntity> = emptyList(),
    val completedTasks: List<TaskEntity> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

// ──────────────────────────────────────────────────────────
// 일회성 이벤트 (Snackbar / 네비게이션 등)
// ──────────────────────────────────────────────────────────

/**
 * UI가 한 번만 처리해야 하는 이벤트.
 * Channel<> 기반으로 전달하여 화면 회전 시 중복 처리를 방지한다.
 */
sealed interface TaskEvent {
    /** 스낵바 메시지 표시 */
    data class ShowMessage(val message: String) : TaskEvent

    /** 삭제 취소(Undo)를 위해 삭제된 Task를 함께 전달 */
    data class TaskDeleted(val task: TaskEntity) : TaskEvent
}

// ──────────────────────────────────────────────────────────
// ViewModel
// ──────────────────────────────────────────────────────────

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository,
) : ViewModel() {

    // ── 내부 로딩 상태 ──────────────────────────────────
    private val _isLoading = MutableStateFlow(true)

    // ── UI State (단일 StateFlow로 통합) ─────────────────
    /**
     * combine(): 4개의 Flow를 하나로 묶어 어떤 Flow가 방출돼도 UiState를 재조합.
     * stateIn(WhileSubscribed(5_000)):
     *   - 마지막 구독자가 떠난 후 5초간 업스트림을 유지 → 화면 회전 시 재구독 비용 없음
     *   - 5초 초과 시 수집 중단 → 백그라운드 배터리 절약
     */
    val uiState: StateFlow<TaskUiState> = combine(
        repository.getTodayTasks(),
        repository.getOverdueTasks(),
        repository.getAllTasks(),
        repository.getCompletedTasks(),
    ) { today, overdue, all, completed ->
        TaskUiState(
            todayTasks = today,
            overdueTasks = overdue,
            allTasks = all,
            completedTasks = completed,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TaskUiState(isLoading = true),
    )

    // ── 일회성 이벤트 채널 ────────────────────────────────
    private val _eventChannel = Channel<TaskEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    // ──────────────────────────────────────────────────────
    // 사용자 액션 핸들러
    // ──────────────────────────────────────────────────────

    /** 새 할 일 추가 */
    fun addTask(
        title: String,
        description: String? = null,
        priority: Int = 1,
        dueDate: Long? = null,
        showOnLockScreen: Boolean = true,
    ) {
        if (title.isBlank()) {
            emitEvent(TaskEvent.ShowMessage("제목을 입력해 주세요."))
            return
        }
        viewModelScope.launch {
            repository.saveTask(
                TaskEntity(
                    title = title.trim(),
                    description = description?.trim(),
                    priority = priority,
                    dueDate = dueDate,
                    showOnLockScreen = showOnLockScreen,
                )
            )
        }
    }

    /** 기존 할 일 수정 */
    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.saveTask(task.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    /**
     * 완료/미완료 토글
     * @param id 변경할 Task의 ID
     * @param currentDone 현재 완료 상태 (반전값으로 저장)
     */
    fun toggleDone(id: Long, currentDone: Boolean) {
        viewModelScope.launch {
            repository.toggleDone(id = id, isDone = !currentDone)
        }
    }

    /**
     * 할 일 삭제 (Undo 지원)
     * 삭제 전 Task를 TaskDeleted 이벤트로 전달 → UI에서 Snackbar "실행 취소" 제공 가능
     */
    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            emitEvent(TaskEvent.TaskDeleted(task))
        }
    }

    /** 삭제 취소: 이전에 삭제한 Task를 복원 */
    fun restoreTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.saveTask(task)
        }
    }

    /** 완료된 항목 전체 삭제 */
    fun clearCompleted() {
        viewModelScope.launch {
            repository.deleteAllCompleted()
            emitEvent(TaskEvent.ShowMessage("완료된 항목을 모두 삭제했습니다."))
        }
    }

    /**
     * 드래그 앤 드롭 완료 시 호출
     * @param reorderedList 새 순서대로 정렬된 Task 목록
     */
    fun updateSortOrders(reorderedList: List<TaskEntity>) {
        viewModelScope.launch {
            val orders = reorderedList.mapIndexed { index, task -> task.id to index }
            repository.updateSortOrders(orders)
        }
    }

    // ──────────────────────────────────────────────────────
    // 내부 헬퍼
    // ──────────────────────────────────────────────────────

    private fun emitEvent(event: TaskEvent) {
        viewModelScope.launch { _eventChannel.send(event) }
    }
}
