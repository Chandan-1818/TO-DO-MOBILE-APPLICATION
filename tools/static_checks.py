#!/usr/bin/env python3
"""Static consistency checks for the Master project. NOT a compiler: it catches the class of errors a
hand-written project is prone to (unresolved own imports, missing resources, bad version-catalog aliases,
unbalanced braces, forbidden features). Run: python3 tools/static_checks.py"""
import re, sys, glob, os, xml.dom.minidom as minidom

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
os.chdir(ROOT)
PKG = "com.chandan.master"
errors, notes = [], []
def err(m): errors.append(m)

kt_files = glob.glob("app/src/**/*.kt", recursive=True)

def strip(src):
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    src = re.sub(r"'(\\.|[^'\\\n])'", "''", src)  # char literals first: '"' must not open a string
    src = re.sub(r'"""(.*?)"""', '""', src, flags=re.S)
    src = re.sub(r'"(\\.|[^"\\\n])*"', '""', src)
    return re.sub(r"//.*", "", src)

# 1. balance
for f in kt_files:
    s = strip(open(f).read())
    for o, c in ("{}", "()", "[]"):
        if s.count(o) != s.count(c):
            err(f"{f}: unbalanced {o}{c} ({s.count(o)} vs {s.count(c)})")

# 2. package declaration matches directory, collect top-level declarations
decl = {}
decl_re = re.compile(r"^(?:@\w+(?:\([^)]*\))?\s+)*(?:(?:public|internal|private|data|sealed|enum|abstract|open|annotation|const|operator|inline)\s+)*(?:class|object|interface|fun|val|var|typealias)\s+(?:<[^>]+>\s*)?(?:[\w.]+\.)?(\w+)", re.M)
for f in kt_files:
    src = open(f).read()
    m = re.search(r"^package\s+([\w.]+)", src, re.M)
    if not m: err(f"{f}: no package"); continue
    rel = os.path.dirname(f).split("/kotlin/")[-1].split("/java/")[-1].replace("/", ".")
    if m.group(1) != rel: err(f"{f}: package {m.group(1)} != directory {rel}")
    for d in decl_re.finditer(strip(src)):
        decl.setdefault(m.group(1), set()).add(d.group(1))

# 3. own imports resolve
for f in kt_files:
    for imp in re.findall(r"^import\s+([\w.]+)$", open(f).read(), re.M):
        if not imp.startswith(PKG + "."): continue
        pkg, _, name = imp.rpartition(".")
        if imp in (PKG + ".R", PKG + ".BuildConfig"): continue
        if name not in decl.get(pkg, set()): err(f"{f}: unresolved import {imp}")

# 3b. scope members are not importable top-level symbols
for f in kt_files:
    for imp in re.findall(r"^import\s+([\w.]+)$", open(f).read(), re.M):
        if re.search(r"\.(matchParentSize|weight|align|alignBy)$", imp) and imp.startswith("androidx.compose.foundation.layout."):
            err(f"{f}: {imp} is a scope member, not importable")

# 4. resources
res = {"plurals": set(), "string": set(), "color": set(), "style": set(), "drawable": set(), "mipmap": set(), "xml": set()}
for f in glob.glob("app/src/main/res/values*/*.xml"):
    d = minidom.parse(f)
    for tag, kind in (("plurals", "plurals"), ("string", "string"), ("color", "color"), ("style", "style")):
        for e in d.getElementsByTagName(tag): res[kind].add(e.getAttribute("name"))
for f in glob.glob("app/src/main/res/*/*"):
    folder, name = f.split("/")[-2], os.path.splitext(os.path.basename(f))[0]
    for kind in ("drawable", "mipmap", "xml"):
        if folder.startswith(kind): res[kind].add(name)
for f in kt_files:
    for kind, name in re.findall(r"\bR\.(string|drawable|mipmap|color|plurals)\.(\w+)", open(f).read()):
        if name not in res[kind]: err(f"{f}: R.{kind}.{name} not found in resources")
