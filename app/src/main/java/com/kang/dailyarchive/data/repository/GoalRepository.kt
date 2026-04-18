package com.kang.dailyarchive.data.repository

import com.kang.dailyarchive.data.local.entity.GoalEntity
import com.kang.dailyarchive.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * Goal Repository 인터페이스
 *
 * [역할]
 * - ViewModel이 데이터 출처(Room)를 직접 알지 못하도록 추상화
 * - 테스트 시 FakeGoalRepository로 교체 가능
 *
 * [목표 진행률 데이터 흐름]
 * - ViewModel은 getAllGoals()와 countCompleted*() Flow를 combine하여
 *   GoalWithProgress 상태를 계산한다.
 */
interface GoalRepository {

    // ── READ ─────────────────────────────────────────────

    /** 전체 목표 목록 스트림 — 최신 생성 순 */
    fun getAllGoals(): Flow<List<GoalEntity>>

    /** 단건 스트림 */
    fun getGoalById(id: Long): Flow<GoalEntity?>

    /**
     * 기간 내 완료 Task 수 — COUNT / FREQ 진행률 분자.
     * @param fromMs 목표 시작일 epoch ms
     * @param toMs   목표 종료일 epoch ms (당일 말미 포함)
     */
    fun countCompletedInRange(goalId: Long, fromMs: Long, toMs: Long): Flow<Int>

    /** 완료 Task 수 (아카이브 제외) — PROJECT 진행률 분자 */
    fun countCompletedTasks(goalId: Long): Flow<Int>

    /** 전체 연결 Task 수 — PROJECT 진행률 분모 */
    fun countTotalTasks(goalId: Long): Flow<Int>

    /** 목표에 연결된 활성 Task 목록 (GoalDetail 화면) */
    fun getActiveTasksByGoalId(goalId: Long): Flow<List<TaskEntity>>

    // ── WRITE ────────────────────────────────────────────

    /** 목표 저장 (신규 삽입 또는 갱신) */
    suspend fun saveGoal(goal: GoalEntity): Long

    /**
     * 목표 삭제.
     * 연결된 Task의 goalId를 null로 해제한 뒤 삭제한다.
     */
    suspend fun deleteGoal(id: Long)
}
