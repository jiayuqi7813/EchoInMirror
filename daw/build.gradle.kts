import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.compose.ComposeBuildConfig
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.9.10"
    id("org.jetbrains.compose")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://mvn.0110.be/releases")
}

kotlin {
    val jvmVersion = extra["jvm.version"] as String
    jvmToolchain(jvmVersion.toInt())
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = jvmVersion
        }
        withJava()
    }
    sourceSets {
        named("jvmMain") {
            dependencies {
                api(project(":components"))
                api(project(":utils"))
                api(project(":api"))
                api(project(":native"))
                api(project(":audio-sources"))

                api(compose.desktop.currentOs) {
                    exclude("org.jetbrains.compose.material")
                }
                api(compose.materialIconsExtended)
                api(compose.material3)

                api("commons-io:commons-io:2.11.0")
                api("org.ocpsoft.prettytime:prettytime:5.0.6.Final")
                api("org.mapdb:mapdb:3.0.9") {
                    exclude(group = "com.google.guava")
                }
                implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0")
                implementation("org.jetbrains.compose.ui:ui-util:${ComposeBuildConfig.composeVersion}")
                implementation("com.github.Apisium:appcenter-sdk-jvm:1.0.6")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "EchoInMirror"
            packageVersion = "1.0.0"
        }
    }
}

fun downloadEIMHost(ext: String, appendExt: Boolean = false): Boolean {
    val file = File(if (appendExt) "EIMHost-$ext" else "EIMHost")
    if (!File("src").exists() || file.exists()) return false
    val connection = URI.create("https://github.com/EchoInMirror/EIMHost/releases/latest/download/EIMHost-$ext")
        .toURL().openConnection() as HttpURLConnection
    connection.connect()
    val input = connection.inputStream
    val output = FileOutputStream(file)
    input.copyTo(output)
    input.close()
    output.close()
    return true
}

project(":daw") {
    task<Copy>("downloadEIMHost") {
        val os = org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem()
        if (os.isWindows) {
            downloadEIMHost("x64.exe", true)
//          downloadEIMHost("x86.exe", true)
        } else if (os.isMacOsX) {
            if (downloadEIMHost("MacOS.zip", true)) {
                from(zipTree(File("EIMHost-MacOS.zip"))).into(".")
            }
        } else {
            downloadEIMHost("Linux")
        }
    }
}

tasks.withType<Jar> {
    manifest {
        attributes(
            "Name" to "EchoInMirror",
            "Main-Class" to "com.eimsound.daw.MainKt",
            "Implementation-Version" to project.version,
            "Release-Time" to System.currentTimeMillis(),
            "App-Center-Secret" to (System.getenv("APP_CENTER_SECRET") ?: "")
        )
    }
}

tasks.withType<ShadowJar> {
    mergeServiceFiles()
    archiveClassifier.set("")
    archiveVersion.set("")
}

tasks.withType<JavaExec> {
    jvmArgs(
        "--enable-preview",
        "--enable-native-access=ALL-UNNAMED",
        "--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED"
    )
}

// Run before build
tasks.withType<GradleBuild> {
    dependsOn(":downloadEIMHost")
}
