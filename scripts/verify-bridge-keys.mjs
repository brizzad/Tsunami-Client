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
import zlib from "node:zlib";

const args = process.argv.slice(2);
const dirFlag = args.indexOf("--config-dir");
const configDir = dirFlag >= 0
    ? args[dirFlag + 1]
    : path.join(os.homedir(), "AppData/Roaming/Tsunami/TsunamiLauncher/data/gameDir/local/config");

const SOURCE = "src/main/kotlin/net/ccbluex/liquidbounce/features/module/modules/misc/ModuleBundledMods.kt";
const source = fs.readFileSync(SOURCE, "utf8");

/*
 * The per-mod helper files beside it. Jade, Sodium Extra and Shield Statuses keep their
 * stores, key constants and readers in their own Bundled*Config.kt - one file per mod,
 * because kept together they outgrow what detekt allows in a single file.
 *
 * Those have to be scanned too. A key constant that moves out of the module file and is
 * not picked up here is not reported as missing: the scan simply never sees it, so
 * coverage silently drops and the run still exits clean. That happened - splitting the
 * shield helpers out took this from 209 keys to 198 with a green result.
 */
const helperDir = path.dirname(SOURCE);
const sources = [source].concat(
    fs
        .readdirSync(helperDir)
        .filter((f) => f.startsWith("Bundled") && f.endsWith("Config.kt"))
        .map((f) => fs.readFileSync(path.join(helperDir, f), "utf8"))
);

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
    ShieldStatuses: "shieldstatus.json",
    Iris: "iris.properties",
    ItemPhysic: "itemphysic-client.json",
    XaerosMinimap: "xaero/minimap/profiles/default.cfg",
    GlintOutline: "enchantment-glint-outline.json",
};

/*
 * Groups that deliberately have no mod config behind them, so "unmapped" is the
 * right answer rather than a gap to fill.
 *
 * Vulkan writes vanilla's own `preferredGraphicsBackend` option through
 * `Options.save()`, not a bundled mod's file. There is no key for this script to
 * resolve, and adding a fake mapping would make it look like there was.
 */
const storelessGroups = new Set(["Vulkan"]);

/** Configs that are an array of named records rather than a key tree. */
const recordFiles = new Set(["shieldstatus.json"]);

/*
 * The line-based configs, and the separator each uses. Anything not listed
 * here is treated as JSON.
 */
const lineFormats = {
    "moreculling.toml": {separator: "=", sections: true},
    "ixeris.toml": {separator: "=", sections: true},
    "badoptimizations.txt": {separator: ":", sections: false},
    "iris.properties": {separator: "=", sections: false},
    "xaero/minimap/profiles/default.cfg": {separator: "=", sections: false},
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

/**
 * Mirrors NamedRecordConfigStore: a `Category/Group/Option` path into an array
 * of named records, as WalksyLib writes for Shield Statuses.
 */
function readNamedRecords(file) {
    const array = JSON.parse(fs.readFileSync(file, "utf8"));
    const out = {};
    const named = (list, name) => (list ?? []).find((x) => x && x.name === name);
    for (const category of array) {
        for (const group of category.groups ?? []) {
            for (const option of group.options ?? []) {
                out[`${category.name}/${group.name}/${option.name}`] = option.value;
            }
        }
    }
    void named;
    return out;
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

const unknownGroups = spans
    .filter((s) => !(s.name in groupStores) && !storelessGroups.has(s.name))
    .map((s) => s.name);

/*
 * Every string literal that addresses a config key: an inline
 * `store.readX("...")`, a helper `jadeBool("...")` / `seInt("...")`, or the
 * group's own `write("..." to value)`.
 */
const keyRe = /(?:read(?:Boolean|Int|Float|String)\(|(?:^|[^A-Za-z])[A-Za-z]*[Ww]rite\()\s*(?:[A-Za-z0-9_.]+\.entries,\s*)?"((?:[^"\\]|\\.)*)"/g;

const byStore = new Map();
let m;

/*
 * Shield Statuses addresses its options through top-level consts rather than
 * literals inside the group, because the paths are long enough to be unreadable
 * inline. Pick those up by their SHIELD_ prefix.
 */
const shieldRe = /^(?:private|internal) const val SHIELD_[A-Z_]+ = "([^"]+\/[^"]+)"/gm;
for (const text of sources) {
    shieldRe.lastIndex = 0;
    while ((m = shieldRe.exec(text))) {
        if (!byStore.has("shieldstatus.json")) byStore.set("shieldstatus.json", new Set());
        byStore.get("shieldstatus.json").add(m[1]);
    }
}

/*
 * Sodium's original five keys are `private const val KEY_*` inside the object,
 * so the literal scan below cannot see them either. Same treatment.
 */
const sodiumConstRe = /^ {4}(?:private|internal) const val KEY_[A-Z_]+ = "([^"]+)"/gm;
while ((m = sodiumConstRe.exec(source))) {
    if (!byStore.has("sodium-options.json")) byStore.set("sodium-options.json", new Set());
    byStore.get("sodium-options.json").add(m[1]);
}
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
        root = recordFiles.has(store)
            ? readNamedRecords(file)
            : lineFormat ? readLines(file, lineFormat) : readJson(file);
    } catch (err) {
        console.log(`  FAIL  ${store} - could not parse: ${err.message}`);
        missing += keys.size;
        continue;
    }
    const flat = lineFormat || recordFiles.has(store);
    const bad = [...keys].filter((k) => (flat ? !(k in root) : !resolves(root, k)));
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

