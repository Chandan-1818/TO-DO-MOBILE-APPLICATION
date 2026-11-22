# Architecture

**Pattern:** MVVM + Clean Architecture in a single Gradle module with enforced package boundaries.

```
presentation (Compose screens, ViewModels)
      │ calls                       ▲ observes StateFlow
      ▼                             │
domain (use cases, models, repository interfaces)   ← pure Kotlin where possible
      ▲ implements
      │
data (Room DAOs/entities, DataStore, repository implementations)
```

* **Single source of truth:** Room for structured data, DataStore for preferences. UI state is derived from them with
  `stateIn(WhileSubscribed(5s))`; nothing important lives only in UI state.
* **Time:** all features must use `DateTimeProvider` (never `LocalDate.now()` directly) so tests can control the clock
  and zone, and so device time-zone changes are honoured (the zone is read on each call).
* **DI:** Hilt. `DatabaseModule`, `DataStoreModule`, `RepositoryModule` (binds interfaces, provides the clock).
* **Database policy:** `exportSchema=true`, explicit `MasterMigrations.ALL`, **no destructive fallback**. Every schema
  change must add a `Migration` and a migration test.
* **Credentials:** the PIN verifier, salt, iteration count, PIN length and failed-attempt counters live in a separate DataStore file
  (`master_credentials`), never in Room. `domain/auth` holds the pure logic (`PinHasher`, `LockoutPolicy`/`AttemptTracker`, `AutoLockPolicy`,
  validators); `data/security` holds the Android parts (`KeystoreCrypto`, `CredentialStore`, `AndroidBiometricKeyProvider`).
  An account is defined by its profile row; missing/corrupt credentials lead to the lock screen's warned reset, never to a silent re-creation.
* **Lock/gating:** `SessionLockManager` (starts locked) + `AuthViewModel` (activity scoped) -> `MasterAppRoot` shows `AuthFlow` (no account), or the app with `LockScreen` drawn over it.
* **Theme:** `MasterPalette` (plain ARGB tokens, unit-tested for WCAG contrast and hue rules) → `Theme.kt` maps every
  Material 3 slot explicitly so no default purple/blue can leak in. Accent is a user preference (5 curated warm accents).
* **Navigation:** `Routes`, `TopLevelDestination` (4 tabs), `MasterNavHost`, `MasterAppRoot` (bottom bar only on top-level routes;
  Home is the root of the tab back stack; Splash is popped after launch).

## Phase 2/3 additions
* **Database v2:** `categories`, `tasks`, `habits`, `habit_completions` (unique per habit/day, cascade delete), `focus_sessions` (unique start time) and `notes`.
  Migration 1→2 is explicit SQL (`MasterMigrations`), verified on SQLite by `tools/verify_migration_sql.py`; never destructive.
* **Pure logic in `domain/logic`:** schedules/streaks, task filtering, reminder times, focus state machine, insights. ViewModels only combine repository flows with these functions.
* **Reminders:** `WorkManagerReminderScheduler` keeps one unique job per task/habit (replaced on edit, cancelled on complete/delete/reset); workers get their dependencies from a Hilt entry point;
  everything is recomputed at launch and on time-zone/clock changes.
* **Focus:** `FocusTimerController` persists the timer (`FocusStateStore`), schedules a finishing job, records sessions idempotently (unique start time) and notifies.
* **Backup:** dependency-free `Json` + `BackupCodec` (strict validation) + `RestorePlanner` (merge/dedupe). `BackupRepositoryImpl` applies a plan inside one Room transaction.
