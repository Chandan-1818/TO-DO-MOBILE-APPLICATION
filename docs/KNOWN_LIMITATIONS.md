# Known limitations (version 0.3.1)

1. **I could not compile or run it.** Your own build found 6 errors (all fixed). Compiler stages after those may still report errors I cannot see; send the output and they get fixed. UI/behaviour bugs on first run are possible.
2. **No APK was produced** (no Android SDK/Maven access in the build environment).
3. **Biometrics, notifications, Keystore, Photo Picker, file dialogs: unverified on hardware.**
4. **Reminders are best-effort.** They use WorkManager (survives reboot); Android may delay them in Doze/battery saver. They are not exact alarms.
   Focus-timer completion alerts have the same caveat. Reminders are recomputed on every app launch and on time-zone/clock changes.
5. **Time zones:** reminders are stored as absolute times and recomputed on a time-zone broadcast or the next launch; a reminder may fire at the old zone's time until then.
6. **Database is not encrypted at rest.** PIN data is protected (salted PBKDF2 + Keystore AES-GCM); tasks, notes etc. rely on Android's app sandbox. A rooted device can read them.
7. **4-digit PINs are guessable** by someone who can edit the stored attempt counters (root). Lockout counters use the wall clock (clamped to 30 min max).
8. **Backups are plain JSON** (no passphrase). They hold no secrets but do hold your notes and tasks: store them privately.
   Restore merges and never deletes; to replace data, reset Master first. The profile picture, PIN and security settings are not part of a backup.
   A backup restores into an existing account (create an account on the new install first).
9. **Restore identity rules** (to avoid duplicates): tasks by title + due date/time + creation time; habits by name + creation time; notes by title + creation time;
   sessions by start time; categories by name. Editing a record's title after backup makes it look "new" on restore.
10. **Undo after delete** re-creates the task/note (new id, creation time reset).
11. Deleting a category keeps its tasks (they become uncategorised); there is no "merge categories" action.
12. **Habits** support only simple done/not-done (no counts or durations); weekly schedule is by weekday.
13. **Calendar** has month view only. **Insights** use a table, not charts, by design.
14. Welcome and splash screens are always dark (the artwork is black ink), so status-bar icons there follow the theme.
15. The lock screen is a Compose overlay (hidden from screen readers while locked), not a separate secure window; screenshots are not blocked.
16. Android Auto Backup is disabled, so uninstalling loses the data unless you exported a backup first.
