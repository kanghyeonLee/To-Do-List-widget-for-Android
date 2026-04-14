package com.kanghyeon.todolist.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.kanghyeon.todolist.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // ──────────────────────────────────────────
    // READ
    // ──────────────────────────────────────────

    @Query(
        """
        SELECT * FROM tasks
        WHERE showOnLockScreen = 1 AND isDone = 0 AND isDeleted = 0
        ORDER BY priority DESC, dueDate ASC, sortOrder ASC
        """
    )
    fun getLockScreenTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id AND isDeleted = 0")
    fun getTaskById(id: Long): Flow<TaskEntity?>

    @Query(
        """
        SELECT * FROM tasks
        WHERE isDone = 1 AND isDeleted = 0
        ORDER BY updatedAt DESC
        """
    )
    fun getCompletedTasks(): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE isDone = 1 AND isDeleted = 0
          AND updatedAt >= :startOfDay
          AND updatedAt <= :endOfDay
        ORDER BY updatedAt DESC
        """
    )
    fun getCompletedTasksByDate(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>>

    @Query(
        """
        DELETE FROM tasks
        WHERE isDone = 1 AND isDeleted = 0
          AND updatedAt >= :startOfDay
          AND updatedAt <= :endOfDay
        """
    )
    suspend fun deleteCompletedByDateRange(startOfDay: Long, endOfDay: Long)

    @Query(
        """
        SELECT * FROM tasks
        WHERE isDone = 0 AND isDeleted = 0
        ORDER BY priority DESC, createdAt DESC
        """
    )
    fun getActiveTasks(): Flow<List<TaskEntity>>

    /** 휴지통 목록 — isDeleted = 1인 항목, 최신 삭제 순 */
    @Query(
        """
        SELECT * FROM tasks
        WHERE isDeleted = 1
        ORDER BY updatedAt DESC
        """
    )
    fun getDeletedTasks(): Flow<List<TaskEntity>>

    /**
     * D-Day 탭: dueDate가 지정된 할 일 중 삭제·아카이브되지 않은 항목.
     * - 미완료 → 완료 순, 마감 임박 순(dueDate ASC) 정렬
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE dueDate IS NOT NULL
          AND isDeleted = 0
          AND isArchived = 0
        ORDER BY isDone ASC, dueDate ASC
        """
    )
    fun getDDayTasks(): Flow<List<TaskEntity>>

    /**
     * 아카이브 탭: 날짜 범위 내 아카이브/완료 항목 통합 조회.
     *
     * [쿼리 조건]
     * 1. 신규 아카이브 (archivedAt 설정됨): isArchived=1 AND archivedAt in [start, end)
     * 2. 구버전 수동 아카이브 (archivedAt null): isArchived=1 AND updatedAt in [start, end)
     * 3. 구버전 완료 항목 호환 (isArchived=0, isDone=1): updatedAt in [start, end) 이고 오늘이전만
     *    → :todayStart 조건으로 오늘 완료 항목이 중복 노출되는 것을 방지
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE isDeleted = 0 AND (
            (isArchived = 1 AND archivedAt IS NOT NULL
             AND archivedAt >= :startOfDay AND archivedAt < :endOfDay)
            OR
            (isArchived = 1 AND archivedAt IS NULL
             AND updatedAt >= :startOfDay AND updatedAt < :endOfDay)
            OR
            (isDone = 1 AND isArchived = 0
             AND updatedAt >= :startOfDay AND updatedAt < :endOfDay
             AND updatedAt < :todayStart)
        )
        ORDER BY COALESCE(archivedAt, updatedAt) DESC
        """
    )
    fun getArchivedTasksByDate(
        startOfDay: Long,
        endOfDay:   Long,
        todayStart: Long,
    ): Flow<List<TaskEntity>>

    /**
     * 자정 동기화용 단건 조회 (Flow 아님).
     * 아카이브·삭제되지 않은 전체 할 일을 반환한다.
     */
    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND isArchived = 0")
    suspend fun getNonArchivedTasksOnce(): List<TaskEntity>

    // ──────────────────────────────────────────
    // WRITE
    // ──────────────────────────────────────────

    @Upsert
    suspend fun upsert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query(
        """
        UPDATE tasks
        SET isDone = :isDone,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateDoneStatus(
        id: Long,
        isDone: Boolean,
        updatedAt: Long = System.currentTimeMillis(),
    )

    /** Soft Delete: 휴지통으로 이동 */
    @Query(
        """
        UPDATE tasks
        SET isDeleted = 1,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun softDelete(id: Long, updatedAt: Long = System.currentTimeMillis())

    /** 휴지통 복구: isDeleted → 0 */
    @Query(
        """
        UPDATE tasks
        SET isDeleted = 0,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun restoreFromTrash(id: Long, updatedAt: Long = System.currentTimeMillis())

    /** 휴지통 비우기: isDeleted = 1인 항목 전부 영구 삭제 */
    @Query("DELETE FROM tasks WHERE isDeleted = 1")
    suspend fun emptyTrash()

    @Query("UPDATE tasks SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    /** 영구 삭제 (단건) */
    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM tasks WHERE isDone = 1 AND isDeleted = 0")
    suspend fun deleteAllCompleted()

    /**
     * 할 일을 아카이브로 이동.
     * isArchived = 1, archivedAt = 지정 날짜(epoch ms), updatedAt 갱신.
     */
    @Query(
        """
        UPDATE tasks
        SET isArchived = 1,
            archivedAt = :archivedAt,
            updatedAt  = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun archiveTask(
        id:         Long,
        archivedAt: Long,
        updatedAt:  Long = System.currentTimeMillis(),
    )
}
