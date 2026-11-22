#!/usr/bin/env python3
"""Static Dagger/Hilt graph check: every constructor dependency of an @Inject class / @HiltViewModel must have exactly one
provider (@Inject constructor, @Provides, @Binds), qualifiers must match, and there must be no dependency cycles.
Approximates what the Dagger annotation processor validates; it is NOT a replacement for the real build."""
import re, glob, sys, os
root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
files = glob.glob(f"{root}/app/src/main/java/**/*.kt", recursive=True)
def strip(s): return re.sub(r"//[^\n]*", "", re.sub(r"/\*.*?\*/", "", s, flags=re.S))

def split_params(txt):
    out, depth, cur = [], 0, ""
    for ch in txt:
        if ch in "<([": depth += 1
        elif ch in ">)]": depth -= 1
        if ch == "," and depth == 0: out.append(cur); cur = ""
        else: cur += ch
    if cur.strip(): out.append(cur)
    return out

def parse_param(p):
    p = p.strip()
    qual = re.findall(r"@(\w+)", p)
    m = re.search(r"(?:val|var)?\s*(\w+)\s*:\s*([\w.<>?, ]+?)(?:\s*=.*)?$", re.sub(r"@\w+(\([^)]*\))?\s*", "", p).strip(), re.S)
    if not m: return None
    return m.group(2).strip().replace("?", ""), qual

provided = {}      # (type, qualifier) -> [source]
needs = {}         # class -> [(type, qualifier)]
bound_impls = {}
def add(t, q, src): provided.setdefault((t, q), []).append(src)

for f in files:
    s = strip(open(f).read())
    for m in re.finditer(r"((?:@\w+(?:\([^)]*\))?\s*)*)\bclass\s+(\w+)\s*(?:@Inject\s+)?constructor\s*\(", s):
        pass
    # injectable classes
    for m in re.finditer(r"((?:@\w+(?:\([^)]*\))?\s*)*)class\s+(\w+)\s*(?:@\w+\s+)*constructor\s*\(", s):
        head = s[max(0, m.start() - 0):m.end()]
        if "@Inject" not in s[m.start():m.end()] and "@Inject constructor" not in s[m.start():m.end()]: continue
        i = m.end(); depth = 1; j = i
        while depth: 
            depth += (s[j] == "(") - (s[j] == ")"); j += 1
        params = [parse_param(p) for p in split_params(s[i:j - 1])]
        cls = m.group(2)
        needs[cls] = [x for x in params if x]
        add(cls, None, f"@Inject {cls}")
    for m in re.finditer(r"@Provides\b(?:\s*@\w+)*\s*fun\s+\w+\s*\(([^)]*)\)\s*(?::\s*([\w.<>?]+))?", s):
        quals = re.findall(r"@(\w+)", s[m.start():m.end()].split("fun")[0])
        q = next((x for x in quals if x in ("SettingsStore", "CredentialsStore")), None)
        ret = m.group(2)
        if not ret:
            rt = re.search(r"fun\s+\w+\s*\([^)]*\)\s*=\s*(\w+)", s[m.start():m.start() + 250])
            ret = rt.group(1) if rt else None
        if ret: add(ret.replace("?", ""), q, f"@Provides in {os.path.basename(f)}")
        for p in split_params(m.group(1)):
            pp = parse_param(p)
            if pp: needs.setdefault(f"provider:{ret}", []).append(pp)
    for m in re.finditer(r"@Binds\s+abstract\s+fun\s+\w+\(\s*\w+:\s*(\w+)\s*\):\s*(\w+)", s):
        add(m.group(2), None, f"@Binds {m.group(1)}"); needs.setdefault(f"binds:{m.group(2)}", []).append((m.group(1), []))

builtin = {"Context", "SavedStateHandle", "WorkerParameters"}
errors = []
for owner, deps in needs.items():
    for t, quals in deps:
        base = re.sub(r"<.*>", "", t).split(".")[-1]
        q = next((x for x in quals if x in ("SettingsStore", "CredentialsStore")), None)
        if base in builtin or "ApplicationContext" in quals: continue
        key = (t if t in {k[0] for k in provided} else base, q)
        candidates = provided.get(key) or provided.get((base, q))
        if not candidates: errors.append(f"{owner}: no provider for {t}" + (f" @{q}" if q else ""))
        elif len(candidates) > 1: errors.append(f"{owner}: {len(candidates)} providers for {t}: {candidates}")
# duplicate bindings for a type
for key, srcs in provided.items():
    if len(srcs) > 1: errors.append(f"duplicate bindings for {key}: {srcs}")
# cycles
graph = {c: [re.sub(r"<.*>", "", t).split(".")[-1] for t, _ in d] for c, d in needs.items() if not c.startswith(("provider:", "binds:"))}
impl_of = {}
for owner, deps in needs.items():
    if owner.startswith("binds:"): impl_of[owner[6:]] = deps[0][0]
def resolve(n): return impl_of.get(n, n)
def dfs(n, path):
    n = resolve(n)
    if n in path: errors.append("dependency cycle: " + " -> ".join(path[path.index(n):] + [n])); return
    for d in graph.get(n, []): dfs(d, path + [n])
for c in list(graph): dfs(c, [])
print(f"injectable classes: {sum(1 for k in needs if not k.startswith(('provider:','binds:')))}, bindings: {len(provided)}")
if errors:
    print("PROBLEMS:"); [print(" -", e) for e in sorted(set(errors))]; sys.exit(1)
print("DI graph check: OK")
