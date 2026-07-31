// Top-level build file. Individual module build files apply the plugins they need.
plugins {
    id("com.android.application") version "8.6.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("com.google.dagger.hilt.android") version "2.57.1" apply false
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
