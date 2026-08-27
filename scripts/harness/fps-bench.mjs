/*
 * This file is part of Tsunami, a fork of LiquidBounce
 * (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2026 Tsunami contributors
 *
 * Tsunami is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tsunami is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tsunami. If not, see <https://www.gnu.org/licenses/>.
 */

/**
 * FPS benchmark.
 *
 * Exists because "this mod makes the game faster" was once accepted without a
 * number behind it, and the change went the other way. A performance claim
 * that cannot be reproduced is a guess, and this is the thing that turns it
 * into a measurement.
 *
 *   node scripts/harness/fps-bench.mjs --log <run.log> --world <levelId> --label before
 *   node scripts/harness/fps-bench.mjs --compare runs/before.json runs/after.json
 *
 * ## How it measures
 *
 * The client is asked for its own frame counter over the interop REST server,
 * so the run needs no keyboard, no mouse and no screenshots. That matters more
 * than it sounds: every input-driven approach nudges the camera, and a camera
 * that moved between two runs has already invalidated both of them.
 *
 * The world is loaded through `POST /client/worlds/join`, which puts the player
 * at the position and camera angle stored in `level.dat` - identical on every
 * run, for free. Nothing has to steer the player there.
 *
 * ## What the numbers actually are
 *
 * `mc.fps` is Minecraft's frames-in-the-last-second counter, updated once a
 * second. Polling it faster returns the same value again, so the real sample
 * size is the number of seconds sampled, not the number of requests.
 *
 * That has a consequence worth being blunt about: **this cannot produce a true
 * 1% low.** A 1% low is a property of frame times, and frame times are not
 * exposed here. `worstSecond` below is the lowest one-second average, which is
 * a coarser stutter proxy. It catches a mod that stalls for a whole second. It
 * will not catch one that drops a single frame every few seconds. Do not quote
 * it as a 1% low.
 *
 * ## The trap this is built around
 *
 * A benchmark whose scene is not the bottleneck measures noise. On an empty
 * superflat platform this client runs at ~350fps, where the limit is frame
 * submission rather than anything Sodium, Lithium or EntityCulling touches -
 * and every change, good or bad, lands inside the run-to-run spread. Point
 * `--world` at somewhere that actually costs something to draw.
 *
 * `--compare` therefore refuses to compare runs whose *conditions* differ
 * (world, render distance, simulation distance, window size) while reporting
 * the mod-set difference as the change under test, since that is the variable
 * you meant to change.
 */

import fs from "fs";
import path from "path";
import { execFileSync } from "child_process";
import { fileURLToPath } from "url";

const RESULT_VERSION = 1;

/**
 * Minecraft throttles to 30fps in several situations, and a benchmark that does
 * not check will report a clean, low-variance 30 and call it a result - the
 * same silent false pass the rest of this harness exists to prevent.
 *
 * The one that actually bit, and it is a nasty one: since 1.21.2 the
 * `inactivityFpsLimit` option defaults to `"afk"`, which drops the game to
 * 30fps after roughly a minute without keyboard or mouse input. This benchmark
 * deliberately sends no input, so that limiter fires on every run longer than a
 * minute. The window was focused and the world was unpaused the entire time.
 * `preflight()` refuses to start until it is set to `"minimized"`.
 */
const THROTTLE_CEILING = 40;

// ---------------------------------------------------------------- arg parsing

function parseArgs(argv) {
  const args = { warmup: 25, sample: 60, poll: 500, label: "run" };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === "--compare") { args.compare = argv.slice(i + 1, i + 3); i += 2; continue; }
    if (!a.startsWith("--")) continue;
    const key = a.slice(2);
    const val = argv[i + 1];
    if (val === undefined || val.startsWith("--")) { args[key] = true; continue; }
    args[key] = /^\d+$/.test(val) ? Number(val) : val;
    i++;
  }
  return args;
}

// ------------------------------------------------------------------ interop

