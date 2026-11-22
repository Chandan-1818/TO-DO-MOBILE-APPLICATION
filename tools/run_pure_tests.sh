#!/usr/bin/env bash
# Compiles and runs the pure-Kotlin (non-Android) unit tests with a standalone kotlinc + JUnit4.
# Compiles only the packages with no Android/coroutines/Hilt dependency: core, domain/model, theme palette.
# This is NOT a replacement for `./gradlew testDebugUnitTest`; it exists so the pure logic can be
# verified in environments without the Android SDK/Gradle. Usage: tools/run_pure_tests.sh <kotlinc_home> <junit_jar> <hamcrest_jar>
set -euo pipefail
KT=${1:-/tmp/kt/kotlinc}; JUNIT=${2:-/usr/share/java/junit4.jar}; HAM=${3:-/usr/share/java/hamcrest-core.jar}
OUT=$(mktemp -d); ROOT=$(cd "$(dirname "$0")/.." && pwd); M=$ROOT/app/src/main/java/com/chandan/master
"$KT/bin/kotlinc" -no-stdlib -nowarn -classpath "$KT/lib/kotlin-stdlib.jar:$JUNIT:$HAM" -d "$OUT" \
  "$M/core" "$M/domain/model" "$M/domain/logic" "$M/domain/backup" $(find "$M/domain/auth" -name "*.kt" ! -name SessionLockManager.kt) "$M/presentation/theme/MasterPalette.kt" "$ROOT/app/src/test/java/com/chandan/master/core" \
  $(find "$ROOT/app/src/test/java/com/chandan/master/domain" -maxdepth 1 -name "*.kt") $(find "$ROOT/app/src/test/java/com/chandan/master/domain/auth" -name "*.kt" ! -name SessionLockManagerTest.kt) "$ROOT/app/src/test/java/com/chandan/master/logic" "$ROOT/app/src/test/java/com/chandan/master/backup" "$ROOT/app/src/test/java/com/chandan/master/presentation"
CLASSES=$(cd "$OUT" && find . -name "*Test.class" | sed 's|^\./||; s|\.class$||; s|/|.|g' | tr '\n' ' ')
java -cp "$OUT:$KT/lib/kotlin-stdlib.jar:$JUNIT:$HAM" org.junit.runner.JUnitCore $CLASSES
