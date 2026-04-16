package com.kanghyeon.todolist.data.repository

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

// ── AI Response DTOs ──

data class AiGeneratedResponse(
    @SerializedName("tasks") val tasks: List<AiTaskDto> = emptyList(),
    @SerializedName("goals") val goals: List<AiGoalDto> = emptyList()
)

data class AiTaskDto(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("priority") val priority: Int = 1, // 0: LOW, 1: MEDIUM, 2: HIGH
    @SerializedName("dueDateString") val dueDateString: String? = null // yyyy-MM-dd
)

data class AiGoalDto(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("priority") val priority: Int = 1, // 0: LOW, 1: MEDIUM, 2: HIGH
    @SerializedName("startDateString") val startDateString: String? = null, // yyyy-MM-dd
    @SerializedName("endDateString") val endDateString: String? = null // yyyy-MM-dd
)

interface AiRepository {
    suspend fun generateTasksAndGoals(prompt: String): Result<AiGeneratedResponse>
}

@Singleton
class AiRepositoryImpl @Inject constructor(
    private val generativeModel: GenerativeModel,
    private val gson: Gson
) : AiRepository {

    override suspend fun generateTasksAndGoals(prompt: String): Result<AiGeneratedResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            val todayDate = LocalDate.now().toString()
            val fullPrompt = """
                오늘 날짜는 $todayDate 입니다.
                사용자의 다음 입력을 분석하여 파생되는 할 일(Tasks)과 목표(Goals)를 추론하세요.
                반드시 아래의 JSON 형식으로만 응답해야 하며, 다른 부가적인 텍스트는 포함하지 마세요. (마크다운 코드 블록도 제외하세요.)
                만약 목표(Goal)가 딱히 구상되지 않는다면 goals 배열을 비워두세요.
                우선순위(priority)는 0(낮음), 1(보통), 2(높음) 중 하나를 사용합니다.
                만약 예측된 마감 날짜나 시작/종료 날짜가 있다면 "yyyy-MM-dd" 형식으로 제공하세요. 날짜가 추론되지 않는다면 생략(null)해도 됩니다.
                
                [형식]
                {
                  "tasks": [
                    {
                      "title": "할 일 제목",
                      "description": "설명 (옵션)",
                      "priority": 1,
                      "dueDateString": "2024-05-10"
                    }
                  ],
                  "goals": [
                    {
                      "title": "목표 제목",
                      "description": "설명 (옵션)",
                      "priority": 1,
                      "startDateString": "2024-05-01",
                      "endDateString": "2024-05-20"
                    }
                  ]
                }
                
                [사용자 입력]
                $prompt
            """.trimIndent()

            val response: GenerateContentResponse = generativeModel.generateContent(
                content {
                    text(fullPrompt)
                }
            )

            var jsonText = response.text ?: "{}"
            // 혹시 마크다운 블록이 포함되어 있다면 제거
            jsonText = jsonText.trim()
            if (jsonText.startsWith("```json")) {
                jsonText = jsonText.substring(7)
            } else if (jsonText.startsWith("```")) {
                jsonText = jsonText.substring(3)
            }
            if (jsonText.endsWith("```")) {
                jsonText = jsonText.substring(0, jsonText.length - 3)
            }
            jsonText = jsonText.trim()

            val parsedResponse = gson.fromJson(jsonText, AiGeneratedResponse::class.java)
            Result.success(parsedResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
