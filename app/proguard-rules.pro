# ──────────────────────────────────────────────────────────────────
# ProGuard / R8 규칙 (release 빌드에서 isMinifyEnabled=true 시 적용)
# ──────────────────────────────────────────────────────────────────

# ── Room ──────────────────────────────────────────────────────────
# Room Entity, DAO, TypeConverter는 리플렉션으로 접근되므로 보존
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# ── Hilt ──────────────────────────────────────────────────────────
# Hilt 생성 컴포넌트는 런타임에 참조되므로 보존
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ActivityComponentManager { *; }

# ── Kotlin Coroutines ─────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }

# ── Kotlin Serialization (DataStore 등 필요 시) ────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# ── 디버그 정보 유지 (크래시 리포트 가독성) ──────────────────────
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
