package com.kanghyeon.todolist.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanghyeon.todolist.data.local.entity.GoalEntity
import com.kanghyeon.todolist.data.local.entity.TaskEntity
import com.kanghyeon.todolist.data.repository.AiGoalDto
import com.kanghyeon.todolist.data.repository.AiRepository
import com.kanghyeon.todolist.data.repository.AiTaskDto
import com.kanghyeon.todolist.data.repository.GoalRepository
import com.kanghyeon.todolist.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject

data class AiUiState(
    val isLoading: Boolean = false,
    val prompt: String = "",
    val suggestedTasks: List<AiTaskDto> = emptyList(),
    val suggestedGoals: List<AiGoalDto> = emptyList(),
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class AiViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val taskRepository: TaskRepository,
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    fun updatePrompt(prompt: String) {
        _uiState.value = _uiState.value.copy(prompt = prompt, errorMessage = null)
    }

    fun generate() {
        val currentPrompt = _uiState.value.prompt
        if (currentPrompt.isBlank()) return

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            suggestedTasks = emptyList(),
            suggestedGoals = emptyList(),
            isSuccess = false
        )

        viewModelScope.launch {
            val result = aiRepository.generateTasksAndGoals(currentPrompt)
            result.onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    suggestedTasks = response.tasks,
                    suggestedGoals = response.goals
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "생성 중 오류가 발생했습니다: ${error.localizedMessage}"
                )
            }
        }
    }

    fun saveSelected(tasks: List<AiTaskDto>, goals: List<AiGoalDto>) {
        viewModelScope.launch {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val zone = ZoneId.systemDefault()

            // Save Goals
            goals.forEach { goalDto ->
                val startMs = parseDateStringToMs(goalDto.startDateString, formatter, zone) ?: System.currentTimeMillis()
                val endMs = parseDateStringToMs(goalDto.endDateString, formatter, zone) ?: (startMs + 7L * 24 * 60 * 60 * 1000)

                val goalEntity = GoalEntity(
                    title = goalDto.title,
                    type = com.kanghyeon.todolist.data.local.entity.GoalType.PROJECT,
                    targetValue = 100, // 임의의 타겟
                    colorHex = "#FF9800", // Default orange
                    startDate = startMs,
                    endDate = endMs
                )
                goalRepository.saveGoal(goalEntity)
            }

            // Save Tasks
            tasks.forEach { taskDto ->
                val dueMs = parseDateStringToMs(taskDto.dueDateString, formatter, zone)
                
                val taskEntity = TaskEntity(
                    title = taskDto.title,
                    description = taskDto.description,
                    priority = taskDto.priority,
                    dueDate = dueMs
                )
                taskRepository.saveTask(taskEntity)
            }

            _uiState.value = AiUiState(isSuccess = true) // Reset and show success
        }
    }

    private fun parseDateStringToMs(dateStr: String?, formatter: DateTimeFormatter, zone: ZoneId): Long? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            val date = LocalDate.parse(dateStr, formatter)
            date.atStartOfDay(zone).toInstant().toEpochMilli()
        } catch (e: DateTimeParseException) {
            null
        }
    }
    
    fun resetState() {
        _uiState.value = AiUiState()
    }
}
