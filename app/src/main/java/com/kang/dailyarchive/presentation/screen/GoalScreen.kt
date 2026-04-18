package com.kang.dailyarchive.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kang.dailyarchive.R
import com.kang.dailyarchive.data.local.entity.GoalEntity
import com.kang.dailyarchive.data.local.entity.GoalType
import com.kang.dailyarchive.data.local.entity.PaceStatus
import com.kang.dailyarchive.data.local.entity.TaskEntity
import com.kang.dailyarchive.presentation.theme.AppIndigo
import com.kang.dailyarchive.presentation.theme.CardBorderColor
import com.kang.dailyarchive.presentation.viewmodel.GoalInputState
import com.kang.dailyarchive.presentation.viewmodel.GoalViewModel
import com.kang.dailyarchive.presentation.viewmodel.GoalWithProgress
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// ──────────────────────────────────────────────────────────────
// 색상 상수
// ──────────────────────────────────────────────────────────────

private val PaceColorSafe      = Color(0xFF10B981)
private val PaceColorNormal    = Color(0xFFF59E0B)
private val PaceColorWarning   = Color(0xFFEF4444)
private val PaceColorCompleted = Color(0xFF6B7280)
private val PaceColorNotStarted = Color(0xFF3B82F6)

private val GoalPresetColors = listOf(
    "#4F46E5",  // 인디고 (기본)
    "#EF4444",  // 빨간색
    "#10B981",  // 초록색
    "#F59E0B",  // 주황/황
    "#8B5CF6",  // 보라색
    "#3B82F6",  // 파란색
)

// ──────────────────────────────────────────────────────────────
// 헬퍼 함수
// ──────────────────────────────────────────────────────────────

private fun hexToColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    AppIndigo
}

private fun PaceStatus.color() = when (this) {
    PaceStatus.SAFE        -> PaceColorSafe
    PaceStatus.NORMAL      -> PaceColorNormal
    PaceStatus.WARNING     -> PaceColorWarning
    PaceStatus.COMPLETED   -> PaceColorCompleted
    PaceStatus.NOT_STARTED -> PaceColorNotStarted
}

private fun PaceStatus.label() = when (this) {
    PaceStatus.SAFE        -> "여유"
    PaceStatus.NORMAL      -> "정상"
    PaceStatus.WARNING     -> "위험"
    PaceStatus.COMPLETED   -> "완료"
    PaceStatus.NOT_STARTED -> "미시작"
}

private fun GoalType.label() = when (this) {
    GoalType.COUNT   -> "횟수"
    GoalType.FREQ    -> "주기"
    GoalType.PROJECT -> "프로젝트"
}

private fun Long.toLocalDateLabel(): String {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
    return date.format(DateTimeFormatter.ofPattern("yyyy.M.d", Locale.KOREA))
}

// ══════════════════════════════════════════════════════════════════
// GoalTabContent — 목표 목록 화면
// ══════════════════════════════════════════════════════════════════

