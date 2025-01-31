pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    plugins {
        kotlin("multiplatform").version(extra["kotlin.version"] as String)
        id("org.jetbrains.compose").version(extra["compose.version"] as String)
    }
}

rootProject.name = "EchoInMirror"

include(
    ":components",
    ":utils",
    ":audio-sources",
    ":dsp",
    ":native",
    ":api",
    ":daw"
)
