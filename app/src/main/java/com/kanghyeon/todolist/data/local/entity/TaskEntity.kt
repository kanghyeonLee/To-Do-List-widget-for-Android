package com.kanghyeon.todolist.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity: 할 일(Task) 테이블
 *
 * [설계 결정]
 * - dueDate를 epoch ms(Long?)로 저장 → SQL 범위 쿼리가 간단해짐
 * - priority를 enum 대신 Int로 저장 → TypeConverter 없이 직접 정렬 가능
 * - sortOrder로 사용자 정의 순서(드래그) 지원
 * - showOnLockScreen 플래그로 잠금화면 노출 여부를 개별 제어
 * - Index(isDone, dueDate) → getLockScreenTasks / getCompletedTasksByDate 쿼리 최적화
 */
@Entity(
    tableName = "tasks",
    indices = [Index(value = ["isDone", "dueDate"])]
)
data class TaskEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 할 일 제목 (필수) */
    val title: String,

    /** 선택적 상세 메모 */
    val description: String? = null,

    /** 완료 여부 */
    val isDone: Boolean = false,

    /** Soft Delete: true면 휴지통으로 이동된 상태 */
    val isDeleted: Boolean = false,

    /**
     * 우선순위
     * [Priority.kt] 참고: LOW=0, MEDIUM=1, HIGH=2
     * Int로 저장해 ORDER BY priority DESC 정렬을 DB 레벨에서 처리
     */
    val priority: Int = Priority.MEDIUM.value,

    /** 잠금화면 알림에 노출할지 여부 */
    val showOnLockScreen: Boolean = true,

    /**
     * 마감일 (epoch milliseconds, null = 기한 없음)
     * 오늘의 할 일 쿼리: startOfDay <= dueDate <= endOfDay
     */
    val dueDate: Long? = null,

    /** 아카이브 동기화 여부 */
    val isArchived: Boolean = false,

    /** 생성 시각 (epoch ms) */
    val createdAt: Long = System.currentTimeMillis(),

    /** 마지막 수정 시각 (epoch ms) — isDone 변경 시에도 갱신 */
    val updatedAt: Long = System.currentTimeMillis(),

    /** 사용자 정의 정렬 순서 (드래그 앤 드롭) */
    val sortOrder: Int = 0,

    /**
     * 반복 유형 (Converters.kt에서 String ↔ RepeatType 변환)
     * NONE / DAILY / WEEKLY / MONTHLY
     */
    val repeatType: RepeatType = RepeatType.NONE,

    /**
     * 사전 알림 시간 (분 단위, null = 알림 없음)
     * 예: 10 → 마감 10분 전 알림. dueDate가 null이면 무시됨.
     */
    val reminderMinutes: Int? = null,

    /**
     * 아카이브 날짜 (epoch ms, null = 아직 아카이브 미등록)
     * 자정 동기화 시 조건에 따라 설정:
     *   - 일반 할 일          → 어제 자정 (todayStart - DAY_MS)
     *   - 완료된 D-Day 할 일  → 완료 날짜 자정 (updatedAt 의 start-of-day)
     *   - 기한 초과 D-Day     → dueDate 날짜 자정 (dueDate 의 start-of-day)
     * 아카이브 탭 날짜 필터링의 기준 컬럼으로 사용됨.
     */
    val archivedAt: Long? = null,
)

// ───────────────────────────────────────────────
// 동반 열거형 — Entity 파일 내에 두어 응집도 유지
// ───────────────────────────────────────────────

enum class Priority(val value: Int) {
    LOW(0), MEDIUM(1), HIGH(2);

    companion object {
        fun from(value: Int) = entries.firstOrNull { it.value == value } ?: MEDIUM
    }
}

enum class RepeatType {
    NONE, DAILY, WEEKLY, MONTHLY
}
