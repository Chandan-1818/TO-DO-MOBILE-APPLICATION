# Testing report (version 0.3.1)

## Environment limits (evidence)
Network checks from the build sandbox: dl.google.com, maven.google.com, services.gradle.org, repo.maven.apache.org and plugins.gradle.org all
failed to connect; `./gradlew --version` failed while downloading Gradle; there is no Android SDK, emulator or phone. Therefore:
**no Gradle build, no APK, no instrumented test and no real-device test was run.** No APK exists and none is claimed.
Re-attempted for 0.3.1: `./gradlew clean assembleDebug` and `./gradlew testDebugUnitTest` both fail immediately with `java.net.UnknownHostException: services.gradle.org` (Gradle itself cannot be downloaded).

## Compile errors reported by the user's build (all fixed)
`AndroidBiometricKeyProvider.kt:29` (`return` in an expression body), `HabitsScreen.kt:132` and `TaskRow.kt:64` (missing `semantics.role` import), `NotesScreen.kt:85` (`PaddingValues` mixing `vertical` with `bottom`), `TaskRow.kt:61,68` (missing `dp` import).
The same bug classes were then searched for across all 156 Kotlin files by new checks in `tools/static_checks.py`: that search also found and fixed `AccountRepositoryImpl.verifyPin` (same expression-body `return` error). The checks reproduce all six reported errors on the old code.
Your build also shows resource linking and KSP (Room query validation) passed before the Kotlin errors, which is good evidence the manifest, resources and DAO queries are valid.
`tools/check_di_graph.py` (new) statically verifies the Hilt graph: 46 injectable classes, every dependency has exactly one provider, no cycles; it fails when a binding is removed.
Also checked: duplicate resource names (none), 306 `stringResource`/plural calls against their format strings (0 mismatches), no TODO/placeholder/empty click handlers.

## Executed here and passing: 110 JVM tests (kotlinc 2.0.21 + JUnit 4.13.2)
| Area | Tests | What is proven |
|---|---|---|
| Habit streaks (`StreakCalculatorTest`) | 12 | consecutive days, pending-today does not break, missed day breaks, weekday-only schedules, unscheduled days never missed, bonus completions, future/before-start ignored, undo/history edit recalculates, longest vs current, weekly row |
| Task rules (`TaskRulesTest`) | 7 | overdue rules (date-only vs timed), filters, case-insensitive search (title+description), category filter, sort order, completed order, title validation |
| Reminders (`ReminderCalculatorTest`) | 6 | at/before due time, date-only → 09:00, none/completed/past → no reminder, time-zone changes the instant, habit next-trigger, start date |
| Focus timer (`FocusTimerLogicTest`) | 10 | start, clock-derived remaining time, pause/resume, no-ops, completion, early stop, stop while paused, 4-session cycle with long break, custom durations |
| Insights (`InsightsCalculatorTest`) | 6 | empty data, task counts/percentage/overdue, habit consistency (today pending), archived excluded, focus totals + weekly, local-day (not UTC) attribution |
| JSON (`JsonTest`) | 6 | round-trip with escapes/Unicode/emoji, pretty output, malformed documents rejected (20 cases), nesting limit |
| Backup format (`BackupCodecTest`) | 11 | full round-trip, empty backup, whitelisted keys and no secrets, garbage/truncated/wrong-file rejection, newer-version rejection, missing/mistyped/out-of-range fields, broken references, duplicate ids, oversize, forward compatibility |
| Restore merge (`RestorePlannerTest`) | 6 | everything new into empty DB, restoring twice adds nothing, partial overlap, duplicates inside file, identity rules, no mutation |
| PIN/auth (`PinHasherTest`, `ValidatorsTest`, `LockoutAndAutoLockTest`) | 23 | hashing, salts, tamper detection, lockout schedule, auto-lock boundaries, username/PIN validation (incl. Hindi/Chinese names) |
| Palette/theme/time/prefs/links (Milestone 1) | 29 | contrast AAA/AA, no blue/teal/purple hue, DST and time-zone days, storage fallbacks, greeting boundaries, link URLs |

Mutation checks (deliberately broke the code, confirmed tests fail): palette colours, streak logic. Bugs the tests found and I fixed:
username rule rejected Hindi and two-character names; dark accent too faint as text; a test over-matched the `pinned` key.

Other executed checks: `static_checks.py` (passes; also catches missing imports, verified by deliberate removal), `verify_migration_sql.py`
(1→2 SQL on real SQLite: profile data preserved, FK `SET NULL`/`CASCADE`, unique indices, idempotent), colour scan (64 literals, none blue/cyan/teal/purple).

## Written, NOT executed (need Gradle / device)
JVM: `SessionLockManagerTest` (6), `PreferencesDataSourceTest` (3), `PreferenceUseCasesTest` (4).
Instrumented (19): `AccountRepositoryTest` (8, real Keystore), `ProfileDaoTest` (3), `ProductivityDaoTest` (4), `BackupRepositoryTest` (3), `DeveloperLinksSectionTest` (1).

## Not tested at all
Room migration 1→2 *through Room's validator* (no schema JSON exists until the first build; SQL was verified on SQLite only), biometric prompts,
notification delivery, WorkManager timing, Photo Picker, SAF file dialogs, all Compose UI behaviour and layout, accessibility with TalkBack, light/dark rendering,
different screen sizes. Use `docs/DEVICE_TEST_PLAN.md`.
