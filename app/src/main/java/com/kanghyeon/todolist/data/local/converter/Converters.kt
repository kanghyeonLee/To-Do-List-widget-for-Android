package com.kanghyeon.todolist.data.local.converter

import androidx.room.TypeConverter
import com.kanghyeon.todolist.data.local.entity.RepeatType

/**
 * Room TypeConverter
 *
 * RepeatType enum ↔ String 변환
 * Room은 기본적으로 enum을 저장할 수 없으므로 TypeConverter 등록 필요.
 * AppDatabase에 @TypeConverters(Converters::class)로 등록한다.
 */
class Converters {

    @TypeConverter
    fun fromRepeatType(type: RepeatType): String = type.name

    @TypeConverter
    fun toRepeatType(value: String): RepeatType =
        runCatching { RepeatType.valueOf(value) }.getOrDefault(RepeatType.NONE)
}
