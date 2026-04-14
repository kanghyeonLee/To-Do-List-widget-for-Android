package com.kanghyeon.todolist.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 루틴 템플릿 테이블 — 매일 자동으로 TaskEntity로 복사될 반복 할 일 설계도.
 *
 * - dueDate 없음: 템플릿은 시간이 없으며, 생성된 Task에도 기본적으로 없음
 * - priority: TaskEntity와 동일한 Int 인코딩 (LOW=0, MEDIUM=1, HIGH=2)
 */
@Entity(tableName = "routine_templates")
data class RoutineTemplateEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 루틴 제목 (필수) */
    val title: String,

    /** 선택적 메모 */
    val description: String? = null,

    /** 우선순위 — Priority.value 참고 */
    val priority: Int = Priority.MEDIUM.value,

    /** 잠금화면 알림 노출 여부 */
    val showOnLockScreen: Boolean = true,

    /** 템플릿 생성 시각 (정렬 기준) */
    val createdAt: Long = System.currentTimeMillis(),
)