for f in glob.glob("app/src/main/**/*.xml", recursive=True):
    for kind, name in re.findall(r"@(string|color|style|drawable|mipmap|xml)/([\w.]+)", open(f).read()):
        name = name.replace(".", "_") if kind == "style" else name
        pool = res[kind] | ({n.replace(".", "_") for n in res["style"]} if kind == "style" else set())
        if name not in pool and name not in {n for n in res[kind]}:
            err(f"{f}: @{kind}/{name} not found")
# every string is used (warn only)
used = set(re.findall(r"R\.string\.(\w+)", "".join(open(f).read() for f in kt_files)))
unused = sorted(res["string"] - used - {"app_name"})
if unused: notes.append("unused strings: " + ", ".join(unused))

# 5. manifest classes
man = open("app/src/main/AndroidManifest.xml").read()
for cls in re.findall(r'android:name="\.(\w+)"', man):
    if cls not in decl.get(PKG, set()): err(f"manifest class .{cls} not declared in {PKG}")

# 6. version catalog aliases
toml = open("gradle/libs.versions.toml").read()
aliases = set(); section = None
for line in toml.splitlines():
    if line.startswith("["): section = line.strip("[]"); continue
    m = re.match(r"([\w-]+)\s*=", line)
    if m and section in ("libraries", "plugins"):
        aliases.add((("plugins." if section == "plugins" else "") + m.group(1)).replace("-", "."))
vers = set(re.findall(r"version\.ref = \"(\w+)\"", toml))
defined_vers = {m.group(1) for m in re.finditer(r"^(\w+)\s*=\s*\"", toml, re.M)}
for v in vers - defined_vers: err(f"catalog: version.ref {v} undefined")
for f in ("build.gradle.kts", "app/build.gradle.kts"):
    for a in re.findall(r"libs\.([\w.]+)", re.sub(r"//.*", "", open(f).read())):
        a = a.rstrip(".")
        if not any(a == x or a.startswith(x + ".") for x in aliases): err(f"{f}: libs.{a} not in catalog")

# 7. forbidden features (product rules)
forbidden = re.compile(r"\b(e-?mail|phone|otp|facebook|google sign|sign.?in with google)\b", re.I)
for f in kt_files + glob.glob("app/src/main/res/values/*.xml") + ["app/src/main/AndroidManifest.xml"]:
    for i, line in enumerate(open(f), 1):
        if forbidden.search(line):
            if re.search(r"(no|never|without|nor|not)\b.*(e-?mail|phone)|(e-?mail|phone).*(no |not |never)", line, re.I) or line.strip().startswith(("//", "*", "/*")):
                notes.append(f"{f}:{i}: mention in a prohibition comment (ok)")
            else:
                err(f"{f}:{i}: forbidden account field/feature: {line.strip()}")

# 8. permissions: none expected in Milestone 1
perms = re.findall(r"uses-permission android:name=\"([^\"]+)\"", man)
if set(perms) - {"android.permission.POST_NOTIFICATIONS"}: err(f"unexpected permissions: {perms}")

# 9. blue-ish colours in XML resources
for f in glob.glob("app/src/main/res/values*/colors.xml"):
    for name, hexv in re.findall(r'name="(\w+)">#([0-9A-Fa-f]{6})<', open(f).read()):
        r, g, b = (int(hexv[i:i+2], 16) for i in (0, 2, 4))
        if b > r + 30 and b > g - 10 and (max(r, g, b) - min(r, g, b)) > 25: err(f"{f}: {name} #{hexv} looks blue")

print(f"Kotlin files: {len(kt_files)}")
for n in notes: print("note:", n)
if errors:
    print(f"\n{len(errors)} problem(s):"); [print(" -", e) for e in errors]; sys.exit(1)
print("static checks: OK")

