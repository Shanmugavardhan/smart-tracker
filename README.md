# AI Expense Tracker (Milestone 1)

AI Expense Tracker is a personal finance Android application designed to minimize the friction of expense logging. The application supports offline-first usage and allows users to record their expenses quickly.

Currently, this repository implements **Milestone 1**: the local expense tracker without AI, focusing on reliable data capture, a fully functioning local database, offline capability, and rigorous unit testing.

## Features (Milestone 1)
- Add, Edit, and Delete Expenses
- Monthly Summary
- Category Breakdown of spending
- 100% Offline functionality (expenses persist across app restarts)

## Setup & Build
1. Open the project in **Android Studio (Giraffe or newer)**.
2. Allow Gradle to sync dependencies.
3. To build the APK from command line:
   ```bash
   ./gradlew assembleDebug
   ```

## Run
Connect an Android device or start an Emulator (minSdk 26 required) and run:
```bash
./gradlew installDebug
```
Or simply click the **Run** button in Android Studio.

## Testing
The application strictly enforces a `> 90%` code coverage requirement and includes Unit tests, DAO Integration tests, and Compose UI tests.
To run the full test suite and view coverage:
```bash
# Run unit tests and generate Jacoco Report
./gradlew jacocoTestReport

# Run instrumented UI and DB tests (Requires connected device/emulator)
./gradlew connectedAndroidTest
```
Coverage reports will be located at:
`app/build/reports/jacoco/jacocoTestReport/html/index.html`

## Architecture Overview
This application follows **Clean Architecture** patterns separated into:
- `presentation/`: Contains Jetpack Compose UI screens, Navigation routing, and Hilt ViewModels holding UI state.
- `domain/`: Contains core Kotlin Data models (`Expense`, `Category`, `CategoryTotal`) and interfaces. It has zero Android dependencies.
- `data/`: Contains the Room Database setup (`ExpenseEntity`, `ExpenseDao`), Entity/Domain Mappers, and the `ExpenseRepositoryImpl`.
- `di/`: Contains Hilt modules for dependency injection (`AppModule`).

**Offline Strategy**: Data is saved to the local Room database instantly, allowing fully offline capability. In Phase 2, entries will be marked as `PENDING_CATEGORIZATION` when offline, allowing WorkManager to sync with the AI backend later.

## Performance
Database operations (Insert, Read) are optimized and tested to execute in `< 100ms`.
UI interactions rely on `StateFlow` and Compose to prevent UI freezes.
