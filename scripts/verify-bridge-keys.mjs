/*
 * Resolve every bridged config key against the mod's own config file.
 *
 * ModuleBundledMods writes into files other mods own, addressed by dotted
 * path. A typo there does not fail the build and does not log: the write
 * lands on a key the mod never reads, the ClickGUI shows the value it just
 * stored, and the setting silently does nothing. That is exactly the failure
 * the Jade `harvest_tool.effective_tool` trap produced.
 *
 * So this reads the keys back out of the Kotlin and checks each one against a
 * real config written by the mod itself.
 *
 *   node scripts/verify-bridge-keys.mjs [--config-dir <dir>]
 *
 * Default config dir is the launcher's game directory, which is where a real
 * launch writes them. A key whose file is absent is reported as unverified,
 * not failed - a mod that is off by default never writes one.
 */

import fs from "node:fs";
import path from "node:path";
import os from "node:os";

const args = process.argv.slice(2);
const dirFlag = args.indexOf("--config-dir");
const configDir = dirFlag >= 0
    ? args[dirFlag + 1]
    : path.join(os.homedir(), "AppData/Roaming/Tsunami/TsunamiLauncher/data/gameDir/local/config");

const SOURCE = "src/main/kotlin/net/ccbluex/liquidbounce/features/module/modules/misc/ModuleBundledMods.kt";
const source = fs.readFileSync(SOURCE, "utf8");

/*
 * Which file each top-level bridge group writes to.
 *
 * Attribution is by group rather than by nearest store declaration, because
 * the helper-based stores (Jade, Sodium Extra) are declared at the *bottom* of
 * the file on purpose - a nested group that reaches into its half-built
 * enclosing object gets a null at startup. Position therefore cannot say who
 * owns a key, but the enclosing group always can.
 *
 * One line per bridge.
 */
const groupStores = {
    Sodium: "sodium-options.json",
    SodiumExtra: "sodium-extra-options.json",
    ImmediatelyFast: "immediatelyfast.json",
    EntityCulling: "entityculling.json",
    SkinLayers: "skinlayers.json",
    Jade: "jade/jade.json",
    AppleSkin: "appleskin.json5",
    MoreCulling: "moreculling.toml",
    Ixeris: "ixeris.toml",
    BadOptimizations: "badoptimizations.txt",
};

/*
 * The line-based configs, and the separator each uses. Anything not listed
 * here is treated as JSON.
 */
const lineFormats = {
    "moreculling.toml": {separator: "=", sections: true},
    "ixeris.toml": {separator: "=", sections: true},
    "badoptimizations.txt": {separator: ":", sections: false},
};

/** Split on unescaped dots, exactly as JsonConfigStore does. */
function segments(key) {
    const out = [];
    let cur = "";
    for (let i = 0; i < key.length; i++) {
        const c = key[i];
        if (c === "\\" && key[i + 1] === ".") { cur += "."; i++; continue; }
        if (c === ".") { out.push(cur); cur = ""; continue; }
        cur += c;
    }
    out.push(cur);
    return out;
}

function resolves(root, key) {
    let node = root;
    const parts = segments(key);
    for (const part of parts.slice(0, -1)) {
        if (node === null || typeof node !== "object" || !(part in node)) return false;
        node = node[part];
    }
    const last = parts[parts.length - 1];
    return node !== null && typeof node === "object" && last in node;
}

/** Gson parses leniently, so AppleSkin's // comments are legal input. Strip them. */
function readJson(file) {
    const raw = fs.readFileSync(file, "utf8");
    return JSON.parse(raw.replace(/^\s*\/\/.*$/gm, ""));
}

/** Mirrors LineConfigStore.entries: section-qualified key to raw value. */
function readLines(file, {separator, sections}) {
    const out = {};
    let section = "";
    for (const line of fs.readFileSync(file, "utf8").split(/\r?\n/)) {
        const t = line.trim();
        if (!t || t.startsWith("#")) continue;
        if (sections && t.startsWith("[") && t.endsWith("]")) {
            section = t.slice(1, -1).trim() + ".";
            continue;
        }
        const at = t.indexOf(separator);
        if (at <= 0) continue;
        out[section + t.slice(0, at).trim()] = t.slice(at + separator.length).trim();
    }
    return out;
}

/*
 * Find where each top-level group starts. Everything from one start to the
 * next belongs to that group, nested sub-groups included.
 */
const groupRe = /^ {4}object (\w+) : ValueGroup\(/gm;
const spans = [];
let g;
while ((g = groupRe.exec(source))) spans.push({name: g[1], at: g.index});
for (let i = 0; i < spans.length; i++) {
    spans[i].end = i + 1 < spans.length ? spans[i + 1].at : source.length;
}

const unknownGroups = spans.filter((s) => !(s.name in groupStores)).map((s) => s.name);

/*
 * Every string literal that addresses a config key: an inline
 * `store.readX("...")`, a helper `jadeBool("...")` / `seInt("...")`, or the
 * group's own `write("..." to value)`.
 */
const keyRe = /(?:read(?:Boolean|Int|Float|String)\(|(?:^|[^A-Za-z])[A-Za-z]*[Ww]rite\()\s*(?:[A-Za-z0-9_.]+\.entries,\s*)?"((?:[^"\\]|\\.)*)"/g;

const byStore = new Map();
let m;
while ((m = keyRe.exec(source))) {
    const span = spans.find((s) => m.index >= s.at && m.index < s.end);
    if (!span) continue;
    const file = groupStores[span.name];
    if (!file) continue;
    // The Kotlin source spells an escaped dot `\\.`; the runtime string is `\.`.
    const key = m[1].replace(/\\\\/g, "\\");
    if (!byStore.has(file)) byStore.set(file, new Set());
    byStore.get(file).add(key);
}

let resolved = 0;
let missing = 0;
let unverified = 0;

for (const [store, keys] of [...byStore].sort()) {
    const file = path.join(configDir, store);
    if (!fs.existsSync(file)) {
        console.log(`  ----  ${store} - no config written yet, ${keys.size} keys unverified`);
        unverified += keys.size;
        continue;
    }
    const lineFormat = lineFormats[store];
    let root;
    try {
        root = lineFormat ? readLines(file, lineFormat) : readJson(file);
    } catch (err) {
        console.log(`  FAIL  ${store} - could not parse: ${err.message}`);
        missing += keys.size;
        continue;
    }
    const bad = [...keys].filter((k) => (lineFormat ? !(k in root) : !resolves(root, k)));
    resolved += keys.size - bad.length;
    missing += bad.length;
    if (bad.length === 0) {
        console.log(`  ok    ${store} - all ${keys.size} keys resolve`);
    } else {
        console.log(`  FAIL  ${store} - ${bad.length}/${keys.size} do not resolve:`);
        for (const k of bad) console.log(`          ${k}`);
    }
}

const silent = Object.keys(groupStores).filter(
    (name) => spans.some((s) => s.name === name) && !byStore.has(groupStores[name]));
if (silent.length) {
    console.log(`\n  note  no literal keys found for: ${silent.join(", ")}`);
    console.log("        their keys are const references, so this cannot see them.");
}

if (unknownGroups.length) {
    console.log(`\n  note  groups with no store mapped, so unchecked: ${unknownGroups.join(", ")}`);
    console.log("        add them to groupStores in this script.");
}

console.log(`\n${resolved} resolved, ${missing} missing, ${unverified} unverified\n`);
process.exit(missing === 0 ? 0 : 1);