# ---- 10. missing imports of project symbols (second pass, separate report)
def missing_imports():
    owners = {}
    for pkg, names in decl.items():
        for n in names: owners.setdefault(n, set()).add(pkg)
    problems = []
    for f in kt_files:
        raw = open(f).read()
        m = re.search(r"^package\s+([\w.]+)", raw, re.M)
        if not m: continue
        pkg = m.group(1)
        imports = set(re.findall(r"^import\s+([\w.]+)", raw, re.M))
        imported_names = {i.rsplit(".", 1)[1] for i in imports}
        body = strip(re.sub(r"^(package|import)\s.*$", "", raw, flags=re.M))
        used = set(re.findall(r"\b[A-Za-z_]\w*\b", body))
        for name in used:
            pk = owners.get(name)
            if not pk or pkg in pk or name in imported_names: continue
            if len(pk) == 1 and name[0].isupper() or (len(pk) == 1 and f"{next(iter(pk))}.{name}" not in imports and name in ("formatDate","formatMinuteOfDay","formatDuration","formatClock","openExternalLink","findFragmentActivity","rememberNotificationPermissionRequest","usernameErrorText","pinErrorText","priorityLabel","scheduleSummary","displayTitle")):
                p = next(iter(pk))
                # a fully qualified use like com.x.Name is fine
                if re.search(re.escape(p) + r"\." + name + r"\b", body): continue
                # nested/shadowed declarations in the same file
                if re.search(r"\b(class|object|interface|enum class|fun|val)\s+" + name + r"\b", body): continue
                problems.append(f"{f}: uses {name} (from {p}) without importing it")
    return problems
mi = missing_imports()
if mi:
    print(f"\n{len(mi)} possible missing import(s):"); [print(" -", x) for x in sorted(set(mi))]; sys.exit(1)

# ---- 11. missing LIBRARY imports: names imported somewhere in the project but used elsewhere without import
def missing_library_imports():
    fq = {}
    for f in kt_files:
        for imp in re.findall(r"^import\s+([\w.]+)$", open(f).read(), re.M):
            if imp.startswith(PKG + "."): continue
            fq.setdefault(imp.rsplit(".", 1)[1], set()).add(imp)
    curated = {"padding","fillMaxWidth","fillMaxSize","fillMaxHeight","height","width","clickable","background","border","clip","verticalScroll","rememberScrollState",
               "statusBarsPadding","navigationBarsPadding","imePadding","aspectRatio","alpha","stringResource","pluralStringResource","painterResource","getValue","setValue",
               "remember","mutableStateOf","collectAsStateWithLifecycle","hiltViewModel","semantics","heading","contentDescription","liveRegion","selectable","toggleable","items","rememberSaveable"}
    problems = []
    for f in kt_files:
        raw = open(f).read()
        imports = set(re.findall(r"^import\s+([\w.]+)", raw, re.M))
        imported = {i.rsplit(".", 1)[1] for i in imports}
        wild = {i for i in re.findall(r"^import\s+([\w.]+)\.\*", raw, re.M)}
        body = strip(re.sub(r"^(package|import)\s.*$", "", raw, flags=re.M))
        declared_here = set(re.findall(r"\b(?:class|object|interface|fun|val|var|typealias)\s+(?:<[^>]+>\s*)?(?:[\w.]+\.)?(\w+)", body))
        used = set(re.findall(r"\b[A-Za-z_]\w*\b", body))
        for name in used:
            if name in imported or name in declared_here or name not in fq or len(fq[name]) != 1: continue
            fqn = next(iter(fq[name]))
            if not (name[0].isupper() or name in curated): continue
            if name[0].isupper():
                # real usage = not a member access (Role.Button) and not a named argument
                if not re.search(r"(?<![\w.])" + name + r"\b(?!\s*=[^=])", body): continue
            elif name == "contentDescription":
                if not re.search(r"semantics[^{]*\{[^}]*\bcontentDescription\s*=", body): continue
            else:
                if not re.search(r"(?<![\w])" + name + r"\s*[({]", body) or re.search(r"\b(fun)\s+" + name, body): continue
            if fqn.rsplit(".", 1)[0] in wild: continue
            if name in ("getValue", "setValue"):  # only needed for `by` delegates
                if " by " not in body: continue
            if re.search(re.escape(fqn) + r"\b", body): continue
            # member of a scope receiver (e.g. weight/matchParentSize) or a type parameter: skip single-letter generics
            if len(name) == 1: continue
            problems.append(f"{f}: uses {name} but does not import {fqn}")
    return problems