@Composable
fun GoalTabContent(
    goalsWithProgress: List<GoalWithProgress>,
    onGoalClick:       (Long) -> Unit,
    modifier:          Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenSectionHeader(
            title    = "나의 목표",
            iconRes  = R.drawable.calendar_check,
            modifier = Modifier.padding(top = 8.dp),
        )

        if (goalsWithProgress.isEmpty()) {
            EmptyContent(
                iconRes    = R.drawable.calendar_check,
                message    = "등록된 목표가 없습니다",
                subMessage = "아래 + 버튼을 눌러 첫 번째 목표를 추가해 보세요.",
            )
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(goalsWithProgress, key = { it.goal.id }) { gwp ->
                    GoalCard(
                        gwp     = gwp,
                        onClick = { onGoalClick(gwp.goal.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// GoalCard — 단일 목표 카드
// ══════════════════════════════════════════════════════════════════

@Composable
fun GoalCard(
    gwp:      GoalWithProgress,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = hexToColor(gwp.goal.colorHex)
    val progressFraction = gwp.progressPercent / 100f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp))
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onClick,
            ),
    ) {
        // 왼쪽 컬러 바
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(72.dp)
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                .background(accentColor),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 제목 + 뱃지 행
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text     = gwp.goal.title,
                    style    = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color    = Color(0xFF1D1D1F),
                    modifier = Modifier.weight(1f),
                )
                // 목표 유형 뱃지
                GoalTypeBadge(gwp.goal.type, accentColor)
                // 페이스 상태 뱃지
                PaceBadge(gwp.paceStatus)
            }

            // 날짜 범위
            Text(
                text  = "${gwp.goal.startDate.toLocalDateLabel()} ~ ${gwp.goal.endDate.toLocalDateLabel()}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF9CA3AF),
            )

            // 진행률 바
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(
                        text  = "${gwp.completedCount} / ${gwp.totalCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6B7280),
                    )
                    Text(
                        text  = "${gwp.progressPercent}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = accentColor,
                    )
                }
                LinearProgressIndicator(
                    progress          = { progressFraction },
                    modifier          = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color             = accentColor,
                    trackColor        = accentColor.copy(alpha = 0.15f),
                    strokeCap         = StrokeCap.Round,
                )
            }

            // 페이스 메시지 / 권장 할당량
            if (gwp.paceMessage.isNotBlank()) {
                Text(
                    text  = gwp.paceMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6B7280),
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// GoalDetailContent — 목표 상세 화면
// ══════════════════════════════════════════════════════════════════

@Composable
fun GoalDetailContent(
    gwp:            GoalWithProgress?,
    tasks:          List<TaskEntity>,
    completedTasks: List<TaskEntity>,
    onBack:         () -> Unit,
    onToggleDone:   (TaskEntity) -> Unit,
    onDeleteTask:   (TaskEntity) -> Unit,
    onEditTask:     (TaskEntity) -> Unit,
    onDeleteGoal:   () -> Unit,
    onEditGoal:     () -> Unit,
    modifier:       Modifier = Modifier,
) {
    if (gwp == null) {
        EmptyContent(
            iconRes    = R.drawable.calendar_check,
            message    = "목표를 불러오는 중입니다.",
            subMessage = "",
        )
        return
    }

    val accentColor      = hexToColor(gwp.goal.colorHex)
    val progressFraction = gwp.progressPercent / 100f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // ── 상단 툴바 ────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter            = painterResource(R.drawable.move_left),
                    contentDescription = "뒤로 가기",
                    tint               = Color(0xFF6B7280),
                    modifier           = Modifier.size(22.dp),
                )
            }
            Text(
                text     = gwp.goal.title,
                style    = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color    = Color(0xFF1D1D1F),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onEditGoal) {
                Icon(
                    painter            = painterResource(R.drawable.pencil),
                    contentDescription = "목표 수정",
                    tint               = Color(0xFF6B7280),
                    modifier           = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = onDeleteGoal) {
                Icon(
                    painter            = painterResource(R.drawable.trash_2),
                    contentDescription = "목표 삭제",
                    tint               = MaterialTheme.colorScheme.error,
                    modifier           = Modifier.size(20.dp),
                )
            }
        }
        HorizontalDivider(color = CardBorderColor)

        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── 목표 요약 카드 ────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // 유형 + 페이스 상태
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GoalTypeBadge(gwp.goal.type, accentColor)
                        PaceBadge(gwp.paceStatus)
                    }

                    // 날짜
                    Text(
                        text  = "${gwp.goal.startDate.toLocalDateLabel()} ~ ${gwp.goal.endDate.toLocalDateLabel()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9CA3AF),
                    )

                    // 진행률 바
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text  = "진행률",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF6B7280),
                            )
                            Text(
                                text  = "${gwp.completedCount} / ${gwp.totalCount}  (${gwp.progressPercent}%)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = accentColor,
                            )
                        }
                        LinearProgressIndicator(
                            progress   = { progressFraction },
                            modifier   = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color      = accentColor,
                            trackColor = accentColor.copy(alpha = 0.15f),
                            strokeCap  = StrokeCap.Round,
                        )
                    }

                    // 페이스 메시지
                    if (gwp.paceMessage.isNotBlank()) {
                        Row(
                            modifier    = Modifier
                                .fillMaxWidth()
                                .background(
                                    gwp.paceStatus.color().copy(alpha = 0.07f),
                                    RoundedCornerShape(8.dp),
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text  = gwp.paceMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = gwp.paceStatus.color(),
                            )
                        }
                    }

                    // 권장 할당량
                    if (gwp.dailyQuota > 0) {
                        Text(
                            text  = "하루 권장 할당량: ${gwp.dailyQuota}개",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = accentColor,
                        )
                    }
                }
            }

            // ── 연결된 할 일 목록 ─────────────────────────────
            if (tasks.isNotEmpty()) {
                item {
                    Text(
                        text     = "연결된 할 일 (${tasks.size})",
                        style    = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color    = Color(0xFF6B7280),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                items(tasks, key = { "goal_task_${it.id}" }) { task ->
                    TaskItem(
                        task         = task,
                        onToggleDone = { onToggleDone(task) },
                        onDelete     = { onDeleteTask(task) },
                        onEdit       = { onEditTask(task) },
                        modifier     = Modifier.animateItem(),
                    )
                }
            } else {
                item {
                    Column(
                        modifier  = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            painter            = painterResource(R.drawable.plus),
                            contentDescription = null,
                            tint               = Color(0xFFD1D5DB),
                            modifier           = Modifier.size(32.dp),
                        )
                        Text(
                            text  = "연결된 할 일이 없습니다",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF9CA3AF),
                        )
                        Text(
                            text  = "+ 버튼으로 이 목표에 할 일을 추가해 보세요.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD1D5DB),
                        )
                    }
                }
            }

            if (completedTasks.isNotEmpty()) {
                item {
                    Text(
                        text     = "완료된 할 일 (${completedTasks.size})",
                        style    = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color    = Color(0xFF9CA3AF), // 활성 할 일보다 살짝 연한 색상
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                    )
                }
                items(completedTasks, key = { "goal_completed_${it.id}" }) { task ->
                    TaskItem(
                        task         = task,
                        onToggleDone = { onToggleDone(task) }, // 체크 해제 시 다시 위로 올라감
                        onDelete     = { onDeleteTask(task) },
                        onEdit       = { onEditTask(task) },
                        modifier     = Modifier.animateItem(),
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// AddGoalBottomSheet — 목표 추가/수정 시트
// ══════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalBottomSheet(
    goalViewModel: GoalViewModel,
    onDismiss:     () -> Unit,
    editGoal:      GoalEntity? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope      = rememberCoroutineScope()
    val zone       = ZoneId.systemDefault()
    val isEditMode = editGoal != null

    // 수정 모드 초기화
    val initial = remember {
        if (editGoal != null) {
            GoalInputState(
                title       = editGoal.title,
                type        = editGoal.type,
                targetValue = editGoal.targetValue,
                startDate   = Instant.ofEpochMilli(editGoal.startDate).atZone(zone).toLocalDate(),
                endDate     = Instant.ofEpochMilli(editGoal.endDate).atZone(zone).toLocalDate(),
                colorHex    = editGoal.colorHex,
            )
        } else GoalInputState()
    }

    var title        by remember { mutableStateOf(initial.title) }
    var type         by remember { mutableStateOf(initial.type) }
    var targetStr    by remember { mutableStateOf(
        if (initial.type == GoalType.PROJECT) "" else initial.targetValue.toString()
    ) }
    var startDate    by remember { mutableStateOf(initial.startDate) }
    var endDate      by remember { mutableStateOf(initial.endDate) }
    var colorHex     by remember { mutableStateOf(initial.colorHex) }
    var titleError   by remember { mutableStateOf(false) }
    var targetError  by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker   by remember { mutableStateOf(false) }

    val targetValue = targetStr.toIntOrNull() ?: 0

    fun submit() {
        titleError  = title.isBlank()
        targetError = type != GoalType.PROJECT && targetValue <= 0
        if (titleError || targetError) return
        goalViewModel.updateInput {
            copy(
                title       = title.trim(),
                type        = type,
                targetValue = if (type == GoalType.PROJECT) 1 else targetValue,
                startDate   = startDate,
                endDate     = endDate,
                colorHex    = colorHex,
            )
        }
        if (isEditMode) {
            goalViewModel.updateGoal(editGoal!!.id) {
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
            }
        } else {
            goalViewModel.saveGoal {
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
            }
        }
    }

    // ── DatePicker: 시작일 ────────────────────────────────────
    if (showStartPicker) {
        GoalDatePickerDialog(
            initial   = startDate,
            onConfirm = { startDate = it; if (endDate.isBefore(it)) endDate = it; showStartPicker = false },
            onDismiss = { showStartPicker = false },
        )
    }

    // ── DatePicker: 종료일 ────────────────────────────────────
    if (showEndPicker) {
        GoalDatePickerDialog(
            initial   = endDate,
            minDate   = startDate,
            onConfirm = { endDate = it; showEndPicker = false },
            onDismiss = { showEndPicker = false },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor   = Color.White,
        dragHandle = {
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE5E7EB)),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── 헤더 ──────────────────────────────────────────
            Text(
                text  = if (isEditMode) "목표 수정" else "새 목표",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1D1D1F),
            )

            // ── 제목 ──────────────────────────────────────────
            TextField(
                value         = title,
                onValueChange = { title = it; if (it.isNotBlank()) titleError = false },
                label         = { Text("목표 제목 *") },
                placeholder   = { Text("어떤 목표를 이루고 싶으세요?") },
                isError       = titleError,
                supportingText = if (titleError) { { Text("제목을 입력해 주세요.") } } else null,
                singleLine    = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction      = ImeAction.Next,
                ),
                colors  = textFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            // ── 목표 유형 ─────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("목표 유형", style = MaterialTheme.typography.labelMedium, color = Color(0xFF6B7280))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoalType.values().forEach { t ->
                        val accentColor = hexToColor(colorHex)
                        FilterChip(
                            selected = type == t,
                            onClick  = { type = t; if (t == GoalType.PROJECT) targetStr = "" },
                            label    = { Text(t.label()) },
                            shape    = CircleShape,
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.15f),
                                selectedLabelColor     = accentColor,
                            ),
                            border   = FilterChipDefaults.filterChipBorder(
                                enabled             = true,
                                selected            = type == t,
                                selectedBorderColor = accentColor,
                                selectedBorderWidth = 1.5.dp,
                            ),
                        )
                    }
                }
            }

            // ── 목표치 (PROJECT 타입 제외) ────────────────────
            AnimatedVisibility(visible = type != GoalType.PROJECT) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val label = if (type == GoalType.FREQ) "주당 목표 횟수 *" else "총 목표 횟수 *"
                    TextField(
                        value         = targetStr,
                        onValueChange = {
                            targetStr   = it.filter { c -> c.isDigit() }
                            targetError = false
                        },
                        label         = { Text(label) },
                        isError       = targetError,
                        supportingText = if (targetError) { { Text("1 이상의 숫자를 입력해 주세요.") } } else null,
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction    = ImeAction.Next,
                        ),
                        colors   = textFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // ── 날짜 범위 ─────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("기간", style = MaterialTheme.typography.labelMedium, color = Color(0xFF6B7280))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    DateSelectBox(
                        label    = "시작일",
                        date     = startDate,
                        onClick  = { showStartPicker = true },
                        modifier = Modifier.weight(1f),
                    )
                    Text("~", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF6B7280))
                    DateSelectBox(
                        label    = "종료일",
                        date     = endDate,
                        onClick  = { showEndPicker = true },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ── 색상 선택 ─────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("강조 색상", style = MaterialTheme.typography.labelMedium, color = Color(0xFF6B7280))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GoalPresetColors.forEach { hex ->
                        val c = hexToColor(hex)
                        val isSelected = colorHex == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .then(
                                    if (isSelected) Modifier.border(2.5.dp, Color(0xFF1D1D1F), CircleShape)
                                    else Modifier
                                )
                                .clickable(
                                    indication        = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick           = { colorHex = hex },
                                ),
                        ) {
                            if (isSelected) {
                                Icon(
                                    painter            = painterResource(R.drawable.check),
                                    contentDescription = null,
                                    tint               = Color.White,
                                    modifier           = Modifier
                                        .size(16.dp)
                                        .align(Alignment.Center),
                                )
                            }
                        }
                    }
                }
            }

            // ── 버튼 행 ───────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                }) {
                    Text("취소")
                }
                Button(
                    onClick = ::submit,
                    enabled = title.isNotBlank() && (type == GoalType.PROJECT || targetValue > 0),
                ) {
                    Text(if (isEditMode) "저장" else "추가")
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ──────────────────────────────────────────────────────────────
// 소형 컴포저블
// ──────────────────────────────────────────────────────────────

@Composable
private fun GoalTypeBadge(type: GoalType, accentColor: Color) {
    Box(
        modifier = Modifier
            .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text  = type.label(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = accentColor,
        )
    }
}

@Composable
private fun PaceBadge(status: PaceStatus) {
    val color = status.color()
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text  = status.label(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color,
        )
    }
}

