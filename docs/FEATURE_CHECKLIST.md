# Feature checklist (version 0.3.1)

Legend: **Impl** = code written · **Tested** = executed here (JVM) · **Device** = still needs a real-phone check · ✗ = not implemented.

| Feature | Impl | Tested here | Device-verified |
|---|---|---|---|
| Local account (username + PIN, no email/phone/OTP/social) | ✓ | validators, hasher | ✗ not yet |
| PIN login, wrong-PIN handling, escalating delays | ✓ | policy + hasher | ✗ |
| Biometric unlock with PIN fallback | ✓ | – | ✗ **must test on phone** |
| App lock (auto-lock delay, lock now, cold start) | ✓ | AutoLockPolicy | ✗ |
| Forgotten PIN → warned reset | ✓ | – | ✗ |
| Profile: username edit, picture (Photo Picker, private storage), default samurai avatar | ✓ | – | ✗ |
| Developer links GitHub + LinkedIn (last section of Profile) | ✓ | URL values | ✗ |
| Theme dark/light/system + accent palette, persisted | ✓ | palette rules, storage | ✗ |
| Home dashboard from real data | ✓ | – | ✗ |
| Tasks: create/edit/delete(undo)/complete, due date+time, priority, category, search, filters | ✓ | TaskRules | ✗ |
| Task reminders | ✓ | trigger calculation | ✗ |
| Habits: create/edit/delete/archive, schedule, check-in/undo, any-day correction, streaks, weekly progress, reminder | ✓ | StreakCalculator, reminders | ✗ |
| Focus: Pomodoro, custom minutes, start/pause/resume/stop/discard, history, background-safe | ✓ | FocusTimerLogic | ✗ |
| Calendar: month view, day list, add task for date | ✓ | – | ✗ |
| Notes: create/edit/pin/search/delete(undo), autosave | ✓ | – | ✗ |
| Notifications: channels, permission request, per-type switches, private lock-screen text | ✓ | – | ✗ |
| Insights: totals, %, consistency, streaks, focus, 7-day table | ✓ | InsightsCalculator | ✗ |
| Backup JSON via SAF, validate, preview + confirm, merge without duplicates, no secrets | ✓ | codec, planner | ✗ |
| Database v2 migration (no destructive fallback) | ✓ | SQL on SQLite | ✗ Room validation unverified |
| Debug APK / Release APK | ✗ not built | – | – |
| Encrypted backup, widgets, voice, guest mode, projects, advanced analytics | ✗ (removed from scope by you) | – | – |
| Categories: create, assign, filter, rename, delete (tasks kept, become uncategorised) | ✓ | – | ✗ |
| About screen (version, privacy, acknowledgements, GitHub + LinkedIn) | ✓ | – | ✗ |
