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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kanghyeon.todolist.R
import com.kanghyeon.todolist.data.local.entity.TaskEntity
import com.kanghyeon.todolist.presentation.theme.OverdueRed
import com.kanghyeon.todolist.presentation.theme.SwipeDeleteBackground
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 단일 할 일 카드 컴포저블 (아카이브 탭 등 독립 사용)
 *
 * [레이아웃]
 * Row: [제목 + 부가정보 (left, weight 1f)] [체크 버튼 (right)]
 *
 * [디자인 원칙]
 * - 20dp 코너, 0dp elevation, 테두리 없음
 * - 제목: titleMedium SemiBold
 * - 체크 아이콘을 오른쪽에 배치해 완료 토글
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
    task: TaskEntity,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
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
        targetValue = if (task.isDone) MaterialTheme.colorScheme.primary else Color(0xFFBDBDBD),
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
        backgroundContent = { SwipeDeleteBg() },
    ) {
        TaskCard(
            task = task,
            checkColor = checkColor,
            titleAlpha = titleAlpha,
            onToggleDone = onToggleDone,
        )
    }
}

@Composable
private fun TaskCard(
    task: TaskEntity,
    checkColor: Color,
    titleAlpha: Float,
    onToggleDone: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isDone)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ── 제목 + 부가 정보 (LEFT) ───────────────────────
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
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

                task.dueDate?.let { DueDateChip(dueDate = it) }
            }

            // ── 체크 버튼 (RIGHT) ─────────────────────────────
            val scale by animateFloatAsState(
                targetValue = if (task.isDone) 1.1f else 1f,
                animationSpec = tween(200),
                label = "iconScale",
            )
            IconButton(
                onClick = onToggleDone,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(
                        if (task.isDone) R.drawable.ic_check_circle_filled
                        else R.drawable.ic_check_circle_outline
                    ),
                    contentDescription = if (task.isDone) "완료 취소" else "완료",
                    tint = checkColor,
                    modifier = Modifier
                        .size(28.dp)
                        .scale(scale),
                )
            }
        }
    }
}

@Composable
private fun DueDateChip(dueDate: Long) {
    val now = System.currentTimeMillis()
    val isOverdue = dueDate < now
    val label = remember(dueDate) {
        SimpleDateFormat("a h:mm", Locale.KOREA).format(Date(dueDate))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.clock),
            contentDescription = null,
            tint = if (isOverdue) OverdueRed else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(11.dp),
        )
        Text(
            text = if (isOverdue) "$label 기한 초과" else label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isOverdue) OverdueRed else MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun SwipeDeleteBg() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(SwipeDeleteBackground)
            .padding(end = 20.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            painter = painterResource(R.drawable.trash_2),
            contentDescription = "삭제",
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}

// ══════════════════════════════════════════════════════════════════
// PriorityGroupCard — 우선순위 섹션 카드
// ══════════════════════════════════════════════════════════════════

/**
 * 동일 우선순위 Task 목록을 하나의 Card로 묶어 보여준다.
 *
 * [시각 구조]
 * Card (20dp 코너, 테두리 없음)
 *   ├── 4dp 좌측 액센트 바
 *   └── Column
 *         ├── GroupedTaskRow(task0) — [제목+메타 left] [체크 right]
 *         ├── HorizontalDivider
 *         └── GroupedTaskRow(task1) ...
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
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
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
                            color = accentColor.copy(alpha = 0.12f),
                            thickness = 1.dp,
                        )
                    }
                }
            }
        }
    }
}

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
        targetValue = if (task.isDone) MaterialTheme.colorScheme.primary else Color(0xFFBDBDBD),
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SwipeDeleteBackground)
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    painter = painterResource(R.drawable.trash_2),
                    contentDescription = "삭제",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onEdit() }
                .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ── 제목 + 부가 정보 (LEFT) ───────────────────────
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
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
                task.dueDate?.let { DueDateChip(it) }
            }

            // ── 체크 버튼 (RIGHT) ─────────────────────────────
            val scale by animateFloatAsState(
                targetValue = if (task.isDone) 1.1f else 1f,
                animationSpec = tween(200),
                label = "groupedIconScale",
            )
            IconButton(
                onClick = onToggleDone,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(
                        if (task.isDone) R.drawable.ic_check_circle_filled
                        else R.drawable.ic_check_circle_outline
                    ),
                    contentDescription = if (task.isDone) "완료 취소" else "완료",
                    tint = checkColor,
                    modifier = Modifier
                        .size(28.dp)
                        .scale(scale),
                )
            }
        }
    }
}
