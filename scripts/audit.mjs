/**
 * Tsunami static audit. Runs in either repo; checks enable themselves based on
 * what the repo contains.
 *
 * Every check here exists because the corresponding mistake was actually made
 * and shipped, and in each case nothing else caught it - the build was green
 * and the app started fine.
 *
 *   phone-home   A CCBlueX host left in code. The launcher connected to their
 *                production API, pulled their live build catalogue, and would
 *                have downloaded and run actual LiquidBounce. It did not look
 *                like a failure; it looked like the app working.
 *
 *   svg-xml      XML forbids a double hyphen inside a comment. Three marks
 *                carried "var(--accent)" in a comment, which made them invalid
 *                XML. They rendered as broken images with nothing logged.
 *
 *   kept-modules Client only, when scripts/keep.txt exists. A mixin method
 *                serving both a removed and a kept module was deleted whole,
 *                so four modules loaded, enabled and appeared in the HUD while
 *                doing nothing. See scripts/audit-mixins.mjs, which this
 *                delegates to.
 *
 *   node scripts/audit.mjs             report unreviewed findings
 *   node scripts/audit.mjs --baseline  accept current findings
 *
 * Exits non-zero on anything unreviewed, so CI can gate on it.
 */
import { execSync } from "child_process";
import fs from "fs";
import path from "path";

const ROOT = process.cwd();
const BASELINE = path.join(ROOT, "scripts/audit-baseline-shared.txt");
const WRITE = process.argv.includes("--baseline");

/** Hosts CCBlueX operates. None of these are ours to talk to. */
const CCBLUEX_HOSTS = [
  "liquidbounce.net",
  "ccbluex.net",
  "liquidproxy.net",
];

const SOURCE_EXT = new Set([".kt", ".java", ".rs", ".js", ".mjs", ".ts", ".svelte", ".json", ".toml"]);
const SKIP_DIR = /(^|[\\/])(\.git|node_modules|build|dist|target|run|\.gradle|gen)([\\/]|$)/;

function walk(dir, out = []) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    const rel = path.relative(ROOT, p);
    if (SKIP_DIR.test(rel)) continue;
    if (e.isDirectory()) walk(p, out);
    else out.push(p);
  }
  return out;
}

const files = walk(ROOT);
const findings = [];
const add = (check, rel, detail) => findings.push(`${check}\t${rel.replace(/\\/g, "/")}\t${detail}`);

