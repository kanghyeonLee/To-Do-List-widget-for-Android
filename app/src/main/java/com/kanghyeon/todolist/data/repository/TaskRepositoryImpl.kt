package com.kanghyeon.todolist.data.repository

import com.kanghyeon.todolist.data.local.dao.TaskDao
import com.kanghyeon.todolist.data.local.entity.TaskEntity
import com.kanghyeon.todolist.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TaskRepository 구현체
 *
 * [설계 결정]
 * 1. dispatcher 주입
 *    - 생성자로 CoroutineDispatcher를 받아 테스트 시 TestDispatcher로 교체 가능
 *    - Flow 쿼리는 .flowOn(dispatcher)로, suspend 쓰기 작업은 withContext(dispatcher)로 격리
 *
 * 2. updateSortOrders()
 *    - 여러 행을 개별 UPDATE로 처리. 항목 수가 많다면 @Transaction으로 묶어
 *      단일 트랜잭션으로 성능을 높일 수 있다.
 *
 * [Hilt 사용 시]
 * AppModule에서 TaskRepository → TaskRepositoryImpl 바인딩 등록 필요:
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

    override fun getActiveTasks(): Flow<List<TaskEntity>> =
        dao.getActiveTasks()
            .flowOn(dispatcher)

    override fun getLockScreenTasks(): Flow<List<TaskEntity>> =
        dao.getLockScreenTasks()
            .flowOn(dispatcher)

    override fun getCompletedTasks(): Flow<List<TaskEntity>> =
        dao.getCompletedTasks()
            .flowOn(dispatcher)

    override fun getCompletedTasksByDate(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>> =
        dao.getCompletedTasksByDate(startOfDay = startOfDay, endOfDay = endOfDay)
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

    override suspend fun updateTask(task: TaskEntity) =
        withContext(dispatcher) {
            dao.update(task)
        }

    override suspend fun toggleDone(id: Long, isDone: Boolean) =
        withContext(dispatcher) {
            dao.updateDoneStatus(
                id        = id,
                isDone    = isDone,
                updatedAt = System.currentTimeMillis(),
            )
        }

    override suspend fun updateSortOrders(orders: List<Pair<Long, Int>>) =
        withContext(dispatcher) {
            orders.forEach { (id, sortOrder) ->
                dao.updateSortOrder(id = id, sortOrder = sortOrder)
            }
        }

    override suspend fun softDeleteTask(id: Long) =
        withContext(dispatcher) { dao.softDelete(id) }

    override suspend fun restoreFromTrash(id: Long) =
        withContext(dispatcher) { dao.restoreFromTrash(id) }

    override suspend fun emptyTrash() =
        withContext(dispatcher) { dao.emptyTrash() }

    override fun getDeletedTasks(): Flow<List<TaskEntity>> =
        dao.getDeletedTasks().flowOn(dispatcher)

    override suspend fun deleteTask(task: TaskEntity) =
        withContext(dispatcher) { dao.delete(task) }

    override suspend fun deleteById(id: Long) =
        withContext(dispatcher) { dao.deleteById(id) }

    override suspend fun deleteAllCompleted() =
        withContext(dispatcher) { dao.deleteAllCompleted() }

    override suspend fun deleteCompletedByDate(startOfDay: Long, endOfDay: Long) =
        withContext(dispatcher) {
            dao.deleteCompletedByDateRange(startOfDay = startOfDay, endOfDay = endOfDay)
        }

    override fun getDDayTasks(): Flow<List<TaskEntity>> =
        dao.getDDayTasks().flowOn(dispatcher)

    override fun getArchivedTasksByDate(
        startOfDay: Long,
        endOfDay:   Long,
        todayStart: Long,
    ): Flow<List<TaskEntity>> =
        dao.getArchivedTasksByDate(
            startOfDay = startOfDay,
            endOfDay   = endOfDay,
            todayStart = todayStart,
        ).flowOn(dispatcher)

    override suspend fun getNonArchivedTasksOnce(): List<TaskEntity> =
        withContext(dispatcher) { dao.getNonArchivedTasksOnce() }

    override suspend fun archiveTask(id: Long, archivedAt: Long) =
        withContext(dispatcher) {
            dao.archiveTask(
                id         = id,
                archivedAt = archivedAt,
                updatedAt  = System.currentTimeMillis(),
            )
        }
}
