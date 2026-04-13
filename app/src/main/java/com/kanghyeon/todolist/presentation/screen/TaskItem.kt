package com.kanghyeon.todolist.presentation.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kanghyeon.todolist.data.local.entity.Priority
import com.kanghyeon.todolist.data.local.entity.TaskEntity
import com.kanghyeon.todolist.presentation.theme.OverdueRed
import com.kanghyeon.todolist.presentation.theme.PriorityHigh
import com.kanghyeon.todolist.presentation.theme.PriorityLow
import com.kanghyeon.todolist.presentation.theme.PriorityMedium
import com.kanghyeon.todolist.presentation.theme.SwipeDeleteBackground
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 단일 할 일 카드 컴포저블
 *
 * [스와이프 삭제]
 * EndToStart(우 → 좌) 스와이프 시 onDelete 호출.
 * SwipeToDismissBox는 Material3 1.3.0+에서 안정화된 API.
 *
 * [체크 애니메이션]
 * isDone 전환 시 체크 아이콘 색상 + 제목 취소선이 animateColorAsState로 부드럽게 변환.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
    task: TaskEntity,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // ── 스와이프 상태 ─────────────────────────────────────────
    var isDeleted by remember { mutableStateOf(false) }

    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart && !isDeleted) {
                isDeleted = true
                onDelete()
                true
            } else false
        },
    )

    // 완료 토글 애니메이션
    val checkColor by animateColorAsState(
        targetValue = if (task.isDone) MaterialTheme.colorScheme.primary else Color.Gray,
        animationSpec = tween(durationMillis = 300),
        label = "checkColor",
    )
    val titleAlpha by animateFloatAsState(
        targetValue = if (task.isDone) 0.4f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "titleAlpha",
    )

    SwipeToDismissBox(
        state = swipeState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = { SwipeDeleteBackground() },
    ) {
        TaskCard(
            task = task,
            checkColor = checkColor,
            titleAlpha = titleAlpha,
            onToggleDone = onToggleDone,
        )
    }
}

// ──────────────────────────────────────────────────────────────────
// 내부 컴포저블 분리 (재구성 범위를 좁히기 위해 별도 함수로)
// ──────────────────────────────────────────────────────────────────

@Composable
private fun TaskCard(
    task: TaskEntity,
    checkColor: Color,
    titleAlpha: Float,
    onToggleDone: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isDone)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.isDone) 0.dp else 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ── 체크 버튼 ─────────────────────────────────────
            IconButton(
                onClick = onToggleDone,
                modifier = Modifier.size(40.dp),
            ) {
                val scale by animateFloatAsState(
                    targetValue = if (task.isDone) 1.1f else 1f,
                    animationSpec = tween(200),
                    label = "iconScale",
                )
                Icon(
                    imageVector = if (task.isDone)
                        Icons.Default.CheckCircle
                    else
                        Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (task.isDone) "완료 취소" else "완료",
                    tint = checkColor,
                    modifier = Modifier.scale(scale),
                )
            }

            Spacer(Modifier.width(4.dp))

            // ── 우선순위 점 ───────────────────────────────────
            PriorityDot(priority = task.priority)

            Spacer(Modifier.width(8.dp))

            // ── 제목 + 부가 정보 ──────────────────────────────
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = titleAlpha),
                        textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (!task.description.isNullOrBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = titleAlpha),
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                task.dueDate?.let { due ->
                    DueDateChip(dueDate = due)
                }
            }
        }
    }
}

