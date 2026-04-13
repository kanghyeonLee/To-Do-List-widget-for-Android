package com.kanghyeon.todolist.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanghyeon.todolist.R
import com.kanghyeon.todolist.data.local.entity.Priority
import com.kanghyeon.todolist.data.local.entity.RoutineTemplateGroupEntity
import com.kanghyeon.todolist.data.local.entity.RoutineTemplateGroupWithTasks
import com.kanghyeon.todolist.data.local.entity.RoutineTemplateTaskEntity
import com.kanghyeon.todolist.presentation.theme.CardBorderColor
import com.kanghyeon.todolist.presentation.theme.PriorityHigh
import com.kanghyeon.todolist.presentation.theme.PriorityLow
import com.kanghyeon.todolist.presentation.theme.PriorityMedium
import com.kanghyeon.todolist.presentation.viewmodel.TaskViewModel

// 취소 버튼에 사용할 빨간색
private val CancelRed = Color(0xFFEF4444)

// ══════════════════════════════════════════════════════════════════
// 루트 BottomSheet — 리스트 ↔ 상세 두 화면을 AnimatedContent로 전환
// ══════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateManageBottomSheet(
    viewModel: TaskViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val groups     by viewModel.templateGroups.collectAsStateWithLifecycle()

    // null = 그룹 목록 화면, non-null = 해당 그룹 상세 화면
    var selectedGroupId by remember { mutableStateOf<Long?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor   = MaterialTheme.colorScheme.surface,
        dragHandle       = null,
    ) {
        // 드래그 핸들 (항상 표시)
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 4.dp)
                    .background(CardBorderColor, CircleShape),
            )
        }

        // 두 화면 전환 (슬라이드 애니메이션)
        AnimatedContent(
            targetState = selectedGroupId,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()) togetherWith
                        (slideOutHorizontally { it } + fadeOut())
                }
            },
            label = "template_nav",
        ) { groupId ->
            if (groupId == null) {
                GroupListScreen(
                    groups        = groups,
                    onGroupClick  = { selectedGroupId = it.id },
                    onToggle      = { id, active -> viewModel.toggleTemplateGroupActive(id, active) },
                    onDeleteGroup = { viewModel.deleteTemplateGroup(it) },
                    onAddGroup    = { viewModel.addTemplateGroup(it) },
                    onDismiss     = onDismiss,
                )
            } else {
                val groupWithTasks = groups.firstOrNull { it.group.id == groupId }
                GroupDetailScreen(
                    groupWithTasks  = groupWithTasks,
                    onBack          = { selectedGroupId = null },
                    onRenameGroup   = { name -> viewModel.renameTemplateGroup(groupId, name) },
                    onAddTask       = { title, desc, priority ->
                        viewModel.addTemplateTask(
                            groupId     = groupId,
                            title       = title,
                            description = desc.ifBlank { null },
                            priority    = priority,
                        )
                    },
                    onDeleteTask    = { viewModel.deleteTemplateTask(it) },
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// 화면 1 — 그룹 목록 (Switch 토글 + 클릭 → 상세)
// ══════════════════════════════════════════════════════════════════

@Composable
private fun GroupListScreen(
    groups:        List<RoutineTemplateGroupWithTasks>,
    onGroupClick:  (RoutineTemplateGroupEntity) -> Unit,
    onToggle:      (id: Long, isActive: Boolean) -> Unit,
    onDeleteGroup: (id: Long) -> Unit,
    onAddGroup:    (name: String) -> Unit,
    onDismiss:     () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        // 헤더
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
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
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            RoundedCornerShape(10.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter            = painterResource(R.drawable.layout_panel_top),
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(18.dp),
                    )
                }
                Text(
                    text  = "루틴 템플릿",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFF1D1D1F),
                    ),
                )
            }
            TextButton(onClick = onDismiss) {
                Text("닫기", color = Color(0xFF6B7280))
            }
        }

        // 안내 배너
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                    RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment     = Alignment.Top,
        ) {
            Icon(
                painter            = painterResource(R.drawable.rotate_ccw),
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier
                    .size(16.dp)
                    .padding(top = 2.dp),
            )
            Text(
                text  = "활성화된 템플릿의 할 일이 매일 앱을 열 때 자동으로 추가됩니다. 하루 한 번만 생성됩니다.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                ),
            )
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = CardBorderColor)
        Spacer(Modifier.height(8.dp))

        // 그룹 목록
        if (groups.isEmpty()) {
            EmptyGroupPlaceholder()
        } else {
            Column(
                modifier            = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                groups.forEach { gWithTasks ->
                    GroupCard(
                        groupWithTasks = gWithTasks,
                        onClick        = { onGroupClick(gWithTasks.group) },
                        onToggle       = { onToggle(gWithTasks.group.id, it) },
                        onDelete       = { onDeleteGroup(gWithTasks.group.id) },
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = CardBorderColor)
        Spacer(Modifier.height(16.dp))

        // 새 그룹 추가 폼
        AddGroupForm(onAdd = onAddGroup)
    }
}

// ── 그룹 카드 ─────────────────────────────────────────────────────

@Composable
private fun GroupCard(
    groupWithTasks: RoutineTemplateGroupWithTasks,
    onClick:        () -> Unit,
    onToggle:       (Boolean) -> Unit,
    onDelete:       () -> Unit,
) {
    val group     = groupWithTasks.group
    val taskCount = groupWithTasks.tasks.size

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp))
            .clickable(
                onClick           = onClick,
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
            )
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (group.isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    else Color(0xFFF3F4F6),
                    RoundedCornerShape(10.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter            = painterResource(R.drawable.layout_panel_top),
                contentDescription = null,
                tint               = if (group.isActive) MaterialTheme.colorScheme.primary
                                     else Color(0xFF9CA3AF),
                modifier           = Modifier.size(18.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = group.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color      = if (group.isActive) Color(0xFF1D1D1F) else Color(0xFF9CA3AF),
                ),
            )
            Text(
                text  = "할 일 ${taskCount}개  ·  탭해서 편집",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9CA3AF)),
            )
        }

        Switch(
            checked         = group.isActive,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedThumbColor    = Color.White,
                checkedTrackColor    = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor  = Color.White,
                uncheckedTrackColor  = Color(0xFFD1D5DB),
                uncheckedBorderColor = Color(0xFFD1D5DB),
            ),
        )

        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(
                painter            = painterResource(R.drawable.trash_2),
                contentDescription = "그룹 삭제",
                tint               = Color(0xFFD1D5DB),
                modifier           = Modifier.size(16.dp),
            )
        }
    }
}

