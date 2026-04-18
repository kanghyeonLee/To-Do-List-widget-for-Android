package com.kang.dailyarchive.data.local.converter

import androidx.room.TypeConverter
import com.kang.dailyarchive.data.local.entity.GoalType
import com.kang.dailyarchive.data.local.entity.RepeatType

/**
 * Room TypeConverter
 *
 * Enum ↔ String 변환 (Room은 enum을 직접 저장할 수 없음)
 * AppDatabase에 @TypeConverters(Converters::class)로 일괄 등록한다.
 *
 * [등록된 TypeConverter]
 * - RepeatType ↔ String
 * - GoalType   ↔ String
 */
class Converters {

    // ── RepeatType ────────────────────────────────────────
    @TypeConverter
    fun fromRepeatType(type: RepeatType): String = type.name

    @TypeConverter
    fun toRepeatType(value: String): RepeatType =
        runCatching { RepeatType.valueOf(value) }.getOrDefault(RepeatType.NONE)

    // ── GoalType ──────────────────────────────────────────
    @TypeConverter
    fun fromGoalType(type: GoalType): String = type.name

    @TypeConverter
    fun toGoalType(value: String): GoalType =
        runCatching { GoalType.valueOf(value) }.getOrDefault(GoalType.COUNT)
}
