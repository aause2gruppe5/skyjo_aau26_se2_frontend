plugins {
    kotlin("jvm") version "2.2.10"
    id("jacoco")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.26.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        xml.outputLocation.set(
            file("${project.projectDir}/build/reports/jacoco/test/jacocoTestReport.xml")
        )
    }
}

sonar {
    properties {
        property("sonar.sources", "src/main/kotlin")
        property("sonar.tests", "src/test/kotlin")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "${project.projectDir}/build/reports/jacoco/test/jacocoTestReport.xml"
        )
        property("sonar.kotlin.file.suffixes", ".kt,.kts")
        property("sonar.sourceEncoding", "UTF-8")
    }
}
