package com.kanghyeon.todolist.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kanghyeon.todolist.data.local.converter.Converters
import com.kanghyeon.todolist.data.local.dao.TaskDao
import com.kanghyeon.todolist.data.local.entity.TaskEntity

/**
 * Room Database 싱글턴
 *
 * [버전 관리 전략]
 * - version을 올릴 때 반드시 Migration 객체를 addMigrations()에 등록할 것
 * - 개발 중에는 fallbackToDestructiveMigration()를 허용하지만
 *   릴리즈 빌드에서는 반드시 명시적 Migration을 사용해야 한다.
 *
 * [Hilt 연동]
 * - @Module / @Provides로 AppDatabase와 TaskDao를 제공하면
 *   companion object의 getInstance()는 사용하지 않아도 된다.
 *   (Hilt 미사용 프로젝트를 위해 getInstance()도 함께 제공)
 */
@Database(
    entities = [TaskEntity::class],
    version = 2,
    exportSchema = true,   // 스키마 JSON 파일 자동 생성 → git으로 버전 추적 가능
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        private const val DB_NAME = "todo_database"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }

        /**
         * v1 → v2: reminderMinutes(INTEGER, nullable) 컬럼 추가.
         * DEFAULT NULL로 기존 행은 알림 없음 상태로 유지.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminderMinutes INTEGER DEFAULT NULL")
            }
        }

        private fun buildDatabase(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME,
            )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigrationOnDowngrade() // 다운그레이드 시에만 파괴적 마이그레이션 허용
                .build()
    }
}
