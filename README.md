# Master — Plan. Focus. Achieve.

A personal, local-first Android productivity app: tasks, habits, focus timer, calendar, notes, reminders, insights and
JSON backup, protected by a username + PIN (optional biometrics). No email, no phone number, no cloud, no internet permission.
Native Kotlin, Jetpack Compose, Material 3, Room, DataStore, Hilt, WorkManager. Samurai artwork is the app identity.

**Version 0.3.1: all planned features are written. The build environment has no Android SDK, Maven or Gradle-download access, so I could not run `gradlew` here
(`gradlew clean assembleDebug` and `testDebugUnitTest` both stop at "UnknownHostException: services.gradle.org"). No build or APK is claimed.**
The 6 compile errors reported by your own build were fixed, and the project was scanned for the same error classes. See [Honest status](#honest-status).

## Honest status

| Label | Meaning |
|---|---|
| **Unit-tested (executed)** | Pure logic compiled with a real Kotlin compiler and run under JUnit here: 110 tests pass |
| **Written, not executed** | Code and tests exist, but need Android Studio / an emulator / a phone |
| **Not verified on a real device** | Everything Android-specific (UI, Keystore, biometrics, notifications, WorkManager, pickers) |

Full details: `docs/TESTING_REPORT.md`, `docs/FEATURE_CHECKLIST.md`, `docs/KNOWN_LIMITATIONS.md`, `docs/DEVICE_TEST_PLAN.md`.

## Features
Home dashboard (today/open/done/overdue, habit and focus progress) · Tasks (create, edit, delete with undo, complete, due date and time,
priority, categories, search, filters, reminders) · Habits (daily or chosen weekdays, check in today, tap any past day to correct it,
streaks, best streak, completion rate, weekly progress, daily reminder, archive) · Focus (Pomodoro 25/5/15, custom minutes, pause/resume/stop,
history, survives backgrounding) · Calendar (month view, tasks on a day, add a task for a date) · Notes (create, edit, pin, search, delete with undo,
autosave on leave) · Insights (totals, completion %, 30-day habit consistency, streaks, focus time, last-7-days table) ·
Profile (username, picture, categories, theme, accent, notifications, security, backup, About, developer links) · Security (PIN, biometrics, auto-lock) ·
JSON backup/restore through the Storage Access Framework.

## Building on Windows (Android Studio)
1. Install **Android Studio** (current stable) and let it install **Android SDK Platform 35** when asked.
2. Unzip `Master-Complete-Updated.zip` to a short path, e.g. `C:\dev\Master`.
3. **File → Open** → select that folder (the one with `settings.gradle.kts`). Wait for **Gradle sync** (several minutes the first time;
   needs internet). Accept the SDK 35 install. Decline any offer to upgrade AGP/Gradle for now.
4. **If sync or build shows errors, copy the full Gradle/Build output and send it to me.** The code has never been compiled; a few
   compile errors are possible and quick to fix.
5. Run the checks: terminal at the project root:
   * `gradlew.bat testDebugUnitTest`  (JVM tests, includes the 110 already-verified ones)
   * `gradlew.bat connectedDebugAndroidTest`  (needs an emulator or a phone with USB debugging; runs Room/Keystore/backup tests)
6. **Commit `app/schemas/`** (created by the first build; needed for future migration tests).
7. **Debug APK:** `gradlew.bat assembleDebug` → `app\build\outputs\apk\debug\app-debug.apk` (application id `com.chandan.master.debug`).
8. **Release APK:** create a signing key once, then build:
   `keytool -genkeypair -v -keystore master-release.jks -alias master -keyalg RSA -keysize 4096 -validity 10000`
   copy `keystore.properties.example` to `keystore.properties`, fill it in, then `gradlew.bat assembleRelease`
   → `app\build\outputs\apk\release\app-release.apk`. Keep the `.jks` and its passwords safe (losing them means you cannot update the app in place).
   Without `keystore.properties` the release APK is unsigned and will not install.

### Install on your phone
Enable *Developer options → USB debugging*, connect the phone, then `adb install -r app\build\outputs\apk\debug\app-debug.apk`
(adb is in `%LOCALAPPDATA%\Android\Sdk\platform-tools`). Or copy the APK to the phone, open it, and allow "Install unknown apps" for your file manager.
Then follow `docs/DEVICE_TEST_PLAN.md`.

## Security summary
PIN: PBKDF2-HMAC-SHA256, 210,000 iterations, unique salt, then AES-256-GCM encrypted by a non-exportable Keystore key; escalating non-permanent lockouts
(5 free tries, then 30 s … 30 min); biometrics via BiometricPrompt with a Keystore key bound to biometrics; auto-lock with a monotonic clock;
a forgotten PIN cannot be recovered (warned reset only). **Not claimed:** the database is not encrypted at rest; backups are plain JSON (no secrets inside).
Details in `docs/ARCHITECTURE.md` and `docs/KNOWN_LIMITATIONS.md`.

## Data and privacy
Everything stays on the device. No internet permission, no analytics. The only permission is POST_NOTIFICATIONS. Android Auto Backup is disabled.
A backup file contains tasks, categories, habits and check-ins, focus sessions, notes, appearance/notification/timer settings and the username
(for reference). It never contains PIN data, keys, the biometric setting, auto-lock, or the profile picture.

## Project structure
```
app/src/main/java/com/chandan/master/
  core/ (time, constants)  domain/ (model, logic, auth, backup, repository, usecase: pure Kotlin where possible)
  data/ (local/db, local/preferences, security, repository, notifications, focus, backup)  di/  presentation/ (theme, components, navigation,
  auth, onboarding, home, tasks, habits, focus, calendar, notes, insights, profile, splash)
app/src/test/ (JVM tests)   app/src/androidTest/ (device tests)   tools/ (asset generator, test runner, static checks, migration verifier)
```
Pure-logic highlights (all unit-tested): `StreakCalculator`, `TaskRules`, `ReminderCalculator`, `FocusTimerLogic`, `InsightsCalculator`,
`Json` + `BackupCodec` + `RestorePlanner`, `PinHasher`, `LockoutPolicy`, `AutoLockPolicy`, `MasterPalette`.

## Verification tools (run anywhere with Java + Python)
* `tools/run_pure_tests.sh <kotlinc_home> <junit.jar> <hamcrest.jar>`: compiles and runs the 110 pure tests.
* `python3 tools/static_checks.py`: imports, resources, plurals, catalog aliases, permissions, forbidden fields, blue colours, missing imports.
* `python3 tools/verify_migration_sql.py`: runs the 1→2 migration SQL on real SQLite.
* `python3 tools/check_di_graph.py`: static Hilt/Dagger check (every dependency has exactly one provider, no cycles).
