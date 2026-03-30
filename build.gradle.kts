// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

// 自定义任务：运行所有检查
tasks.register("runAllChecks") {
    dependsOn(":app:lint")
    dependsOn(":app:test")
    dependsOn(":app:jacocoTestReport")

    doLast {
        println("All checks passed!")
    }
}
