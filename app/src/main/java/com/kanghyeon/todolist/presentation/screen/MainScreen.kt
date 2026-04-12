package com.kanghyeon.todolist.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanghyeon.todolist.data.local.entity.TaskEntity
import com.kanghyeon.todolist.presentation.theme.OverdueRed
import com.kanghyeon.todolist.presentation.viewmodel.TaskEvent
import com.kanghyeon.todolist.presentation.viewmodel.TaskUiState
import com.kanghyeon.todolist.presentation.viewmodel.TaskViewModel

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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
                    // 완료 항목 전체 삭제 버튼
                    if (uiState.completedTasks.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearCompleted) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "완료 항목 전체 삭제",
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
            ExtendedFloatingActionButton(
                onClick = { showBottomSheet = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("할 일 추가") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->

        Column(modifier = Modifier.padding(innerPadding)) {

            // ── 탭: 오늘 / 전체 ───────────────────────────────
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        val count = uiState.todayTasks.size + uiState.overdueTasks.size
                        Text(if (count > 0) "오늘 ($count)" else "오늘")
                    },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        val count = uiState.allTasks.count { !it.isDone }
                        Text(if (count > 0) "전체 ($count)" else "전체")
                    },
                )
            }

            // ── 탭 콘텐츠 ────────────────────────────────────
            when {
                uiState.isLoading -> LoadingContent()
                selectedTab == 0  -> TodayContent(uiState, viewModel)
                else              -> AllContent(uiState, viewModel)
            }
        }
    }

    // ── 할 일 추가 BottomSheet ────────────────────────────────
    if (showBottomSheet) {
        AddTaskBottomSheet(
            onDismiss = { showBottomSheet = false },
            onAdd = { title, desc, priority, dueDate, showOnLock ->
                viewModel.addTask(
                    title = title,
                    description = desc,
                    priority = priority,
                    dueDate = dueDate,
                    showOnLockScreen = showOnLock,
                )
                showBottomSheet = false
            },
        )
    }
}

// ══════════════════════════════════════════════════════════════════
// 탭 콘텐츠: 오늘
// ══════════════════════════════════════════════════════════════════

@Composable
private fun TodayContent(
    uiState: TaskUiState,
    viewModel: TaskViewModel,
) {
    val hasOverdue = uiState.overdueTasks.isNotEmpty()
    val hasToday   = uiState.todayTasks.isNotEmpty()

    if (!hasOverdue && !hasToday) {
        EmptyContent(
            icon = Icons.Outlined.CheckCircle,
            message = "오늘 할 일이 없어요",
            subMessage = "할 일을 추가하거나\n'오늘 마감'으로 설정해 보세요.",
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ── 기한 초과 섹션 ─────────────────────────────────
        if (hasOverdue) {
            item(key = "overdue_header") {
                SectionHeader(
                    title = "기한 초과",
                    count = uiState.overdueTasks.size,
                    titleColor = OverdueRed,
                )
            }
            items(
                items = uiState.overdueTasks,
                key = { "overdue_${it.id}" },
            ) { task ->
                TaskItem(
                    task = task,
                    onToggleDone = { viewModel.toggleDone(task.id, task.isDone) },
                    onDelete = { viewModel.deleteTask(task) },
                    modifier = Modifier.animateItem(),
                )
            }
            item(key = "overdue_spacer") { Spacer(Modifier.height(8.dp)) }
        }

        // ── 오늘 섹션 ─────────────────────────────────────
        if (hasToday) {
            item(key = "today_header") {
                SectionHeader(
                    title = "오늘",
                    count = uiState.todayTasks.size,
                )
            }
            items(
                items = uiState.todayTasks,
                key = { "today_${it.id}" },
            ) { task ->
                TaskItem(
                    task = task,
                    onToggleDone = { viewModel.toggleDone(task.id, task.isDone) },
                    onDelete = { viewModel.deleteTask(task) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        item { Spacer(Modifier.height(80.dp)) } // FAB 여백
    }
}

// ══════════════════════════════════════════════════════════════════
// 탭 콘텐츠: 전체
// ══════════════════════════════════════════════════════════════════

@Composable
private fun AllContent(
    uiState: TaskUiState,
    viewModel: TaskViewModel,
) {
    val incompleteTasks = uiState.allTasks.filter { !it.isDone }
    val completedTasks  = uiState.completedTasks

    if (incompleteTasks.isEmpty() && completedTasks.isEmpty()) {
        EmptyContent(
            icon = Icons.Outlined.CheckCircle,
            message = "할 일이 없어요",
            subMessage = "아래 버튼을 눌러 첫 번째 할 일을 추가해 보세요.",
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ── 미완료 목록 ───────────────────────────────────
        if (incompleteTasks.isNotEmpty()) {
            item(key = "incomplete_header") {
                SectionHeader(title = "할 일", count = incompleteTasks.size)
            }
            items(
                items = incompleteTasks,
                key = { "all_${it.id}" },
            ) { task ->
                TaskItem(
                    task = task,
                    onToggleDone = { viewModel.toggleDone(task.id, task.isDone) },
                    onDelete = { viewModel.deleteTask(task) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        // ── 완료 목록 ─────────────────────────────────────
        if (completedTasks.isNotEmpty()) {
            item(key = "completed_header") {
                Spacer(Modifier.height(8.dp))
                SectionHeader(
                    title = "완료됨",
                    count = completedTasks.size,
                    titleColor = MaterialTheme.colorScheme.outline,
                )
            }
            items(
                items = completedTasks,
                key = { "done_${it.id}" },
            ) { task ->
                TaskItem(
                    task = task,
                    onToggleDone = { viewModel.toggleDone(task.id, task.isDone) },
                    onDelete = { viewModel.deleteTask(task) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
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
