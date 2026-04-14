package com.kanghyeon.todolist.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanghyeon.todolist.data.local.entity.Priority
import com.kanghyeon.todolist.data.local.entity.RoutineTemplateGroupWithTasks
import com.kanghyeon.todolist.data.local.entity.RoutineTemplateTaskEntity
import com.kanghyeon.todolist.data.local.entity.TaskEntity
import com.kanghyeon.todolist.data.repository.RoutineTemplateRepository
import com.kanghyeon.todolist.data.repository.TaskRepository
import com.kanghyeon.todolist.service.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject


// ──────────────────────────────────────────────────────────
// 새 할 일 작성 임시 저장 (Draft)
// ──────────────────────────────────────────────────────────

/**
 * 새 할 일 추가 시트의 입력 내용을 임시 보관하는 상태 객체.
 *
 * BottomSheet가 닫혀도 ViewModel이 살아있는 한 유지된다.
 * 저장 완료 또는 명시적 취소(취소 버튼) 시 초기화.
 * 스와이프·백 제스처로 닫을 때는 유지 → 재진입 시 복원.
 *
 * @param selectedHour   선택된 마감 시간 (null = 미설정)
 * @param selectedMinute 선택된 마감 분   (null = 미설정)
 */
data class NewTaskDraft(
    val title: String = "",
    val description: String = "",
    val priority: Int = Priority.MEDIUM.value,
    val selectedHour: Int? = null,
    val selectedMinute: Int? = null,
    val showOnLockScreen: Boolean = true,
    val reminderMinutes: Int? = null,
    /** 마감 날짜 (epoch ms, 선택된 날짜의 로컬 자정 기준) — null = 날짜 미선택 */
    val selectedDateMs: Long? = null,
) {
    /** 기본값과 동일하면 비어 있는 draft로 간주 */
    val isEmpty: Boolean
        get() = title.isBlank() && description.isBlank() &&
                selectedHour == null && selectedDateMs == null
}

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
    /** * 메인 '할 일' 탭 표시용 목록.
     * - 아직 완료되지 않은 항목(isDone=false) 전체
     * - 오늘 완료된 항목(isDone=true && updatedAt >= 오늘자정) 포함
     * 정렬: 미완료 우선 -> 우선순위 높은 순 -> 최신순
     */
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
    private val templateRepository: RoutineTemplateRepository,
    private val alarmScheduler: AlarmScheduler,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    // ── SharedPreferences — 루틴 마지막 생성 날짜 ─────────────────
    private val routinePrefs by lazy {
        context.getSharedPreferences("routine_prefs", Context.MODE_PRIVATE)
    }

    // ── SharedPreferences — 자정 동기화 마지막 실행 날짜 ────────────
    private val syncPrefs by lazy {
        context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
    }

    // ── 루틴 템플릿 그룹 목록 (그룹 + 소속 할 일) ───────────────────
    val templateGroups: StateFlow<List<RoutineTemplateGroupWithTasks>> =
        templateRepository.getAllGroupsWithTasks()
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    // ── 앱 시작 시: 자정 동기화 + 루틴 자동 생성 ────────────────────
    init {
        performMidnightSync()
        generateDailyRoutines()
    }

    // ── UI State ─────────────────────────────────────────
    /**
     * combine(active, completed):
     * 두 Flow 중 하나라도 방출되면 UiState 재조합.
     *
     * stateIn(WhileSubscribed(5_000)):
     * - 마지막 구독자가 떠난 후 5초간 업스트림 유지 → 화면 회전 시 재구독 비용 없음
     * - 5초 초과 시 수집 중단 → 백그라운드 배터리 절약
     */
    /**
     * '할 일' 탭 상태.
     * - dueDate가 있는 D-Day 할 일은 D-Day 탭에서 별도 관리하므로 제외.
     * - 미완료 + 오늘 완료(D-Day 아닌 것)만 포함.
     */
    val uiState: StateFlow<TaskUiState> = combine(
        repository.getActiveTasks(),
        repository.getCompletedTasks(),
    ) { active, completed ->
        val todayStart = todayStartMs()

        // D-Day 할 일(dueDate != null)은 '할 일' 탭에서 제외
        val completedToday = completed.filter { it.updatedAt >= todayStart && it.dueDate == null }
        val nonDDayActive  = active.filter { it.dueDate == null }

        val mainTasks = (nonDDayActive + completedToday).sortedWith(
            compareBy<TaskEntity> { it.isDone }
                .thenByDescending { it.priority }
                .thenByDescending { it.createdAt }
        )
        TaskUiState(
            activeTasks    = mainTasks,
            completedTasks = completed,
            isLoading      = false,
        )
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = TaskUiState(isLoading = true),
    )

    // ── D-Day 탭 할 일 목록 ──────────────────────────────────────
    /** dueDate가 지정된 할 일 (마감일 오름차순, 완료 항목은 하단) */
    val dDayTasks: StateFlow<List<TaskEntity>> = repository.getDDayTasks()
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    // ── 아카이브 날짜 선택 상태 ──────────────────────────
    /**
     * 아카이브 탭 Day Selector의 선택 날짜 (00:00:00 epoch ms).
     * [moveArchiveDate]로 ±1일 이동하며, 오늘 이후로는 이동 불가.
     */
    private val _selectedArchiveDate = MutableStateFlow(todayStartMs())
    val selectedArchiveDate: StateFlow<Long> = _selectedArchiveDate.asStateFlow()

    /**
     * 선택된 날짜에 아카이브된 할 일 목록.
     *
     * [아카이브 정책]
     * - 자정 자동 동기화 후 archivedAt이 설정된 항목 우선 표시
     * - 구버전 데이터(archivedAt=null, isDone=1) 하위 호환 포함
     * - 오늘 날짜 선택 시: 수동 동기화된(isArchived=1) 항목만 표시
     *
     * flatMapLatest: 날짜가 바뀌면 이전 쿼리를 즉시 취소하고 새 날짜로 재구독.
     */
    val archiveTasks: StateFlow<List<TaskEntity>> = _selectedArchiveDate
        .flatMapLatest { startMs ->
            repository.getArchivedTasksByDate(
                startOfDay = startMs,
                endOfDay   = startMs + DAY_MS,
                todayStart = todayStartMs(),
            )
        }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    // ── 휴지통 목록 ───────────────────────────────────────
    val deletedTasks: StateFlow<List<TaskEntity>> = repository.getDeletedTasks()
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    // ── 편집 중인 Task ────────────────────────────────────
    private val _editingTask = MutableStateFlow<TaskEntity?>(null)
    val editingTask: StateFlow<TaskEntity?> = _editingTask.asStateFlow()

    fun setEditingTask(task: TaskEntity?) { _editingTask.value = task }

    // ── 새 할 일 Draft ────────────────────────────────────
    private val _newTaskDraft = MutableStateFlow(NewTaskDraft())
    val newTaskDraft: StateFlow<NewTaskDraft> = _newTaskDraft.asStateFlow()

    /** 시트 내용이 바뀔 때마다 호출 — draft 갱신 */
    fun updateNewTaskDraft(draft: NewTaskDraft) {
        _newTaskDraft.value = draft
    }

    /** 저장 완료 또는 명시적 취소 버튼 클릭 시 draft 초기화 */
    fun clearNewTaskDraft() {
        _newTaskDraft.value = NewTaskDraft()
    }

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
                // 새 할 일 저장 성공 → draft 초기화
                _newTaskDraft.value = NewTaskDraft()
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
     * 완료/미완료 토글 핸들러.
     * * [UX 개선] 
     * 완료(isDone=true) 처리되어도 즉시 아카이브로 이동하지 않고, 
     * 뷰모델의 필터링 로직에 의해 당일 동안은 메인 화면에 취소선 상태로 유지.
     */
    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            val newDone = !task.isDone
            repository.toggleDone(id = task.id, isDone = newDone)
            if (newDone) {
                // 완료: 알람 취소
                alarmScheduler.cancel(task.id)
            } else {
                // 미완료 복구: dueDate가 미래면 알람 재예약
                alarmScheduler.schedule(task.copy(isDone = false))
            }
        }
    }

    /**
     * 할 일을 휴지통으로 이동 (Soft Delete).
     * Snackbar "실행 취소"로 즉시 복구 가능.
     */
    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            alarmScheduler.cancel(task.id)
            repository.softDeleteTask(task.id)
            emitEvent(TaskEvent.TaskDeleted(task))
        }
    }

    /** 휴지통 이동 취소 (Snackbar Undo) */
    fun restoreTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.restoreFromTrash(task.id)
            alarmScheduler.schedule(task)
        }
    }

    /** 휴지통에서 복구 (TrashScreen 복구 버튼) */
    fun restoreFromTrash(task: TaskEntity) {
        viewModelScope.launch {
            repository.restoreFromTrash(task.id)
            alarmScheduler.schedule(task)
        }
    }

    /** 영구 삭제 (단건) */
    fun permanentlyDeleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    /** 휴지통 비우기 */
    fun emptyAllTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            emitEvent(TaskEvent.ShowMessage("휴지통을 비웠습니다."))
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
     * 아카이브 탭에서 현재 선택된 날짜의 완료 항목을 휴지통으로 일괄 이동.
     *
     * [버그 수정] 기존 deleteCompletedByDate()는 영구 삭제였음.
     * 각 항목에 softDeleteTask()를 적용하여 isDeleted = true 처리 후
     * 휴지통에서 복구 또는 영구 삭제할 수 있도록 변경.
     *
     * archiveTasks.value는 현재 선택 날짜의 완료 항목 목록이므로
     * 별도 날짜 범위 쿼리 없이 바로 사용 가능.
     */
    fun clearCompletedForSelectedDate() {
        viewModelScope.launch {
            val tasks = archiveTasks.value
            tasks.forEach { task ->
                alarmScheduler.cancel(task.id)
                repository.softDeleteTask(task.id)
            }
            emitEvent(TaskEvent.ShowMessage("${tasks.size}개 항목을 휴지통으로 이동했습니다."))
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

    /**
     * 오늘 완료된 항목을 아카이브로 수동 동기화 (즉시 기록).
     *
     * [UX 개선: 수동 아카이브]
     * - 기본적으로 완료 항목은 자정이 지나야 아카이브로 이동하지만,
     * 사용자가 원할 때 즉시 'isArchived = true'로 변경하여 아카이브에 기록.
     * - 동기화 후에도 당일 동안은 메인 화면(activeTasks)에 계속 유지된다.
     */
    fun syncCompletedTasksToArchive() {
        viewModelScope.launch {
            val todayStart = todayStartMs()
            val zone       = ZoneId.systemDefault()

            val targetsToSync = uiState.value.activeTasks.filter { task ->
                task.isDone && !task.isArchived && task.updatedAt >= todayStart
            }

            if (targetsToSync.isEmpty()) {
                emitEvent(TaskEvent.ShowMessage("새롭게 동기화할 완료 항목이 없습니다."))
                return@launch
            }

            targetsToSync.forEach { task ->
                // 완료 날짜(updatedAt)의 자정을 archivedAt으로 설정
                val completionDay = Instant.ofEpochMilli(task.updatedAt)
                    .atZone(zone).toLocalDate()
                    .atStartOfDay(zone).toInstant().toEpochMilli()
                repository.archiveTask(task.id, completionDay)
            }

            emitEvent(TaskEvent.ShowMessage("${targetsToSync.size}개의 할 일을 아카이브에 기록했습니다."))
        }
    }

    // ──────────────────────────────────────────────────────
    // 루틴 템플릿 그룹 CRUD
    // ──────────────────────────────────────────────────────

    /** 새 그룹 생성 */
    fun addTemplateGroup(name: String) {
        if (name.isBlank()) {
            emitEvent(TaskEvent.ShowMessage("그룹 이름을 입력해 주세요."))
            return
        }
        viewModelScope.launch {
            templateRepository.addGroup(name.trim())
        }
    }

    /** 그룹 활성화/비활성화 토글 */
    fun toggleTemplateGroupActive(id: Long, isActive: Boolean) {
        viewModelScope.launch {
            templateRepository.updateGroupActiveState(id, isActive)
        }
    }

    /** 그룹명 변경 */
    fun renameTemplateGroup(id: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            templateRepository.updateGroupName(id, name.trim())
        }
    }

    /** 그룹 삭제 (소속 할 일 CASCADE 삭제) */
    fun deleteTemplateGroup(id: Long) {
        viewModelScope.launch {
            templateRepository.deleteGroup(id)
        }
    }

    // ──────────────────────────────────────────────────────
    // 루틴 템플릿 할 일 CRUD
    // ──────────────────────────────────────────────────────

    /** 그룹에 할 일 추가 */
    fun addTemplateTask(
        groupId: Long,
        title: String,
        description: String? = null,
        priority: Int = Priority.MEDIUM.value,
        showOnLockScreen: Boolean = true,
    ) {
        if (title.isBlank()) {
            emitEvent(TaskEvent.ShowMessage("제목을 입력해 주세요."))
            return
        }
        viewModelScope.launch {
            templateRepository.addTask(
                RoutineTemplateTaskEntity(
                    groupId          = groupId,
                    title            = title.trim(),
                    description      = description?.trim(),
                    priority         = priority,
                    showOnLockScreen = showOnLockScreen,
                )
            )
        }
    }

    /** 할 일 단건 삭제 */
    fun deleteTemplateTask(id: Long) {
        viewModelScope.launch {
            templateRepository.deleteTask(id)
        }
    }

    // ──────────────────────────────────────────────────────
    // 자정 자동 아카이브 동기화 (하루 1회)
    // ──────────────────────────────────────────────────────

    /**
     * 자정이 지난 후 앱이 켜지거나 ViewModel이 init 될 때 한 번 실행.
     *
     * [아카이브 조건]
     * 1. 일반 할 일 (dueDate = null)                 → 어제 자정으로 아카이브
     * 2. 완료된 D-Day 할 일 (isDone=true)             → 완료 날짜(updatedAt) 자정으로 아카이브
     * 3. 기한 초과 미완료 D-Day (dueDate < today)     → dueDate 날짜 자정으로 아카이브
     * 4. 기한이 남은 D-Day (dueDate >= today, !done)  → D-Day 탭 유지 (아카이브 안 함)
     */
    private fun performMidnightSync() {
        viewModelScope.launch {
            val today = LocalDate.now().toString()   // "yyyy-MM-dd"
            val lastSync = syncPrefs.getString(PREF_KEY_LAST_MIDNIGHT_SYNC, null)
            if (lastSync == today) return@launch     // 오늘 이미 실행됨

            val todayStart    = todayStartMs()
            val yesterdayStart = todayStart - DAY_MS
            val zone          = ZoneId.systemDefault()

            val tasks = repository.getNonArchivedTasksOnce()
            var archivedCount = 0

            tasks.forEach { task ->
                val archivedAt: Long? = when {
                    // 조건 1: 일반 할 일 (dueDate 없음) → 어제 자정
                    task.dueDate == null ->
                        yesterdayStart

                    // 조건 2: 완료된 D-Day → 완료 날짜 자정
                    task.isDone ->
                        Instant.ofEpochMilli(task.updatedAt)
                            .atZone(zone).toLocalDate()
                            .atStartOfDay(zone).toInstant().toEpochMilli()

                    // 조건 3: 기한 초과 미완료 D-Day → dueDate 날짜 자정
                    task.dueDate < todayStart ->
                        Instant.ofEpochMilli(task.dueDate)
                            .atZone(zone).toLocalDate()
                            .atStartOfDay(zone).toInstant().toEpochMilli()

                    // 조건 4: 기한이 남은 미완료 D-Day → 아카이브 안 함
                    else -> null
                }

                if (archivedAt != null) {
                    repository.archiveTask(task.id, archivedAt)
                    archivedCount++
                }
            }

            syncPrefs.edit()
                .putString(PREF_KEY_LAST_MIDNIGHT_SYNC, today)
                .apply()
        }
    }

    // ──────────────────────────────────────────────────────
    // 루틴 자동 생성 (하루 1회, isActive 그룹만)
    // ──────────────────────────────────────────────────────

    /**
     * 앱 시작 시 오늘 날짜를 확인하고, 아직 루틴이 생성되지 않았으면
     * isActive == true 인 그룹의 할 일들을 TaskEntity로 복사한다.
     *
     * - 중복 방지: SharedPreferences의 last_routine_date 가 오늘이면 즉시 반환
     * - 비활성 그룹 제외: getActiveGroupsWithTasksOnce()로 isActive=1만 조회
     * - 날짜 기록: 템플릿이 없어도 오늘 날짜를 저장해 불필요한 재진입을 방지
     */
    private fun generateDailyRoutines() {
        viewModelScope.launch {
            val today = LocalDate.now().toString()   // "yyyy-MM-dd"
            val lastDate = routinePrefs.getString(PREF_KEY_LAST_ROUTINE_DATE, null)
            if (lastDate == today) return@launch

            val activeGroups = templateRepository.getActiveGroupsWithTasksOnce()
            var taskCount = 0

            activeGroups.forEach { groupWithTasks ->
                groupWithTasks.tasks.forEach { templateTask ->
                    repository.saveTask(
                        TaskEntity(
                            title            = templateTask.title,
                            description      = templateTask.description,
                            priority         = templateTask.priority,
                            showOnLockScreen = templateTask.showOnLockScreen,
                        )
                    )
                    taskCount++
                }
            }

            routinePrefs.edit()
                .putString(PREF_KEY_LAST_ROUTINE_DATE, today)
                .apply()

            if (taskCount > 0) {
                emitEvent(TaskEvent.ShowMessage("루틴 ${taskCount}개가 오늘의 할 일에 추가됐습니다."))
            }
        }
    }

    // ──────────────────────────────────────────────────────
    // 루틴 템플릿 — 즉시 주입 (Manual Injection)
    // ──────────────────────────────────────────────────────

    /**
     * 특정 템플릿 그룹의 할 일들을 오늘의 할 일로 즉시 복사·저장.
     *
     * - dueDate 없음 (기한 없는 오늘 할 일로 생성)
     * - isDone = false, isDeleted = false (기본값)
     * - 완료 시 ShowMessage 이벤트 방출 → MainScreen Snackbar 표시
     */
    fun applyTemplateNow(groupId: Long) {
        viewModelScope.launch {
            val groupWithTasks = templateRepository.getGroupWithTasksOnce(groupId) ?: return@launch
            if (groupWithTasks.tasks.isEmpty()) {
                emitEvent(TaskEvent.ShowMessage("이 템플릿에 등록된 할 일이 없습니다."))
                return@launch
            }
            groupWithTasks.tasks.forEach { templateTask ->
                repository.saveTask(
                    TaskEntity(
                        title           = templateTask.title,
                        description     = templateTask.description,
                        priority        = templateTask.priority,
                        showOnLockScreen = templateTask.showOnLockScreen,
                    )
                )
            }
            emitEvent(TaskEvent.ShowMessage("오늘 할 일에 추가되었습니다."))
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
        private const val PREF_KEY_LAST_ROUTINE_DATE    = "last_routine_date"
        private const val PREF_KEY_LAST_MIDNIGHT_SYNC   = "last_midnight_sync"

        /** 오늘 00:00:00.000 epoch ms (로컬 타임존 기준) */
        fun todayStartMs(): Long =
            LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
    }
}
