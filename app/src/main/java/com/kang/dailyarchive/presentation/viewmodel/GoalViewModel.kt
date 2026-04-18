package com.kang.dailyarchive.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kang.dailyarchive.data.local.entity.GoalEntity
import com.kang.dailyarchive.data.local.entity.GoalType
import com.kang.dailyarchive.data.local.entity.PaceStatus
import com.kang.dailyarchive.data.local.entity.TaskEntity
import com.kang.dailyarchive.data.repository.GoalRepository
import com.kang.dailyarchive.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.roundToInt

// ──────────────────────────────────────────────────────────────
// UI 표현용 데이터 클래스
// ──────────────────────────────────────────────────────────────

/**
 * 하나의 목표에 대한 진행률·페이스 정보를 담는 UI 상태 클래스.
 *
 * @param goal             원본 GoalEntity
 * @param progressPercent  현재 달성률 0~100 (Int)
 * @param completedCount   완료된 Task / 달성 횟수
 * @param totalCount       목표치 또는 전체 Task 수
 * @param paceStatus       SAFE / NORMAL / WARNING / COMPLETED / NOT_STARTED
 * @param dailyQuota       하루 권장 할당량 (0이면 표시 불필요)
 * @param paceMessage      권장 할당량을 포함한 안내 문구
 */
data class GoalWithProgress(
    val goal: GoalEntity,
    val progressPercent: Int        = 0,
    val completedCount: Int         = 0,
    val totalCount: Int             = 0,
    val paceStatus: PaceStatus      = PaceStatus.NOT_STARTED,
    val dailyQuota: Int             = 0,
    val paceMessage: String         = "",
)

/**
 * 목표 추가/수정 다이얼로그 입력 상태.
 */
data class GoalInputState(
    val title: String          = "",
    val type: GoalType         = GoalType.COUNT,
    val targetValue: Int       = 10,
    val startDate: LocalDate   = LocalDate.now(),
    val endDate: LocalDate     = LocalDate.now().plusDays(30),
    val colorHex: String       = "#4F46E5",
) {
    val isValid: Boolean
        get() = title.isNotBlank() &&
                targetValue > 0 &&
                !endDate.isBefore(startDate)
}

