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
 * [오늘의 할 일 정의]
 * - 마감일(dueDate)이 오늘 범위 내인 미완료 항목
 * - 마감일이 없고 미완료인 항목 (언제든 해야 하는 일)
 * - 기한 초과(overdue) 항목은 별도 스트림으로 분리 제공
 */
interface TaskRepository {

    /** 전체 할 일 스트림 */
    fun getAllTasks(): Flow<List<TaskEntity>>

    /**
     * 오늘의 할 일 스트림
     * Repository 구현체가 오늘의 시간 범위를 계산하여 DAO에 전달
     */
    fun getTodayTasks(): Flow<List<TaskEntity>>

    /** 기한 초과 할 일 스트림 */
    fun getOverdueTasks(): Flow<List<TaskEntity>>

    /** 잠금화면용 할 일 스트림 (ForegroundService가 구독) */
    fun getLockScreenTasks(): Flow<List<TaskEntity>>

    /** 완료된 할 일 스트림 */
    fun getCompletedTasks(): Flow<List<TaskEntity>>

    /** 단건 조회 */
    fun getTaskById(id: Long): Flow<TaskEntity?>

    /**
     * 할 일 저장 (신규 또는 수정)
     * @return 삽입된 경우 새 id, 수정된 경우 -1
     */
    suspend fun saveTask(task: TaskEntity): Long

    /** 완료 상태 토글 */
    suspend fun toggleDone(id: Long, isDone: Boolean)

    /** 순서 변경 (드래그 앤 드롭 완료 시 호출) */
    suspend fun updateSortOrders(orders: List<Pair<Long, Int>>)

    /** 단건 삭제 */
    suspend fun deleteTask(task: TaskEntity)

    /** ID로 삭제 */
    suspend fun deleteById(id: Long)

    /** 완료 항목 전체 삭제 */
    suspend fun deleteAllCompleted()
}
