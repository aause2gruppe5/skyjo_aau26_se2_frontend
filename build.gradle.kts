plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("org.sonarqube") version "5.0.0.4638"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
}

sonar {
    properties {
        property("sonar.projectKey", "aause2gruppe5_skyjo_aau26_se2_frontend")
        property("sonar.organization", "aause2gruppe5")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}
