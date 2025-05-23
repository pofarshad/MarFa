plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.6" apply false
    id("org.owasp.dependencycheck") version "8.4.1" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}