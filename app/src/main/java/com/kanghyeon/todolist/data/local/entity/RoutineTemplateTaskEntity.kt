package com.kanghyeon.todolist.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 루틴 템플릿 그룹에 속한 개별 할 일
 *
 * - groupId: RoutineTemplateGroupEntity.id 참조 (CASCADE 삭제)
 * - 자동 추가 시 TaskEntity로 복사되는 청사진(blueprint)
 */
@Entity(
    tableName = "routine_template_tasks",
    foreignKeys = [
        ForeignKey(
            entity        = RoutineTemplateGroupEntity::class,
            parentColumns = ["id"],
            childColumns  = ["groupId"],
            onDelete      = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("groupId")],
)
data class RoutineTemplateTaskEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 소속 그룹 ID */
    val groupId: Long,

    /** 할 일 제목 */
    val title: String,

    /** 선택적 메모 */
    val description: String? = null,

    /** 우선순위 (Priority.value) */
    val priority: Int = Priority.MEDIUM.value,

    /** 잠금화면 알림 여부 */
    val showOnLockScreen: Boolean = true,
)
