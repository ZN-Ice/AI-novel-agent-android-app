pluginManagement {
    repositories {
        // 国内镜像（阿里云）- 优先尝试
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        // 国外原始仓库 - 备用
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 国内镜像（阿里云）- 优先尝试
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        // 国外原始仓库 - 备用
        google()
        mavenCentral()
        // ffmpeg-kit: 使用社区fork版本 com.antonkarpenko:ffmpeg-kit-full
        // 官方版本 com.arthenica:ffmpeg-kit-full 已于2025年1月退休
    }
}

rootProject.name = "AI-Novel-Agent"

include(":app")
