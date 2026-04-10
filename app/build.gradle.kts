plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("jacoco")
}

// Custom configuration so the JaCoCo plugin's artifact transform does not interfere
val jacocoRuntime by configurations.creating

sonar {
    properties {
        property("sonar.sources", "src/main/kotlin")
        property("sonar.tests", "src/test/kotlin")
        property("sonar.coverage.jacoco.xmlReportPaths", "${project.projectDir}/build/reports/coverage/test/debug/report.xml")
        property("sonar.androidLint.reportPaths", "${project.projectDir}/build/reports/lint-results-debug.xml")
        property("sonar.kotlin.file.suffixes", ".kt,.kts")
        property("sonar.exclusions", "**/*.xml,**/res/**")
        property("sonar.sourceEncoding", "UTF-8")
    }
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
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // enableUnitTestCoverage (AGP offline instrumentation) is intentionally OFF —
            // it conflicts with the JaCoCo JVM agent used below for Robolectric compatibility
        }
    }

    lint {
        disable += "NullSafeMutableLiveData"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            // JaCoCo JVM agent attaches AFTER Robolectric's InstrumentingClassLoader
            // transforms bytecode, so probes survive and coverage is captured correctly.
            all { test ->
                val agentJar = configurations.getByName("jacocoRuntime").asPath
                val execFile = layout.buildDirectory.file("jacoco/test.exec").get().asFile
                test.jvmArgs("-javaagent:$agentJar=destfile=$execFile,append=false")
            }
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
    testImplementation(composeBom)
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

    // JaCoCo agent jar — resolved via custom config to avoid the plugin's ZIP-extract transform
    jacocoRuntime("org.jacoco:org.jacoco.agent:0.8.12:runtime")

    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

jacoco {
    toolVersion = "0.8.12"
}

// JaCoCo report task — uses execution data from the JVM agent (Robolectric-compatible)
tasks.register<JacocoReport>("jacocoDebugReport") {
    group = "verification"
    dependsOn("testDebugUnitTest")

    val execFile = layout.buildDirectory.file("jacoco/test.exec")
    executionData(execFile.map { f -> if (f.asFile.exists()) files(f) else files() })

    sourceDirectories.setFrom(files("src/main/kotlin"))
    classDirectories.setFrom(
        layout.buildDirectory.dir("tmp/kotlin-classes/debug").map { dir ->
            fileTree(dir) {
                exclude(
                    "**/R.class",
                    "**/R\$*.class",
                    "**/BuildConfig.class",
                    "**/Manifest*.class",
                    "**/*\$\$serializer.class"
                )
            }
        }
    )

    reports {
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/coverage/test/debug/report.xml"))
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/coverage/test/debug/html"))
    }
}

// createDebugUnitTestCoverageReport delegates to jacocoDebugReport
tasks.register("createDebugUnitTestCoverageReport") {
    dependsOn("jacocoDebugReport")
}