/*
 * Second check: does each bridge name a mod id that actually exists?
 *
 * `applyTo` gates every write on `ModConfigStore.isModLoaded(modId)`, and that
 * takes the **Fabric** mod id out of `fabric.mod.json` - not the Modrinth slug
 * the launcher installs by. Where the two differ the failure is silent in the
 * worst possible way: the key resolves, so the check above passes; the ClickGUI
 * stores the value and shows it back; and the write is dropped with a chat line.
 * Nothing tells you the setting did nothing.
 *
 * That shipped twice. `3dskinlayers` should have been `skinlayers3d` - the slug
 * and the id are reversed - and all six SkinLayers settings had never written
 * anything at all. `enchantment-glint-outline` should have been
 * `enchant-outline`. Both were found by pushing a value through a running client
 * and reading the mod's own file back, which is far too slow a way to catch a
 * typo.
 */
function fabricIdsFrom(dir) {
    const ids = new Map();
    let jars = [];
    try {
        jars = fs.readdirSync(dir).filter((f) => f.endsWith(".jar")).map((f) => path.join(dir, f));
    } catch {
        return ids;
    }

    for (const jar of jars) {
        const id = readFabricId(jar);
        if (id) ids.set(id, path.basename(jar));
    }
    return ids;
}

/**
 * Pulls `fabric.mod.json` out of a jar and returns its `id`, or null.
 *
 * Reads the central directory rather than walking local file headers. Most mod
 * jars are written with data descriptors, which leave the compressed size as 0
 * in the local header and fill it in after the data - so a local-header walk
 * finds nothing for exactly the jars this needs to read. The central directory
 * always carries the real sizes.
 */
function readFabricId(jar) {
    let buf;
    try {
        buf = fs.readFileSync(jar);
    } catch {
        return null;
    }

    // End of Central Directory, scanning back over the optional trailing comment.
    let eocd = -1;
    for (let i = buf.length - 22; i >= 0 && i > buf.length - 70000; i--) {
        if (buf.readUInt32LE(i) === 0x06054b50) {
            eocd = i;
            break;
        }
    }
    if (eocd < 0) return null;

    let at = buf.readUInt32LE(eocd + 16);
    const count = buf.readUInt16LE(eocd + 10);

    for (let i = 0; i < count; i++) {
        const nameLen = buf.readUInt16LE(at + 28);
        const extraLen = buf.readUInt16LE(at + 30);
        const cmtLen = buf.readUInt16LE(at + 32);
        const name = buf.subarray(at + 46, at + 46 + nameLen).toString("utf8");

        if (name === "fabric.mod.json") {
            const method = buf.readUInt16LE(at + 10);
            const compSize = buf.readUInt32LE(at + 20);
            const lho = buf.readUInt32LE(at + 42);
            const dataAt = lho + 30 + buf.readUInt16LE(lho + 26) + buf.readUInt16LE(lho + 28);
            const entry = buf.subarray(dataAt, dataAt + compSize);
            try {
                const text = method === 0
                    ? entry.toString("utf8")
                    : zlib.inflateRawSync(entry).toString("utf8");
                // Some mods ship trailing commas, which JSON.parse rejects.
                return JSON.parse(text.replace(/,(\s*[}\]])/g, "$1")).id ?? null;
            } catch {
                return null;
            }
        }

        at += 46 + nameLen + extraLen + cmtLen;
    }
    return null;
}


