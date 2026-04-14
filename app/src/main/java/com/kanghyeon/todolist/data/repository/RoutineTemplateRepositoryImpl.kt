package com.kanghyeon.todolist.data.repository

import com.kanghyeon.todolist.data.local.dao.RoutineTemplateDao
import com.kanghyeon.todolist.data.local.entity.RoutineTemplateGroupEntity
import com.kanghyeon.todolist.data.local.entity.RoutineTemplateGroupWithTasks
import com.kanghyeon.todolist.data.local.entity.RoutineTemplateTaskEntity
import com.kanghyeon.todolist.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutineTemplateRepositoryImpl @Inject constructor(
    private val dao: RoutineTemplateDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : RoutineTemplateRepository {

    override fun getAllGroupsWithTasks(): Flow<List<RoutineTemplateGroupWithTasks>> =
        dao.getAllGroupsWithTasks().flowOn(dispatcher)

    override suspend fun getActiveGroupsWithTasksOnce(): List<RoutineTemplateGroupWithTasks> =
        withContext(dispatcher) { dao.getActiveGroupsWithTasksOnce() }

    override suspend fun addGroup(name: String): Long =
        withContext(dispatcher) {
            dao.insertGroup(RoutineTemplateGroupEntity(name = name))
        }

    override suspend fun updateGroupActiveState(id: Long, isActive: Boolean) =
        withContext(dispatcher) { dao.updateGroupActiveState(id, isActive) }

    override suspend fun updateGroupName(id: Long, name: String) =
        withContext(dispatcher) { dao.updateGroupName(id, name) }

    override suspend fun deleteGroup(id: Long) =
        withContext(dispatcher) { dao.deleteGroupById(id) }

    override suspend fun addTask(task: RoutineTemplateTaskEntity): Long =
        withContext(dispatcher) { dao.insertTask(task) }

    override suspend fun deleteTask(id: Long) =
        withContext(dispatcher) { dao.deleteTaskById(id) }
}
