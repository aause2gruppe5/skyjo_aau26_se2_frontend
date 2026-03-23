# Skyjo Frontend

Android App (Kotlin) für das Skyjo Spiel – AAU SE2 Gruppe 5.

## Tech Stack

- Kotlin + Android SDK 34
- WebSocket (OkHttp / Java-WebSocket)
- Gradle (Kotlin DSL)
- JUnit 4 + JaCoCo
- SonarCloud Quality Gates

## Setup

Android Studio öffnen → "Open" → dieses Verzeichnis auswählen.

```bash
# Build
./gradlew assembleDebug

# Tests
./gradlew testDebugUnitTest
```

## CI/CD

GitHub Actions führt bei jedem Push/PR auf `main` automatisch aus:
1. Build (debug)
2. Unit Tests + JaCoCo Coverage Report
3. SonarCloud Scan

## Branch-Workflow

- Feature-Branches: `feature/<beschreibung>`
- Commit-Convention: [Conventional Commits](https://www.conventionalcommits.org/)
- Merges nur via Pull Request (kein Squash/Rebase)
- `main` ist protected und muss jederzeit lauffähig sein
