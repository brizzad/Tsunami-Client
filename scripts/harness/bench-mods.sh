#!/usr/bin/env bash
# A/B of extra performance mods against the dev client, ABBA-ordered so that
# anything drifting across the session cannot line up with the variable.
#
# The mods are dropped into run/mods rather than added to build.gradle.kts:
# Fabric loads that directory, nothing in the repo has to change, and swapping
# sides is a file move.
#
# Both sides run the launcher's stock G1 flags, so the JVM is identical across
# the comparison and closer to what ships than Loom's default ZGC.
set -u
CLIENT="$HOME/Documents/Tsunami-Client"
LAUNCHER="$HOME/Documents/Tsunami"
cd "$CLIENT" || exit 1
SP="$1"; WORLD="$2"; PAIRS="$3"

MODS_DIR="$CLIENT/run/mods"
JARS="$SP/modjars"
mkdir -p "$MODS_DIR"

STOCK=$(awk '/JvmTuning::Stock =>/,/^            }$/' "$LAUNCHER/src-tauri/src/minecraft/version.rs" \
  | grep -oE '"-X[^"]*"|format!\("-X[^"]*"' | sed 's/format!(//; s/"//g; s/{}M/4096M/' | tr '\n' ' ')
[ -z "$STOCK" ] && { echo "could not extract stock flags"; exit 1; }

set_side() {
  rm -f "$MODS_DIR"/*.jar
  if [ "$1" = "mods" ]; then
    cp "$JARS"/*.jar "$MODS_DIR"/
  fi
  echo "  run/mods now: $(ls -1 "$MODS_DIR" | tr '\n' ' ')"
}

ready() {
  local log="$1" port code
  port=$(grep -aoE "127\.0\.0\.1:[0-9]+" "$log" 2>/dev/null | tail -1 | cut -d: -f2)
  code=$(grep -aoE "lb_code=[A-Za-z0-9]+" "$log" 2>/dev/null | tail -1 | cut -d= -f2)
  [ -z "$port" ] || [ -z "$code" ] && return 1
  curl -s -m 5 -o /dev/null -f "http://127.0.0.1:$port/api/v1/client/info?lb_code=$code"
}

kill_client() {
  local log="$1" port code
  port=$(grep -aoE "127\.0\.0\.1:[0-9]+" "$log" 2>/dev/null | tail -1 | cut -d: -f2)
  code=$(grep -aoE "lb_code=[A-Za-z0-9]+" "$log" 2>/dev/null | tail -1 | cut -d= -f2)
  if [ -n "$port" ] && [ -n "$code" ]; then
    curl -s -m 10 -X POST -o /dev/null "http://127.0.0.1:$port/api/v1/client/exit?lb_code=$code"
  else
    powershell -Command "Get-Process java -ErrorAction SilentlyContinue | Where-Object { \$_.MainWindowTitle -like '*Tsunami*' } | Stop-Process -Force" 2>/dev/null
  fi
}

# Bounded replacement for a bare `wait`: a client that never comes up, or that
# fails at loader init, otherwise leaves Gradle holding the suite open (one such
# run sat for 14h44m). Kills the dev client by command line so the long-lived
# Gradle daemon is never touched.
reap() {
  local gpid="$1" waited=0
  while kill -0 "$gpid" 2>/dev/null; do
    if [ $waited -ge 90 ]; then
      echo "  reaping stuck run after ${waited}s"
      powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -match 'KnotClient|devlaunchinjector' } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Force -ErrorAction SilentlyContinue }" >/dev/null 2>&1
      kill -9 "$gpid" 2>/dev/null
      break
    fi
    sleep 3; waited=$((waited+3))
  done
  wait "$gpid" 2>/dev/null
  return 0
}

run_one() {
  local variant="$1" idx="$2"
  local log="$SP/logs/mods-$variant-$idx.log"
  echo "=== $variant #$idx ==="
  set_side "$variant"

  ./gradlew runClient --init-script scripts/harness/bench-jvm.init.gradle \
      -PbenchJvmArgs="$STOCK" > "$log" 2>&1 &
  local gpid=$!

  local waited=0
  until ready "$log"; do
    sleep 5; waited=$((waited+5))
    [ $waited -gt 420 ] && { echo "  never came up"; break; }
    kill -0 $gpid 2>/dev/null || { echo "  gradle exited"; break; }
  done

  if ready "$log"; then
    # Prove the mods actually loaded rather than trusting the file copy.
    local loaded
    loaded=$(grep -aoE "moreculling|sodium-extra|badoptimizations|ixeris|cloth-config" "$log" | sort -u | tr '\n' ' ')
    echo "  loader reports: ${loaded:-<none>}"
    if [ "$variant" = "mods" ] && [ -z "$loaded" ]; then
      echo "  MODS DID NOT LOAD - skipping run"
      kill_client "$log"; reap $gpid; return
    fi

    node scripts/harness/fps-bench.mjs --log "$log" --world "$WORLD" \
      --warmup 25 --sample 60 --label "$variant" --variant "$variant" \
      --out "$SP/runs/mods-$variant-$idx.json" || echo "  refused"
  fi

  kill_client "$log"
  reap $gpid
  sleep 5
}

for i in $(seq 1 "$PAIRS"); do
  if [ $((i % 2)) -eq 1 ]; then
    run_one base "$i"; run_one mods "$i"
  else
    run_one mods "$i"; run_one base "$i"
  fi
done
rm -f "$MODS_DIR"/*.jar
echo "=== done (run/mods emptied) ==="