/* ------------------------------------------------------------------ *
 * phone-home
 * ------------------------------------------------------------------ */
{
  // Attribution in a comment is required by the GPL and is not a network call,
  // so comment lines are skipped. A host inside a string literal is a call.
  const isComment = (l) => /^\s*(\*|\/\/|#|<!--)/.test(l);
  const hostRe = new RegExp("(" + CCBLUEX_HOSTS.map((h) => h.replace(/\./g, "\\.")).join("|") + ")");

  for (const abs of files) {
    if (!SOURCE_EXT.has(path.extname(abs))) continue;
    const rel = path.relative(ROOT, abs);
    let text;
    try { text = fs.readFileSync(abs, "utf8"); } catch { continue; }
    if (!hostRe.test(text)) continue;

    text.split("\n").forEach((line, i) => {
      if (isComment(line)) return;
      if (!hostRe.test(line)) return;
      // Only care when it appears as a URL in code.
      if (!/https?:\/\//.test(line)) return;
      add("phone-home", rel, `${i + 1}: ${line.trim().replace(/\s+/g, " ").slice(0, 100)}`);
    });
  }
}

/* ------------------------------------------------------------------ *
 * svg-xml
 * ------------------------------------------------------------------ */
{
  for (const abs of files) {
    if (path.extname(abs) !== ".svg") continue;
    const rel = path.relative(ROOT, abs);
    const text = fs.readFileSync(abs, "utf8");

    // Upstream ships some flag "SVGs" as one-line redirect stubs whose whole
    // content is another filename (bq.svg contains "bq-bo.svg"), committed as
    // regular files rather than symlinks. Not markup, so not this check to make.
    if (/^[\w.-]+\.svg\s*$/.test(text)) continue;

    for (const m of text.matchAll(/<!--([\s\S]*?)-->/g)) {
      if (m[1].includes("--")) {
        add("svg-xml", rel, "double hyphen inside an XML comment - file will not parse");
      }
    }
    if (!/<svg[\s>]/.test(text)) add("svg-xml", rel, "no <svg> root element");
    else if (!/<\/svg>\s*$/.test(text.trim())) add("svg-xml", rel, "missing closing </svg>");
  }
}

/* ------------------------------------------------------------------ *
 * kept-modules (client only) - delegate
 * ------------------------------------------------------------------ */
let delegated = null;
if (fs.existsSync(path.join(ROOT, "scripts/keep.txt")) &&
    fs.existsSync(path.join(ROOT, "scripts/audit-mixins.mjs"))) {
  try {
    execSync(`node scripts/audit-mixins.mjs${WRITE ? " --baseline" : ""}`, { stdio: "pipe" });
    delegated = { ok: true, out: "" };
  } catch (e) {
    delegated = { ok: false, out: (e.stdout || Buffer.from("")).toString() };
  }
}

/* ------------------------------------------------------------------ *
 * report
 * ------------------------------------------------------------------ */
const unique = [...new Set(findings)].sort();

if (WRITE) {
  fs.writeFileSync(
    BASELINE,
    [
      "# Findings reviewed and accepted.",
      "#",
      "# A CCBlueX host listed here is an outbound LINK a user clicks, not an",
      "# automatic call - those are pending real Tsunami destinations. Anything",
      "# not listed is unreviewed and fails the audit.",
      "#",
      "# Regenerate: node scripts/audit.mjs --baseline",
      "",
    ].join("\n") + unique.join("\n") + "\n"
  );
  console.log(`shared baseline written: ${unique.length} accepted`);
  if (delegated) console.log("kept-modules baseline also refreshed");
  process.exit(0);
}

const baseline = fs.existsSync(BASELINE)
  ? new Set(fs.readFileSync(BASELINE, "utf8").split("\n").map((l) => l.replace(/\r$/, "")).filter((l) => l && !l.startsWith("#")))
  : new Set();

const fresh = unique.filter((f) => !baseline.has(f));

// An accepted finding that no longer exists was fixed, and the entry should go.
// Left in place it is a small trap of its own: reintroducing that exact line
// would pass silently, because it is already on the accept list.
const have = new Set(unique);
const stale = [...baseline].filter((b) => !have.has(b));
const byCheck = {};
for (const f of unique) {
  const c = f.split("\t")[0];
  byCheck[c] = (byCheck[c] || 0) + 1;
}

console.log(`repo ${path.basename(ROOT)} | files ${files.length} | findings ${unique.length} | accepted ${baseline.size}`);
for (const c of Object.keys(byCheck).sort()) console.log(`  ${c.padEnd(12)} ${byCheck[c]}`);
if (delegated) console.log(`  kept-modules ${delegated.ok ? "pass" : "FAIL"}`);

let bad = false;

if (fresh.length) {
  bad = true;
  console.log("\nUNREVIEWED:\n");
  for (const f of fresh) {
    const [check, rel, detail] = f.split("\t");
    console.log(`  [${check}] ${rel}`);
    console.log(`      ${detail}`);
  }
  console.log("\nIf these are acceptable: node scripts/audit.mjs --baseline");
}

if (delegated && !delegated.ok) {
  bad = true;
  console.log("\nkept-modules check failed:\n");
  console.log(delegated.out.split("\n").map((l) => "  " + l).join("\n"));
}

if (stale.length) {
  console.log(`\n${stale.length} accepted finding(s) no longer present - fixed since baselining.`);
  for (const f of stale) {
    const [check, rel] = f.split("\t");
    console.log(`  [${check}] ${rel}`);
  }
  console.log("Drop them: node scripts/audit.mjs --baseline");
}

if (!bad) console.log("\nClean.");
process.exit(bad ? 1 : 0);
