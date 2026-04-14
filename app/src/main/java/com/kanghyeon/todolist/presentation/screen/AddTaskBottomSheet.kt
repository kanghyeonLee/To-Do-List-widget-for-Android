package com.kanghyeon.todolist.presentation.screen

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import android.os.Build
import android.view.LayoutInflater
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import com.kanghyeon.todolist.R
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.kanghyeon.todolist.data.local.entity.Priority
import com.kanghyeon.todolist.data.local.entity.TaskEntity
import com.kanghyeon.todolist.presentation.theme.CardBorderColor
import com.kanghyeon.todolist.presentation.theme.PriorityHigh
import com.kanghyeon.todolist.presentation.theme.PriorityLow
import com.kanghyeon.todolist.presentation.theme.PriorityMedium
import com.kanghyeon.todolist.presentation.viewmodel.NewTaskDraft
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 할 일 추가/수정 ModalBottomSheet
 *
 * @param task  null = 추가 모드, non-null = 수정 모드 (기존 데이터 pre-populate)
 * @param onSave 저장 콜백
 *
 * [마감 시간]
 * 날짜는 오늘 고정, 시간만 TimePicker로 선택 → dueDate (epoch ms)
 *
 * [유효성]
 * selectedTime 기준 dueDate < 현재+5분이면 저장 버튼 비활성화 + 에러 메시지 노출
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskBottomSheet(
    onDismiss: () -> Unit,
    onCancel: () -> Unit = onDismiss,
    onSave: (
        title: String,
        description: String?,
        priority: Int,
        dueDate: Long?,
        showOnLockScreen: Boolean,
        reminderMinutes: Int?,
    ) -> Unit,
    onDraftChange: (NewTaskDraft) -> Unit = {},
    task: TaskEntity? = null,
    initialDraft: NewTaskDraft = NewTaskDraft(),
) {
    val isEditMode = task != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope      = rememberCoroutineScope()
    val zone       = ZoneId.systemDefault()

    // 수정 모드: 기존 Task에서 날짜/시간 추출 / 추가 모드: draft에서 복원
    val initialDate: LocalDate? = remember {
        if (task != null) {
            task.dueDate?.let { ms ->
                Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
            }
        } else {
            initialDraft.selectedDateMs?.let { ms ->
                Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
            }
        }
    }
    val initialTime: LocalTime? = remember {
        if (task != null) {
            task.dueDate?.let { ms ->
                val lt = Instant.ofEpochMilli(ms).atZone(zone).toLocalTime()
                // 자정(00:00)이면 날짜만 선택된 것으로 간주
                if (lt.hour == 0 && lt.minute == 0) null
                else LocalTime.of(lt.hour, lt.minute)
            }
        } else {
            if (initialDraft.selectedHour != null && initialDraft.selectedMinute != null)
                LocalTime.of(initialDraft.selectedHour, initialDraft.selectedMinute)
            else null
        }
    }

    // 추가 모드: draft 값으로 초기화 / 수정 모드: task 값으로 초기화
    var title            by remember { mutableStateOf(task?.title ?: initialDraft.title) }
    var description      by remember { mutableStateOf(task?.description ?: initialDraft.description) }
    var selectedPriority by remember { mutableIntStateOf(task?.priority ?: initialDraft.priority) }
    var showOnLockScreen by remember { mutableStateOf(task?.showOnLockScreen ?: initialDraft.showOnLockScreen) }
    var titleError       by remember { mutableStateOf(false) }
    var selectedDate     by remember { mutableStateOf<LocalDate?>(initialDate) }
    var selectedTime     by remember { mutableStateOf<LocalTime?>(initialTime) }
    var reminderMinutes  by remember { mutableStateOf<Int?>(task?.reminderMinutes ?: initialDraft.reminderMinutes) }
    var showTimePicker   by remember { mutableStateOf(false) }
    var showDatePicker   by remember { mutableStateOf(false) }

    // 상태 변경 시 draft 동기화 (추가 모드에서만 의미 있음)
    fun syncDraft() {
        if (!isEditMode) {
            onDraftChange(
                NewTaskDraft(
                    title            = title,
                    description      = description,
                    priority         = selectedPriority,
                    selectedHour     = selectedTime?.hour,
                    selectedMinute   = selectedTime?.minute,
                    showOnLockScreen = showOnLockScreen,
                    reminderMinutes  = reminderMinutes,
                    selectedDateMs   = selectedDate
                        ?.atStartOfDay(zone)?.toInstant()?.toEpochMilli(),
                )
            )
        }
    }

    // 마감일 계산:
    //   날짜 + 시간 → 정확한 타임스탬프
    //   날짜만     → 해당 날짜 자정 (D-Day 분류용)
    //   시간만     → 오늘 + 시간 (기존 동작 유지)
    //   둘 다 없음 → null
    val dueDate: Long? = when {
        selectedDate != null && selectedTime != null ->
            selectedDate!!.atTime(selectedTime!!)
                .atZone(zone).toInstant().toEpochMilli()
        selectedDate != null ->
            selectedDate!!.atStartOfDay(zone).toInstant().toEpochMilli()
        selectedTime != null ->
            LocalDate.now().atTime(selectedTime!!)
                .atZone(zone).toInstant().toEpochMilli()
        else -> null
    }

    // 유효성 검사: 오늘 날짜(또는 날짜 미선택=오늘)이면 5분 이후 시간 필요
    val effectiveDate = selectedDate ?: if (selectedTime != null) LocalDate.now() else null
    val isTooSoon = dueDate != null &&
            effectiveDate == LocalDate.now() &&
            selectedTime != null &&
            dueDate < System.currentTimeMillis() + 5 * 60_000L

    // 시간이 없으면 알림도 초기화
    if (selectedTime == null) reminderMinutes = null

    fun submit() {
        if (title.isBlank()) { titleError = true; return }
        if (isTooSoon) return
        onSave(
            title.trim(),
            description.trim().ifBlank { null },
            selectedPriority,
            dueDate,
            showOnLockScreen,
            reminderMinutes,
        )
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    // ── DatePickerDialog (마감 날짜 선택) ──────────────────────
    if (showDatePicker) {
        val todayUtcMs = remember {
            LocalDate.now(zone)
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val initialUtcMs = remember(selectedDate) {
            (selectedDate ?: LocalDate.now(zone))
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val dpState = rememberDatePickerState(
            initialSelectedDateMillis = initialUtcMs,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    utcTimeMillis >= todayUtcMs     // 오늘 이후만 선택 가능
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
                    dpState.selectedDateMillis?.let { utcMs ->
                        selectedDate = Instant.ofEpochMilli(utcMs)
                            .atZone(ZoneOffset.UTC).toLocalDate()
                        syncDraft()
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
            DatePicker(state = dpState, colors = dpColors)
        }
    }

    // ── TimePickerDialog (스피너 휠 방식) ────────────────────
    if (showTimePicker) {
        val now = LocalTime.now()
        // 내부 상태: setOnTimeChangedListener → 상태 업데이트 → 확인 버튼에서 읽기
        var pickerHour   by remember { mutableIntStateOf(selectedTime?.hour   ?: now.hour) }
        var pickerMinute by remember { mutableIntStateOf(selectedTime?.minute ?: now.minute) }

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = LocalTime.of(pickerHour, pickerMinute)
                    showTimePicker = false
                    syncDraft()
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("취소") }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            text = {
                // AndroidView로 XML에서만 지정 가능한 timePickerMode="spinner" 적용
                AndroidView(
                    factory = { ctx ->
                        val tp = LayoutInflater.from(ctx)
                            .inflate(R.layout.time_picker_spinner, null)
                            as android.widget.TimePicker
                        tp.setIs24HourView(false) // 12시간제 (AM/PM)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            tp.hour   = pickerHour
                            tp.minute = pickerMinute
                        } else {
                            @Suppress("DEPRECATION")
                            tp.currentHour   = pickerHour
                            @Suppress("DEPRECATION")
                            tp.currentMinute = pickerMinute
                        }
                        tp.setOnTimeChangedListener { _, h, m ->
                            pickerHour   = h
                            pickerMinute = m
                        }
                        tp
                    },
                )
            },
        )
    }
    val focusManager = LocalFocusManager.current

    // ── BottomSheet ───────────────────────────────────────────
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
                        .background(Color(0xFFE5E7EB)),   // 흰 배경 위 연회색 핸들
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
                .navigationBarsPadding()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                },
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── 헤더 ─────────────────────────────────────────
            Text(
                text  = if (isEditMode) "할 일 수정" else "새 할 일",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF1D1D1F),
                ),
            )

            // ── 제목 입력 ─────────────────────────────────────
            TextField(
                value = title,
                onValueChange = {
                    title = it
                    if (it.isNotBlank()) titleError = false
                    syncDraft()
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
                colors = TextFieldDefaults.colors(
                    focusedContainerColor      = Color.Transparent,
                    unfocusedContainerColor    = Color.Transparent,
                    focusedIndicatorColor      = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor    = Color(0xFFE5E7EB),
                    errorContainerColor        = Color.Transparent,
                    errorIndicatorColor        = MaterialTheme.colorScheme.error,
                    disabledContainerColor     = Color.Transparent,
                    focusedTextColor           = Color(0xFF1D1D1F),
                    unfocusedTextColor         = Color(0xFF1D1D1F),
                    errorTextColor             = Color(0xFF1D1D1F),
                    focusedLabelColor          = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor        = Color(0xFF6B7280),
                    errorLabelColor            = MaterialTheme.colorScheme.error,
                    focusedPlaceholderColor    = Color(0xFF9CA3AF),
                    unfocusedPlaceholderColor  = Color(0xFF9CA3AF),
                    errorSupportingTextColor   = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier
                    .fillMaxWidth()
            )

            // ── 메모 입력 (선택) ──────────────────────────────
            TextField(
                value = description,
                onValueChange = { description = it; syncDraft() },
                label = { Text("메모 (선택)") },
                minLines = 2,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor     = Color.Transparent,
                    unfocusedContainerColor   = Color.Transparent,
                    focusedIndicatorColor     = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor   = Color(0xFFE5E7EB),
                    disabledContainerColor    = Color.Transparent,
                    focusedTextColor          = Color(0xFF1D1D1F),
                    unfocusedTextColor        = Color(0xFF1D1D1F),
                    focusedLabelColor         = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor       = Color(0xFF6B7280),
                    focusedPlaceholderColor   = Color(0xFF9CA3AF),
                    unfocusedPlaceholderColor = Color(0xFF9CA3AF),
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            // ── 우선순위 ──────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "우선순위",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF6B7280),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriorityChip(
                        label = "낮음",
                        selected = selectedPriority == Priority.LOW.value,
                        selectedColor = PriorityLow,
                        onClick = { selectedPriority = Priority.LOW.value; syncDraft() },
                    )
                    PriorityChip(
                        label = "보통",
                        selected = selectedPriority == Priority.MEDIUM.value,
                        selectedColor = PriorityMedium,
                        onClick = { selectedPriority = Priority.MEDIUM.value; syncDraft() },
                    )
                    PriorityChip(
                        label = "높음",
                        selected = selectedPriority == Priority.HIGH.value,
                        selectedColor = PriorityHigh,
                        onClick = { selectedPriority = Priority.HIGH.value; syncDraft() },
                    )
                }
            }

            // ── 마감 날짜 (DatePicker) ────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text  = "마감 날짜",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF6B7280),
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    val dateLabel = selectedDate
                        ?.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", Locale.KOREA))
                        ?: "날짜 선택 (D-Day 기능)"
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (selectedDate != null)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
                                else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                            )
                            .border(
                                width = 1.dp,
                                color = if (selectedDate != null) MaterialTheme.colorScheme.primary
                                        else CardBorderColor,
                                shape = RoundedCornerShape(8.dp),
                            )
                            .clickable(
                                onClick           = { showDatePicker = true },
                                indication        = null,
                                interactionSource = remember { MutableInteractionSource() },
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            painter            = painterResource(R.drawable.calendar),
                            contentDescription = null,
                            tint               = if (selectedDate != null)
                                MaterialTheme.colorScheme.primary
                            else Color(0xFF6B7280),
                            modifier           = Modifier.size(16.dp),
                        )
                        Text(
                            text  = dateLabel,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color      = if (selectedDate != null) MaterialTheme.colorScheme.primary
                                             else Color(0xFF9CA3AF),
                                fontWeight = if (selectedDate != null)
                                    androidx.compose.ui.text.font.FontWeight.Medium
                                else androidx.compose.ui.text.font.FontWeight.Normal,
                            ),
                        )
                    }
                    if (selectedDate != null) {
                        TextButton(onClick = { selectedDate = null; syncDraft() }) {
                            Text("삭제")
                        }
                    }
                }
            }

            // ── 마감 시간 ─────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "마감 시간",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF6B7280),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = selectedTime
                                ?.format(DateTimeFormatter.ofPattern("a h:mm", Locale.KOREA))
                                ?: "시간 선택",
                        )
                    }
                    if (selectedTime != null) {
                        TextButton(onClick = {
                            selectedTime = null
                            syncDraft()
                        }) {
                            Text("삭제")
                        }
                    }
                }
                if (isTooSoon) {
                    Text(
                        text = "현재 시각 기준 5분 이후 시간을 선택해 주세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            // ── 사전 알림 (시간이 설정된 경우) ───────────────
            if (selectedTime != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "사전 알림",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF6B7280),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReminderChip("없음",  reminderMinutes == null) { reminderMinutes = null; syncDraft() }
                        ReminderChip("10분",  reminderMinutes == 10)   { reminderMinutes = 10;  syncDraft() }
                        ReminderChip("30분",  reminderMinutes == 30)   { reminderMinutes = 30;  syncDraft() }
                        ReminderChip("1시간", reminderMinutes == 60)   { reminderMinutes = 60;  syncDraft() }
                    }
                }
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
                        painter = painterResource(R.drawable.flag),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Column {
                        Text(
                            text  = "잠금화면에 표시",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF1D1D1F),
                            ),
                        )
                        Text(
                            text = "알림창에서 바로 완료 처리 가능",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280),
                        )
                    }
                }
                Switch(
                    checked = showOnLockScreen,
                    onCheckedChange = { showOnLockScreen = it; syncDraft() },
                )
            }

            // ── 버튼 행 ───────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onCancel() }
                }) {
                    Text("취소")
                }
                Button(
                    onClick = ::submit,
                    enabled = title.isNotBlank() && !isTooSoon,
                ) {
                    Text(if (isEditMode) "저장" else "추가")
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
        shape = CircleShape,
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

@Composable
private fun ReminderChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = CircleShape,
    )
}