/** The port and session code change every launch, so both come from the log. */
function interop(logPath) {
  const log = fs.readFileSync(logPath, "latin1");
  const ports = [...log.matchAll(/127\.0\.0\.1:(\d+)/g)].map((m) => m[1]);
  const codes = [...log.matchAll(/lb_code=([A-Za-z0-9]+)/g)].map((m) => m[1]);
  if (!ports.length || !codes.length) {
    die("could not find the interop port / session code in the log - is the client up?");
  }
  const port = ports.at(-1);
  const code = codes.at(-1);
  return {
    log,
    url: (p) => `http://127.0.0.1:${port}/api/v1/${p}${p.includes("?") ? "&" : "?"}lb_code=${code}`,
  };
}

async function get(io, p) {
  const res = await fetch(io.url(p));
  if (!res.ok) throw new Error(`GET ${p} -> ${res.status}`);
  return res.json();
}

async function post(io, p, body) {
  const res = await fetch(io.url(p), {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`POST ${p} -> ${res.status}`);
  return res;
}

// -------------------------------------------------------------- fingerprint

function readOptions(info) {
  const optionsPath = path.join(info.gameDir, "options.txt");
  const options = {};
  if (!fs.existsSync(optionsPath)) return options;
  for (const line of fs.readFileSync(optionsPath, "utf8").split("\n")) {
    const idx = line.indexOf(":");
    if (idx > 0) options[line.slice(0, idx).trim()] = line.slice(idx + 1).trim();
  }
  return options;
}

/**
 * Everything that has to match for two runs to be comparable, plus the mod set,
 * which is the thing that is supposed to differ.
 */
function fingerprint(io, info, world) {
  const mods = [...io.log.matchAll(/^\t- ([a-z0-9_-]+) (\S+)/gm)].map((m) => `${m[1]} ${m[2]}`);

  const all = readOptions(info);
  const options = {};
  for (const k of ["renderDistance", "simulationDistance", "graphicsMode", "maxFps",
                   "enableVsync", "entityDistanceScaling", "particles", "biomeBlendRadius",
                   "cloudRange", "ao"]) {
    if (k in all) options[k] = all[k];
  }

  const grab = (re) => (io.log.match(re)?.[1] ?? "").trim();

  return {
    world,
    // Free-text note for a change the client cannot report about itself - JVM
    // flags being the case this exists for, since a process cannot be asked
    // which flags it was started with over a REST API. Deliberately not in
    // MUST_MATCH: this is the thing under test, so it is *supposed* to differ.
    variant: args.variant ?? null,
    clientVersion: info.clientVersion,
    options,
    gpu: grab(/GPU: (.+?)(?:\r|\n)/),
    java: grab(/Java: (\S+)/),
    // Window size changes pixel count, which changes FPS more than most mods do.
    display: grab(/Display: (\d+x\d+)/),
    mods: mods.sort(),
  };
}

// ------------------------------------------------------------------- stats

function stats(fps) {
  const sorted = [...fps].sort((a, b) => a - b);
  const at = (p) => sorted[Math.min(sorted.length - 1, Math.floor(p * sorted.length))];
  const mean = fps.reduce((a, b) => a + b, 0) / fps.length;
  const variance = fps.reduce((a, b) => a + (b - mean) ** 2, 0) / fps.length;
  return {
    seconds: fps.length,
    mean: +mean.toFixed(1),
    median: at(0.5),
    p5: at(0.05),
    // Lowest one-second average. NOT a 1% low - see the header.
    worstSecond: sorted[0],
    max: sorted.at(-1),
    stdDev: +Math.sqrt(variance).toFixed(1),
  };
}

// ------------------------------------------------------------------ running

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
function die(msg) { console.error(msg); process.exit(1); }

/**
 * Refuse to start when a setting guarantees the run measures a throttle rather
 * than the game. Both of these produce a confident, low-variance, completely
 * meaningless number, which is worse than an error.
 */
function preflight(options, args) {
  if (args["no-focus"]) return;
  const problems = [];

  // Fires on every input-free run over a minute long. This is the one that
  // produced a flat 30fps with the window focused and the world unpaused.
  if ((options.inactivityFpsLimit ?? "").replace(/"/g, "").toLowerCase() !== "minimized") {
    problems.push(
      `  inactivityFpsLimit is ${options.inactivityFpsLimit} - it must be "minimized".\n` +
      "    At \"afk\" the game drops to 30fps after ~60s without input, and this\n" +
      "    benchmark sends no input at all, so every run longer than a minute\n" +
      "    measures the limiter. Options > Video Settings.");
  }

  // Losing focus here does not merely throttle: it opens the Game Menu and
  // pauses the world, so the sampler reads a frozen scene.
  if (options.pauseOnLostFocus === "true") {
    problems.push(
      "  pauseOnLostFocus is true - it must be false.\n" +
      "    A stray click mid-run pauses the world and the average becomes noise.");
  }

  if (problems.length) {
    die("cannot benchmark with these video settings:\n\n" + problems.join("\n\n") +
        "\n\nchange them with the game closed (run/options.txt) or in Video Settings, then re-run.");
  }
}

/**
 * Bring the game window to the front. Anything that steals focus mid-run - a
 * terminal, an editor, another script's screenshot - silently rewrites the
 * result, so this runs immediately before warmup and the sampler re-checks
 * afterwards.
 */
function focusWindow() {
  const script = path.join(path.dirname(fileURLToPath(import.meta.url)), "window.ps1");
  try {
    execFileSync("powershell", ["-File", script, "-Action", "focus"], { stdio: "pipe" });
  } catch (e) {
    die(`could not focus the game window: ${e.message}\n` +
        "pass --no-focus to measure anyway, but expect the unfocused throttle.");
  }
}

async function waitFor(io, pred, timeoutMs, what) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    try {
      const info = await get(io, "client/info");
      if (pred(info)) return info;
    } catch { /* the client restarts its server on world load; keep trying */ }
    await sleep(1000);
  }
  die(`timed out waiting for ${what}`);
}

