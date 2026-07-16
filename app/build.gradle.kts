plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

import java.security.MessageDigest
import java.util.Properties

val isWindowsNonAsciiPath = System.getProperty("os.name").startsWith("Windows") &&
    rootProject.projectDir.absolutePath.any { it.code > 127 }
if (isWindowsNonAsciiPath) {
    layout.buildDirectory.set(
        file("${System.getProperty("user.home")}/.gradle/pikclick-build/app"),
    )
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}

android {
    namespace = "com.pikclick.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pikclick.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 21
        versionName = "2.1.0"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = keystoreProperties.getProperty("storeFile")
            if (!storeFilePath.isNullOrBlank()) {
                storeFile = rootProject.file(storeFilePath)
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.register<Copy>("copyReleaseApk") {
    dependsOn("assembleRelease")
    from(layout.buildDirectory.file("outputs/apk/release/app-release.apk"))
    into(rootProject.layout.projectDirectory.dir("dist"))
    rename { "PikClick-v${android.defaultConfig.versionName}-release.apk" }
}

tasks.register("checksumReleaseApk") {
    dependsOn("copyReleaseApk")
    val apk = rootProject.layout.projectDirectory.file(
        "dist/PikClick-v${android.defaultConfig.versionName}-release.apk",
    )
    val checksum = rootProject.layout.projectDirectory.file(
        "dist/PikClick-v${android.defaultConfig.versionName}-release.apk.sha256",
    )
    inputs.file(apk)
    outputs.file(checksum)
    doLast {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(apk.asFile.readBytes())
            .joinToString("") { "%02x".format(it) }
        checksum.asFile.writeText("$digest  ${apk.asFile.name}\n")
    }
}
