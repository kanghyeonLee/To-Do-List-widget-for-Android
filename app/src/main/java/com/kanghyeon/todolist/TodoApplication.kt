package com.kanghyeon.todolist

import android.app.Application
import com.kanghyeon.todolist.service.NotificationHelper
import dagger.hilt.android.HiltAndroidApp

/**
 * Hilt 진입점 Application 클래스
 *
 * @HiltAndroidApp: Hilt 코드 생성 트리거.
 * AndroidManifest.xml의 android:name=".TodoApplication"에 등록 필요.
 *
 * 앱 시작 시점에 알림 채널을 생성한다.
 * (채널은 이미 존재하면 무시되므로 매번 호출해도 안전)
 */
@HiltAndroidApp
class TodoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }
}
