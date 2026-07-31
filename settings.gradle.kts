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
        google()          // required for AGP, Jetpack libs, and com.google.ai.edge.litertlm
        mavenCentral()
    }
}

rootProject.name = "AdaptiveOperatorAI"
include(":app")
