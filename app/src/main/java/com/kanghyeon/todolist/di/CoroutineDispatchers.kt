package com.kanghyeon.todolist.di

import javax.inject.Qualifier

/**
 * Coroutine Dispatcher Qualifier 어노테이션
 *
 * Hilt는 같은 타입(CoroutineDispatcher)이 여러 개 존재할 때
 * Qualifier 없이는 어느 것을 주입할지 알 수 없다.
 * @IoDispatcher / @MainDispatcher / @DefaultDispatcher 로 구분한다.
 *
 * [사용법]
 * - 주입 받는 곳: @IoDispatcher dispatcher: CoroutineDispatcher
 * - 테스트: @IoDispatcher 바인딩을 TestDispatcher로 교체하면
 *   전체 IO 작업을 단번에 제어 가능
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
