package com.kang.dailyarchive.data.repository

import com.kang.dailyarchive.data.local.dao.GoalDao
import com.kang.dailyarchive.data.local.dao.TaskDao
import com.kang.dailyarchive.data.local.entity.GoalEntity
import com.kang.dailyarchive.data.local.entity.TaskEntity
import com.kang.dailyarchive.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao,
    private val taskDao: TaskDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : GoalRepository {

    override fun getAllGoals(): Flow<List<GoalEntity>> =
        goalDao.getAllGoals().flowOn(dispatcher)

    override fun getGoalById(id: Long): Flow<GoalEntity?> =
        goalDao.getGoalById(id).flowOn(dispatcher)

    override fun countCompletedInRange(goalId: Long, fromMs: Long, toMs: Long): Flow<Int> =
        goalDao.countCompletedInRange(goalId, fromMs, toMs).flowOn(dispatcher)

    override fun countCompletedTasks(goalId: Long): Flow<Int> =
        goalDao.countCompletedTasks(goalId).flowOn(dispatcher)

    override fun countTotalTasks(goalId: Long): Flow<Int> =
        goalDao.countTotalTasks(goalId).flowOn(dispatcher)

    override fun getActiveTasksByGoalId(goalId: Long): Flow<List<TaskEntity>> =
        taskDao.getActiveTasksByGoalId(goalId).flowOn(dispatcher)

    override suspend fun saveGoal(goal: GoalEntity): Long =
        withContext(dispatcher) { goalDao.upsert(goal) }

    override suspend fun deleteGoal(id: Long) =
        withContext(dispatcher) {
            taskDao.unlinkGoalFromTasks(id)   // 연결 Task goalId → null 처리
            goalDao.deleteById(id)
        }
}
