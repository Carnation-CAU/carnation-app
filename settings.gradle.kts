pluginManagement {
    repositories {
        google {
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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CarnationFallAlert"

// :contract 는 앱과 서버가 함께 쓰는 순수 Kotlin 모듈. 계약이 한 곳에만 존재하도록 강제한다.
include(":contract")
include(":server")
include(":app")
