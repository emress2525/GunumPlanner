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
    }
}

rootProject.name = "NamazV8"
include(":app")
include(":core:model")
include(":core:settings")
include(":core:prayer")
include(":feature:today")
include(":feature:prayer")
