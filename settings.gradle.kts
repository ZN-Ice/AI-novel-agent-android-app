pluginManagement {
    repositories {
        // 官方仓库优先（确保KSP等插件能找到）
        google()
        mavenCentral()
        gradlePluginPortal()
        // 国内镜像备用
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 国内镜像优先（加速依赖下载）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        // 官方仓库备用
        google()
        mavenCentral()
        // ffmpeg-kit: 使用社区fork版本 com.antonkarpenko:ffmpeg-kit-full
        // 官方版本 com.arthenica:ffmpeg-kit-full 已于2025年1月退休
    }
}

rootProject.name = "AI-Novel-Agent"

include(":app")
