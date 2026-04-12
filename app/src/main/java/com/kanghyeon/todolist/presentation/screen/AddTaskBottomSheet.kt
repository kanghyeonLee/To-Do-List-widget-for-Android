package com.kanghyeon.todolist.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.kanghyeon.todolist.data.local.entity.Priority
import com.kanghyeon.todolist.presentation.theme.PriorityHigh
import com.kanghyeon.todolist.presentation.theme.PriorityLow
import com.kanghyeon.todolist.presentation.theme.PriorityMedium
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 할 일 추가 ModalBottomSheet
 *
 * [사용 흐름]
 * FAB 클릭 → showBottomSheet=true → AddTaskBottomSheet 표시
 * → 사용자 입력 → "추가" 버튼 → onAdd 콜백 → ViewModel.addTask()
 *
 * [skipPartiallyExpanded]
 * true로 설정해 키보드 올라올 때 시트가 반쯤 열리는 상태를 건너뜀.
 * imePadding()과 함께 사용해 키보드가 입력 필드를 가리지 않게 처리.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskBottomSheet(
    onDismiss: () -> Unit,
    onAdd: (
        title: String,
        description: String?,
        priority: Int,
        dueDate: Long?,
        showOnLockScreen: Boolean,
    ) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // ── 입력 상태 ─────────────────────────────────────────────
    var title          by remember { mutableStateOf("") }
    var description    by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableIntStateOf(Priority.MEDIUM.value) }
    var dueTodayChecked  by remember { mutableStateOf(false) }
    var showOnLockScreen by remember { mutableStateOf(true) }
    var titleError     by remember { mutableStateOf(false) }

    // 시트가 열리면 제목 필드에 자동 포커스
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // 오늘 자정 epoch ms (마감일 "오늘" 선택 시 사용)
    val todayEndOfDay = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    fun submit() {
        if (title.isBlank()) {
            titleError = true
            return
        }
        onAdd(
            title.trim(),
            description.trim().ifBlank { null },
            selectedPriority,
            if (dueTodayChecked) todayEndOfDay else null,
            showOnLockScreen,
        )
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .imePadding()                 // 키보드 위로 시트 끌어올림
                .navigationBarsPadding(),     // 하단 네비게이션 바 여백
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── 헤더 ─────────────────────────────────────────
            Text(
                text = "새 할 일",
                style = MaterialTheme.typography.titleLarge,
            )

            // ── 제목 입력 ─────────────────────────────────────
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (it.isNotBlank()) titleError = false
                },
                label = { Text("제목 *") },
                placeholder = { Text("무엇을 해야 하나요?") },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text("제목을 입력해 주세요.") }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )

            // ── 메모 입력 (선택) ──────────────────────────────
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("메모 (선택)") },
                minLines = 2,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth(),
            )

            // ── 우선순위 ──────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "우선순위",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriorityChip(
                        label = "낮음",
                        selected = selectedPriority == Priority.LOW.value,
                        selectedColor = PriorityLow,
                        onClick = { selectedPriority = Priority.LOW.value },
                    )
                    PriorityChip(
                        label = "보통",
                        selected = selectedPriority == Priority.MEDIUM.value,
                        selectedColor = PriorityMedium,
                        onClick = { selectedPriority = Priority.MEDIUM.value },
                    )
                    PriorityChip(
                        label = "높음",
                        selected = selectedPriority == Priority.HIGH.value,
                        selectedColor = PriorityHigh,
                        onClick = { selectedPriority = Priority.HIGH.value },
                    )
                }
            }

            // ── 오늘 마감 토글 ────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "오늘 마감",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "오늘의 할 일 목록에 표시됩니다",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = dueTodayChecked,
                    onCheckedChange = { dueTodayChecked = it },
                )
            }

            // ── 잠금화면 표시 토글 ────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column {
                        Text(
                            text = "잠금화면에 표시",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "알림창에서 바로 완료 처리 가능",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(
                    checked = showOnLockScreen,
                    onCheckedChange = { showOnLockScreen = it },
                )
            }

            // ── 버튼 행 ───────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                }) {
                    Text("취소")
                }
                Button(
                    onClick = ::submit,
                    enabled = title.isNotBlank(),
                ) {
                    Text("추가")
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PriorityChip(
    label: String,
    selected: Boolean,
    selectedColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = selectedColor.copy(alpha = 0.15f),
            selectedLabelColor = selectedColor,
            selectedLeadingIconColor = selectedColor,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            selectedBorderColor = selectedColor,
            selectedBorderWidth = 1.5.dp,
        ),
    )
}
