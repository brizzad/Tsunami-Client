/**
 * Removes every module classified REMOVE by scripts/classify.mjs.
 *
 * A module is either a single ModuleX.kt in a category root, or a ModuleX.kt
 * with helper files in its own subpackage. A subpackage is deleted whole only
 * when every module inside it is being removed — otherwise just the file goes,
 * so a kept module never loses the package it shares.
 *
 * Also strips the module's import and its entry in the registerInbuilt()
 * array in ModuleManager.kt, which is the only place modules are registered.
 *
 * Run with --apply to actually delete; defaults to a dry run.
 */
import fs from "fs";
import path from "path";

const ROOT = process.cwd();
const MODULES = path.join(ROOT, "src/main/kotlin/net/ccbluex/liquidbounce/features/module/modules");
const MANAGER = path.join(ROOT, "src/main/kotlin/net/ccbluex/liquidbounce/features/module/ModuleManager.kt");
const APPLY = process.argv.includes("--apply");

const keepNames = new Set(
  fs.readFileSync(path.join(ROOT, "scripts/keep.txt"), "utf8")
    .split("\n").map((s) => s.trim()).filter(Boolean)
);

function walk(dir, out = []) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, out);
    else if (/^Module[A-Za-z0-9]+\.kt$/.test(e.name)) out.push(p);
  }
  return out;
}

const all = walk(MODULES).map((file) => ({
  file,
  name: path.basename(file, ".kt").replace(/^Module/, ""),
  dir: path.dirname(file),
}));

const categoryRoots = new Set(
  fs.readdirSync(MODULES).map((d) => path.join(MODULES, d))
);

// Group by containing directory to decide whole-package vs single-file deletes.
const byDir = new Map();
for (const m of all) {
  if (!byDir.has(m.dir)) byDir.set(m.dir, []);
  byDir.get(m.dir).push(m);
}

const deleteDirs = [];
const deleteFiles = [];
const removedNames = [];

for (const [dir, mods] of byDir) {
  const removing = mods.filter((m) => !keepNames.has(m.name));
  if (removing.length === 0) continue;

  if (!categoryRoots.has(dir) && removing.length === mods.length) {
    deleteDirs.push({ dir, mods: removing.map((m) => m.name) });
    removedNames.push(...removing.map((m) => m.name));
  } else {
    for (const m of removing) {
      deleteFiles.push(m.file);
      removedNames.push(m.name);
    }
  }
}

// Category directories that end up with no modules at all go entirely.
const emptyCategories = [];
for (const root of categoryRoots) {
  const mods = all.filter((m) => m.file.startsWith(root + path.sep));
  if (mods.length && mods.every((m) => !keepNames.has(m.name))) {
    emptyCategories.push(root);
  }
}

console.log(APPLY ? "APPLYING" : "DRY RUN (pass --apply to delete)");
console.log(`modules total ${all.length}  keep ${all.length - removedNames.length}  remove ${removedNames.length}`);
console.log(`whole packages: ${deleteDirs.length}   single files: ${deleteFiles.length}`);
console.log(`categories emptied entirely: ${emptyCategories.map((d) => path.basename(d)).join(", ") || "none"}`);

const missing = [...keepNames].filter((n) => !all.some((m) => m.name === n));
if (missing.length) {
  console.error("keep.txt names not found in the tree: " + missing.join(", "));
  process.exit(1);
}

if (APPLY) {
  for (const root of emptyCategories) fs.rmSync(root, { recursive: true, force: true });
  for (const { dir } of deleteDirs) fs.rmSync(dir, { recursive: true, force: true });
  for (const f of deleteFiles) if (fs.existsSync(f)) fs.rmSync(f);

  // ModuleManager: drop the import lines and the registerInbuilt() entries.
  let mgr = fs.readFileSync(MANAGER, "utf8");
  const before = mgr.split("\n").length;
  const gone = new Set(removedNames);

  mgr = mgr.split("\n").filter((line) => {
    const imp = line.match(/^import .*\.modules\..*\.Module([A-Za-z0-9]+)$/);
    if (imp && gone.has(imp[1])) return false;
    const entry = line.match(/^\s*Module([A-Za-z0-9]+),\s*$/);
    if (entry && gone.has(entry[1])) return false;
    return true;
  }).join("\n");

  fs.writeFileSync(MANAGER, mgr);
  console.log(`ModuleManager.kt: ${before} -> ${mgr.split("\n").length} lines`);
}
