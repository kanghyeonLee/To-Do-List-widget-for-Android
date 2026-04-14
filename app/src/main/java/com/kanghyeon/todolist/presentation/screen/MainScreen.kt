package com.kanghyeon.todolist.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanghyeon.todolist.R
import com.kanghyeon.todolist.data.local.entity.Priority
import com.kanghyeon.todolist.data.local.entity.TaskEntity
import com.kanghyeon.todolist.presentation.theme.CardBorderColor
import com.kanghyeon.todolist.presentation.theme.PriorityHigh
import com.kanghyeon.todolist.presentation.theme.PriorityLow
import com.kanghyeon.todolist.presentation.theme.PriorityMedium
import com.kanghyeon.todolist.presentation.viewmodel.NewTaskDraft
import com.kanghyeon.todolist.presentation.viewmodel.TaskEvent
import com.kanghyeon.todolist.presentation.viewmodel.TaskUiState
import com.kanghyeon.todolist.presentation.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// ══════════════════════════════════════════════════════════════════
// 우선순위 페이지 메타 — HorizontalPager 각 페이지 설명
// ══════════════════════════════════════════════════════════════════

private data class PriorityPageMeta(
    val priority:    Priority,
    val label:       String,
    val accentColor: Color,
)

// ══════════════════════════════════════════════════════════════════
// MainScreen — 루트 컴포저블
// ══════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: TaskViewModel = hiltViewModel(),
) {
    val uiState              by viewModel.uiState.collectAsStateWithLifecycle()
    val archiveDate          by viewModel.selectedArchiveDate.collectAsStateWithLifecycle()
    val archiveTasks         by viewModel.archiveTasks.collectAsStateWithLifecycle()
    val dDayTasks            by viewModel.dDayTasks.collectAsStateWithLifecycle()
    val editingTask          by viewModel.editingTask.collectAsStateWithLifecycle()
    val newTaskDraft         by viewModel.newTaskDraft.collectAsStateWithLifecycle()

    val snackbarHostState      = remember { SnackbarHostState() }
    var showBottomSheet        by remember { mutableStateOf(false) }
    var showTrashScreen        by remember { mutableStateOf(false) }
    var showTemplateSheet      by remember { mutableStateOf(false) }
    var selectedTab            by remember { mutableIntStateOf(0) }
    // 아카이브 일괄 삭제 확인 다이얼로그 표시 여부
    var showBulkDeleteConfirm    by remember { mutableStateOf(false) }
    var showSyncConfirm          by remember { mutableStateOf(false) }
    var showTemplateManage       by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // ── 일회성 이벤트 수집 ─────────────────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TaskEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        message  = event.message,
                        duration = SnackbarDuration.Short,
                    )
                }
                is TaskEvent.TaskDeleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message     = "휴지통으로 이동했습니다.",
                        actionLabel = "실행 취소",
                        duration    = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.restoreTask(event.task)
                    }
                }
            }
        }
    }

    // ── 시스템 뒤로 가기(Back Press) 통합 핸들링 ──────────────────────
    val isAnyOverlayOpen = showTrashScreen || showTemplateManage || showBottomSheet || 
                           showSyncConfirm || showBulkDeleteConfirm || editingTask != null

    BackHandler(enabled = isAnyOverlayOpen) {
        when {
            showSyncConfirm -> showSyncConfirm = false
            showBulkDeleteConfirm -> showBulkDeleteConfirm = false
            
           
            showTrashScreen -> showTrashScreen = false
            showTemplateManage -> showTemplateManage = false
            
           
            showBottomSheet || editingTask != null -> {
                showBottomSheet = false
                viewModel.setEditingTask(null)
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text  = "To Do List",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color      = Color(0xFF1D1D1F),
                        ),
                    )
                },
                actions = {
                    // 루틴 템플릿 관리 버튼
                    IconButton(onClick = { showTemplateSheet = true }) {
                        Icon(
                            painter            = painterResource(R.drawable.layout_panel_top),
                            contentDescription = "루틴 템플릿 관리",
                            tint               = Color(0xFF6B7280),
                            modifier           = Modifier.size(22.dp),
                        )
                    }
                    IconButton(onClick = { showTrashScreen = true }) {
                        Icon(
                            painter            = painterResource(R.drawable.trash_2),
                            contentDescription = "휴지통",
                            tint               = Color(0xFF6B7280),
                            modifier           = Modifier.size(22.dp),
                        )
                    }

                    if (selectedTab == 0 || selectedTab == 2) {
                        IconButton(onClick = { showSyncConfirm = true }) {
                            Icon(
                                painter            = painterResource(R.drawable.archive_restore),
                                contentDescription = "아카이브 수동 동기화",
                                tint               = MaterialTheme.colorScheme.primary,
                                modifier           = Modifier.size(22.dp),
                            )
                        }
                    }

                    if (selectedTab == 1 && archiveTasks.isNotEmpty()) {
                        IconButton(onClick = { showBulkDeleteConfirm = true }) {
                            Icon(
                                painter            = painterResource(R.drawable.trash_2),
                                contentDescription = "이 날의 완료 항목 전체 휴지통 이동",
                                tint               = MaterialTheme.colorScheme.error,
                                modifier           = Modifier.size(22.dp),
                            )
                        }
                    }
                    if (showSyncConfirm) {
                        AlertDialog(
                            onDismissRequest = { showSyncConfirm = false },
                            icon = {
                                Icon(
                                    painter            = painterResource(R.drawable.archive_restore),
                                    contentDescription = null,
                                    tint               = MaterialTheme.colorScheme.primary,
                                    modifier           = Modifier.size(28.dp),
                                )
                            },
                            title = {
                                Text(
                                    text  = "아카이브 수동 동기화",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color      = Color(0xFF1D1D1F),
                                    ),
                                )
                            },
                            text = {
                                Text(
                                    text  = "오늘 완료한 할 일들을 지금 바로 아카이브에 기록하시겠습니까?\n\n(기록 후에도 자정 전까지는 메인 화면에 계속 유지됩니다.)",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color(0xFF6B7280),
                                    ),
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        viewModel.syncCompletedTasksToArchive()
                                        showSyncConfirm = false
                                    },
                                ) {
                                    Text(
                                        text  = "동기화",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                    )
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showSyncConfirm = false }) {
                                    Text("취소", color = Color(0xFF6B7280))
                                }
                            },
                            shape          = RoundedCornerShape(16.dp),
                            containerColor = Color.White,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor         = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor      = Color(0xFF1D1D1F),
                    actionIconContentColor = Color(0xFF6B7280),
                ),
            )
        },
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 2) {
                FloatingActionButton(
                    onClick        = { showBottomSheet = true },
                    shape          = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = Color.White,
                    elevation      = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp,
                        hoveredElevation = 10.dp,
                    ),
                ) {
                    Icon(
                        painter            = painterResource(R.drawable.plus),
                        contentDescription = "할 일 추가",
                        modifier           = Modifier.size(24.dp),
                    )
                }
            }
        },
        snackbarHost  = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->

        Column(modifier = Modifier.padding(innerPadding)) {

            // ── 탭: 할 일 / 아카이브 / D-Day ─────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = MaterialTheme.colorScheme.surface,
                contentColor     = MaterialTheme.colorScheme.primary,
                indicator        = { tabPositions ->
                    Box(
                        Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(2.dp)
                            .padding(horizontal = 20.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp),
                            ),
                    )
                },
                divider = {
                    HorizontalDivider(color = CardBorderColor, thickness = 1.dp)
                },
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0 },
                    text     = {
                        val totalCount     = uiState.activeTasks.size
                        val completedCount = uiState.activeTasks.count { it.isDone }
                        Text(
                            text  = "할 일 ($completedCount / $totalCount)",
                            color = if (selectedTab == 0) MaterialTheme.colorScheme.primary
                                    else Color(0xFF6B7280),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selectedTab == 0) FontWeight.SemiBold
                                             else FontWeight.Normal,
                            ),
                        )
                    },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1 },
                    text     = {
                        Text(
                            text  = "아카이브",
                            color = if (selectedTab == 1) MaterialTheme.colorScheme.primary
                                    else Color(0xFF6B7280),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selectedTab == 1) FontWeight.SemiBold
                                             else FontWeight.Normal,
                            ),
                        )
                    },
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick  = { selectedTab = 2 },
                    text     = {
                        val count = dDayTasks.count { !it.isDone }
                        val label = if (count > 0) "D-Day ($count)" else "D-Day"
                        Text(
                            text  = label,
                            color = if (selectedTab == 2) MaterialTheme.colorScheme.primary
                                    else Color(0xFF6B7280),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selectedTab == 2) FontWeight.SemiBold
                                             else FontWeight.Normal,
                            ),
                        )
                    },
                )
            }

            // ── 탭 콘텐츠 ─────────────────────────────────────────
            when {
                uiState.isLoading -> LoadingContent()
                selectedTab == 0  -> TodoContent(
                    uiState   = uiState,
                    viewModel = viewModel,
                    onEdit    = { viewModel.setEditingTask(it) },
                )
                selectedTab == 1  -> ArchiveContent(archiveDate, archiveTasks, viewModel)
                else              -> DDayContent(
                    dDayTasks = dDayTasks,
                    viewModel = viewModel,
                    onEdit    = { viewModel.setEditingTask(it) },
                )
            }
        }
    }

    // ── 아카이브 일괄 삭제 확인 다이얼로그 ────────────────────────
    // [버그 수정] 즉시 영구 삭제 → 확인 후 휴지통 이동으로 변경
    if (showBulkDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            icon = {
                Icon(
                    painter            = painterResource(R.drawable.trash_2),
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.error,
                    modifier           = Modifier.size(28.dp),
                )
            },
            title = {
                Text(
                    text  = "완료 항목 일괄 삭제",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFF1D1D1F),
                    ),
                )
            },
            text = {
                Text(
                    text  = "이 날짜의 완료된 할 일을 모두\n휴지통으로 이동하시겠습니까?\n\n휴지통에서 복구하거나 영구 삭제할 수 있습니다.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280),
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCompletedForSelectedDate()
                        showBulkDeleteConfirm = false
                    },
                ) {
                    Text(
                        text  = "휴지통으로 이동",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) {
                    Text("취소")
                }
            },
            shape          = RoundedCornerShape(16.dp),
            containerColor = Color.White,
        )
    }

    // ── 휴지통 화면 ────────────────────────────────────────────────
    if (showTrashScreen) {
        TrashScreen(
            viewModel = viewModel,
            onBack    = { showTrashScreen = false },
        )
    }

    // ── 루틴 템플릿 관리 BottomSheet ──────────────────────────────
    if (showTemplateSheet) {
        TemplateManageBottomSheet(
            viewModel = viewModel,
            onDismiss = { showTemplateSheet = false },
        )
    }

    // ── 할 일 추가 / 수정 BottomSheet ──────────────────────────────
    if (showBottomSheet || editingTask != null) {
        val isNewTask = editingTask == null
        AddTaskBottomSheet(
            task          = editingTask,
            initialDraft  = if (isNewTask) newTaskDraft else NewTaskDraft(),
            onDraftChange = { draft -> if (isNewTask) viewModel.updateNewTaskDraft(draft) },
            onDismiss     = {
                showBottomSheet = false
                viewModel.setEditingTask(null)
            },
            onCancel      = {
                showBottomSheet = false
                viewModel.setEditingTask(null)
                if (isNewTask) viewModel.clearNewTaskDraft()
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
// 탭 콘텐츠: 할 일 — HorizontalPager (우선순위별 페이지)
// ══════════════════════════════════════════════════════════════════

/**
 * 미완료 할 일을 우선순위별 페이지(높음·보통·낮음)로 나눠 표시.
 *
 * [구조]
 * Column
 *   ├─ ScreenSectionHeader ("오늘의 할 일")
 *   ├─ PriorityTabBar  ← 현재 페이지와 양방향 연동
 *   └─ HorizontalPager ← 스와이프로 페이지 전환
 *       ├─ Page 0: HIGH  priority tasks  (또는 EmptyContent)
 *       ├─ Page 1: MEDIUM priority tasks (또는 EmptyContent)
 *       └─ Page 2: LOW   priority tasks  (또는 EmptyContent)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TodoContent(
    uiState: TaskUiState,
    viewModel: TaskViewModel,
    onEdit: (TaskEntity) -> Unit,
) {
    // Pager 관련 상태 — 조건부 return 전에 호출해야 Compose 규칙 준수
    val pages = remember {
        listOf(
            PriorityPageMeta(Priority.HIGH,   "높음", PriorityHigh),
            PriorityPageMeta(Priority.MEDIUM, "보통", PriorityMedium),
            PriorityPageMeta(Priority.LOW,    "낮음", PriorityLow),
        )
    }
    val grouped    = uiState.activeTasks.groupBy { Priority.from(it.priority) }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope      = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenSectionHeader(
            title    = "오늘의 할 일",
            iconRes  = R.drawable.house,
            modifier = Modifier.padding(top = 8.dp),
        )

        if (uiState.activeTasks.isEmpty()) {
            // 전체 빈 상태 — 모든 우선순위에 할 일 없음
            EmptyContent(
                iconRes    = R.drawable.house,
                message    = "현재 등록된 할 일이 없습니다",
                subMessage = "아래 + 버튼을 눌러 첫 번째 할 일을 추가해 보세요.",
            )
        } else {
            // ── 우선순위 탭 바 ──────────────────────────────────
            PriorityTabBar(
                pages      = pages,
                grouped    = grouped,
                pagerState = pagerState,
                onTabClick = { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
                },
            )

            // ── 우선순위별 Pager ────────────────────────────────
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { pageIndex ->
                val meta  = pages[pageIndex]
                val tasks = grouped[meta.priority] ?: emptyList()

                if (tasks.isEmpty()) {
                    EmptyContent(
                        iconRes    = R.drawable.flag,
                        message    = "'${meta.label}' 우선순위 할 일이 없습니다",
                        subMessage = "다른 탭을 확인하거나 + 버튼으로 새 할 일을 추가해 보세요.",
                    )
                } else {
                    LazyColumn(
                        modifier            = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item(key = "group_${meta.priority.name}") {
                            PriorityGroupCard(
                                tasks        = tasks,
                                accentColor  = meta.accentColor,
                                bgColor      = MaterialTheme.colorScheme.surface,
                                onToggleDone = { task -> viewModel.toggleTaskCompletion(task) },
                                onDelete     = { task -> viewModel.deleteTask(task) },
                                onEdit       = { task -> onEdit(task) },
                                modifier     = Modifier.animateItem(),
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// 탭 콘텐츠: D-Day — 마감일이 지정된 할 일 목록
// ══════════════════════════════════════════════════════════════════

/**
 * D-Day 탭: dueDate가 있는 할 일을 마감 임박 순으로 표시.
 *
 * [정렬]
 * - 미완료 우선, 마감일 오름차순 (가장 임박한 것 최상단)
 * - 완료된 D-Day 항목은 하단에 표시 (자정 아카이브 전까지)
 *
 * [D-Day 뱃지]
 * - "D-3"  → 3일 후 마감 (파란색)
 * - "D-Day" → 오늘이 마감 (주황색)
 * - "D+2"  → 2일 초과 (빨간색)
 */
@Composable
private fun DDayContent(
    dDayTasks: List<com.kanghyeon.todolist.data.local.entity.TaskEntity>,
    viewModel: TaskViewModel,
    onEdit: (com.kanghyeon.todolist.data.local.entity.TaskEntity) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenSectionHeader(
            title   = "D-Day 마감",
            iconRes = R.drawable.calendar_clock,
            modifier = Modifier.padding(top = 8.dp),
        )

        if (dDayTasks.isEmpty()) {
            EmptyContent(
                iconRes    = R.drawable.calendar_check,
                message    = "D-Day 할 일이 없습니다",
                subMessage = "날짜가 지정된 할 일을 추가하면\n여기에 마감일 순으로 표시됩니다.",
            )
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = dDayTasks,
                    key   = { "dday_${it.id}" },
                ) { task ->
                    val label = task.dueDate?.let { dDayLabel(it) }
                    TaskItem(
                        task         = task,
                        onToggleDone = { viewModel.toggleTaskCompletion(task) },
                        onDelete     = { viewModel.deleteTask(task) },
                        onEdit       = { onEdit(task) },
                        dDayLabel    = label,
                        modifier     = Modifier.animateItem(),
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// PriorityTabBar — Pager와 양방향 연동되는 우선순위 칩 탭
// ══════════════════════════════════════════════════════════════════

/**
 * 세 개의 우선순위 칩(높음·보통·낮음)을 가로로 배치.
 *
 * - 선택된 칩: 해당 우선순위 색으로 배경 채움 + 흰 텍스트
 * - 미선택 칩: 투명 배경 + 1.5dp 색상 테두리 + 색상 텍스트
 * - 할 일 개수 뱃지: 칩 안에 반투명 pill 형태로 표시
 * - 칩 클릭 → pager.animateScrollToPage, 스와이프 → 칩 자동 전환
 */
@Composable
private fun PriorityTabBar(
    pages:      List<PriorityPageMeta>,
    grouped:    Map<Priority, List<TaskEntity>>,
    pagerState: PagerState,
    onTabClick: (Int) -> Unit,
    modifier:   Modifier = Modifier,
) {
    Row(
        modifier            = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        pages.forEachIndexed { index, meta ->
            val isSelected = pagerState.currentPage == index
            val count      = grouped[meta.priority]?.size ?: 0

            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (isSelected) meta.accentColor else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isSelected) meta.accentColor
                                else meta.accentColor.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .clickable(
                        onClick           = { onTabClick(index) },
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() },
                    )
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text  = meta.label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color      = if (isSelected) Color.White else meta.accentColor,
                    ),
                )
                // 할 일 개수 뱃지
                if (count > 0) {
                    Spacer(Modifier.width(5.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) Color.White.copy(alpha = 0.25f)
                                        else meta.accentColor.copy(alpha = 0.12f),
                                shape = CircleShape,
                            )
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text  = count.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color      = if (isSelected) Color.White else meta.accentColor,
                            ),
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// 탭 콘텐츠: 아카이브 (날짜별 완료 항목)
// ══════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchiveContent(
    archiveDate:  Long,
    archiveTasks: List<TaskEntity>,
    viewModel:    TaskViewModel,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    val initialUtcMillis = remember(archiveDate) {
        Instant.ofEpochMilli(archiveDate)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(java.time.ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialUtcMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    
                    val todayUtc = java.time.LocalDate.now(ZoneId.systemDefault())
                        .atStartOfDay(java.time.ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                    return utcTimeMillis <= todayUtc
                }
            },
        )

        // DatePicker 화이트 톤 색상 — 선택 강조색은 기존 인디고 유지
        val dpColors = DatePickerDefaults.colors(
            containerColor             = Color.White,
            titleContentColor          = Color(0xFF6B7280),
            headlineContentColor       = Color(0xFF1D1D1F),
            weekdayContentColor        = Color(0xFF6B7280),
            subheadContentColor        = Color(0xFF1D1D1F),
            navigationContentColor     = Color(0xFF1D1D1F),
            yearContentColor           = Color(0xFF1D1D1F),
            currentYearContentColor    = MaterialTheme.colorScheme.primary,
            selectedYearContentColor   = Color.White,
            selectedYearContainerColor = MaterialTheme.colorScheme.primary,
            dayContentColor            = Color(0xFF1D1D1F),
            disabledDayContentColor    = Color(0xFFD1D5DB),
            selectedDayContentColor    = Color.White,
            selectedDayContainerColor  = MaterialTheme.colorScheme.primary,
            todayContentColor          = MaterialTheme.colorScheme.primary,
            todayDateBorderColor       = MaterialTheme.colorScheme.primary,
            dividerColor               = Color(0xFFE5E7EB),
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { utcMillis ->
                        val localMidnightMillis = Instant.ofEpochMilli(utcMillis)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                        viewModel.selectArchiveDate(localMidnightMillis)
                    }
                    showDatePicker = false
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("취소") }
            },
            shape  = RoundedCornerShape(20.dp),
            colors = dpColors,
        ) {
            DatePicker(state = datePickerState, colors = dpColors)
        }
    }

    val selectedLocalDate = remember(archiveDate) {
        Instant.ofEpochMilli(archiveDate).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    val dateLabel = remember(selectedLocalDate) {
        selectedLocalDate.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.KOREA))
    }

    Column(modifier = Modifier.fillMaxSize()) {

        ScreenSectionHeader(
            title    = "아카이브",
            iconRes  = R.drawable.calendar_check,
            modifier = Modifier.padding(top = 8.dp),
        )

        // ── 날짜 선택 버튼 — 미니멀 카드 스타일 ─────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp))
                .clickable { showDatePicker = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter            = painterResource(R.drawable.calendar),
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text  = dateLabel,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color      = Color(0xFF1D1D1F),
                ),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text  = "▼",
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF6B7280)),
            )
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = CardBorderColor, thickness = 1.dp)

        // ── 해당 날짜 완료 목록 ──────────────────────────────────
        if (archiveTasks.isEmpty()) {
            EmptyContent(
                iconRes    = R.drawable.calendar_check,
                message    = "현재 등록된 할 일이 없습니다",
                subMessage = "이 날짜에 완료된 할 일이 없어요.\n할 일을 체크하면 날짜별로 기록됩니다.",
            )
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = archiveTasks,
                    key   = { "archive_${it.id}" },
                ) { task ->
                    TaskItem(
                        task         = task,
                        onToggleDone = { viewModel.toggleTaskCompletion(task) },
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
// 공통 서브 컴포저블
// ══════════════════════════════════════════════════════════════════

@Composable
private fun ScreenSectionHeader(
    title:    String,
    iconRes:  Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(10.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter            = painterResource(iconRes),
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(18.dp),
            )
        }
        Text(
            text  = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color      = Color(0xFF1D1D1F),
            ),
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent(
    iconRes:    Int,
    message:    String,
    subMessage: String,
) {
    AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier         = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter            = painterResource(iconRes),
                        contentDescription = null,
                        modifier           = Modifier.size(40.dp),
                        tint               = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = message,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color      = Color(0xFF1D1D1F),
                    ),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text      = subMessage,
                    style     = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
