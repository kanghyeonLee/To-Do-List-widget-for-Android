package com.kang.dailyarchive.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity: 목표(Goal) 테이블
 *
 * [GoalType 별 진행률 계산 방식]
 * - COUNT   : goalId가 일치하는 완료 Task 수 / targetValue
 * - FREQ    : 현재 주(또는 기간 내) 완료 횟수 / targetValue (주 N회)
 * - PROJECT : goalId가 일치하는 완료 Task 수 / 전체 연결 Task 수
 *
 * [페이스 메이커]
 * - 기간 내 경과 비율 vs 달성률을 비교해 SAFE / NORMAL / WARNING 상태 반환
 * - 남은 기간 대비 권장 일일 할당량 산출
 *
 * @param colorHex   목표 카드 강조색 (#RRGGBB 형식, 기본값 인디고)
 * @param targetValue COUNT / FREQ 타입에서 목표치. PROJECT 타입은 연결 Task 수가 기준.
 */
@Entity(tableName = "goals")
data class GoalEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 목표 제목 */
    val title: String,

    /** 목표 유형 (TypeConverter로 String 변환 저장) */
    val type: GoalType,

    /**
     * 목표 수치
     * - COUNT  : 완료해야 할 Task 총 개수
     * - FREQ   : 주당 달성해야 할 횟수
     * - PROJECT: 직접 사용하지 않음 (연결 Task 수가 기준)
     */
    val targetValue: Int,

    /** 목표 시작일 (epoch ms, 로컬 자정) */
    val startDate: Long,

    /** 목표 종료일 (epoch ms, 로컬 자정) */
    val endDate: Long,

    /** 카드 강조색 — #RRGGBB 형식 (기본: 인디고 #4F46E5) */
    val colorHex: String = "#4F46E5",

    /** 생성 시각 (epoch ms) */
    val createdAt: Long = System.currentTimeMillis(),
)

// ─────────────────────────────────────────────────────────────
// 목표 유형 열거형
// ─────────────────────────────────────────────────────────────

/**
 * GoalType: 목표 달성 방식을 결정하는 열거형.
 *
 * [COUNT]
 *   기간 내 지정된 총 개수의 Task를 완료하는 목표.
 *   예) "이번 달 운동 30회 달성"
 *
 * [FREQ]
 *   정해진 주당 횟수를 반복하는 습관형 목표.
 *   예) "매주 3회 독서"
 *   진행률 = (전체 기간 주 수 × targetValue) 대비 현재 완료 수
 *
 * [PROJECT]
 *   하위 Task 목록을 기반으로 진행률을 계산하는 프로젝트형 목표.
 *   완료된 하위 Task 수 / 전체 하위 Task 수 × 100
 */
enum class GoalType {
    COUNT,
    FREQ,
    PROJECT,
}

// ─────────────────────────────────────────────────────────────
// 페이스 메이커 상태
// ─────────────────────────────────────────────────────────────

/**
 * 현재 달성률과 기간 경과율을 비교해 도출한 진행 상태.
 *
 * [기준]
 * - paceRatio = actualProgress / expectedProgress
 * - SAFE    : paceRatio >= 1.0   (달성률 ≥ 기대치)
 * - NORMAL  : paceRatio >= 0.75  (달성률이 기대치의 75% 이상)
 * - WARNING : paceRatio < 0.75   (달성률이 기대치의 75% 미만)
 *
 * 목표 시작 전이거나 완료된 경우 별도 처리.
 */
enum class PaceStatus {
    SAFE,       // 여유  — 녹색
    NORMAL,     // 정상  — 노란색
    WARNING,    // 위험  — 빨간색
    COMPLETED,  // 완료  — 회색
    NOT_STARTED // 미시작 — 파란색
}
