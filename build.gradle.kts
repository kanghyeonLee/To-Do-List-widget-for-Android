// ──────────────────────────────────────────────────────────────────
// 루트 build.gradle.kts
//
// [역할]
// 서브모듈(:app 등)에 적용될 플러그인을 선언만 하고 apply는 false로 둔다.
// 실제 적용은 각 모듈의 build.gradle.kts 에서 alias()로 이루어진다.
//
// [apply false 이유]
// 루트에서 apply하면 루트 프로젝트에도 플러그인이 적용되어
// 불필요한 태스크가 생성되거나 충돌이 발생할 수 있다.
// ──────────────────────────────────────────────────────────────────

plugins {
    alias(libs.plugins.android.application)  apply false
    alias(libs.plugins.kotlin.android)       apply false
    alias(libs.plugins.kotlin.compose)       apply false
    alias(libs.plugins.hilt)                 apply false
    alias(libs.plugins.ksp)                  apply false
    alias(libs.plugins.room)                 apply false
}
