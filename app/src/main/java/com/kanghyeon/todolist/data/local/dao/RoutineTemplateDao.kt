package com.kanghyeon.todolist.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kanghyeon.todolist.data.local.entity.RoutineTemplateGroupEntity
import com.kanghyeon.todolist.data.local.entity.RoutineTemplateGroupWithTasks
import com.kanghyeon.todolist.data.local.entity.RoutineTemplateTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineTemplateDao {

    // ── 그룹 READ ──────────────────────────────────────────────────

    /** 전체 그룹 + 할 일 목록 (실시간 스트림) */
    @Transaction
    @Query("SELECT * FROM routine_template_groups ORDER BY createdAt ASC")
    fun getAllGroupsWithTasks(): Flow<List<RoutineTemplateGroupWithTasks>>

    /** 활성화된 그룹 + 할 일 목록 (일회성, 루틴 자동 생성 전용) */
    @Transaction
    @Query("SELECT * FROM routine_template_groups WHERE isActive = 1")
    suspend fun getActiveGroupsWithTasksOnce(): List<RoutineTemplateGroupWithTasks>

    // ── 그룹 WRITE ─────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: RoutineTemplateGroupEntity): Long

    /** 활성화 토글 */
    @Query("UPDATE routine_template_groups SET isActive = :isActive WHERE id = :id")
    suspend fun updateGroupActiveState(id: Long, isActive: Boolean)

    /** 그룹명 변경 */
    @Query("UPDATE routine_template_groups SET name = :name WHERE id = :id")
    suspend fun updateGroupName(id: Long, name: String)

    /** 그룹 삭제 — CASCADE로 소속 task도 자동 삭제됨 */
    @Query("DELETE FROM routine_template_groups WHERE id = :id")
    suspend fun deleteGroupById(id: Long)

    // ── 할 일 WRITE ────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: RoutineTemplateTaskEntity): Long

    @Query("DELETE FROM routine_template_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)
}
