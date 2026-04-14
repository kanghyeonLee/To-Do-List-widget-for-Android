package com.kanghyeon.todolist.presentation.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kanghyeon.todolist.R
import com.kanghyeon.todolist.data.local.entity.TaskEntity
import com.kanghyeon.todolist.presentation.theme.ArchiveCardBg
import com.kanghyeon.todolist.presentation.theme.CardBorderColor
import com.kanghyeon.todolist.presentation.theme.OverdueRed
import com.kanghyeon.todolist.presentation.theme.SwipeDeleteBackground
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

/**
 * 단일 할 일 카드 컴포저블 (아카이브 탭 독립 사용)
 *
 * [디자인 원칙]
 * - 16dp 코너, 0dp elevation, 1dp 연회색 테두리
 * - 완료 항목: 아주 연한 파란색(#E3F2FD) 배경
 * - 체크박스: 커스텀 원형 — unchecked=회색 테두리, checked=딥 인디고 채움 + 흰 체크
 */
/**
 * @param dDayLabel  null이면 D-Day 뱃지 미표시. "D-3", "D-Day", "D+1" 형식 문자열.
 * @param onEdit     null이면 클릭 동작 없음.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
    task: TaskEntity,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    dDayLabel: String? = null,
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

    val titleAlpha by animateFloatAsState(
        targetValue    = if (task.isDone) 0.4f else 1f,
        animationSpec  = tween(durationMillis = 300),
        label          = "titleAlpha",
    )

    SwipeToDismissBox(
        state                      = swipeState,
        modifier                   = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent          = { SwipeDeleteBg() },
    ) {
        TaskCard(
            task         = task,
            titleAlpha   = titleAlpha,
            onToggleDone = onToggleDone,
            onEdit       = onEdit,
            dDayLabel    = dDayLabel,
        )
    }
}

@Composable
private fun TaskCard(
    task: TaskEntity,
    titleAlpha: Float,
    onToggleDone: () -> Unit,
    onEdit: (() -> Unit)? = null,
    dDayLabel: String? = null,
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp))
            .then(if (onEdit != null) Modifier.clickable(onClick = onEdit) else Modifier),
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isDone) ArchiveCardBg else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ── 제목 + 부가 정보 ──────────────────────────────
            Column(
                modifier            = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text  = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight     = FontWeight.SemiBold,
                        color          = Color(0xFF1D1D1F).copy(alpha = titleAlpha),
                        textDecoration = if (task.isDone) TextDecoration.LineThrough
                                         else TextDecoration.None,
                    ),
                    maxLines  = 2,
                    overflow  = TextOverflow.Ellipsis,
                )
                if (!task.description.isNullOrBlank()) {
                    Text(
                        text  = task.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280).copy(alpha = titleAlpha),
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                task.dueDate?.let { DueDateChip(dueDate = it) }
            }

            // ── D-Day 뱃지 + 원형 체크박스 ──────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (dDayLabel != null) {
                    DDayBadge(label = dDayLabel, isDone = task.isDone)
                }
                CircularCheckbox(
                    checked  = task.isDone,
                    onClick  = onToggleDone,
                    primary  = primaryColor,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// D-Day 뱃지
// ══════════════════════════════════════════════════════════════════

/**
 * "D-3", "D-Day", "D+2" 형태의 pill 뱃지.
 * 완료된 항목은 반투명 회색으로 표시.
 */
@Composable
private fun DDayBadge(label: String, isDone: Boolean) {
    val isOverdue  = label.startsWith("D+")
    val isDDayToday = label == "D-Day"

    val baseColor = when {
        isDone      -> Color(0xFF9CA3AF)
        isOverdue   -> OverdueRed
        isDDayToday -> Color(0xFFF59E0B)   // Amber — D-Day 당일
        else        -> Color(0xFF4F46E5)   // Indigo — 기한 남음
    }

    Box(
        modifier = Modifier
            .background(
                color = baseColor.copy(alpha = if (isDone) 0.15f else 0.12f),
                shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color      = if (isDone) Color(0xFF9CA3AF) else baseColor,
            ),
        )
    }
}

/**
 * dueDate(epoch ms)를 기준으로 D-Day 라벨 문자열을 반환한다.
 *   양수 차이 (미래) → "D-3"
 *   0              → "D-Day"
 *   음수 차이 (과거) → "D+2"
 */
fun dDayLabel(dueDate: Long): String {
    val today     = LocalDate.now()
    val dueLocal  = Instant.ofEpochMilli(dueDate)
        .atZone(ZoneId.systemDefault()).toLocalDate()
    val days      = ChronoUnit.DAYS.between(today, dueLocal).toInt()
    return when {
        days > 0  -> "D-$days"
        days == 0 -> "D-Day"
        else      -> "D+${-days}"
    }
}

// ══════════════════════════════════════════════════════════════════
// 원형 체크박스 — 커스텀 Canvas 구현
// ══════════════════════════════════════════════════════════════════

/**
 * 안드로이드 표준 원형 체크박스.
 * - unchecked : 회색(#D1D5DB) 원형 테두리만
 * - checked   : [primary] 색으로 원 채움 + 흰색 체크마크 (애니메이션)
 *
 * 48dp 터치 영역 확보 후 중앙에 24dp 원을 그린다.
 */
