# On-device test plan (tick each; report failures with a screenshot)

**Setup:** install the debug APK on your phone. Do this once on a fresh install.

## Authentication
- [ ] Welcome screen shows samurai art; Create account → username + PIN + confirm; mismatch and short PIN show errors.
- [ ] Optional picture via picker; app opens Home.
- [ ] Close app (swipe away) → reopen → PIN screen; correct PIN opens; wrong PIN shows "attempts left".
- [ ] 5 wrong PINs → 30 s delay counter; correct PIN is refused until it ends.
- [ ] Profile → Account security → turn on biometrics (fingerprint/face prompt) → Lock now → unlock with biometrics; also "Use PIN".
- [ ] Add a new fingerprint in Android settings → biometric unlock turns itself off with a message (expected).
- [ ] Auto-lock: set "Immediately", press Home, return → locked. Set 1 minute, wait, return → locked; return quickly → not locked.
- [ ] Change PIN; old PIN fails, new works. Forgot PIN → Reset shows the warning; confirm → back at Welcome with all data gone.

## Tasks / Calendar
- [ ] Add task with date + time + priority + new category + reminder "10 min before" (allow notification permission).
- [ ] Edit it; complete it; un-complete; delete → Undo restores it.
- [ ] Search by title and description; filters Open/Today/Upcoming/Overdue/Completed; category chip.
- [ ] Task due in 2 minutes with reminder "At due time" → notification arrives (may be a little late). Completing it first → no notification.
- [ ] Calendar: tap a date → shows its tasks; "Add task on this day" pre-fills the date; dots show open/done days.
- [ ] Restart the phone → tasks and pending reminders still there.

## Habits
- [ ] Create daily habit; "Done today"; "Undo today". Create Mon/Wed/Fri habit; weekend days are dimmed.
- [ ] Tap yesterday's dot to correct history; streak and rate change. Future dots do nothing.
- [ ] Habit reminder 2 minutes ahead → notification only if not done today.
- [ ] Archive hides it ("Archived" chip shows it); delete asks for confirmation.

## Focus
- [ ] Start 25 min (or set 1 min via the − button first), pause, resume, stop early (≥1 min is saved as "stopped early").
- [ ] Start a 1-minute session, lock the phone / leave the app → a notification arrives when done; reopen → history shows it.
- [ ] Kill the app during a session → reopen → timer still correct.

## Notes
- [ ] New note, type, press Back → saved. Pin, search, delete → Undo.

## Insights, themes, profile, backup
- [ ] Insights numbers match what you created; empty app shows the empty state.
- [ ] Profile → Appearance: Light/Dark/System and each accent; relaunch keeps them. No blue anywhere (including date/time pickers).
- [ ] Edit profile: change username and picture; restart app → both persist. Restore default picture.
- [ ] GitHub and LinkedIn rows open the browser to the right profiles.
- [ ] Notifications screen: switches persist after restart; turning off "Task reminders" stops task alerts.
- [ ] Backup: export to Downloads; open the JSON and confirm there is no PIN/hash. Add one task. Import the file → preview shows counts → Restore → no duplicates; import again → "0 new".
- [ ] Import a random text file and a truncated copy → clear error, nothing changes.
