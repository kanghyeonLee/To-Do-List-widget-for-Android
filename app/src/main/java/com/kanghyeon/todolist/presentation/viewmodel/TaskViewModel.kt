package com.kanghyeon.todolist.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanghyeon.todolist.data.local.entity.TaskEntity
import com.kanghyeon.todolist.data.repository.TaskRepository
import com.kanghyeon.todolist.service.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

// ──────────────────────────────────────────────────────────
// UI State
// ──────────────────────────────────────────────────────────

/**
 * 화면 전체 상태를 표현하는 단일 불변 데이터 클래스.
 *
 * [탭 구조와 필드 매핑]
 * - '할 일' 탭  : activeTasks  — isDone=false, priority·createdAt 정렬
 * - '아카이브' 탭: completedTasks — 배지 카운트 전용 (날짜 필터링은 archiveTasks StateFlow 사용)
 */
data class TaskUiState(
    /** 전체 미완료 할 일 (priority DESC, createdAt DESC) — '할 일' 탭 표시용 */
    val activeTasks: List<TaskEntity> = emptyList(),
    /** 완료된 할 일 전체 — '아카이브' 탭 배지 카운트용 */
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
    private val alarmScheduler: AlarmScheduler,
) : ViewModel() {

    // ── UI State ─────────────────────────────────────────
    /**
     * combine(active, completed):
     * 두 Flow 중 하나라도 방출되면 UiState 재조합.
     *
     * stateIn(WhileSubscribed(5_000)):
     * - 마지막 구독자가 떠난 후 5초간 업스트림 유지 → 화면 회전 시 재구독 비용 없음
     * - 5초 초과 시 수집 중단 → 백그라운드 배터리 절약
     */
    val uiState: StateFlow<TaskUiState> = combine(
        repository.getActiveTasks(),
        repository.getCompletedTasks(),
    ) { active, completed ->
        TaskUiState(
            activeTasks    = active,
            completedTasks = completed,
            isLoading      = false,
        )
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = TaskUiState(isLoading = true),
    )

    // ── 아카이브 날짜 선택 상태 ──────────────────────────
    /**
     * 아카이브 탭 Day Selector의 선택 날짜 (00:00:00 epoch ms).
     * [moveArchiveDate]로 ±1일 이동하며, 오늘 이후로는 이동 불가.
     */
    private val _selectedArchiveDate = MutableStateFlow(todayStartMs())
    val selectedArchiveDate: StateFlow<Long> = _selectedArchiveDate.asStateFlow()

    /**
     * 선택된 날짜에 완료된 할 일 목록 (updatedAt 기준, DESC 정렬).
     *
     * flatMapLatest: _selectedArchiveDate가 바뀌면 이전 구독을 즉시 취소하고
     * 새 날짜 쿼리로 재구독 → 날짜 전환 시 이전 데이터 잔류 없음.
     */
    val archiveTasks: StateFlow<List<TaskEntity>> = _selectedArchiveDate
        .flatMapLatest { startMs ->
            repository.getCompletedTasksByDate(
                startOfDay = startMs,
                endOfDay   = startMs + DAY_MS - 1,
            )
        }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    // ── 편집 중인 Task ────────────────────────────────────
    private val _editingTask = MutableStateFlow<TaskEntity?>(null)
    val editingTask: StateFlow<TaskEntity?> = _editingTask.asStateFlow()

    fun setEditingTask(task: TaskEntity?) { _editingTask.value = task }

    // ── 일회성 이벤트 채널 ────────────────────────────────
    private val _eventChannel = Channel<TaskEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    // ──────────────────────────────────────────────────────
    // 사용자 액션 핸들러
    // ──────────────────────────────────────────────────────

    /**
     * 추가/수정 통합 저장
     * editingTask가 있으면 update, 없으면 insert
     */
    fun saveCurrentTask(
        title: String,
        description: String? = null,
        priority: Int = 1,
        dueDate: Long? = null,
        showOnLockScreen: Boolean = true,
        reminderMinutes: Int? = null,
    ) {
        if (title.isBlank()) {
            emitEvent(TaskEvent.ShowMessage("제목을 입력해 주세요."))
            return
        }
        val editing = _editingTask.value
        viewModelScope.launch {
            if (editing != null) {
                val updated = editing.copy(
                    title            = title.trim(),
                    description      = description?.trim(),
                    priority         = priority,
                    dueDate          = dueDate,
                    showOnLockScreen = showOnLockScreen,
                    reminderMinutes  = reminderMinutes,
                    updatedAt        = System.currentTimeMillis(),
                )
                repository.updateTask(updated)
                alarmScheduler.cancel(editing.id)
                alarmScheduler.schedule(updated)
            } else {
                val task = TaskEntity(
                    title            = title.trim(),
                    description      = description?.trim(),
                    priority         = priority,
                    dueDate          = dueDate,
                    showOnLockScreen = showOnLockScreen,
                    reminderMinutes  = reminderMinutes,
                )
                val id = repository.saveTask(task)
                alarmScheduler.schedule(task.copy(id = id))
            }
            _editingTask.value = null
        }
    }

    /** 새 할 일 추가 */
    fun addTask(
        title: String,
        description: String? = null,
        priority: Int = 1,
        dueDate: Long? = null,
        showOnLockScreen: Boolean = true,
        reminderMinutes: Int? = null,
    ) {
        if (title.isBlank()) {
            emitEvent(TaskEvent.ShowMessage("제목을 입력해 주세요."))
            return
        }
        viewModelScope.launch {
            val task = TaskEntity(
                title            = title.trim(),
                description      = description?.trim(),
                priority         = priority,
                dueDate          = dueDate,
                showOnLockScreen = showOnLockScreen,
                reminderMinutes  = reminderMinutes,
            )
            val id = repository.saveTask(task)
            alarmScheduler.schedule(task.copy(id = id))
        }
    }

    /** 기존 할 일 수정 */
    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            val updated = task.copy(updatedAt = System.currentTimeMillis())
            repository.saveTask(updated)
            alarmScheduler.cancel(task.id)
            alarmScheduler.schedule(updated)
        }
    }

    /**
     * 완료/미완료 토글
     * @param id          변경할 Task의 ID
     * @param currentDone 현재 완료 상태 (반전값으로 저장)
     */
    fun toggleDone(id: Long, currentDone: Boolean) {
        viewModelScope.launch {
            val isDone = !currentDone
            repository.toggleDone(id = id, isDone = isDone)
            // 완료 시 알람 취소, 미완료 복구 시 재예약은 Task 전체 정보가 필요하므로
            // 완료 전환만 취소 (미완료 복구는 사용자가 직접 날짜·알림을 다시 설정)
            if (isDone) alarmScheduler.cancel(id)
        }
    }

    /**
     * 할 일 삭제 (Undo 지원)
     * 삭제 전 Task를 TaskDeleted 이벤트로 전달 → UI에서 Snackbar "실행 취소" 제공 가능
     */
    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            alarmScheduler.cancel(task.id)
            repository.deleteTask(task)
            emitEvent(TaskEvent.TaskDeleted(task))
        }
    }

    /** 삭제 취소: 이전에 삭제한 Task를 복원 */
    fun restoreTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.saveTask(task)
            alarmScheduler.schedule(task)
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
     * 아카이브 탭에서 현재 선택된 날짜의 완료 항목만 삭제.
     * 다른 날짜의 아카이브는 보존된다.
     */
    fun clearCompletedForSelectedDate() {
        val startMs = _selectedArchiveDate.value
        viewModelScope.launch {
            repository.deleteCompletedByDate(
                startOfDay = startMs,
                endOfDay   = startMs + DAY_MS - 1,
            )
            emitEvent(TaskEvent.ShowMessage("선택한 날의 완료 항목을 삭제했습니다."))
        }
    }

    /**
     * 아카이브 날짜를 deltaDays만큼 이동.
     * java.time.LocalDate 기반으로 DST·윤달을 안전하게 처리.
     * 오늘 이후로는 이동 불가 (UI 버튼 비활성화와 병행).
     *
     * @param deltaDays +1 = 다음 날, -1 = 이전 날
     */
    fun moveArchiveDate(deltaDays: Int) {
        _selectedArchiveDate.update { currentMs ->
            val zone    = ZoneId.systemDefault()
            val newDate = Instant.ofEpochMilli(currentMs)
                .atZone(zone)
                .toLocalDate()
                .plusDays(deltaDays.toLong())
            if (newDate.isAfter(LocalDate.now(zone))) return@update currentMs
            newDate.atStartOfDay(zone).toInstant().toEpochMilli()
        }
    }

    /**
     * 아카이브 날짜를 특정 날짜로 직접 설정.
     * HorizontalDateStrip 날짜 클릭 및 DatePicker 선택 시 사용.
     *
     * @param dateMs 선택된 날짜 epoch ms (UTC 자정 또는 로컬 자정 모두 허용)
     */
    fun selectArchiveDate(dateMs: Long) {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(dateMs).atZone(zone).toLocalDate()
        if (date.isAfter(LocalDate.now(zone))) return
        _selectedArchiveDate.value = date.atStartOfDay(zone).toInstant().toEpochMilli()
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

    companion object {
        private const val DAY_MS = 24 * 60 * 60 * 1_000L

        /** 오늘 00:00:00.000 epoch ms (로컬 타임존 기준) */
        fun todayStartMs(): Long =
            LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
    }
}
