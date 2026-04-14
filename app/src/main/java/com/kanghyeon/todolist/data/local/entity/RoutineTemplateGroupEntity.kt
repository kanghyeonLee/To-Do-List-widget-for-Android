package com.kanghyeon.todolist.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 루틴 템플릿 그룹 (예: "출근 루틴", "주말 루틴")
 *
 * - isActive: true인 그룹의 할 일만 매일 TaskEntity로 자동 복사됨
 * - CASCADE 삭제: 그룹 삭제 시 연결된 RoutineTemplateTaskEntity도 함께 제거
 */
@Entity(tableName = "routine_template_groups")
data class RoutineTemplateGroupEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 템플릿 그룹 이름 */
    val name: String,

    /** 활성화 여부 — false이면 자동 추가 대상에서 제외 */
    val isActive: Boolean = true,

    /** 생성 시각 (정렬 기준) */
    val createdAt: Long = System.currentTimeMillis(),
)
