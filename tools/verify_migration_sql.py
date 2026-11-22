#!/usr/bin/env python3
"""Runs MasterMigrations.V2_STATEMENTS against real SQLite on top of a version-1 database that already holds a profile,
and checks data survival, foreign-key behaviour, unique indices and idempotence.
It proves the SQL is valid and behaves as designed; it does NOT prove Room's schema validator accepts it (needs a device test)."""
import re, sqlite3, sys, os
root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
src = open(f"{root}/app/src/main/java/com/chandan/master/data/local/db/MasterMigrations.kt").read()
stmts = re.findall(r'^\s+"(CREATE [^"]*)",$', src, re.M)
assert len(stmts) == 10, len(stmts)
db = sqlite3.connect(":memory:"); db.execute("PRAGMA foreign_keys=ON")
db.execute("CREATE TABLE `profiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `username` TEXT NOT NULL, `avatar_uri` TEXT, `is_guest` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)")
db.execute("INSERT INTO profiles(username,is_guest,created_at,updated_at) VALUES('ronin',0,1,1)")
for s in stmts: db.execute(s)
assert db.execute("SELECT username FROM profiles").fetchall() == [("ronin",)], "profile lost"
db.execute("INSERT INTO categories(name) VALUES('Work')")
db.execute("INSERT INTO tasks(title,description,priority,category_id,completed,created_at,updated_at) VALUES('a','',1,1,0,1,1)")
db.execute("DELETE FROM categories WHERE id=1")
assert db.execute("SELECT category_id FROM tasks").fetchone() == (None,), "SET NULL failed"
db.execute("INSERT INTO habits(name,description,schedule_mask,start_day,archived,created_at) VALUES('h','',127,1,0,1)")
db.execute("INSERT INTO habit_completions(habit_id,day) VALUES(1,5)")
try: db.execute("INSERT INTO habit_completions(habit_id,day) VALUES(1,5)"); sys.exit("duplicate completion allowed")
except sqlite3.IntegrityError: pass
db.execute("INSERT INTO focus_sessions(started_at,planned_seconds,actual_seconds,completed) VALUES(1,1500,1500,1)")
try: db.execute("INSERT INTO focus_sessions(started_at,planned_seconds,actual_seconds,completed) VALUES(1,1500,10,0)"); sys.exit("duplicate session allowed")
except sqlite3.IntegrityError: pass
db.execute("DELETE FROM habits WHERE id=1")
assert db.execute("SELECT count(*) FROM habit_completions").fetchone() == (0,), "cascade failed"
for s in stmts: db.execute(s)  # idempotent
print("migration SQL OK:", len(stmts), "statements; data preserved; FK SET NULL / CASCADE; unique indices; idempotent")