@Composable
private fun DateSelectBox(
    label:    String,
    date:     LocalDate,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
            .border(1.dp, CardBorderColor, RoundedCornerShape(8.dp))
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter            = painterResource(R.drawable.calendar),
            contentDescription = null,
            tint               = Color(0xFF6B7280),
            modifier           = Modifier.size(14.dp),
        )
        Column {
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF9CA3AF),
            )
            Text(
                text  = date.format(DateTimeFormatter.ofPattern("yy.M.d", Locale.KOREA)),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFF1D1D1F),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalDatePickerDialog(
    initial:   LocalDate,
    minDate:   LocalDate? = null,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialUtcMs = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val minUtcMs     = minDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()

    val dpState = rememberDatePickerState(
        initialSelectedDateMillis = initialUtcMs,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) =
                if (minUtcMs != null) utcTimeMillis >= minUtcMs else true
        },
    )
    val dpColors = DatePickerDefaults.colors(
        containerColor             = Color.White,
        titleContentColor          = Color(0xFF6B7280),
        headlineContentColor       = Color(0xFF1D1D1F),
        weekdayContentColor        = Color(0xFF6B7280),
        subheadContentColor        = Color(0xFF1D1D1F),
        navigationContentColor     = Color(0xFF1D1D1F),
        yearContentColor           = Color(0xFF1D1D1F),
        currentYearContentColor    = AppIndigo,
        selectedYearContentColor   = Color.White,
        selectedYearContainerColor = AppIndigo,
        dayContentColor            = Color(0xFF1D1D1F),
        disabledDayContentColor    = Color(0xFFD1D5DB),
        selectedDayContentColor    = Color.White,
        selectedDayContainerColor  = AppIndigo,
        todayContentColor          = AppIndigo,
        todayDateBorderColor       = AppIndigo,
        dividerColor               = Color(0xFFE5E7EB),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                dpState.selectedDateMillis?.let { utcMs ->
                    val date = Instant.ofEpochMilli(utcMs)
                        .atZone(ZoneOffset.UTC).toLocalDate()
                    onConfirm(date)
                }
            }) { Text("확인") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
        shape  = RoundedCornerShape(20.dp),
        colors = dpColors,
    ) {
        DatePicker(state = dpState, colors = dpColors)
    }
}

@Composable
private fun textFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor      = Color.Transparent,
    unfocusedContainerColor    = Color.Transparent,
    focusedIndicatorColor      = AppIndigo,
    unfocusedIndicatorColor    = Color(0xFFE5E7EB),
    errorContainerColor        = Color.Transparent,
    errorIndicatorColor        = Color(0xFFEF4444),
    disabledContainerColor     = Color.Transparent,
    focusedTextColor           = Color(0xFF1D1D1F),
    unfocusedTextColor         = Color(0xFF1D1D1F),
    focusedLabelColor          = AppIndigo,
    unfocusedLabelColor        = Color(0xFF6B7280),
    errorSupportingTextColor   = Color(0xFFEF4444),
)
