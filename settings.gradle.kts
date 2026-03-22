// CI环境检测
val isCi = System.getenv("CI") == "true"

pluginManagement {
    repositories {
        if (isCi) {
            // CI环境：使用原始仓库
            google()
            mavenCentral()
            gradlePluginPortal()
        } else {
            // 本地开发：使用阿里云镜像加速
            maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/central") }
            // 备用原始仓库
            google()
            mavenCentral()
            gradlePluginPortal()
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (isCi) {
            // CI环境：使用原始仓库
            google()
            mavenCentral()
        } else {
            // 本地开发：使用阿里云镜像加速
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/central") }
            // 备用原始仓库
            google()
            mavenCentral()
        }
        // ffmpeg-kit: 使用社区fork版本 com.antonkarpenko:ffmpeg-kit-full
        // 官方版本 com.arthenica:ffmpeg-kit-full 已于2025年1月退休
    }
}

rootProject.name = "AI-Novel-Agent"

include(":app")