async function run(args) {
  if (!args.log) die("--log <path to the run log> is required");
  const io = interop(args.log);

  let info = await get(io, "client/info");

  if (args.world) {
    // findLevelCandidates() reports nothing while a world is open, so joining
    // has to happen from the title screen. This is why the benchmark runs
    // right after launch rather than against an already-loaded world.
    if (info.inGame) {
      die("client is already in a world - restart it and run this from the title screen,\n" +
          "or drop --world to measure whatever is currently loaded");
    }
    const worlds = await get(io, "client/worlds");
    const match = worlds.find((w) => w.name === args.world || w.displayName === args.world);
    if (!match) {
      die(`world "${args.world}" not found. available: ${worlds.map((w) => w.name).join(", ") || "(none)"}`);
    }
    console.error(`joining world ${match.name} ...`);
    await post(io, "client/worlds/join", { name: match.name });
    info = await waitFor(io, (i) => i.inGame, 180_000, "the world to load");
  } else if (!info.inGame) {
    die("client is not in a world - pass --world <levelId>, or load one first");
  }

  preflight(readOptions(info), args);

  if (!args["no-focus"]) focusWindow();

  // Chunk loading, mesh building and the JIT all settle during warmup. Sampling
  // through it measures the loading screen, not the game.
  console.error(`warming up ${args.warmup}s ...`);
  await sleep(args.warmup * 1000);

  console.error(`sampling ${args.sample}s ...`);
  const seen = [];
  let last = null;
  const deadline = Date.now() + args.sample * 1000;
  while (Date.now() < deadline) {
    try {
      const i = await get(io, "client/info");
      // mc.fps updates once a second; only keep a value when it actually moves
      // on, so `seconds` means seconds rather than requests.
      if (last === null || i.fps !== last) { seen.push(i.fps); last = i.fps; }
      if (!i.inGame) die("left the world mid-run - result discarded");
    } catch { /* transient */ }
    await sleep(args.poll);
  }

  if (seen.length < 5) die(`only ${seen.length} distinct samples - sample for longer`);

  // Refuse a throttled run rather than reporting it. A whole run under the
  // ceiling means the window was never focused; a few samples under it mean
  // something stole focus partway through and the average is now meaningless.
  const throttled = seen.filter((f) => f <= THROTTLE_CEILING).length;
  if (!args["no-focus"] && throttled) {
    const whole = throttled === seen.length;
    die(whole
      ? `every sample was at or below ${THROTTLE_CEILING}fps - the game window was not focused.\n` +
        "nothing was measured. re-run without clicking away from the game."
      : `${throttled} of ${seen.length} samples sat at or below ${THROTTLE_CEILING}fps while the rest did not.\n` +
        "the game was throttled partway through, so this average is meaningless.\n" +
        "usual cause: the inactivity limiter engaging mid-run, or something stealing focus.");
  }

  const result = {
    resultVersion: RESULT_VERSION,
    label: args.label,
    at: new Date().toISOString(),
    fps: stats(seen),
    conditions: fingerprint(io, info, args.world ?? "(already loaded)"),
  };

  const summary = result.fps;
  console.log(`\n${result.label}: median ${summary.median} | mean ${summary.mean} | ` +
              `p5 ${summary.p5} | worst second ${summary.worstSecond} | sd ${summary.stdDev} ` +
              `(${summary.seconds}s)`);

  if (args.out) {
    fs.mkdirSync(path.dirname(args.out), { recursive: true });
    fs.writeFileSync(args.out, JSON.stringify(result, null, 2));
    console.error(`wrote ${args.out}`);
  }
  return result;
}

