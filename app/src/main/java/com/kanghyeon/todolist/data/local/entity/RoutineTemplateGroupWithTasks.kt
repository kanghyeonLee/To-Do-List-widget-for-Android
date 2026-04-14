package com.kanghyeon.todolist.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room @Relation 결과 클래스 — 그룹 + 소속 할 일 목록을 한 번에 조회
 *
 * @Transaction 어노테이션이 붙은 DAO 메서드에서만 사용.
 */
data class RoutineTemplateGroupWithTasks(
    @Embedded
    val group: RoutineTemplateGroupEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "groupId",
    )
    val tasks: List<RoutineTemplateTaskEntity>,
)