// ── 빈 상태 플레이스홀더 ──────────────────────────────────────────

@Composable
private fun EmptyGroupPlaceholder() {
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter            = painterResource(R.drawable.layout_panel_top),
                contentDescription = null,
                tint               = Color(0xFFD1D5DB),
                modifier           = Modifier.size(36.dp),
            )
            Text(
                text  = "등록된 루틴 템플릿이 없습니다",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color      = Color(0xFF9CA3AF),
                    fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                text  = "아래에서 첫 번째 그룹을 만들어 보세요.",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFBCC1CA)),
            )
        }
    }
}

// ── 새 그룹 추가 폼 ───────────────────────────────────────────────
// · + 아이콘 없음
// · 취소 상태 → 빨간 배경

@Composable
private fun AddGroupForm(onAdd: (String) -> Unit) {
    var name         by remember { mutableStateOf("") }
    var expanded     by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        // 토글 버튼
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (expanded) CancelRed
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                )
                .clickable(
                    onClick           = { expanded = !expanded },
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() },
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = if (expanded) "취소" else "새 그룹 만들기",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color      = if (expanded) Color.White
                                 else MaterialTheme.colorScheme.primary,
                ),
            )
        }

        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    placeholder   = { Text("그룹 이름 (예: 출근 루틴)", color = Color(0xFFBCC1CA)) },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction      = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (name.isNotBlank()) {
                                onAdd(name)
                                name     = ""
                                expanded = false
                                focusManager.clearFocus()
                            }
                        },
                    ),
                    shape  = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = CardBorderColor,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onAdd(name)
                            name     = ""
                            expanded = false
                            focusManager.clearFocus()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                    enabled  = name.isNotBlank(),
                ) {
                    Text(
                        text  = "그룹 생성",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// 화면 2 — 그룹 상세 (이름 인라인 편집 + 할 일 목록 + 추가 폼)
// ══════════════════════════════════════════════════════════════════

@Composable
private fun GroupDetailScreen(
    groupWithTasks: RoutineTemplateGroupWithTasks?,
    onBack:         () -> Unit,
    onRenameGroup:  (newName: String) -> Unit,
    onAddTask:      (title: String, description: String, priority: Int) -> Unit,
    onDeleteTask:   (id: Long) -> Unit,
) {
    // 이름 편집 상태
    var isEditingName by remember { mutableStateOf(false) }
    var editingName   by remember(groupWithTasks?.group?.name) {
        mutableStateOf(groupWithTasks?.group?.name ?: "")
    }
    val focusManager  = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        // ── 헤더 ──────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 왼쪽: 뒤로 가기 (X 아이콘)
            IconButton(onClick = {
                isEditingName = false
                onBack()
            }) {
                Icon(
                    painter            = painterResource(R.drawable.x),
                    contentDescription = "뒤로",
                    tint               = Color(0xFF6B7280),
                    modifier           = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.width(4.dp))

            // 가운데: 그룹명 (일반 표시 or 인라인 편집)
            if (isEditingName) {
                OutlinedTextField(
                    value         = editingName,
                    onValueChange = { editingName = it },
                    modifier      = Modifier.weight(1f),
                    singleLine    = true,
                    textStyle     = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFF1D1D1F),
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction      = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (editingName.isNotBlank()) {
                                onRenameGroup(editingName)
                            }
                            isEditingName = false
                            focusManager.clearFocus()
                        },
                    ),
                    shape  = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = CardBorderColor,
                    ),
                )
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = groupWithTasks?.group?.name ?: "",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color      = Color(0xFF1D1D1F),
                        ),
                    )
                    Text(
                        text  = "루틴 템플릿 편집",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9CA3AF)),
                    )
                }
            }

            // 오른쪽: 편집 중이면 확인/취소, 아니면 연필
            if (isEditingName) {
                // 확인
                IconButton(
                    onClick = {
                        if (editingName.isNotBlank()) onRenameGroup(editingName)
                        isEditingName = false
                        focusManager.clearFocus()
                    },
                ) {
                    Icon(
                        painter            = painterResource(R.drawable.check),
                        contentDescription = "이름 저장",
                        tint               = MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(20.dp),
                    )
                }
                // 취소
                IconButton(
                    onClick = {
                        editingName   = groupWithTasks?.group?.name ?: ""
                        isEditingName = false
                        focusManager.clearFocus()
                    },
                ) {
                    Icon(
                        painter            = painterResource(R.drawable.x),
                        contentDescription = "이름 편집 취소",
                        tint               = CancelRed,
                        modifier           = Modifier.size(20.dp),
                    )
                }
            } else {
                // 연필 — 이름 편집 진입
                IconButton(onClick = { isEditingName = true }) {
                    Icon(
                        painter            = painterResource(R.drawable.pencil),
                        contentDescription = "그룹 이름 편집",
                        tint               = Color(0xFF6B7280),
                        modifier           = Modifier.size(18.dp),
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color    = CardBorderColor,
        )
        Spacer(Modifier.height(12.dp))

        // ── 할 일 목록 ────────────────────────────────────────────
        val tasks = groupWithTasks?.tasks ?: emptyList()
        if (tasks.isEmpty()) {
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter            = painterResource(R.drawable.flag),
                        contentDescription = null,
                        tint               = Color(0xFFD1D5DB),
                        modifier           = Modifier.size(32.dp),
                    )
                    Text(
                        text  = "아직 할 일이 없습니다",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color      = Color(0xFF9CA3AF),
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    Text(
                        text  = "아래 폼으로 이 템플릿에 할 일을 추가해 보세요.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFBCC1CA)),
                    )
                }
            }
        } else {
            Column(
                modifier            = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tasks.forEach { task ->
                    TemplateTaskItem(
                        task     = task,
                        onDelete = { onDeleteTask(task.id) },
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = CardBorderColor)
        Spacer(Modifier.height(16.dp))

        // ── 할 일 추가 폼 ─────────────────────────────────────────
        AddTemplateTaskForm(
            onSave = { title, desc, priority -> onAddTask(title, desc, priority) },
        )
    }
}

// ── 템플릿 할 일 카드 ─────────────────────────────────────────────

@Composable
private fun TemplateTaskItem(
    task:     RoutineTemplateTaskEntity,
    onDelete: () -> Unit,
) {
    val accentColor = when (Priority.from(task.priority)) {
        Priority.HIGH   -> PriorityHigh
        Priority.MEDIUM -> PriorityMedium
        Priority.LOW    -> PriorityLow
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp))
            .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(accentColor, CircleShape),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = task.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color      = Color(0xFF1D1D1F),
                ),
            )
            if (!task.description.isNullOrBlank()) {
                Text(
                    text     = task.description,
                    style    = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)),
                    maxLines = 1,
                )
            }
        }

        Box(
            modifier = Modifier
                .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = when (Priority.from(task.priority)) {
                    Priority.HIGH   -> "높음"
                    Priority.MEDIUM -> "보통"
                    Priority.LOW    -> "낮음"
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    color      = accentColor,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(
                painter            = painterResource(R.drawable.trash_2),
                contentDescription = "삭제",
                tint               = Color(0xFFD1D5DB),
                modifier           = Modifier.size(16.dp),
            )
        }
    }
}

