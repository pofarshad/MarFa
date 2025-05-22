plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("org.owasp.dependencycheck") version "8.4.2"
    id("jacoco")
    id("com.google.gms.google-services") version "4.4.0"
    id("com.google.firebase.crashlytics") version "2.9.9"
}

android {
    namespace = "net.marfanet.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "net.marfanet.android"
        minSdk = 21
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            // LeakCanary for memory leak detection
            isMinifyEnabled = false
        }
        
        create("beta") {
            initWith(getByName("release"))
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"
            isDebuggable = false
            // Enable Crashlytics for beta builds
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "true"
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            // R8 full mode for performance
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += listOf(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlin.time.ExperimentalTime"
        )
    }

    buildFeatures {
        viewBinding = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("keystore/marfanet-release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = "marfanet"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    
    // Coroutines & Flow for performance
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    
    // WorkManager for routing updates
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Room for latency tracking
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // Preferences
    implementation("androidx.preference:preference-ktx:1.2.1")
    
    // Network & JSON
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // GFW Knocker module
    implementation(project(":gfwknocker"))
    
    // Firebase Analytics & Crashlytics
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-config-ktx")
    
    // Memory leak detection (debug only)
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.work:work-testing:2.9.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.room:room-testing:2.6.1")
    
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.work:work-testing:2.9.0")
}

// OWASP Dependency Check Configuration
dependencyCheck {
    format = "ALL"
    outputDirectory = "build/reports"
    analyzers {
        assemblyEnabled = false
        nuspecEnabled = false
    }
    data {
        directory = "${rootProject.buildDir}/dependency-check-data"
    }
}

// JaCoCo Test Coverage Configuration
tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    
    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*"
    )
    
    val debugTree = fileTree("${buildDir}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }
    
    val mainSrc = "${project.projectDir}/src/main/kotlin"
    
    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(buildDir) {
        include("jacoco/testDebugUnitTest.exec")
    })
}