plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("jacoco")
    id("org.jetbrains.kotlin.plugin.compose")
}

val composeBomVersion = "2025.05.01"
val navigationComposeVersion = "2.9.0"

android {
    namespace = "at.aau.se2.skyjo"
    compileSdk = 35

    defaultConfig {
        applicationId = "at.aau.se2.skyjo"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            enableUnitTestCoverage = true
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:$composeBomVersion")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.navigation:navigation-compose:$navigationComposeVersion")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // WebSocket
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.java-websocket:Java-WebSocket:1.5.6")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test:core-ktx:1.6.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    sourceDirectories.setFrom(files("src/main/kotlin"))
    classDirectories.setFrom(
        fileTree("build/tmp/kotlin-classes/debug") {
            exclude("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*")
        }
    )
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        }
    )
}