@Composable
private fun CircularCheckbox(
    checked: Boolean,
    onClick: () -> Unit,
    primary: Color,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue   = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label         = "checkProgress",
    )
    val scale by animateFloatAsState(
        targetValue   = if (checked) 1.08f else 1f,
        animationSpec = tween(durationMillis = 200),
        label         = "checkScale",
    )
    val uncheckedBorder = Color(0xFFD1D5DB)

    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(
                onClick            = onClick,
                indication         = null,
                interactionSource  = remember { MutableInteractionSource() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(24.dp)
                .scale(scale),
        ) {
            val r          = size.minDimension / 2f
            val strokePx   = 2.dp.toPx()

            // 채움 원 (checked 시 primary 색, 애니메이션)
            if (progress > 0f) {
                drawCircle(
                    color  = primary.copy(alpha = progress),
                    radius = r,
                )
            }

            // 테두리 원
            drawCircle(
                color  = if (checked) primary else uncheckedBorder,
                radius = r - strokePx / 2f,
                style  = Stroke(width = strokePx),
            )

            // 체크마크 (progress > 0.3f 이후 fade-in)
            if (progress > 0.3f) {
                val w      = size.width
                val h      = size.height
                val alpha  = ((progress - 0.3f) / 0.7f).coerceIn(0f, 1f)

                val checkPath = Path().apply {
                    moveTo(w * 0.22f, h * 0.52f)
                    lineTo(w * 0.43f, h * 0.70f)
                    lineTo(w * 0.78f, h * 0.33f)
                }
                drawPath(
                    path  = checkPath,
                    color = Color.White.copy(alpha = alpha),
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap   = StrokeCap.Round,
                        join  = StrokeJoin.Round,
                    ),
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// DueDateChip
// ══════════════════════════════════════════════════════════════════

@Composable
private fun DueDateChip(dueDate: Long) {
    val now      = System.currentTimeMillis()
    val isOverdue = dueDate < now
    val label    = remember(dueDate) {
        SimpleDateFormat("a h:mm", Locale.KOREA).format(Date(dueDate))
    }

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            painter            = painterResource(R.drawable.clock),
            contentDescription = null,
            tint               = if (isOverdue) OverdueRed else Color(0xFF6B7280),
            modifier           = Modifier.size(11.dp),
        )
        Text(
            text  = if (isOverdue) "$label 기한 초과" else label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isOverdue) OverdueRed else Color(0xFF6B7280),
        )
    }
}

// ══════════════════════════════════════════════════════════════════
// SwipeDeleteBg
// ══════════════════════════════════════════════════════════════════

@Composable
private fun SwipeDeleteBg() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SwipeDeleteBackground, RoundedCornerShape(16.dp))
            .padding(end = 20.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            painter            = painterResource(R.drawable.trash_2),
            contentDescription = "삭제",
            tint               = Color.White,
            modifier           = Modifier.size(22.dp),
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
 * Card (16dp 코너, 1dp 연회색 테두리, 0dp elevation)
 *   ├── 4dp 좌측 액센트 바 (priority color)
 *   └── Column
 *         ├── GroupedTaskRow(task0) — [제목+메타 left] [원형 체크박스 right]
 *         ├── HorizontalDivider (액센트 컬러 12% opacity)
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
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp)),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = bgColor),
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
                    key(task.id) { 
                        GroupedTaskRow(
                            task         = task,
                            accentColor  = accentColor,
                            onToggleDone = { onToggleDone(task) },
                            onDelete     = { onDelete(task) },
                            onEdit       = { onEdit(task) },
                        )
                    }   
                    if (index < tasks.lastIndex) {
                        HorizontalDivider(
                            color     = accentColor.copy(alpha = 0.12f),
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
    accentColor: Color,
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

    val titleAlpha by animateFloatAsState(
        targetValue   = if (task.isDone) 0.4f else 1f,
        animationSpec = tween(300),
        label         = "groupedTitleAlpha",
    )
    val primaryColor = MaterialTheme.colorScheme.primary

    SwipeToDismissBox(
        state                      = swipeState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            // 카드 내부이므로 별도 코너 불필요 — 부모 Card가 클리핑
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SwipeDeleteBackground)
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    painter            = painterResource(R.drawable.trash_2),
                    contentDescription = "삭제",
                    tint               = Color.White,
                    modifier           = Modifier.size(22.dp),
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
            // ── 제목 + 부가 정보 ──────────────────────────────
            Column(
                modifier            = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text  = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight     = FontWeight.SemiBold,
                        color          = Color(0xFF1D1D1F).copy(alpha = titleAlpha),
                        textDecoration = if (task.isDone) TextDecoration.LineThrough
                                         else TextDecoration.None,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!task.description.isNullOrBlank()) {
                    Text(
                        text  = task.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280).copy(alpha = titleAlpha),
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                task.dueDate?.let { DueDateChip(it) }
            }

            // ── 원형 체크박스 ─────────────────────────────────
            CircularCheckbox(
                checked  = task.isDone,
                onClick  = onToggleDone,
                primary  = primaryColor,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}
