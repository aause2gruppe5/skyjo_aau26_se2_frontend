plugins {
    id("com.android.application") version "9.1.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("org.sonarqube") version "6.0.1.5171"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
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
