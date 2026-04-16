package com.kanghyeon.todolist.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kanghyeon.todolist.data.repository.AiGoalDto
import com.kanghyeon.todolist.data.repository.AiTaskDto
import com.kanghyeon.todolist.presentation.viewmodel.AiUiState

@Composable
fun AiScreen(
    uiState: AiUiState,
    onPromptChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onSaveSelected: (List<AiTaskDto>, List<AiGoalDto>) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val selectedTasks = remember { mutableStateListOf<AiTaskDto>() }
    val selectedGoals = remember { mutableStateListOf<AiGoalDto>() }

    LaunchedEffect(uiState.suggestedTasks, uiState.suggestedGoals) {
        selectedTasks.clear()
        selectedTasks.addAll(uiState.suggestedTasks)
        selectedGoals.clear()
        selectedGoals.addAll(uiState.suggestedGoals)
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onShowMessage("성공적으로 저장되었습니다!")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "AI 할 일 자동 생성",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "자연어로 일정을 입력해 주시면 AI가 분석하여 목표와 할 일을 자동으로 정리해 줍니다.",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.prompt,
                onValueChange = onPromptChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                placeholder = { Text("예: 다음 주 제주도 여행 준비물 목록") },
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )
            IconButton(
                onClick = onGenerate,
                enabled = uiState.prompt.isNotBlank() && !uiState.isLoading,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "생성")
            }
        }

        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 32.dp))
        } else if (uiState.suggestedGoals.isNotEmpty() || uiState.suggestedTasks.isNotEmpty()) {
            Text(
                text = "작성된 미리보기 결과를 확인하고 저장할 항목을 선택하세요.",
                style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(modifier = Modifier.weight(1f)) {
                if (uiState.suggestedGoals.isNotEmpty()) {
                    item {
                        Text(
                            "추천 목표",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(uiState.suggestedGoals) { goal ->
                        val isSelected = selectedGoals.contains(goal)
                        PreviewItemCard(
                            title = goal.title,
                            description = goal.description,
                            dateString = "${goal.startDateString ?: ""} ~ ${goal.endDateString ?: ""}",
                            priority = goal.priority,
                            isSelected = isSelected,
                            onToggle = {
                                if (isSelected) selectedGoals.remove(goal) else selectedGoals.add(goal)
                            }
                        )
                    }
                }

                if (uiState.suggestedTasks.isNotEmpty()) {
                    item {
                        Text(
                            "추천 할 일",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(uiState.suggestedTasks) { task ->
                        val isSelected = selectedTasks.contains(task)
                        PreviewItemCard(
                            title = task.title,
                            description = task.description,
                            dateString = task.dueDateString ?: "기한 없음",
                            priority = task.priority,
                            isSelected = isSelected,
                            onToggle = {
                                if (isSelected) selectedTasks.remove(task) else selectedTasks.add(task)
                            }
                        )
                    }
                }
            }
            
            Button(
                onClick = { onSaveSelected(selectedTasks.toList(), selectedGoals.toList()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("선택한 항목 저장하기", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PreviewItemCard(
    title: String,
    description: String?,
    dateString: String,
    priority: Int,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                modifier = Modifier.padding(end = 8.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold)
                if (!description.isNullOrBlank()) {
                    Text(text = description, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = "기한: $dateString | ${getPriorityLabel(priority)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun getPriorityLabel(priority: Int): String {
    return when (priority) {
        0 -> "우선순위: 낮음"
        1 -> "우선순위: 보통"
        2 -> "우선순위: 높음"
        else -> "우선순위: 보통"
    }
}
