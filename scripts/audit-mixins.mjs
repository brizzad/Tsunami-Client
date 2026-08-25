/**
 * Static regression audit: did the strip delete code that a KEPT module needed?
 *
 * This exists because four modules were silently broken by the strip. Each was
 * registered, enabled and visible in the HUD while doing nothing, because the
 * mixin method implementing it also served a removed module and was deleted
 * whole:
 *
 *   injectBobView          -> NoBob        view bobbing never cancelled
 *   injectXRayFullBright   -> FullBright   gamma override lost
 *   modifyPlayerName       -> BetterTab    game-mode suffix lost
 *   shouldRender           -> AntiBlind    falling-block suppression lost
 *
 * None of them failed a build. Only playing the game found the first one.
 *
 * Rather than parse method declarations (brittle), this diffs every source file
 * against upstream and flags any REMOVED line naming a kept module. Lines that
 * still exist in the current file are ignored, since a --unified=0 diff shows
 * reindented or reordered code as removed-and-readded. Genuine-but-correct
 * trims (dropping one condition from a shared expression) are recorded in
 * scripts/audit-baseline.txt so only new findings are reported.
 *
 *   node scripts/audit-mixins.mjs             report new findings
 *   node scripts/audit-mixins.mjs --baseline  accept current findings
 *
 * Exits non-zero on a new finding, so CI can gate on it.
 */
import { execSync } from "child_process";
import fs from "fs";
import path from "path";

const ROOT = process.cwd();
const UPSTREAM = process.env.TSUNAMI_UPSTREAM || "1dd09d11a";
const BASELINE = path.join(ROOT, "scripts/audit-baseline.txt");
const WRITE = process.argv.includes("--baseline");

const keep = fs
  .readFileSync(path.join(ROOT, "scripts/keep.txt"), "utf8")
  .split("\n")
  .map((s) => s.trim())
  .filter(Boolean);

if (!keep.length) {
  console.error("scripts/keep.txt is empty; nothing to audit against");
  process.exit(2);
}

const keptRe = new RegExp("\\bModule(" + keep.join("|") + ")\\b");
const squash = (s) => s.replace(/\s+/g, " ").trim();

let diff;
try {
  diff = execSync(`git diff --unified=0 ${UPSTREAM} HEAD -- "*.java" "*.kt"`, {
    maxBuffer: 1 << 28,
  }).toString();
} catch {
  console.error("git diff failed - is TSUNAMI_UPSTREAM a reachable commit?");
  process.exit(2);
}

const cache = new Map();
function stillPresent(rel, code) {
  if (!cache.has(rel)) {
    const abs = path.join(ROOT, rel);
    cache.set(rel, fs.existsSync(abs) ? squash(fs.readFileSync(abs, "utf8")) : null);
  }
  const cur = cache.get(rel);
  return cur !== null && cur.includes(squash(code));
}

const findings = [];
let file = null;
let removedFrom = null;

for (const line of diff.split("\n")) {
  // A deleted file's header is "+++ /dev/null", so tracking only "+++ b/"
  // leaves `file` pointing at whichever file came before it, and every finding
  // in the deleted file is reported against that innocent path. Worse, which
  // path it lands on shifts whenever the diff order changes, so a baselined
  // finding silently reappears as new. Fall back to the "--- a/" side.
  if (line.startsWith("--- a/")) {
    removedFrom = line.slice(6);
    continue;
  }
  if (line.startsWith("+++ ")) {
    file = line.startsWith("+++ b/") ? line.slice(6) : removedFrom;
    continue;
  }
  if (!file) continue;
  if (!line.startsWith("-") || line.startsWith("---")) continue;

  const code = line.slice(1).trim();
  if (!code || code.startsWith("*") || code.startsWith("//")) continue;
  if (code.startsWith("import ")) continue;

  const m = code.match(keptRe);
  if (!m) continue;

  // Reindented or reordered code shows as removed in a zero-context diff.
  if (stillPresent(file, code)) continue;

  findings.push(`${file}\t${m[1]}\t${squash(code).slice(0, 110)}`);
}

const unique = [...new Set(findings)].sort();

if (WRITE) {
  const header = [
    "# Removals that name a kept module, reviewed and accepted.",
    "#",
    "# A correct trim - dropping one condition from an expression shared with a",
    "# removed module - legitimately deletes a line naming a module we keep, so",
    "# these are expected. Anything NOT listed here is unreviewed and the audit",
    "# will fail on it.",
    "#",
    "# Regenerate: node scripts/audit-mixins.mjs --baseline",
    "",
  ].join("\n");
  fs.writeFileSync(BASELINE, header + unique.join("\n") + "\n");
  console.log(`baseline written: ${unique.length} accepted entries`);
  process.exit(0);
}

const baseline = fs.existsSync(BASELINE)
  ? new Set(
      fs
        .readFileSync(BASELINE, "utf8")
        .split("\n")
        .map((l) => l.replace(/\r$/, ""))
        .filter((l) => l && !l.startsWith("#"))
    )
  : new Set();

const fresh = unique.filter((f) => !baseline.has(f));

console.log(
  `upstream ${UPSTREAM} | kept ${keep.length} | findings ${unique.length} | accepted ${baseline.size}`
);

if (!fresh.length) {
  console.log("\nNo unreviewed removals touch a kept module.");
  process.exit(0);
}

console.log("\nUNREVIEWED removals naming a kept module - confirm each still works:\n");
for (const f of fresh) {
  const [rel, mod, code] = f.split("\t");
  console.log(`  ${mod.padEnd(16)} ${path.basename(rel)}`);
  console.log(`      ${code}`);
}
console.log("\nIf a finding is a correct trim, accept it: node scripts/audit-mixins.mjs --baseline");
process.exit(1);
