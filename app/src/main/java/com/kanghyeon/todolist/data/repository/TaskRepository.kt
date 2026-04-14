package com.kanghyeon.todolist.data.repository

import com.kanghyeon.todolist.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * Task Repository 인터페이스
 *
 * [역할]
 * - ViewModel이 데이터 출처(Room, Network 등)를 알 필요 없도록 추상화
 * - 테스트 시 FakeTaskRepository로 교체 가능
 *
 * [탭 구조와 데이터 흐름]
 * - '할 일' 탭  : getActiveTasks()  — 미완료 전체, priority + createdAt 정렬
 * - '아카이브' 탭: getCompletedTasksByDate() — 날짜별 완료 항목
 * - 잠금화면   : getLockScreenTasks() — ForegroundService가 구독
 */
interface TaskRepository {

    /**
     * 전체 활성(미완료) 할 일 스트림 — '할 일' 탭 전용.
     * priority DESC, createdAt DESC 정렬.
     */
    fun getActiveTasks(): Flow<List<TaskEntity>>

    /** 잠금화면용 할 일 스트림 (ForegroundService가 구독) */
    fun getLockScreenTasks(): Flow<List<TaskEntity>>

    /** 완료된 할 일 스트림 — 전체 (아카이브 탭 배지 카운트용) */
    fun getCompletedTasks(): Flow<List<TaskEntity>>

    /**
     * 특정 날짜 범위에 완료된 할 일 스트림 — 아카이브 Day Selector 필터링.
     * updatedAt 기준: isDone 전환 시 갱신되므로 완료 시각으로 취급.
     */
    fun getCompletedTasksByDate(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>>

    /** 단건 조회 */
    fun getTaskById(id: Long): Flow<TaskEntity?>

    /**
     * 할 일 저장 (신규 또는 수정)
     * @return 삽입된 경우 새 id, 수정된 경우 -1
     */
    suspend fun saveTask(task: TaskEntity): Long

    /** 기존 할 일 수정 (id가 일치하는 행만 업데이트) */
    suspend fun updateTask(task: TaskEntity)

    /** 완료 상태 토글 */
    suspend fun toggleDone(id: Long, isDone: Boolean)

    /** 순서 변경 (드래그 앤 드롭 완료 시 호출) */
    suspend fun updateSortOrders(orders: List<Pair<Long, Int>>)

    /** Soft Delete: 휴지통으로 이동 */
    suspend fun softDeleteTask(id: Long)

    /** 휴지통에서 복구 */
    suspend fun restoreFromTrash(id: Long)

    /** 휴지통 비우기 (isDeleted = 1 전부 영구 삭제) */
    suspend fun emptyTrash()

    /** 단건 영구 삭제 */
    suspend fun deleteTask(task: TaskEntity)

    /** ID로 영구 삭제 */
    suspend fun deleteById(id: Long)

    /** 완료 항목 전체 삭제 */
    suspend fun deleteAllCompleted()

    /** 특정 날짜 범위에 완료된 항목 삭제 */
    suspend fun deleteCompletedByDate(startOfDay: Long, endOfDay: Long)

    /** 휴지통 목록 스트림 */
    fun getDeletedTasks(): Flow<List<TaskEntity>>

    /**
     * D-Day 탭: dueDate가 있고 아카이브되지 않은 할 일 스트림.
     * 마감일 오름차순(임박 순) 정렬.
     */
    fun getDDayTasks(): Flow<List<TaskEntity>>

    /**
     * 아카이브 탭 날짜 필터 스트림.
     * 신규 archivedAt 기반 + 구버전 updatedAt 기반 항목 통합 반환.
     *
     * @param todayStart 오늘 00:00:00 epoch ms — 오늘 완료 항목의 중복 노출 방지에 사용
     */
    fun getArchivedTasksByDate(
        startOfDay: Long,
        endOfDay:   Long,
        todayStart: Long,
    ): Flow<List<TaskEntity>>

    /** 자정 동기화용: 아카이브되지 않은 전체 활성 할 일 단건 조회 */
    suspend fun getNonArchivedTasksOnce(): List<TaskEntity>

    /** 특정 할 일을 아카이브로 이동 (isArchived=1, archivedAt 설정) */
    suspend fun archiveTask(id: Long, archivedAt: Long)
}
