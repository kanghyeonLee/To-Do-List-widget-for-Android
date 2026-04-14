package com.kanghyeon.todolist.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kanghyeon.todolist.data.local.converter.Converters
import com.kanghyeon.todolist.data.local.dao.RoutineTemplateDao
import com.kanghyeon.todolist.data.local.dao.TaskDao
import com.kanghyeon.todolist.data.local.entity.RoutineTemplateGroupEntity
import com.kanghyeon.todolist.data.local.entity.RoutineTemplateTaskEntity
import com.kanghyeon.todolist.data.local.entity.TaskEntity

/**
 * Room Database 싱글턴
 *
 * [버전 관리 전략]
 * - version을 올릴 때 반드시 Migration 객체를 addMigrations()에 등록할 것
 * - 개발 중에는 fallbackToDestructiveMigrationOnDowngrade()를 허용하지만
 *   릴리즈 빌드에서는 반드시 명시적 Migration을 사용해야 한다.
 *
 * [Hilt 연동]
 * - @Module / @Provides로 AppDatabase와 TaskDao를 제공하면
 *   companion object의 getInstance()는 사용하지 않아도 된다.
 *   (Hilt 미사용 프로젝트를 위해 getInstance()도 함께 제공)
 */
@Database(
    entities = [
        TaskEntity::class,
        RoutineTemplateGroupEntity::class,
        RoutineTemplateTaskEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    abstract fun routineTemplateDao(): RoutineTemplateDao

    companion object {
        private const val DB_NAME = "todo_database"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }

        /** v1 → v2: reminderMinutes(INTEGER, nullable) 컬럼 추가. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminderMinutes INTEGER DEFAULT NULL")
            }
        }

        /** v2 → v3: isDeleted(INTEGER, NOT NULL DEFAULT 0) 컬럼 추가. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * v3 → v4: 루틴 템플릿 단일 테이블 생성 (임시 구조).
         * v4 → v5 마이그레이션에서 이 테이블을 제거하고 1:N 구조로 전환한다.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS routine_templates (
                        id               INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title            TEXT    NOT NULL,
                        description      TEXT,
                        priority         INTEGER NOT NULL DEFAULT 1,
                        showOnLockScreen INTEGER NOT NULL DEFAULT 1,
                        createdAt        INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * v4 → v5: 루틴 템플릿을 1:N 구조로 고도화.
         *
         * - routine_templates (단일 테이블) 제거
         * - routine_template_groups (그룹, isActive 포함) 신규 생성
         * - routine_template_tasks  (그룹별 할 일, FK CASCADE) 신규 생성
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS routine_templates")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS routine_template_groups (
                        id        INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name      TEXT    NOT NULL,
                        isActive  INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS routine_template_tasks (
                        id               INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        groupId          INTEGER NOT NULL,
                        title            TEXT    NOT NULL,
                        description      TEXT,
                        priority         INTEGER NOT NULL DEFAULT 1,
                        showOnLockScreen INTEGER NOT NULL DEFAULT 1,
                        FOREIGN KEY(groupId) REFERENCES routine_template_groups(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_routine_template_tasks_groupId " +
                    "ON routine_template_tasks(groupId)"
                )
            }
        }

        /**
         * v5 → v6: tasks 테이블에 archivedAt(INTEGER, nullable) 컬럼 추가.
         * 자정 동기화 시 아카이브 날짜를 명시적으로 저장하기 위한 컬럼.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN archivedAt INTEGER DEFAULT NULL")
            }
        }

        private fun buildDatabase(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME,
            )
                .addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                    MIGRATION_5_6,
                )
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
    }
}
