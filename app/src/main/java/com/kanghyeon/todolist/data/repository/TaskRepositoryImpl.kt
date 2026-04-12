package com.kanghyeon.todolist.data.repository

import com.kanghyeon.todolist.data.local.dao.TaskDao
import com.kanghyeon.todolist.data.local.entity.TaskEntity
import com.kanghyeon.todolist.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TaskRepository 구현체
 *
 * [설계 결정]
 *
 * 1. dispatcher 주입
 *    - 생성자로 CoroutineDispatcher를 받아 테스트 시 TestDispatcher로 교체 가능
 *    - Flow 쿼리는 .flowOn(dispatcher)로, suspend 쓰기 작업은 withContext(dispatcher)로 격리
 *
 * 2. getTodayTasks()의 시간 범위 계산
 *    - Calendar.getInstance()로 로컬 타임존 기준 자정을 계산
 *    - Flow는 구독 시점에 한 번 범위를 계산한다.
 *      → 자정이 지나면 ViewModel을 재구독하거나 앱을 재시작해야 갱신됨.
 *      (더 정교한 처리가 필요하면 ViewModel에서 날짜 변경 감지 후 재구독)
 *
 * 3. updateSortOrders()
 *    - 여러 행을 개별 UPDATE로 처리. 항목 수가 많다면 @Transaction으로 묶어
 *      단일 트랜잭션으로 성능을 높일 수 있다.
 *
 * [Hilt 사용 시]
 * @Singleton + @Inject constructor로 Hilt가 자동 주입.
 * AppModule에서 TaskRepository 인터페이스 → TaskRepositoryImpl 바인딩 등록 필요:
 *   @Binds fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository
 */
@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val dao: TaskDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : TaskRepository {

    // ──────────────────────────────────────────
    // READ
    // ──────────────────────────────────────────

    override fun getAllTasks(): Flow<List<TaskEntity>> =
        dao.getAllTasks()
            .flowOn(dispatcher)

    override fun getTodayTasks(): Flow<List<TaskEntity>> {
        val (start, end) = todayRange()
        return dao.getTodayTasks(startOfDay = start, endOfDay = end)
            .flowOn(dispatcher)
    }

    override fun getOverdueTasks(): Flow<List<TaskEntity>> {
        val (startOfToday, _) = todayRange()
        return dao.getOverdueTasks(startOfToday = startOfToday)
            .flowOn(dispatcher)
    }

    override fun getLockScreenTasks(): Flow<List<TaskEntity>> =
        dao.getLockScreenTasks()
            .flowOn(dispatcher)

    override fun getCompletedTasks(): Flow<List<TaskEntity>> =
        dao.getCompletedTasks()
            .flowOn(dispatcher)

    override fun getTaskById(id: Long): Flow<TaskEntity?> =
        dao.getTaskById(id)
            .flowOn(dispatcher)

    // ──────────────────────────────────────────
    // WRITE
    // ──────────────────────────────────────────

    override suspend fun saveTask(task: TaskEntity): Long =
        withContext(dispatcher) {
            dao.upsert(task)
        }

    override suspend fun toggleDone(id: Long, isDone: Boolean) =
        withContext(dispatcher) {
            dao.updateDoneStatus(
                id = id,
                isDone = isDone,
                updatedAt = System.currentTimeMillis(),
            )
        }

    override suspend fun updateSortOrders(orders: List<Pair<Long, Int>>) =
        withContext(dispatcher) {
            orders.forEach { (id, sortOrder) ->
                dao.updateSortOrder(id = id, sortOrder = sortOrder)
            }
        }

    override suspend fun deleteTask(task: TaskEntity) =
        withContext(dispatcher) {
            dao.delete(task)
        }

    override suspend fun deleteById(id: Long) =
        withContext(dispatcher) {
            dao.deleteById(id)
        }

    override suspend fun deleteAllCompleted() =
        withContext(dispatcher) {
            dao.deleteAllCompleted()
        }

    // ──────────────────────────────────────────
    // 내부 헬퍼
    // ──────────────────────────────────────────

    /**
     * 오늘의 시작(00:00:00.000)과 끝(23:59:59.999) epoch ms를 반환
     * 로컬 타임존 기준으로 계산
     */
    private fun todayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        val endOfDay = startOfDay + DAY_MILLIS - 1
        return startOfDay to endOfDay
    }

    companion object {
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
