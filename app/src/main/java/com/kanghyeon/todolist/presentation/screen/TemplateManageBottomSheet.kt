package com.kanghyeon.todolist.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanghyeon.todolist.R
import com.kanghyeon.todolist.data.local.entity.Priority
import com.kanghyeon.todolist.data.local.entity.RoutineTemplateGroupWithTasks
import com.kanghyeon.todolist.presentation.theme.CardBorderColor
import com.kanghyeon.todolist.presentation.theme.PriorityHigh
import com.kanghyeon.todolist.presentation.theme.PriorityLow
import com.kanghyeon.todolist.presentation.theme.PriorityMedium
import com.kanghyeon.todolist.presentation.viewmodel.TaskViewModel

private sealed interface TemplateScreen {
    data object GroupList : TemplateScreen
    data class GroupDetail(val groupId: Long) : TemplateScreen
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateManageBottomSheet(
    viewModel: TaskViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState     = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val templateGroups by viewModel.templateGroups.collectAsStateWithLifecycle()
    var currentScreen  by remember { mutableStateOf<TemplateScreen>(TemplateScreen.GroupList) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                if (targetState is TemplateScreen.GroupDetail) {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(
                        slideOutHorizontally { -it } + fadeOut()
                    )
                } else {
                    (slideInHorizontally { -it } + fadeIn()).togetherWith(
                        slideOutHorizontally { it } + fadeOut()
                    )
                }
            },
            label = "template_screen_transition",
        ) { screen ->
            when (screen) {
                is TemplateScreen.GroupList   -> GroupListScreen(
                    groups         = templateGroups,
                    onGroupClick   = { groupId -> currentScreen = TemplateScreen.GroupDetail(groupId) },
                    onAddGroup     = { name -> viewModel.addTemplateGroup(name) },
                    onToggleActive = { id, isActive -> viewModel.toggleTemplateGroupActive(id, isActive) },
                    onDeleteGroup  = { id -> viewModel.deleteTemplateGroup(id) },
                )
                is TemplateScreen.GroupDetail -> {
                    val group = templateGroups.find { it.group.id == screen.groupId }
                    if (group != null) {
                        GroupDetailScreen(
                            group        = group,
                            onBack       = { currentScreen = TemplateScreen.GroupList },
                            onAddTask    = { title, desc, priority ->
                                viewModel.addTemplateTask(screen.groupId, title, desc, priority)
                            },
                            onDeleteTask = { id -> viewModel.deleteTemplateTask(id) },
                            onApplyNow   = { viewModel.applyTemplateNow(screen.groupId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupListScreen(
    groups:         List<RoutineTemplateGroupWithTasks>,
    onGroupClick:   (Long) -> Unit,
    onAddGroup:     (String) -> Unit,
    onToggleActive: (Long, Boolean) -> Unit,
    onDeleteGroup:  (Long) -> Unit,
) {
    var showAddForm     by remember { mutableStateOf(false) }
    var newGroupName    by remember { mutableStateOf("") }
    var confirmDeleteId by remember { mutableStateOf<Long?>(null) }

    confirmDeleteId?.let { groupId ->
        val group = groups.find { it.group.id == groupId }
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
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
                    text  = "템플릿 삭제",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
            },
            text = {
                Text(
                    text  = "\"${group?.group?.name}\" 템플릿과\n소속된 할 일들이 모두 삭제됩니다.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280)),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteGroup(groupId)
                    confirmDeleteId = null
                }) {
                    Text(
                        text  = "삭제",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteId = null }) { Text("취소") }
            },
            shape = RoundedCornerShape(16.dp),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
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
                        painter            = painterResource(R.drawable.archive_restore),
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(18.dp),
                    )
                }
                Text(
                    text  = "루틴 템플릿 관리",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFF1D1D1F),
                    ),
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text  = "활성화된 템플릿은 매일 자정에 오늘의 할 일에 자동 추가됩니다.",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)),
        )
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = CardBorderColor)
        Spacer(Modifier.height(12.dp))