ml = missing_library_imports()
if ml:
    print(f"\n{len(ml)} possible missing library import(s):"); [print(" -", x.replace('app/src/main/java/com/chandan/master/','')) for x in sorted(set(ml))]; sys.exit(1)

# ---- 12. error classes that the real Kotlin compiler reported on this project (kept as regression checks)
def compiler_class_checks():
    problems = []
    for f in kt_files:
        raw = open(f).read()
        body = strip(raw)
        imports = set(re.findall(r"^import\s+([\w.]+)", raw, re.M))
        wild = set(re.findall(r"^import\s+([\w.]+)\.\*", raw, re.M))
        rel = f.replace("app/src/main/java/com/chandan/master/", "")
        # (a) `.dp` / `.sp` literals need androidx.compose.ui.unit imports
        for unit in ("dp", "sp"):
            if re.search(r"\b\d[\d_]*(\.\d+)?f?\." + unit + r"\b", body) and f"androidx.compose.ui.unit.{unit}" not in imports and "androidx.compose.ui.unit" not in wild:
                problems.append(f"{rel}: uses .{unit} without importing androidx.compose.ui.unit.{unit}")
        # (b) semantics properties used inside semantics { } blocks need imports
        for block in re.findall(r"semantics\s*(?:\([^)]*\))?\s*\{([^{}]*)\}", body):
            for prop in ("role", "heading", "contentDescription", "liveRegion", "stateDescription", "selected", "disabled", "testTag", "text"):
                if re.search(r"(?<![\w.])(?:this\.)?" + prop + r"\s*(=|\()", block) and f"androidx.compose.ui.semantics.{prop}" not in imports and "androidx.compose.ui.semantics" not in wild:
                    problems.append(f"{rel}: semantics block uses '{prop}' without importing androidx.compose.ui.semantics.{prop}")
        # (c) PaddingValues: only (all) | (horizontal, vertical) | (start, top, end, bottom) argument families
        for m in re.finditer(r"\bPaddingValues\s*\(([^()]*(?:\([^()]*\)[^()]*)*)\)", body):
            names = set(re.findall(r"\b(horizontal|vertical|start|top|end|bottom|all)\s*=", m.group(1)))
            fam = {"horizontal": 1, "vertical": 1, "start": 2, "top": 2, "end": 2, "bottom": 2, "all": 3}
            if len({fam[n] for n in names}) > 1:
                problems.append(f"{rel}: PaddingValues mixes argument families {sorted(names)}")
        # (d) `return` inside an expression-bodied function is a compile error
        for m in re.finditer(r"\bfun\b[^{=\n]*(?:\([^)]*\))?[^{=\n]*=\s*", body):
            start = m.end()
            if body[m.start():start].count("(") != body[m.start():start].count(")"): continue
            depth, i, seg_end = 0, start, len(body)
            while i < len(body):
                ch = body[i]
                if ch in "({[": depth += 1
                elif ch in ")}]":
                    depth -= 1
                    if depth < 0: seg_end = i; break
                elif ch == "\n" and depth == 0 and not re.match(r"\s*(\.|\?:|&&|\|\||else|catch|finally)", body[i + 1:i + 20]) and i > start + 1:
                    seg_end = i; break
                i += 1
            seg = body[start:seg_end]
            if re.search(r"\breturn\b(?!@)", seg):
                problems.append(f"{rel}: 'return' inside an expression-bodied function: {body[m.start():m.start()+60].strip()}…")
    return problems
cc = compiler_class_checks()
if cc:
    print(f"\n{len(cc)} compiler-class problem(s):"); [print(" -", x) for x in sorted(set(cc))]; sys.exit(1)
