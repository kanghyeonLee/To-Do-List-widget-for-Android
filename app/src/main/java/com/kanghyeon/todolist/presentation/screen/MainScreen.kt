package com.kanghyeon.todolist.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanghyeon.todolist.data.local.entity.Priority
import com.kanghyeon.todolist.data.local.entity.TaskEntity
import com.kanghyeon.todolist.presentation.theme.PriorityHigh
import com.kanghyeon.todolist.presentation.theme.PriorityLow
import com.kanghyeon.todolist.presentation.theme.PriorityMedium
import com.kanghyeon.todolist.presentation.viewmodel.TaskEvent
import com.kanghyeon.todolist.presentation.viewmodel.TaskUiState
import com.kanghyeon.todolist.presentation.viewmodel.TaskViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ══════════════════════════════════════════════════════════════════
// MainScreen — 루트 컴포저블
// ══════════════════════════════════════════════════════════════════

/**
 * 앱의 루트 화면.
 *
 * [연동 흐름]
 * ViewModel.uiState (StateFlow)
 *   └─ collectAsStateWithLifecycle() → 생명주기 인식 수집
 *       └─ TaskList UI 재구성
 *
 * ViewModel.events (Channel → Flow)
 *   └─ LaunchedEffect로 수집 → SnackbarHost 표시
 *       └─ TaskDeleted: "실행 취소" 버튼 제공 → restoreTask() 호출
 *
 * 사용자 액션 → ViewModel → Repository → Room DB 변경
 *   └─ TodoForegroundService가 getLockScreenTasks() Flow를 구독 중
 *       └─ DB 변경 감지 → 잠금화면 알림 자동 갱신 ✓
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: TaskViewModel = hiltViewModel(),
) {
    val uiState      by viewModel.uiState.collectAsStateWithLifecycle()
    val archiveDate  by viewModel.selectedArchiveDate.collectAsStateWithLifecycle()
    val archiveTasks by viewModel.archiveTasks.collectAsStateWithLifecycle()

    val editingTask     by viewModel.editingTask.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // TopAppBar 스크롤 연동 (스크롤 시 TopAppBar 축소)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // ── 일회성 이벤트 수집 ─────────────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TaskEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short,
                    )
                }
                is TaskEvent.TaskDeleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "「${event.task.title}」을(를) 삭제했습니다.",
                        actionLabel = "실행 취소",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.restoreTask(event.task)
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "할 일",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    )
                },
                actions = {
                    // 아카이브 탭 + 선택 날짜에 항목이 있을 때만 삭제 버튼 노출
                    if (selectedTab == 1 && archiveTasks.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearCompletedForSelectedDate) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "이 날의 완료 항목 삭제",
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            // 아카이브 탭에서는 FAB 숨김
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showBottomSheet = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("할 일 추가") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->

        Column(modifier = Modifier.padding(innerPadding)) {

            // ── 탭: 할 일 / 아카이브 ─────────────────────────
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        val count = uiState.activeTasks.size
                        Text(if (count > 0) "할 일 ($count)" else "할 일")
                    },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        val count = uiState.completedTasks.size
                        Text(if (count > 0) "아카이브 ($count)" else "아카이브")
                    },
                )
            }

            // ── 탭 콘텐츠 ────────────────────────────────────
            when {
                uiState.isLoading -> LoadingContent()
                selectedTab == 0  -> TodoContent(uiState, viewModel, onEdit = { viewModel.setEditingTask(it) })
                else              -> ArchiveContent(archiveDate, archiveTasks, viewModel)
            }
        }
    }

    // ── 할 일 추가 / 수정 BottomSheet ────────────────────────
    if (showBottomSheet || editingTask != null) {
        AddTaskBottomSheet(
            task = editingTask,
            onDismiss = {
                showBottomSheet = false
                viewModel.setEditingTask(null)
            },
            onSave = { title, desc, priority, dueDate, showOnLock, reminderMinutes ->
                viewModel.saveCurrentTask(
                    title            = title,
                    description      = desc,
                    priority         = priority,
                    dueDate          = dueDate,
                    showOnLockScreen = showOnLock,
                    reminderMinutes  = reminderMinutes,
                )
                showBottomSheet = false
            },
        )
    }
}

// ══════════════════════════════════════════════════════════════════
// 탭 콘텐츠: 할 일 (미완료 priority 섹션)
// ══════════════════════════════════════════════════════════════════

/**
 * 미완료 할 일 전체를 HIGH / MEDIUM / LOW 섹션으로 나눠 표시.
 *
 * 데이터 출처: [TaskUiState.activeTasks]
 *   - DAO: isDone = 0, ORDER BY priority DESC, createdAt DESC
 *   - UI : groupBy(Priority) → 섹션별 PriorityGroupCard
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TodoContent(
    uiState: TaskUiState,
    viewModel: TaskViewModel,
    onEdit: (TaskEntity) -> Unit,
) {
    if (uiState.activeTasks.isEmpty()) {
        EmptyContent(
            icon = Icons.Outlined.CheckCircle,
            message = "할 일이 없어요",
            subMessage = "아래 버튼을 눌러 첫 번째 할 일을 추가해 보세요.",
        )
        return
    }

    val grouped = uiState.activeTasks.groupBy { Priority.from(it.priority) }

    data class PriorityMeta(val label: String, val accent: Color)
    val priorityMeta = mapOf(
        Priority.HIGH   to PriorityMeta("높음", PriorityHigh),
        Priority.MEDIUM to PriorityMeta("중간", PriorityMedium),
        Priority.LOW    to PriorityMeta("낮음", PriorityLow),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Priority.entries
            .sortedByDescending { it.value }
            .forEach { priority ->
                val tasks = grouped[priority]
                if (!tasks.isNullOrEmpty()) {
                    val meta = priorityMeta.getValue(priority)

                    stickyHeader(key = "priority_header_${priority.name}") {
                        PrioritySectionHeader(
                            label = meta.label,
                            count = tasks.size,
                            dotColor = meta.accent,
                        )
                    }

                    item(key = "priority_card_${priority.name}") {
                        PriorityGroupCard(
                            tasks = tasks,
                            accentColor = meta.accent,
                            bgColor = MaterialTheme.colorScheme.surface,
                            onToggleDone = { task -> viewModel.toggleDone(task.id, task.isDone) },
                            onDelete = { task -> viewModel.deleteTask(task) },
                            onEdit = { task -> onEdit(task) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }

        item { Spacer(Modifier.height(80.dp)) } // FAB 여백
    }
}

// ══════════════════════════════════════════════════════════════════
// 탭 콘텐츠: 아카이브 (날짜별 완료 항목)
// ══════════════════════════════════════════════════════════════════

/**
 * 상단 HorizontalDateStrip으로 날짜를 선택하면 해당 날짜에 완료된 할 일만 표시.
 *
 * 데이터 흐름:
 *   ViewModel._selectedArchiveDate (MutableStateFlow)
 *     └─ flatMapLatest → repository.getCompletedTasksByDate()
 *         └─ archiveTasks (StateFlow) → 이 함수에서 수신
 *
 * @param archiveDate  현재 선택된 날짜의 시작 epoch ms
 * @param archiveTasks 해당 날짜에 완료된 할 일 목록 (updatedAt DESC)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchiveContent(
    archiveDate: Long,
    archiveTasks: List<TaskEntity>,
    viewModel: TaskViewModel,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    // ── 백업용 DatePickerDialog ───────────────────────────
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = archiveDate,
            selectableDates = object : SelectableDates {
                // UTC 자정 기준 오늘 이하만 허용 (+1일 버퍼로 타임존 오차 흡수)
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= System.currentTimeMillis() + 86_400_000L
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.selectArchiveDate(it) }
                    showDatePicker = false
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("취소") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── 가로 날짜 바 ──────────────────────────────────
        HorizontalDateStrip(
            selectedDateMs  = archiveDate,
            onDateSelected  = { viewModel.selectArchiveDate(it) },
            onCalendarClick = { showDatePicker = true },
        )
        HorizontalDivider()

        // ── 해당 날짜 완료 목록 ───────────────────────────
        if (archiveTasks.isEmpty()) {
            EmptyContent(
                icon       = Icons.Outlined.CheckCircle,
                message    = "이 날 완료된 할 일이 없어요",
                subMessage = "할 일을 체크하면 날짜별로 기록됩니다.",
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = archiveTasks,
                    key   = { "archive_${it.id}" },
                ) { task ->
                    TaskItem(
                        task         = task,
                        onToggleDone = { viewModel.toggleDone(task.id, task.isDone) },
                        onDelete     = { viewModel.deleteTask(task) },
                        modifier     = Modifier.animateItem(),
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// HorizontalDateStrip — 가로 스크롤형 날짜 바
// ══════════════════════════════════════════════════════════════════

/** 날짜 바에 표시할 과거 날짜 수 (오늘 포함 총 DATE_STRIP_TOTAL일) */
private const val DATE_STRIP_DAYS_BACK = 89  // 오늘 + 89일 전 = 90개

