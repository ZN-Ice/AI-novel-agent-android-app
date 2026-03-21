pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // ffmpeg-kit 已迁移到 Maven Central，不再需要 JitPack
    }
}

rootProject.name = "AI-Novel-Agent"

include(":app")