@Composable
private fun PriorityDot(priority: Int) {
    val color = when (Priority.from(priority)) {
        Priority.HIGH   -> PriorityHigh
        Priority.MEDIUM -> PriorityMedium
        Priority.LOW    -> PriorityLow
    }
    // LOW는 점을 숨김 (불필요한 시각적 노이즈 제거)
    if (priority == Priority.LOW.value) return

    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun DueDateChip(dueDate: Long) {
    val now = System.currentTimeMillis()
    val isOverdue = dueDate < now
    val label = remember(dueDate) {
        SimpleDateFormat("HH:mm", Locale.KOREA).format(Date(dueDate))
    }

    Text(
        text = if (isOverdue) "⚠ $label 기한 초과" else label,
        style = MaterialTheme.typography.labelSmall,
        color = if (isOverdue) OverdueRed else MaterialTheme.colorScheme.outline,
    )
}

@Composable
private fun SwipeDeleteBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(SwipeDeleteBackground)
            .padding(end = 20.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "삭제",
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}

// ══════════════════════════════════════════════════════════════════
// 그룹 카드 — 전체 탭 우선순위 섹션에서 사용
// ══════════════════════════════════════════════════════════════════

/**
 * 동일 우선순위 Task 목록을 하나의 ElevatedCard로 묶어 보여준다.
 *
 * [시각 구조]
 * ElevatedCard (파스텔 배경, 좌측 4dp 액센트 바)
 *   └── Column
 *         ├── GroupedTaskRow(task0)
 *         ├── HorizontalDivider
 *         ├── GroupedTaskRow(task1)
 *         └── ...
 *
 * @param accentColor  우선순위 대표 색상 (액센트 바 + 구분선)
 * @param bgColor      파스텔 배경색 (accentColor.copy(alpha = 0.08f) 권장)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriorityGroupCard(
    tasks: List<TaskEntity>,
    accentColor: Color,
    bgColor: Color,
    onToggleDone: (TaskEntity) -> Unit,
    onDelete: (TaskEntity) -> Unit,
    onEdit: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = bgColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        // 좌측 액센트 바 + 태스크 목록을 가로로 배치
        Row {
            // 4dp 세로 액센트 바
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor),
            )

            Column(modifier = Modifier.weight(1f)) {
                tasks.forEachIndexed { index, task ->
                    GroupedTaskRow(
                        task = task,
                        onToggleDone = { onToggleDone(task) },
                        onDelete = { onDelete(task) },
                        onEdit = { onEdit(task) },
                    )
                    if (index < tasks.lastIndex) {
                        HorizontalDivider(
                            color = accentColor.copy(alpha = 0.15f),
                            thickness = 1.dp,
                        )
                    }
                }
            }
        }
    }
}

/**
 * PriorityGroupCard 내부 단일 행 — 스와이프 삭제 지원.
 *
 * Card 셸이 없으므로 배경은 부모(ElevatedCard)에서 제공받는다.
 * 액센트 바도 카드 레벨에서 처리하므로 이 Row는 순수 콘텐츠만 담는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupedTaskRow(
    task: TaskEntity,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    var isDeleted by remember { mutableStateOf(false) }

    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart && !isDeleted) {
                isDeleted = true
                onDelete()
                true
            } else false
        },
    )

    val checkColor by animateColorAsState(
        targetValue = if (task.isDone) MaterialTheme.colorScheme.primary else Color.Gray,
        animationSpec = tween(300),
        label = "groupedCheckColor",
    )
    val titleAlpha by animateFloatAsState(
        targetValue = if (task.isDone) 0.4f else 1f,
        animationSpec = tween(300),
        label = "groupedTitleAlpha",
    )

    SwipeToDismissBox(
        state = swipeState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            // 카드 안에서 스와이프 시 보이는 삭제 배경 (모서리 없이 꽉 채움)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SwipeDeleteBackground)
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "삭제",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onEdit() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 체크 버튼
            IconButton(
                onClick = onToggleDone,
                modifier = Modifier.size(40.dp),
            ) {
                val scale by animateFloatAsState(
                    targetValue = if (task.isDone) 1.1f else 1f,
                    animationSpec = tween(200),
                    label = "groupedIconScale",
                )
                Icon(
                    imageVector = if (task.isDone)
                        Icons.Default.CheckCircle
                    else
                        Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (task.isDone) "완료 취소" else "완료",
                    tint = checkColor,
                    modifier = Modifier.scale(scale),
                )
            }

            Spacer(Modifier.width(4.dp))

            // 제목 + 부가 정보
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = titleAlpha),
                        textDecoration = if (task.isDone)
                            TextDecoration.LineThrough else TextDecoration.None,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!task.description.isNullOrBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                .copy(alpha = titleAlpha),
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                task.dueDate?.let { DueDateChip(it) }
            }
        }
    }
}