        if (groups.isEmpty()) {
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text      = "등록된 템플릿이 없습니다.\n아래 버튼으로 템플릿을 추가해 보세요.",
                    style     = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF9CA3AF)),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            groups.forEach { groupWithTasks ->
                GroupCard(
                    group          = groupWithTasks,
                    onGroupClick   = { onGroupClick(groupWithTasks.group.id) },
                    onToggleActive = { onToggleActive(groupWithTasks.group.id, it) },
                    onDeleteClick  = { confirmDeleteId = groupWithTasks.group.id },
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        if (showAddForm) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                        shape = RoundedCornerShape(14.dp),
                    )
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text  = "새 템플릿 이름",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.primary,
                    ),
                )
                TextField(
                    value         = newGroupName,
                    onValueChange = { newGroupName = it },
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = { Text("예: 출근 루틴", style = MaterialTheme.typography.bodyMedium) },
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction      = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        if (newGroupName.isNotBlank()) {
                            onAddGroup(newGroupName)
                            newGroupName = ""
                            showAddForm  = false
                        }
                    }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor   = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = CardBorderColor,
                    ),
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { newGroupName = ""; showAddForm = false }) { Text("취소") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newGroupName.isNotBlank()) {
                                onAddGroup(newGroupName)
                                newGroupName = ""
                                showAddForm  = false
                            }
                        },
                        enabled = newGroupName.isNotBlank(),
                        shape   = RoundedCornerShape(10.dp),
                    ) { Text("추가") }
                }
            }
        } else {
            OutlinedButton(
                onClick  = { showAddForm = true },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                border   = androidx.compose.foundation.BorderStroke(
                    1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                ),
            ) {
                Icon(painterResource(R.drawable.plus), null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text  = "새 템플릿 추가",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
private fun GroupCard(
    group:          RoutineTemplateGroupWithTasks,
    onGroupClick:   () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onDeleteClick:  () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onGroupClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = group.group.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color      = Color(0xFF1D1D1F),
                ),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text  = "할 일 ${group.tasks.size}개  ›",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)),
            )
        }
        Switch(
            checked         = group.group.isActive,
            onCheckedChange = onToggleActive,
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = Color.White,
                checkedTrackColor   = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFD1D5DB),
            ),
        )
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onDeleteClick) {
            Icon(
                painter            = painterResource(R.drawable.trash_2),
                contentDescription = "삭제",
                tint               = Color(0xFF9CA3AF),
                modifier           = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun GroupDetailScreen(
    group:        RoutineTemplateGroupWithTasks,
    onBack:       () -> Unit,
    onAddTask:    (title: String, description: String?, priority: Int) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onApplyNow:   () -> Unit,
) {
    var showApplyConfirm by remember { mutableStateOf(false) }
    var showAddTaskForm  by remember { mutableStateOf(false) }
    var taskTitle        by remember { mutableStateOf("") }
    var taskDesc         by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableIntStateOf(Priority.MEDIUM.value) }

    if (showApplyConfirm) {
        AlertDialog(
            onDismissRequest = { showApplyConfirm = false },
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
                    text  = "오늘 할 일에 즉시 추가",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
            },
            text = {
                Text(
                    text  = "이 템플릿의 할 일들을 오늘의 목록에\n바로 추가하시겠습니까?\n\n(할 일 ${group.tasks.size}개가 추가됩니다)",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280)),
                )
            },
            confirmButton = {
                Button(
                    onClick = { onApplyNow(); showApplyConfirm = false },
                    shape   = RoundedCornerShape(10.dp),
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text(
                        text  = "확인",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showApplyConfirm = false }) { Text("취소") }
            },
            shape = RoundedCornerShape(16.dp),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter            = painterResource(R.drawable.x),
                    contentDescription = "뒤로가기",
                    tint               = Color(0xFF6B7280),
                    modifier           = Modifier.size(20.dp),
                )
            }
            Text(
                text  = group.group.name,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF1D1D1F),
                ),
            )
        }

        Spacer(Modifier.height(2.dp))
        HorizontalDivider(color = CardBorderColor)
        Spacer(Modifier.height(16.dp))

        // ── 오늘 할 일에 즉시 추가하기 버튼 ───────────────────────
        Button(
            onClick  = { if (group.tasks.isNotEmpty()) showApplyConfirm = true else onApplyNow() },
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = Color.White,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
        ) {
            Icon(painterResource(R.drawable.plus), null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text  = "오늘 할 일에 즉시 추가하기",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            )
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text  = "템플릿 할 일 목록",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color      = Color(0xFF1D1D1F),
                ),
            )
            Text(
                text  = "${group.tasks.size}개",
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF6B7280)),
            )
        }
        Spacer(Modifier.height(8.dp))

        if (group.tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text      = "등록된 할 일이 없습니다.\n아래 버튼으로 추가해 보세요.",
                    style     = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9CA3AF)),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                group.tasks.forEach { task ->
                    TemplateTaskItem(
                        title       = task.title,
                        description = task.description,
                        priority    = task.priority,
                        onDelete    = { onDeleteTask(task.id) },
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (showAddTaskForm) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                        shape = RoundedCornerShape(14.dp),
                    )
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text  = "새 할 일",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.primary,
                    ),
                )
                TextField(
                    value         = taskTitle,
                    onValueChange = { taskTitle = it },
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = { Text("할 일 제목", style = MaterialTheme.typography.bodyMedium) },
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction      = ImeAction.Next,
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor   = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = CardBorderColor,
                    ),
                )
                TextField(
                    value         = taskDesc,
                    onValueChange = { taskDesc = it },
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = { Text("메모 (선택)", style = MaterialTheme.typography.bodyMedium) },
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction      = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        if (taskTitle.isNotBlank()) {
                            onAddTask(taskTitle, taskDesc.ifBlank { null }, selectedPriority)
                            taskTitle = ""; taskDesc = ""
                            selectedPriority = Priority.MEDIUM.value
                            showAddTaskForm = false
                        }
                    }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor   = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = CardBorderColor,
                    ),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple(Priority.HIGH.value,   "높음", PriorityHigh),
                        Triple(Priority.MEDIUM.value, "보통", PriorityMedium),
                        Triple(Priority.LOW.value,    "낮음", PriorityLow),
                    ).forEach { (value, label, color) ->
                        FilterChip(
                            selected = selectedPriority == value,
                            onClick  = { selectedPriority = value },
                            label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor   = color.copy(alpha = 0.15f),
                                selectedLabelColor       = color,
                                selectedLeadingIconColor = color,
                            ),
                        )
                    }
                }
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = {
                        taskTitle = ""; taskDesc = ""
                        selectedPriority = Priority.MEDIUM.value
                        showAddTaskForm = false
                    }) { Text("취소") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (taskTitle.isNotBlank()) {
                                onAddTask(taskTitle, taskDesc.ifBlank { null }, selectedPriority)
                                taskTitle = ""; taskDesc = ""
                                selectedPriority = Priority.MEDIUM.value
                                showAddTaskForm = false
                            }
                        },
                        enabled = taskTitle.isNotBlank(),
                        shape   = RoundedCornerShape(10.dp),
                    ) { Text("추가") }
                }
            }
        } else {
            OutlinedButton(
                onClick  = { showAddTaskForm = true },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                border   = androidx.compose.foundation.BorderStroke(
                    1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                ),
            ) {
                Icon(painterResource(R.drawable.plus), null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text  = "할 일 추가",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
private fun TemplateTaskItem(
    title:       String,
    description: String?,
    priority:    Int,
    onDelete:    () -> Unit,
) {
    val (priorityLabel, priorityColor) = when (priority) {
        Priority.HIGH.value -> "높음" to PriorityHigh
        Priority.LOW.value  -> "낮음" to PriorityLow
        else                -> "보통" to PriorityMedium
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(modifier = Modifier.size(8.dp).background(priorityColor, CircleShape))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color      = Color(0xFF1D1D1F),
                ),
            )
            if (!description.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = description,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .background(priorityColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text(
                text  = priorityLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color      = priorityColor,
                ),
            )
        }
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                painter            = painterResource(R.drawable.x),
                contentDescription = "삭제",
                tint               = Color(0xFFD1D5DB),
                modifier           = Modifier.size(16.dp),
            )
        }
    }
}
