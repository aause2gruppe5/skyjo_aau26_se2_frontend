plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("jacoco")
}

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
            // enableUnitTestCoverage uses AGP offline instrumentation which conflicts
            // with the JaCoCo JVM agent approach needed for Robolectric compatibility
        }
    }

    lint {
        disable += "NullSafeMutableLiveData"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            // CRITICAL: Disable Robolectric's re-instrumentation to preserve JaCoCo probes
            // Robolectric's InstrumentingClassLoader corrupts offline instrumentation
            // See: https://github.com/robolectric/robolectric/issues/7527
            all { test ->
                test.jvmArgs("-javaagent:${configurations.jacocoAgent.asPath}=destfile=${buildDir}/jacoco/test.exec,append=false")
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

    // JaCoCo agent for runtime coverage collection (needed for Robolectric compat)
    jacocoAgent("org.jacoco:org.jacoco.agent:0.8.11:runtime")

    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

// ─────────────────────────────────────────────────────────────────────────────
// JaCoCo Coverage Configuration
// ─────────────────────────────────────────────────────────────────────────────
// PROBLEM: AGP's enableUnitTestCoverage uses offline instrumentation, but
// Robolectric's InstrumentingClassLoader re-instruments bytecode at runtime,
// corrupting JaCoCo's compile-time probes. This causes 0% coverage for Robolectric tests.
//
// SOLUTION: Use runtime instrumentation with JaCoCo Java agent instead of
// offline instrumentation. The agent preserves JaCoCo probes through Robolectric's
// re-instrumentation because it instruments AFTER Robolectric does.
// ─────────────────────────────────────────────────────────────────────────────

jacoco {
    toolVersion = "0.8.11"
}

// Register JaCoCo report task for debug unit tests
tasks.register<JacocoReport>("jacocoDebugReport") {
    group = "verification"
    description = "Generate JaCoCo coverage report for unit tests (Robolectric-compatible)"

    dependsOn("testDebugUnitTest")

    // Use the execution data collected via JaCoCo agent at runtime
    executionData(files("$buildDir/jacoco/test.exec").filter { it.exists() })

    sourceDirectories.setFrom(files("src/main/kotlin", "src/main/java"))
    classDirectories.setFrom(files(
        fileTree("$buildDir/tmp/kotlin-classes/debug") {
            exclude(
                "**/R.class",
                "**/R\$*.class",
                "**/BuildConfig.class",
                "**/Manifest*.class",
                "**/*\$\$serializer.class"
            )
        }
    ))

    reports {
        xml.required.set(true)
        xml.outputLocation.set(File("$buildDir/reports/coverage/test/debug/report.xml"))
        html.required.set(true)
        html.outputLocation.set(File("$buildDir/reports/coverage/test/debug/html"))
    }
}

// Override AGP's createDebugUnitTestCoverageReport to use our JaCoCo agent approach
tasks.register("createDebugUnitTestCoverageReport") {
    dependsOn("jacocoDebugReport")
    doLast {
        val reportPath = "$buildDir/reports/coverage/test/debug/report.xml"
        val reportFile = File(reportPath)
        if (reportFile.exists()) {
            println("JaCoCo coverage report generated successfully at: $reportPath")
        } else {
            println("WARNING: Coverage report not found at $reportPath")
            println("This may indicate no tests were executed or execution data was not collected.")
        }
    }
}

// Convenience task for CI: runs tests with JaCoCo agent and generates report
tasks.register("testDebugWithCoverage") {
    dependsOn("createDebugUnitTestCoverageReport")
    doLast {
        println("Coverage report generated at: app/build/reports/coverage/test/debug/report.xml")
    }
}
