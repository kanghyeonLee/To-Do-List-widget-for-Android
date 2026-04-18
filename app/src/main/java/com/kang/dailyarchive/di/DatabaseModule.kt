package com.kang.dailyarchive.di

import android.content.Context
import com.kang.dailyarchive.data.local.AppDatabase
import com.kang.dailyarchive.data.local.dao.GoalDao
import com.kang.dailyarchive.data.local.dao.RoutineTemplateDao
import com.kang.dailyarchive.data.local.dao.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Room Database / DAO 제공 모듈
 *
 * @InstallIn(SingletonComponent) → 앱 생명주기 동안 단 하나의 인스턴스만 존재
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = AppDatabase.getInstance(context)

    /**
     * AppDatabase가 Singleton이므로 taskDao()도 항상 동일 인스턴스 반환.
     * 별도의 @Singleton 불필요.
     */
    @Provides
    fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideGoalDao(database: AppDatabase): GoalDao = database.goalDao()

    @Provides
    fun provideRoutineTemplateDao(database: AppDatabase): RoutineTemplateDao =
        database.routineTemplateDao()
}
