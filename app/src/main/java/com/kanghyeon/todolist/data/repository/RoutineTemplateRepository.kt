package com.kanghyeon.todolist.data.repository

import com.kanghyeon.todolist.data.local.entity.RoutineTemplateGroupEntity
import com.kanghyeon.todolist.data.local.entity.RoutineTemplateGroupWithTasks
import com.kanghyeon.todolist.data.local.entity.RoutineTemplateTaskEntity
import kotlinx.coroutines.flow.Flow

interface RoutineTemplateRepository {

    // ── 그룹 READ ──────────────────────────────────────────────────

    /** 전체 그룹 + 할 일 실시간 스트림 */
    fun getAllGroupsWithTasks(): Flow<List<RoutineTemplateGroupWithTasks>>

    /** 활성화된 그룹 + 할 일 일회성 조회 (루틴 자동 생성용) */
    suspend fun getActiveGroupsWithTasksOnce(): List<RoutineTemplateGroupWithTasks>

    // ── 그룹 WRITE ─────────────────────────────────────────────────

    /** 새 그룹 추가 */
    suspend fun addGroup(name: String): Long

    /** 활성화/비활성화 토글 */
    suspend fun updateGroupActiveState(id: Long, isActive: Boolean)

    /** 그룹명 변경 */
    suspend fun updateGroupName(id: Long, name: String)

    /** 그룹 삭제 (소속 task CASCADE 삭제) */
    suspend fun deleteGroup(id: Long)

    // ── 할 일 WRITE ────────────────────────────────────────────────

    /** 그룹에 할 일 추가 */
    suspend fun addTask(task: RoutineTemplateTaskEntity): Long

    /** 할 일 단건 삭제 */
    suspend fun deleteTask(id: Long)
}
