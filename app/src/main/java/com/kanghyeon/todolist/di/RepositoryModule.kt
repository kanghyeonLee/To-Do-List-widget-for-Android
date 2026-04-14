package com.kanghyeon.todolist.di

import com.kanghyeon.todolist.data.repository.RoutineTemplateRepository
import com.kanghyeon.todolist.data.repository.RoutineTemplateRepositoryImpl
import com.kanghyeon.todolist.data.repository.TaskRepository
import com.kanghyeon.todolist.data.repository.TaskRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository 바인딩 모듈
 *
 * @Binds: 인터페이스 → 구현체 매핑만 수행 (object 대신 abstract class 필수)
 * @Provides와 달리 코드 생성이 최소화되어 빌드 속도가 빠르다.
 *
 * TaskRepository(인터페이스)를 요청하면 TaskRepositoryImpl(구현체)을 주입한다.
 * ViewModel은 TaskRepository 인터페이스만 알면 되므로
 * 구현체 교체(FakeRepository 등)가 쉬워진다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        impl: TaskRepositoryImpl,
    ): TaskRepository

    @Binds
    @Singleton
    abstract fun bindRoutineTemplateRepository(
        impl: RoutineTemplateRepositoryImpl,
    ): RoutineTemplateRepository
}