/**
 * 가로 스크롤 날짜 선택 바.
 *
 * [설계]
 * - 오늘 포함 최근 90일을 LazyRow로 나열 (오래된 날짜가 왼쪽)
 * - 선택된 날짜는 자동으로 중앙 근처로 스크롤 (animateScrollToItem)
 * - 오른쪽 끝 캘린더 아이콘 → 백업 DatePickerDialog 호출
 *
 * @param selectedDateMs  현재 선택된 날짜의 시작 epoch ms (로컬 자정)
 * @param onDateSelected  날짜 칩 클릭 시 해당 날짜의 시작 epoch ms 전달
 * @param onCalendarClick 캘린더 아이콘 클릭 콜백
 */
@Composable
private fun HorizontalDateStrip(
    selectedDateMs: Long,
    onDateSelected: (Long) -> Unit,
    onCalendarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone  = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }

    // 89일 전 ~ 오늘 (총 90개)
    val dates = remember(today) {
        (DATE_STRIP_DAYS_BACK downTo 0).map { daysBack ->
            today.minusDays(daysBack.toLong())
        }
    }

    val selectedDate = remember(selectedDateMs) {
        Instant.ofEpochMilli(selectedDateMs).atZone(zone).toLocalDate()
    }

    // 선택 날짜의 목록 인덱스
    val selectedIndex = remember(selectedDate) {
        dates.indexOfFirst { it == selectedDate }.coerceAtLeast(0)
    }

    // 선택 날짜에 해당하는 월 레이블 (선택 변경 시 갱신)
    val monthLabel = remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREA))
    }

    val listState = rememberLazyListState()

    // 선택 날짜가 바뀌면 해당 항목이 뷰 중앙에 오도록 스크롤
    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem(
            index  = (selectedIndex - 3).coerceAtLeast(0),
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {

        // ── 헤더: 월 레이블 + 캘린더 아이콘 ─────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                text  = monthLabel,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
            IconButton(onClick = onCalendarClick) {
                Icon(
                    imageVector        = Icons.Outlined.CalendarMonth,
                    contentDescription = "달력에서 날짜 선택",
                    tint               = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // ── 날짜 칩 LazyRow ────────────────────────────────
        LazyRow(
            state           = listState,
            contentPadding  = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier        = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
        ) {
            items(
                count = dates.size,
                key   = { dates[it].toEpochDay() },
            ) { idx ->
                val date    = dates[idx]
                val startMs = date.atStartOfDay(zone).toInstant().toEpochMilli()
                DateChip(
                    date       = date,
                    isSelected = date == selectedDate,
                    isToday    = date == today,
                    onClick    = { onDateSelected(startMs) },
                )
            }
        }
    }
}

/**
 * 날짜 바의 개별 날짜 칩.
 *
 * [시각 언어]
 * - 선택됨   : primary 배경 + onPrimary 텍스트
 * - 오늘(미선택): primary 텍스트 + 굵게 + 하단 점
 * - 일요일   : error 색상
 * - 토요일   : tertiary 색상
 * - 평일     : onSurfaceVariant
 */
@Composable
private fun DateChip(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    val dayOfWeek = date.dayOfWeek

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    val baseTextColor = when {
        isToday              -> MaterialTheme.colorScheme.primary
        dayOfWeek == DayOfWeek.SUNDAY   -> MaterialTheme.colorScheme.error
        dayOfWeek == DayOfWeek.SATURDAY -> MaterialTheme.colorScheme.tertiary
        else                 -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else baseTextColor

    // 하단 점: 오늘 표시 (선택된 경우 onPrimary, 미선택이면 primary)
    val dotColor = when {
        isToday && isSelected  -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
        isToday && !isSelected -> MaterialTheme.colorScheme.primary
        else                   -> Color.Transparent
    }

    val weekdayLabel = date.format(DateTimeFormatter.ofPattern("E", Locale.KOREA))

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .width(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 2.dp),
    ) {
        // 요일 (월 / 화 / …)
        Text(
            text  = weekdayLabel,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
        )
        // 일 숫자
        Text(
            text  = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            ),
            color = textColor,
        )
        // 오늘 표시 점
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
    }
}

// ══════════════════════════════════════════════════════════════════
// 공통 서브 컴포저블
// ══════════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Text(
        text = "$title  $count",
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.SemiBold,
            color = titleColor,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    )
}

/**
 * 중요도 섹션용 sticky 헤더.
 *
 * 스크롤 시 상단에 고정되므로 반드시 [MaterialTheme.colorScheme.surface] 배경을 깔아
 * 아래 아이템이 비쳐 보이지 않도록 한다.
 *
 * @param dotColor 우선순위를 나타내는 색상 점 (HIGH=빨강, MEDIUM=주황, LOW=회색)
 */
@Composable
private fun PrioritySectionHeader(
    label: String,
    count: Int,
    dotColor: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)  // sticky 시 아이템 가림
            .padding(vertical = 6.dp),
    ) {
        // 우선순위 색상 점
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = dotColor,
            ),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleSmall.copy(
                color = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    subMessage: String,
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .let {
                            // 아이콘 크기 조정
                            it
                        },
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = subMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}
