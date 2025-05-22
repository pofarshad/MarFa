// MarFaNet Android - Root Build Script

plugins {
    id("com.android.application") version "9.0.0-alpha01" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0-Beta3" apply false
    id("org.owasp.dependencycheck") version "9.0.9" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

// MarFaNet version configuration
extra["marfanetVersionName"] = "1.0.1"
extra["marfanetVersionCode"] = 3
extra["targetSdkVersion"] = 34
extra["minSdkVersion"] = 21