// ----------------------------------------------------------------- comparing

/** Conditions that change FPS independently of the mods, so must match. */
const MUST_MATCH = ["world", "display", "options"];

/**
 * Each side takes several runs, comma-separated:
 *
 *   --compare before1.json,before2.json,before3.json after1.json,after2.json,after3.json
 *
 * Several rather than one because of a mistake this file made and this comment
 * exists to stop anyone repeating. The first version judged a change against
 * the *within-run* standard deviation - how much FPS wobbles second to second
 * inside one run. That is not the relevant spread. What matters is how much two
 * runs of the *identical* build differ, which also carries thermal state, JIT
 * warmup, GC timing, GPU boost behaviour and whatever else the machine was
 * doing.
 *
 * On the laptop this was written against, two byte-identical baseline runs came
 * out at median 383 and median 290. The old logic called that a 24.3%
 * regression, in bold, with no changes of any kind between them. A benchmark
 * that confidently reports a two-digit regression on an unmodified build is
 * worse than having no benchmark.
 *
 * So the noise floor is measured rather than assumed: repeat each side, take
 * the spread of the repeats, and only call an effect real when the difference
 * clears it. If that means the answer is "this machine cannot resolve a change
 * this small", that is the honest answer and it should be said out loud.
 */
function loadSide(spec) {
  const paths = String(spec).split(",").map((s) => s.trim()).filter(Boolean);
  const runs = paths.map((p) => ({ path: p, ...JSON.parse(fs.readFileSync(p, "utf8")) }));
  if (!runs.length) die(`no run files in "${spec}"`);
  return runs;
}

const medianOf = (xs) => [...xs].sort((a, b) => a - b)[Math.floor(xs.length / 2)];

/** Median across runs of one metric, plus the spread of those per-run values. */
function across(runs, key) {
  const values = runs.map((r) => r.fps[key]);
  return {
    values,
    median: medianOf(values),
    spread: Math.max(...values) - Math.min(...values),
  };
}

function summarise(runs) {
  const medians = runs.map((r) => r.fps.median);
  return {
    label: runs[0].label,
    n: runs.length,
    medians,
    median: medianOf(medians),
    // Observed between-run spread. With one run this is 0 and therefore useless,
    // which is exactly why one run per side is refused below.
    spread: Math.max(...medians) - Math.min(...medians),
    // Garbage-collector work moves stutter far more than it moves the median,
    // so reporting only the median hides the thing such a change is for.
    metrics: Object.fromEntries(
      ["median", "mean", "p5", "worstSecond", "stdDev"].map((k) => [k, across(runs, k)])
    ),
  };
}

