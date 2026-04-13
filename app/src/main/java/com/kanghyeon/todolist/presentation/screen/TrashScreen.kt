package com.kanghyeon.todolist.presentation.screen

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import com.kanghyeon.todolist.R
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanghyeon.todolist.data.local.entity.Priority
import com.kanghyeon.todolist.data.local.entity.TaskEntity
import com.kanghyeon.todolist.presentation.theme.AppOutline
import com.kanghyeon.todolist.presentation.theme.PriorityHigh
import com.kanghyeon.todolist.presentation.theme.PriorityLow
import com.kanghyeon.todolist.presentation.theme.PriorityMedium
import com.kanghyeon.todolist.presentation.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: TaskViewModel,
    onBack: () -> Unit,
) {
    val deletedTasks by viewModel.deletedTasks.collectAsStateWithLifecycle()
    var showEmptyConfirm by remember { mutableStateOf(false) }

    // 휴지통 비우기 확인 다이얼로그
    if (showEmptyConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirm = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.trash_2),
                    contentDescription = null,
                    modifier = Modifier.padding(4.dp),
                )
            },
            title = { Text("휴지통 비우기") },
            text = { Text("휴지통의 모든 항목이 영구 삭제됩니다.\n이 작업은 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.emptyAllTrash()
                        showEmptyConfirm = false
                    }
                ) { Text("비우기", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirm = false }) { Text("취소") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "휴지통",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.x),
                            contentDescription = "뒤로",
                            modifier = Modifier.size(22.dp),
                        )
                    }
                },
                actions = {
                    if (deletedTasks.isNotEmpty()) {
                        IconButton(onClick = { showEmptyConfirm = true }) {
                            Icon(
                                painter = painterResource(R.drawable.trash_2),
                                contentDescription = "휴지통 비우기",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        if (deletedTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.trash_2),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        text = "휴지통이 비어 있어요",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = deletedTasks,
                    key = { "trash_${it.id}" },
                ) { task ->
                    TrashTaskItem(
                        task = task,
                        onRestore = { viewModel.restoreFromTrash(task) },
                        onDelete  = { viewModel.permanentlyDeleteTask(task) },
                        modifier  = Modifier.animateItem(),
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun TrashTaskItem(
    task: TaskEntity,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("영구 삭제") },
            text = { Text("「${task.title}」을(를) 영구 삭제할까요?\n이 작업은 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(); showDeleteConfirm = false }
                ) { Text("삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("취소") }
            },
        )
    }

    val accentColor = when (Priority.from(task.priority)) {
        Priority.HIGH   -> PriorityHigh
        Priority.MEDIUM -> PriorityMedium
        Priority.LOW    -> PriorityLow
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, AppOutline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 우선순위 색상 도트
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .let { it },
            ) {
                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier
                        .height(20.dp)
                        .padding(end = 4.dp)
                )
            }

            // 제목 + 설명
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!task.description.isNullOrBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // 우선순위 라벨
                Text(
                    text = when (Priority.from(task.priority)) {
                        Priority.HIGH   -> "높음"
                        Priority.MEDIUM -> "보통"
                        Priority.LOW    -> "낮음"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                )
            }

            // 복구 버튼
            IconButton(onClick = onRestore) {
                Icon(
                    painter = painterResource(R.drawable.rotate_ccw),
                    contentDescription = "복구",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }

            // 영구 삭제 버튼
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    painter = painterResource(R.drawable.trash_2),
                    contentDescription = "영구 삭제",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