// ──────────────────────────────────────────────────────────────
// ViewModel
// ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GoalViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val taskRepository: TaskRepository,
) : ViewModel() {

    private val zone = ZoneId.systemDefault()

    // ── 입력 폼 상태 ────────────────────────────────────────
    private val _inputState = MutableStateFlow(GoalInputState())
    val inputState: StateFlow<GoalInputState> = _inputState.asStateFlow()

    fun updateInput(update: GoalInputState.() -> GoalInputState) {
        _inputState.value = _inputState.value.update()
    }

    fun resetInput(goal: GoalEntity? = null) {
        _inputState.value = if (goal != null) {
            GoalInputState(
                title       = goal.title,
                type        = goal.type,
                targetValue = goal.targetValue,
                startDate   = epochToLocalDate(goal.startDate),
                endDate     = epochToLocalDate(goal.endDate),
                colorHex    = goal.colorHex,
            )
        } else {
            GoalInputState()
        }
    }

    // ── 선택된 목표 (GoalDetail 화면용) ─────────────────────
    private val _selectedGoalId = MutableStateFlow<Long?>(null)
    val selectedGoalId: StateFlow<Long?> = _selectedGoalId.asStateFlow()

    fun selectGoal(id: Long?) { _selectedGoalId.value = id }

    /** 선택된 목표의 활성 Task 목록 */
    val selectedGoalTasks: StateFlow<List<TaskEntity>> = _selectedGoalId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else goalRepository.getActiveTasksByGoalId(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 선택된 목표의 완료된 Task 목록 */
    val completedGoalTasks: StateFlow<List<TaskEntity>> = _selectedGoalId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else taskRepository.getCompletedTasksByGoalId(id) 
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── 전체 목표 + 진행률 목록 ─────────────────────────────
    /**
     * 전체 목표에 대해 GoalWithProgress를 계산한다.
     *
     * [구현 전략]
     * - getAllGoals() Flow를 flatMapLatest로 목록 변경 감지
     * - 각 Goal에 대해 countCompleted* Flow를 combine
     * - 목록 크기가 커지면 개별 Flow combine 비용이 높아질 수 있으므로
     *   실제 대규모 데이터에서는 Room @DatabaseView 또는 집계 쿼리로 전환 권장
     */
    val goalsWithProgress: StateFlow<List<GoalWithProgress>> =
        goalRepository.getAllGoals()
            .flatMapLatest { goals ->
                if (goals.isEmpty()) return@flatMapLatest flowOf(emptyList())

                // 각 Goal별 Flow<GoalWithProgress> 생성 후 combine
                val progressFlows: List<Flow<GoalWithProgress>> = goals.map { goal ->
                    buildProgressFlow(goal)
                }

                // List<Flow<T>> → Flow<List<T>>
                combine(progressFlows) { array -> array.toList() }
            }
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    // ── 단일 목표 상세 (GoalDetail 화면) ────────────────────
    val selectedGoalWithProgress: StateFlow<GoalWithProgress?> = _selectedGoalId
        .flatMapLatest { id ->
            if (id == null) return@flatMapLatest flowOf(null)
            goalRepository.getGoalById(id)
                .flatMapLatest { goal ->
                    if (goal == null) flowOf(null)
                    else buildProgressFlow(goal).map { it as GoalWithProgress? }
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── 저장 / 삭제 ─────────────────────────────────────────

    fun saveGoal(onSuccess: (Long) -> Unit = {}) {
        val s = _inputState.value
        if (!s.isValid) return
        viewModelScope.launch {
            val id = goalRepository.saveGoal(
                GoalEntity(
                    title       = s.title.trim(),
                    type        = s.type,
                    targetValue = s.targetValue,
                    startDate   = s.startDate.atStartOfDay(zone).toInstant().toEpochMilli(),
                    endDate     = s.endDate.atStartOfDay(zone).toInstant().toEpochMilli(),
                    colorHex    = s.colorHex,
                )
            )
            resetInput()
            onSuccess(id)
        }
    }

    fun updateGoal(goalId: Long, onSuccess: () -> Unit = {}) {
        val s = _inputState.value
        if (!s.isValid) return
        viewModelScope.launch {
            goalRepository.saveGoal(
                GoalEntity(
                    id          = goalId,
                    title       = s.title.trim(),
                    type        = s.type,
                    targetValue = s.targetValue,
                    startDate   = s.startDate.atStartOfDay(zone).toInstant().toEpochMilli(),
                    endDate     = s.endDate.atStartOfDay(zone).toInstant().toEpochMilli(),
                    colorHex    = s.colorHex,
                )
            )
            resetInput()
            onSuccess()
        }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch { goalRepository.deleteGoal(id) }
    }

    // ──────────────────────────────────────────────────────
    // 페이스 메이커 핵심 계산 로직
    // ──────────────────────────────────────────────────────

    /**
     * 하나의 [GoalEntity]에 대한 진행률 + 페이스 Flow를 생성한다.
     *
     * [GoalType 별 분기]
     * - COUNT   : 기간 내 완료 횟수 / targetValue
     * - FREQ    : 기간 전체 주 수 × targetValue 대비 완료 횟수
     * - PROJECT : 전체 연결 Task 중 완료 Task 비율
     */
    private fun buildProgressFlow(goal: GoalEntity): Flow<GoalWithProgress> {
        val endOfDay = goal.endDate + DAY_MS - 1L   // 종료일 23:59:59.999

        return when (goal.type) {

            GoalType.COUNT -> combine(
                goalRepository.countCompletedInRange(goal.id, goal.startDate, endOfDay),
                flowOf(goal.targetValue),
            ) { completed, target ->
                val progress = calcProgress(completed, target)
                val pace     = calcPace(goal, completed, target)
                GoalWithProgress(
                    goal            = goal,
                    progressPercent = progress,
                    completedCount  = completed,
                    totalCount      = target,
                    paceStatus      = pace.status,
                    dailyQuota      = pace.dailyQuota,
                    paceMessage     = pace.message,
                )
            }

            GoalType.FREQ -> combine(
                goalRepository.countCompletedInRange(goal.id, goal.startDate, endOfDay),
                flowOf(calcFreqTotal(goal)),
            ) { completed, freqTotal ->
                val progress = calcProgress(completed, freqTotal)
                val pace     = calcPace(goal, completed, freqTotal)
                GoalWithProgress(
                    goal            = goal,
                    progressPercent = progress,
                    completedCount  = completed,
                    totalCount      = freqTotal,
                    paceStatus      = pace.status,
                    dailyQuota      = pace.dailyQuota,
                    paceMessage     = pace.message,
                )
            }

            GoalType.PROJECT -> combine(
                goalRepository.countCompletedTasks(goal.id),
                goalRepository.countTotalTasks(goal.id),
            ) { completed, total ->
                val safeTotal = total.coerceAtLeast(1)
                val progress  = calcProgress(completed, safeTotal)
                val pace      = calcPace(goal, completed, safeTotal)
                GoalWithProgress(
                    goal            = goal,
                    progressPercent = progress,
                    completedCount  = completed,
                    totalCount      = total,
                    paceStatus      = pace.status,
                    dailyQuota      = pace.dailyQuota,
                    paceMessage     = pace.message,
                )
            }
        }
    }

    // ──────────────────────────────────────────────────────
    // 페이스 메이커 — 내부 계산 헬퍼
    // ──────────────────────────────────────────────────────

    /** 계산 결과를 담는 내부 데이터 클래스 */
    private data class PaceResult(
        val status:     PaceStatus,
        val dailyQuota: Int,
        val message:    String,
    )

    /**
     * 페이스 메이커 핵심 로직.
     *
     * [계산 공식]
     * - totalDays      = endDate ~ startDate 일수
     * - elapsedDays    = today ~ startDate 일수 (0 이상, totalDays 이하로 clamp)
     * - expectedRatio  = elapsedDays / totalDays  (기간 경과율)
     * - actualRatio    = completedCount / totalTarget (실제 달성률)
     * - paceRatio      = actualRatio / expectedRatio  (상대 달성률)
     *
     * [상태 기준]
     *   paceRatio >= 1.0  → SAFE
     *   paceRatio >= 0.75 → NORMAL
     *   paceRatio < 0.75  → WARNING
     *
     * [권장 할당량]
     * - remaining = totalTarget - completedCount
     * - daysLeft  = endDate - today (0 이하면 마감)
     * - dailyQuota = ceil(remaining / daysLeft)
     */
    private fun calcPace(
        goal:       GoalEntity,
        completed:  Int,
        total:      Int,
    ): PaceResult {
        val today      = LocalDate.now(zone)
        val startLocal = epochToLocalDate(goal.startDate)
        val endLocal   = epochToLocalDate(goal.endDate)

        // 시작 전
        if (today.isBefore(startLocal)) {
            return PaceResult(PaceStatus.NOT_STARTED, 0, "아직 시작 전인 목표예요.")
        }

        // 완료
        if (completed >= total && total > 0) {
            return PaceResult(PaceStatus.COMPLETED, 0, "🎉 목표를 달성했어요!")
        }

        val totalDays   = ChronoUnit.DAYS.between(startLocal, endLocal).toInt().coerceAtLeast(1)
        val elapsedDays = ChronoUnit.DAYS.between(startLocal, today).toInt()
            .coerceIn(0, totalDays)
        val daysLeft    = ChronoUnit.DAYS.between(today, endLocal).toInt()

        // 마감 초과
        if (daysLeft < 0) {
            return PaceResult(
                status     = PaceStatus.WARNING,
                dailyQuota = 0,
                message    = "마감일이 지났어요. (미달성: ${total - completed}개)",
            )
        }

        val expectedRatio = elapsedDays.toFloat() / totalDays
        val actualRatio   = if (total > 0) completed.toFloat() / total else 0f

        // 기간 시작 직후 expectedRatio=0: 뭘 해도 SAFE
        val paceRatio = if (expectedRatio == 0f) 1f else actualRatio / expectedRatio

        val remaining  = (total - completed).coerceAtLeast(0)
        val dailyQuota = if (daysLeft > 0) ceil(remaining.toDouble() / daysLeft).toInt()
                         else remaining

        val status = when {
            paceRatio >= 1.0f  -> PaceStatus.SAFE
            paceRatio >= 0.75f -> PaceStatus.NORMAL
            else               -> PaceStatus.WARNING
        }

        val statusEmoji = when (status) {
            PaceStatus.SAFE    -> "✅"
            PaceStatus.NORMAL  -> "🟡"
            PaceStatus.WARNING -> "⚠️"
            else               -> ""
        }

        val message = buildString {
            append("$statusEmoji ")
            when (status) {
                PaceStatus.SAFE    -> append("여유 있어요! ")
                PaceStatus.NORMAL  -> append("잘 가고 있어요. ")
                PaceStatus.WARNING -> append("속도를 높여야 해요! ")
                else               -> {}
            }
            if (dailyQuota > 0) {
                append("하루 권장 ${dailyQuota}개 · 남은 ${daysLeft}일")
            }
        }

        return PaceResult(status, dailyQuota, message)
    }

    /** 진행률 0~100 계산 */
    private fun calcProgress(completed: Int, total: Int): Int {
        if (total <= 0) return 0
        return ((completed.toFloat() / total) * 100).roundToInt().coerceIn(0, 100)
    }

    /**
     * FREQ 타입의 전체 목표 횟수 계산.
     * 기간을 주(week) 단위로 나눠 (주 수 × targetValue) 를 총 목표치로 사용한다.
     * 부분 주도 1주로 올림 처리한다.
     */
    private fun calcFreqTotal(goal: GoalEntity): Int {
        val start = epochToLocalDate(goal.startDate)
        val end   = epochToLocalDate(goal.endDate)
        val days  = ChronoUnit.DAYS.between(start, end).toInt().coerceAtLeast(1)
        val weeks = ceil(days / 7.0).toInt().coerceAtLeast(1)
        return weeks * goal.targetValue
    }

    private fun epochToLocalDate(epochMs: Long): LocalDate =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()

    companion object {
        private const val DAY_MS = 24 * 60 * 60 * 1_000L
    }
}