// ── 할 일 추가 폼 ─────────────────────────────────────────────────
// · + 아이콘 없음
// · 취소 상태 → 빨간 배경

@Composable
private fun AddTemplateTaskForm(
    onSave: (title: String, description: String, priority: Int) -> Unit,
) {
    var title        by remember { mutableStateOf("") }
    var description  by remember { mutableStateOf("") }
    var priority     by remember { mutableIntStateOf(Priority.MEDIUM.value) }
    var expanded     by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        // 토글 버튼
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (expanded) CancelRed
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                )
                .clickable(
                    onClick           = { expanded = !expanded },
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() },
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = if (expanded) "취소" else "할 일 추가",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color      = if (expanded) Color.White
                                 else MaterialTheme.colorScheme.primary,
                ),
            )
        }

        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                // 제목
                OutlinedTextField(
                    value         = title,
                    onValueChange = { title = it },
                    placeholder   = { Text("할 일 제목 (필수)", color = Color(0xFFBCC1CA)) },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction      = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                    shape  = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = CardBorderColor,
                    ),
                )

                Spacer(Modifier.height(8.dp))

                // 메모 (선택)
                OutlinedTextField(
                    value         = description,
                    onValueChange = { description = it },
                    placeholder   = { Text("메모 (선택)", color = Color(0xFFBCC1CA)) },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction      = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() },
                    ),
                    shape  = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = CardBorderColor,
                    ),
                )

                Spacer(Modifier.height(8.dp))

                // 우선순위 선택
                val priorityOptions = listOf(
                    Triple(Priority.HIGH.value,   "높음", PriorityHigh),
                    Triple(Priority.MEDIUM.value, "보통", PriorityMedium),
                    Triple(Priority.LOW.value,    "낮음", PriorityLow),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    priorityOptions.forEach { (value, label, color) ->
                        val isSelected = priority == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) color else Color.Transparent,
                                    RoundedCornerShape(10.dp),
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (isSelected) color else color.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .clickable(
                                    onClick           = { priority = value },
                                    indication        = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                )
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text  = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color      = if (isSelected) Color.White else color,
                                ),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 저장
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSave(title, description, priority)
                            title       = ""
                            description = ""
                            priority    = Priority.MEDIUM.value
                            expanded    = false
                            focusManager.clearFocus()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                    enabled  = title.isNotBlank(),
                ) {
                    Text(
                        text  = "할 일 저장",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }
        }
    }
}
