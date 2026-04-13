package com.kanghyeon.todolist.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.kanghyeon.todolist.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO: Task CRUD + 특화 쿼리
 *
 * [설계 결정]
 * - @Upsert: insert + update를 하나로 처리 (Room 2.5.0+)
 *   → id=0이면 INSERT, id가 존재하면 REPLACE
 * - 반환 타입을 Flow<>로 선언 → DB 변경 시 자동 재방출 (observe 패턴)
 * - 단건 조회는 Flow<TaskEntity?> → null-safe하게 존재 여부 확인 가능
 * - suspend fun으로 선언된 쓰기 작업은 Repository에서 withContext(IO)로 호출
 */
@Dao
interface TaskDao {

    // ──────────────────────────────────────────
    // READ
    // ──────────────────────────────────────────

    /**
     * 잠금화면 노출 대상
     * showOnLockScreen = true 이고 미완료인 항목 (최대 표시 개수는 Service에서 take)
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE showOnLockScreen = 1 AND isDone = 0
        ORDER BY priority DESC, dueDate ASC, sortOrder ASC
        """
    )
    fun getLockScreenTasks(): Flow<List<TaskEntity>>

    /** 단건 조회 (편집 화면 진입 시 사용) */
    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskById(id: Long): Flow<TaskEntity?>

    /** 완료된 할 일만 조회 — 전체 (아카이브 탭 배지 카운트용) */
    @Query(
        """
        SELECT * FROM tasks
        WHERE isDone = 1
        ORDER BY updatedAt DESC
        """
    )
    fun getCompletedTasks(): Flow<List<TaskEntity>>

    /**
     * 특정 날짜에 완료된 할 일 조회 — 아카이브 Day Selector에서 사용.
     *
     * 완료 시각 기준: [updateDoneStatus]에서 updatedAt을 갱신하므로
     * updatedAt이 해당 날짜 범위에 속하면 "이 날 완료된 항목"으로 취급한다.
     *
     * 정렬: updatedAt DESC (최근 완료 순)
     *
     * :startOfDay = 00:00:00.000 epoch ms
     * :endOfDay   = 23:59:59.999 epoch ms
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE isDone = 1
          AND updatedAt >= :startOfDay
          AND updatedAt <= :endOfDay
        ORDER BY updatedAt DESC
        """
    )
    fun getCompletedTasksByDate(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>>

    /**
     * 특정 날짜에 완료된 항목 일괄 삭제 — 아카이브 탭 삭제 버튼에서 사용.
     */
    @Query(
        """
        DELETE FROM tasks
        WHERE isDone = 1
          AND updatedAt >= :startOfDay
          AND updatedAt <= :endOfDay
        """
    )
    suspend fun deleteCompletedByDateRange(startOfDay: Long, endOfDay: Long)

    /**
     * 전체 활성(미완료) 할 일 — '할 일' 탭 메인 쿼리.
     *
     * 조건: isDone = 0 (날짜 필터 없음)
     * 정렬: priority DESC (HIGH → MEDIUM → LOW)
     *       → 동일 priority 내에서 createdAt DESC (최근 생성 순)
     *
     * UI에서 groupBy(Priority) 후 LazyColumn stickyHeader 섹션으로 표시.
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE isDone = 0
        ORDER BY priority DESC, createdAt DESC
        """
    )
    fun getActiveTasks(): Flow<List<TaskEntity>>

    // ──────────────────────────────────────────
    // WRITE
    // ──────────────────────────────────────────

    /**
     * 삽입 / 수정 통합 (id=0 → INSERT, id 존재 → REPLACE)
     * @return 삽입된 row의 id (수정 시에는 -1 반환)
     */
    @Upsert
    suspend fun upsert(task: TaskEntity): Long

    /** 기존 할 일 수정 (id가 없으면 무시) */
    @Update
    suspend fun update(task: TaskEntity)

    /**
     * 완료 상태만 업데이트
     * updatedAt을 함께 갱신해 변경 이력 추적
     */
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

    /**
     * 드래그 앤 드롭 후 여러 항목의 순서를 한 번에 갱신
     * ViewModel에서 List<Pair<id, sortOrder>>를 전달
     */
    @Query("UPDATE tasks SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    /** 단건 삭제 */
    @Delete
    suspend fun delete(task: TaskEntity)

    /** ID로 삭제 (Entity 객체 없이 호출 가능) */
    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** 완료된 할 일 일괄 삭제 */
    @Query("DELETE FROM tasks WHERE isDone = 1")
    suspend fun deleteAllCompleted()
}