function compare(aSpec, bSpec) {
  const aRuns = loadSide(aSpec);
  const bRuns = loadSide(bSpec);

  const problems = [];
  for (const key of MUST_MATCH) {
    const values = new Set([...aRuns, ...bRuns].map((r) => JSON.stringify(r.conditions[key])));
    if (values.size > 1) {
      problems.push(`  ${key} differs across the runs:\n` +
        [...aRuns, ...bRuns].map((r) => `    ${r.path}: ${JSON.stringify(r.conditions[key])}`).join("\n"));
    }
  }
  if (problems.length) {
    console.error("these runs are not comparable - the conditions differ:\n" + problems.join("\n"));
    console.error("\nre-run everything with the same world, window size and video settings.");
    process.exit(1);
  }

  const a = summarise(aRuns);
  const b = summarise(bRuns);

  const setA = new Set(aRuns[0].conditions.mods);
  const setB = new Set(bRuns[0].conditions.mods);
  const added = [...setB].filter((m) => !setA.has(m));
  const removed = [...setA].filter((m) => !setB.has(m));

  console.log(`\n${a.label} (n=${a.n})  ->  ${b.label} (n=${b.n})\n`);
  if (added.length) console.log(`  added:   ${added.join(", ")}`);
  if (removed.length) console.log(`  removed: ${removed.join(", ")}`);

  const va = aRuns[0].conditions.variant;
  const vb = bRuns[0].conditions.variant;
  if (va !== vb) {
    console.log(`  variant: ${va ?? "(none)"}  ->  ${vb ?? "(none)"}`);
  } else if (!added.length && !removed.length) {
    console.log("  nothing differs between these runs - this measures the noise floor itself");
  }

  console.log(`\n  ${a.label} medians: ${a.medians.join(", ")}   (spread ${a.spread})`);
  console.log(`  ${b.label} medians: ${b.medians.join(", ")}   (spread ${b.spread})`);

  // Every metric, each against its own between-run spread. stdDev is the odd
  // one out: lower is steadier, so a fall there is an improvement.
  console.log("\n  metric          a  ->  b     delta   floor   clears?");
  for (const key of ["median", "mean", "p5", "worstSecond", "stdDev"]) {
    const ma = a.metrics[key], mb = b.metrics[key];
    const delta = +(mb.median - ma.median).toFixed(1);
    const floor = Math.max(ma.spread, mb.spread);
    const clears = Math.abs(delta) > floor ? "yes" : "no";
    const sign = delta > 0 ? "+" : "";
    console.log(
      `  ${key.padEnd(13)} ${String(ma.median).padStart(5)} -> ${String(mb.median).padStart(5)}` +
      ` ${(sign + delta).padStart(7)} ${String(+floor.toFixed(1)).padStart(7)}   ${clears}`
    );
  }

  const delta = b.median - a.median;
  const pct = a.median ? ((delta / a.median) * 100).toFixed(1) : "0.0";
  const sign = delta > 0 ? "+" : "";
  console.log(`\n  median of medians: ${a.median} -> ${b.median}   ${sign}${delta} (${sign}${pct}%)`);

  if (a.n < 2 || b.n < 2) {
    console.log("\n  VERDICT: not enough runs. One run per side cannot measure its own noise floor,\n" +
                "  and on this scene two identical runs have differed by 24%. Give each side at\n" +
                "  least 3 runs (--repeat, or several --out files) before believing any number.");
    process.exit(2);
  }

  const noise = Math.max(a.spread, b.spread);
  console.log(`  noise floor (worst between-run spread): ${noise}`);
  if (Math.abs(delta) <= noise) {
    console.log(`\n  VERDICT: moved ${sign}${delta}, inside the ${noise} noise floor. No measurable effect.`);
  } else {
    console.log(`\n  VERDICT: moved ${sign}${delta}, clearing the ${noise} noise floor. ` +
                `${delta > 0 ? "Faster." : "SLOWER."}`);
  }
}

// -------------------------------------------------------------------- entry

const args = parseArgs(process.argv.slice(2));
if (args.compare) {
  compare(args.compare[0], args.compare[1]);
} else {
  await run(args);
}
