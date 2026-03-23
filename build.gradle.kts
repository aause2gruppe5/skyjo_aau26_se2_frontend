plugins {
    id("com.android.application") version "8.3.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
    id("org.sonarqube") version "5.0.0.4638"
}

sonar {
    properties {
        property("sonar.projectKey", "aause2gruppe5_skyjo_aau26_se2_frontend")
        property("sonar.organization", "aause2gruppe5")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.sources", "app/src/main/kotlin")
        property("sonar.tests", "app/src/test/kotlin")
        property("sonar.coverage.jacoco.xmlReportPaths", "app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
        property("sonar.kotlin.file.suffixes", ".kt,.kts")
    }
}