const modsDir = path.join(path.dirname(configDir), "mods");
const installed = fabricIdsFrom(modsDir);

/*
 * The dev client resolves its mods through Gradle rather than a mods folder, so
 * its jars sit in the Modrinth cache. Read those too - otherwise a mod the
 * launcher bundles but has not downloaded yet reads as a wrong id, which is the
 * one thing this check exists to tell apart.
 */
const cacheRoot = path.join(os.homedir(), ".gradle/caches/modules-2/files-2.1/maven.modrinth");

function cachedJars(root, depth = 0) {
    if (depth > 4) return [];
    let entries = [];
    try {
        entries = fs.readdirSync(root, {withFileTypes: true});
    } catch {
        return [];
    }
    const out = [];
    for (const e of entries) {
        const p = path.join(root, e.name);
        if (e.isDirectory()) {
            out.push(...cachedJars(p, depth + 1));
        } else if (e.name.endsWith(".jar")) {
            out.push(p);
        }
    }
    return out;
}

for (const jar of cachedJars(cacheRoot)) {
    const id = readFabricId(jar);
    if (id && !installed.has(id)) installed.set(id, path.basename(jar));
}

console.log("\nbridge mod ids");

const idRe = /applyTo\(\s*store\s*,\s*"([^"]+)"/g;
const bridgeIds = new Set();
for (const text of sources) {
    idRe.lastIndex = 0;
    let hit;
    while ((hit = idRe.exec(text))) bridgeIds.add(hit[1]);
}

const squash = (v) => v.replace(/[^a-z0-9]/g, "");
let wrongIds = 0;
let uncheckedIds = 0;

if (installed.size === 0) {
    console.log(`  ----  no jars found, so ${bridgeIds.size} id(s) unchecked`);
    uncheckedIds = bridgeIds.size;
} else {
    for (const id of [...bridgeIds].sort()) {
        if (installed.has(id)) {
            console.log(`  ok    ${id}`);
            continue;
        }

        /*
         * Not installed here is not the same as wrong. A near match *is* wrong:
         * it means the mod is present under a different id, which is exactly how
         * `3dskinlayers` sat beside a jar declaring `skinlayers3d` while all six
         * of its settings silently did nothing.
         */
        const near = [...installed.keys()].filter((k) => {
            const a = squash(k);
            const b = squash(id);
            if (a === b) return false;
            const sorted = (v) => [...v].sort().join("");
            return a.includes(b) || b.includes(a) || sorted(a) === sorted(b);
        });

        if (near.length) {
            wrongIds++;
            console.log(`  FAIL  ${id} - no jar declares this; did you mean ${near.join(", ")}?`);
        } else {
            uncheckedIds++;
            console.log(`  ----  ${id} - not installed here, so unchecked`);
        }
    }
}

if (wrongIds > 0) {
    console.log(`\n${wrongIds} bridge(s) name a mod id nothing declares - those writes are dropped silently\n`);
    process.exit(1);
}

const checked = bridgeIds.size - uncheckedIds;
console.log(`\n  ${checked}/${bridgeIds.size} bridge mod ids match an installed jar` +
    (uncheckedIds ? `, ${uncheckedIds} not installed here` : ""));
console.log();


process.exit(missing === 0 ? 0 : 1);
