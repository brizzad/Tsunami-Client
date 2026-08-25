/**
 * Checks the RUNNING client's module registry against scripts/keep.txt.
 *
 * The client exposes its state over a local REST interop server. That is the
 * authoritative answer to "did every module we kept actually register and
 * enable", rather than reading the config file or squinting at the HUD.
 *
 * Start the client (gradlew runClient), then:
 *
 *   node scripts/harness/verify-modules.mjs <path-to-run-log>
 *
 * The port and session code are scraped from the log, since both change every
 * launch. Exits non-zero if a kept module is missing or the client reports a
 * module we did not expect.
 *
 * Note: modules declared in source but never registered in ModuleManager will
 * be reported missing. BetterTitle is one such case and is upstream's own
 * omission, not a fork regression - it is listed in EXPECT_UNREGISTERED.
 */
import fs from "fs";
import path from "path";

const ROOT = process.cwd();
const logPath = process.argv[2];

if (!logPath || !fs.existsSync(logPath)) {
  console.error("usage: node scripts/harness/verify-modules.mjs <run-log>");
  process.exit(2);
}

/** Declared in source, never wired into ModuleManager upstream either. */
const EXPECT_UNREGISTERED = new Set(["BetterTitle"]);

const log = fs.readFileSync(logPath, "utf8");
const ports = [...log.matchAll(/127\.0\.0\.1:(\d+)/g)].map((m) => m[1]);
const codes = [...log.matchAll(/lb_code=([A-Za-z0-9]+)/g)].map((m) => m[1]);

if (!ports.length || !codes.length) {
  console.error("could not find the interop port / session code in the log - is the client up?");
  process.exit(2);
}

const port = ports[ports.length - 1];
const code = codes[0];
const url = `http://127.0.0.1:${port}/api/v1/client/modules?lb_code=${code}`;

const keep = fs
  .readFileSync(path.join(ROOT, "scripts/keep.txt"), "utf8")
  .split("\n")
  .map((s) => s.trim())
  .filter(Boolean);

let reported;
try {
  const res = await fetch(url, { signal: AbortSignal.timeout(10000) });
  if (!res.ok) throw new Error("HTTP " + res.status);
  reported = await res.json();
} catch (e) {
  console.error(`could not reach the client at ${url}\n  ${e.message}`);
  process.exit(2);
}

// keep.txt holds Kotlin class names; the client registers display names, and
// the two differ in case for a few (ClickGui/ClickGUI, Hud/HUD, NoFov/NoFOV).
// Compare case-insensitively so the harness does not cry wolf over casing.
const norm = (s) => s.toLowerCase();
const live = new Map(reported.map((m) => [norm(m.name), m]));
const keepNorm = new Set(keep.map(norm));
const expected = keep.filter((k) => !EXPECT_UNREGISTERED.has(k));

const missing = expected.filter((k) => !live.has(norm(k)));
const unexpected = reported.map((m) => m.name).filter((n) => !keepNorm.has(norm(n)));
const noDescription = reported.filter((m) => !m.description).map((m) => m.name);

const byCat = {};
for (const m of reported) (byCat[m.category] ||= []).push(m.name);

console.log(`client reports ${reported.length} modules (${reported.filter((m) => m.enabled).length} enabled)`);
for (const c of Object.keys(byCat).sort()) {
  console.log(`  ${c.padEnd(9)} ${String(byCat[c].length).padStart(2)}`);
}

let bad = false;
if (missing.length) {
  bad = true;
  console.log(`\nMISSING - kept but not registered: ${missing.join(", ")}`);
}
if (unexpected.length) {
  bad = true;
  console.log(`\nUNEXPECTED - registered but not in keep.txt: ${unexpected.join(", ")}`);
}
if (noDescription.length) {
  console.log(`\nwarning - no description: ${noDescription.join(", ")}`);
}

if (!bad) console.log("\nEvery kept module is registered, and nothing extra.");
process.exit(bad ? 1 : 0);
