// ──────────────────────────────────────────────────────────────────
// settings.gradle.kts — 프로젝트 전역 설정
//
// [역할]
// 1. 플러그인 리포지토리 선언 (pluginManagement)
// 2. 라이브러리 리포지토리 선언 (dependencyResolutionManagement)
// 3. 포함할 모듈 선언 (include)
//
// [주의] pluginManagement / dependencyResolutionManagement 는
//        settings.gradle.kts 의 최상단에 위치해야 한다.
// ──────────────────────────────────────────────────────────────────

pluginManagement {
    repositories {
        google {
            // Google 리포지토리에서는 Android/Google/AndroidX 그룹만 가져옴
            // → 불필요한 메타데이터 다운로드를 줄여 빌드 속도 향상
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // FAIL_ON_PROJECT_REPOS: 서브 모듈이 자체 repositories {} 블록을 갖지 못하게 강제
    // → 의존성 출처를 settings 에서 일원화
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ToDoList"
include(":app")
