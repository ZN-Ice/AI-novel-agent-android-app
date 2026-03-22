plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.novelapp.aiagent"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.novelapp.aiagent"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // BuildConfig字段（API配置）
        buildConfigField("String", "API_BASE_URL", "\"https://api.novelapp.ai/v1\"")
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"

            buildConfigField("String", "API_BASE_URL", "\"https://api-dev.novelapp.ai/v1\"")
        }

        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
        )
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    lint {
        lintConfig = file("$rootDir/lint.xml")
        abortOnError = true
        warningsAsErrors = false
        checkReleaseBuilds = true
    }

    // 测试选项
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    // 打包选项
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    // AndroidX核心
    implementation(libs.bundles.androidx.core)
    implementation(libs.bundles.androidx.ui)

    // Lifecycle
    implementation(libs.bundles.androidx.lifecycle)

    // Room数据库
    implementation(libs.bundles.androidx.room)
    kapt(libs.androidx.room.compiler)

    // Navigation
    implementation(libs.bundles.androidx.navigation)

    // Hilt依赖注入
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // 网络请求
    implementation(libs.bundles.network)
    implementation(libs.kotlinx.serialization.json)

    // 协程
    implementation(libs.bundles.coroutines)

    // ffmpeg语音处理
    implementation(libs.bundles.ffmpeg)

    // 图片加载
    implementation(libs.glide)

    // 日志
    implementation(libs.timber)

    // 安全加密
    implementation(libs.bundles.security)

    // ============ 测试依赖 ============
    // 单元测试
    testImplementation(libs.bundles.test)

    // Android测试
    androidTestImplementation(libs.bundles.android.test)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

// Ktlint配置（可选）
// plugins {
//     id("org.jlleitschuh.gradle.ktlint")
// }
//
// ktlint {
//     android.set(true)
//     outputColorName.set("RED")
// }
