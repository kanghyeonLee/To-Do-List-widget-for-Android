// ──────────────────────────────────────────────────────────────────
// app/build.gradle.kts
// ──────────────────────────────────────────────────────────────────

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Kotlin 2.0+: Compose 컴파일러를 별도 플러그인으로 분리
    // → composeOptions.kotlinCompilerExtensionVersion 설정 불필요
    alias(libs.plugins.kotlin.compose)
    // Hilt: @HiltAndroidApp / @AndroidEntryPoint / @HiltViewModel 코드 생성
    alias(libs.plugins.hilt)
    // KSP: Hilt·Room 어노테이션 프로세서 실행 (kapt보다 빌드 속도 2~3배 빠름)
    alias(libs.plugins.ksp)
    // Room Gradle Plugin: room {} 블록으로 스키마 설정 가능
    alias(libs.plugins.room)
}

android {
    namespace   = "com.kanghyeon.todolist"
    compileSdk  = 35          // 최신 SDK로 컴파일 (API 35 = Android 15)

    defaultConfig {
        applicationId   = "com.kanghyeon.todolist"
        minSdk          = 26  // Android 8.0: startForegroundService(), NotificationChannel 등 지원
        targetSdk       = 35
        versionCode     = 1
        versionName     = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room: 스키마 JSON 파일 내보내기 위치 (room {} 블록에서 설정)
        // 이 파일을 git으로 추적하면 마이그레이션 이력을 관리할 수 있음
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled    = true   // R8 코드 축소 활성화
            isShrinkResources  = true   // 미사용 리소스 제거
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        // Kotlin 1.9+ / AGP 8.x 기준 권장 JVM 버전
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        // 경고를 오류로 처리 (선택사항 — 개발 중에는 주석 처리 가능)
        // freeCompilerArgs += listOf("-Xjvm-default=all", "-Werror")
    }

    buildFeatures {
        compose = true        // Jetpack Compose 활성화
        buildConfig = true    // BuildConfig 클래스 생성 활성화
    }

    // ── Room Gradle Plugin 설정 ──────────────────────────────────
    // ksp { arg("room.schemaLocation", ...) } 방식보다 간결
    room {
        schemaDirectory("$projectDir/schemas")
    }

    // ── 패키징 옵션 ───────────────────────────────────────────────
    packaging {
        resources {
            // 중복 라이선스 파일 제거 (없으면 빌드 경고 발생)
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// ──────────────────────────────────────────────────────────────────
// 의존성 선언
// ──────────────────────────────────────────────────────────────────
dependencies {

    // ── AndroidX Core ──────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // ── Lifecycle ──────────────────────────────────────────────
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // collectAsStateWithLifecycle() — 생명주기 인식 Flow 수집
    implementation(libs.androidx.lifecycle.runtime.compose)
    // hiltViewModel() 내부 ViewModel 팩토리
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // ── Jetpack Compose ────────────────────────────────────────
    // BOM을 platform()으로 감싸면 하위 라이브러리 버전을 BOM이 결정
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // Icons.Default.CheckCircle, Icons.Outlined.RadioButtonUnchecked 등
    implementation(libs.androidx.material.icons.extended)

    // ── Hilt (DI) ─────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)                 // @HiltAndroidApp, @AndroidEntryPoint 코드 생성
    // hiltViewModel() Composable 지원
    implementation(libs.hilt.navigation.compose)

    // ── Room (로컬 DB) ────────────────────────────────────────
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)           // Flow, suspend 확장 함수 포함
    ksp(libs.room.compiler)                 // @Entity, @Dao 등 코드 생성

    // ── DataStore ─────────────────────────────────────────────
    // 앱 설정(정렬 방식 등) 저장용 (Room과 병행 사용)
    implementation(libs.datastore.preferences)

    // ── Coroutines ────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)

    // ── 단위 테스트 ────────────────────────────────────────────
    testImplementation(libs.junit)

    // ── 계측 테스트 (Android) ──────────────────────────────────
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // ── 디버그 전용 ────────────────────────────────────────────
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
