/**
 * Samples the running client's session readouts (CPS, combo, reach, speed,
 * memory, ...) so a change can be measured instead of eyeballed.
 *
 *   node scripts/harness/sample-session.mjs <run-log> [seconds] [field...]
 *
 * Fields are dotted paths, e.g. `cps.left`, `speed`, `combo`, `memory.percent`.
 * With none given it prints one whole sample, then summarises a default set.
 *
 * Prints min/mean/max per field, which is what makes "standing still" versus
 * "holding W" a measurement rather than an impression.
 *
 * Polls GET /api/v1/client/session rather than the WebSocket. The socket is the
 * live path the HUD uses, but its auth only accepts the `lb_auth` cookie, never
 * the `lb_code` parameter, so a socket opened with the code from the log
 * connects and then silently receives nothing at all - which reads exactly like
 * a broken feature. Both paths call the same SessionStats.snapshot().
 */
import fs from "fs";

const logPath = process.argv[2];
const seconds = Number(process.argv[3] || 5);
const fields = process.argv.slice(4);

if (!logPath || !fs.existsSync(logPath)) {
  console.error("usage: node scripts/harness/sample-session.mjs <run-log> [seconds] [field...]");
  process.exit(2);
}

const log = fs.readFileSync(logPath, "utf8");
const ports = [...log.matchAll(/127\.0\.0\.1:(\d+)/g)].map((m) => m[1]);
const codes = [...log.matchAll(/lb_code=([A-Za-z0-9]+)/g)].map((m) => m[1]);

if (!ports.length || !codes.length) {
  console.error("could not find the interop port / session code in the log - is the client up?");
  process.exit(2);
}

const url = `http://127.0.0.1:${ports[ports.length - 1]}/api/v1/client/session?lb_code=${codes[0]}`;
const DEFAULT_FIELDS = ["fps", "cps.left", "cps.right", "combo", "reach", "speed", "ping", "memory.percent"];
const watch = fields.length ? fields : DEFAULT_FIELDS;

const resolve = (obj, path) => path.split(".").reduce((o, k) => (o == null ? undefined : o[k]), obj);

const INTERVAL_MS = 100;
const samples = [];
const deadline = Date.now() + seconds * 1000;

while (Date.now() < deadline) {
  try {
    const res = await fetch(url, { signal: AbortSignal.timeout(3000) });
    if (!res.ok) {
      console.error(`HTTP ${res.status} from ${url}`);
      process.exit(2);
    }
    samples.push(await res.json());
  } catch (e) {
    console.error(`could not reach the client at ${url}\n  ${e.message}`);
    process.exit(2);
  }
  await new Promise((r) => setTimeout(r, INTERVAL_MS));
}

if (!samples.length) {
  console.error("no samples collected");
  process.exit(1);
}

if (!fields.length) {
  console.log("one full sample:");
  console.log(JSON.stringify(samples[0], null, 2));
  console.log();
}

console.log(`${samples.length} samples over ${seconds}s\n`);
console.log("field".padEnd(16) + "min".padStart(10) + "mean".padStart(10) + "max".padStart(10));

for (const f of watch) {
  const values = samples.map((s) => resolve(s, f)).filter((v) => typeof v === "number");
  if (!values.length) {
    console.log(f.padEnd(16) + "(not a number)".padStart(30));
    continue;
  }
  const min = Math.min(...values);
  const max = Math.max(...values);
  const mean = values.reduce((a, b) => a + b, 0) / values.length;
  const fmt = (n) => (Number.isInteger(n) ? String(n) : n.toFixed(2));
  console.log(f.padEnd(16) + fmt(min).padStart(10) + fmt(mean).padStart(10) + fmt(max).padStart(10));
}
