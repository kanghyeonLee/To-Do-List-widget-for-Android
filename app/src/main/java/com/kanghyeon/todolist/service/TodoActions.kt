package com.kanghyeon.todolist.service

/**
 * Service ↔ BroadcastReceiver 간 공유 Intent 액션 / Extra 상수
 *
 * 패키지명을 접두어로 사용해 다른 앱의 암시적 인텐트와 충돌 방지.
 */
object TodoActions {
    private const val BASE = "com.kanghyeon.todolist"

    /** 서비스 시작 (BootReceiver → Service) */
    const val ACTION_START  = "$BASE.ACTION_START"

    /** 서비스 종료 (알림 삭제 버튼 → Service) */
    const val ACTION_STOP   = "$BASE.ACTION_STOP"

    /** 할 일 완료 처리 (알림 체크 버튼 → TaskActionReceiver) */
    const val ACTION_MARK_DONE = "$BASE.ACTION_MARK_DONE"

    /** 할 일 삭제 (알림 삭제 버튼 → TaskActionReceiver) */
    const val ACTION_DELETE = "$BASE.ACTION_DELETE"

    /** Intent extra: 대상 Task의 id (Long) */
    const val EXTRA_TASK_ID = "$BASE.EXTRA_TASK_ID"
}
