package com.kang.dailyarchive.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kang.dailyarchive.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

/**
 * GoalDao — goals 테이블 CRUD + 진행률 계산용 집계 쿼리
 *
 * [설계 결정]
 * - 진행률에 필요한 Task 집계는 GoalDao에서 직접 처리.
 *   (JOIN 없이 goalId 인덱스만 사용 → 단순·성능 균형)
 * - Flow 반환: UI 계층이 Room LiveData처럼 자동 갱신 구독 가능.
 * - suspend 쓰기: ViewModel에서 withContext(IO) 없이 직접 호출 가능.
 */
@Dao
interface GoalDao {

    // ──────────────────────────────────────────
    // READ — goals 테이블
    // ──────────────────────────────────────────

    /** 전체 목표 목록 — 최신 생성 순 */
    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    /** 단건 조회 */
    @Query("SELECT * FROM goals WHERE id = :id")
    fun getGoalById(id: Long): Flow<GoalEntity?>

    // ──────────────────────────────────────────
    // READ — tasks 집계 (진행률 계산용)
    // ──────────────────────────────────────────

    /**
     * 특정 목표에 연결된 완료 Task 수 (COUNT / PROJECT 진행률 분자).
     * isDeleted=0 조건으로 휴지통 항목은 제외한다.
     */
    @Query(
        """
        SELECT COUNT(*) FROM tasks
        WHERE goalId = :goalId
          AND isDone  = 1
          AND isDeleted = 0
          AND isArchived = 0
        """
    )
    fun countCompletedTasks(goalId: Long): Flow<Int>

    /**
     * 특정 목표에 연결된 전체 Task 수 (PROJECT 진행률 분모).
     * 완료 여부 무관, 휴지통 제외.
     */
    @Query(
        """
        SELECT COUNT(*) FROM tasks
        WHERE goalId = :goalId
          AND isDeleted = 0
          AND isArchived = 0
        """
    )
    fun countTotalTasks(goalId: Long): Flow<Int>

    /**
     * COUNT / FREQ 타입 전체 완료 수 (아카이브 포함).
     * 기간 내에 완료·아카이브된 항목까지 집계해야 정확한 누적값을 얻는다.
     *
     * @param goalId   목표 ID
     * @param fromMs   목표 시작일 epoch ms
     * @param toMs     목표 종료일 epoch ms (당일 23:59:59 포함)
     */
    @Query(
        """
        SELECT COUNT(*) FROM tasks
        WHERE goalId   = :goalId
          AND isDone   = 1
          AND isDeleted = 0
          AND updatedAt >= :fromMs
          AND updatedAt <= :toMs
        """
    )
    fun countCompletedInRange(goalId: Long, fromMs: Long, toMs: Long): Flow<Int>

    // ──────────────────────────────────────────
    // WRITE
    // ──────────────────────────────────────────

    /** 삽입 또는 갱신 (id=0 이면 신규 삽입, id≠0 이면 교체) */
    @Upsert
    suspend fun upsert(goal: GoalEntity): Long

    /** 단건 삭제 */
    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: Long)
}
