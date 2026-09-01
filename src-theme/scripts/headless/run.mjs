/*
 * Headless harness for the theme's Svelte components.
 *
 * `runClient` is this project's bar for "done", but MCEF downloads ~150 MB
 * before the ClickGUI exists at all, and no click can be scripted inside it.
 * This mounts the real components in jsdom instead, so a fault in what the UI
 * *commits* — as opposed to what it draws — can be caught in seconds.
 *
 *   npm i --no-save jsdom
 *   node scripts/headless/run.mjs
 *
 * jsdom is deliberately not a dependency in package.json: it is a few hundred
 * files that nothing shipped in the jar needs.
 */

import {fileURLToPath, pathToFileURL} from "node:url";
import path from "node:path";
import fs from "node:fs";
import os from "node:os";

const here = path.dirname(fileURLToPath(import.meta.url));
const themeRoot = path.resolve(here, "../..");

// ---------------------------------------------------------------- jsdom

let JSDOM;
try {
    ({JSDOM} = await import("jsdom"));
} catch {
    console.error("This harness needs jsdom, which is not a project dependency.\n");
    console.error("  cd src-theme && npm i --no-save jsdom\n");
    console.error("--no-save keeps it out of package.json; it is a test-only tool.");
    process.exit(2);
}

// ---------------------------------------------------------------- build

/**
 * Stub the two modules that cannot run outside the game: ws.ts opens a
 * WebSocket the moment it is imported, and rest.ts talks HTTP to the client.
 * Matching on the specifier's tail catches every relative spelling of them.
 */
const stubs = {
    "integration/rest": path.join(here, "stubs/rest.ts"),
    "integration/ws": path.join(here, "stubs/ws.ts"),
};

const exported = (source) =>
    [...source.matchAll(/export\s+(?:async\s+)?function\s+(\w+)/g)].map((m) => m[1]);

const stubPlugin = {
    name: "headless-stubs",
    enforce: "pre",
    resolveId(source) {
        for (const [tail, target] of Object.entries(stubs)) {
            if (source === tail || source.endsWith(`/${tail}`)) return target;
        }
        return null;
    },
    /*
     * Fill in whatever the stub does not implement, read from the module it
     * stands in for. A stub that has to be edited by hand every time somebody
     * adds a rest call is a stub that breaks the next person's build for a
     * reason that has nothing to do with their change.
     *
     * The filler returns [], which is iterable, mappable and harmlessly falsy
     * enough for guard code. If a test needs one of these to behave, implement
     * it properly in the stub file and this stops generating it.
     */
    transform(code, id) {
        const same = (a, b) => path.resolve(a) === path.resolve(b);
        for (const [tail, target] of Object.entries(stubs)) {
            if (!same(id, target)) continue;
            const real = path.join(themeRoot, "src", `${tail}.ts`);
            if (!fs.existsSync(real)) continue;
            const defined = new Set(exported(code));
            const filler = exported(fs.readFileSync(real, "utf8"))
                .filter((name) => !defined.has(name))
                .map((name) => `export function ${name}() { return []; }`);
            if (!filler.length) return null;
            const header = "// generated from src/" + tail + ".ts";
            return [code, "", header, filler.join("\n"), ""].join("\n");
        }
        return null;
    },
};

const outDir = fs.mkdtempSync(path.join(os.tmpdir(), "tsunami-headless-"));

const {build} = await import("vite");
const {svelte} = await import("@sveltejs/vite-plugin-svelte");

await build({
    root: themeRoot,
    configFile: false,
    logLevel: "error",
    plugins: [stubPlugin, svelte()],
    css: {preprocessorOptions: {scss: {api: "modern"}}},
    build: {
        outDir,
        emptyOutDir: true,
        minify: false,
        cssCodeSplit: false,
        lib: {entry: path.join(here, "entry.ts"), formats: ["es"], fileName: "headless"},
    },
});

// ---------------------------------------------------------------- dom

const dom = new JSDOM("<!doctype html><html><body></body></html>", {
    // A real origin, or localStorage throws SecurityError on an opaque one.
    url: "http://localhost/",
    pretendToBeVisual: true,
});

const globals = [
    "window", "document", "navigator", "location", "history",
    "Node", "Element", "HTMLElement", "HTMLMediaElement", "HTMLInputElement",
    "HTMLSelectElement", "HTMLTextAreaElement", "HTMLButtonElement",
    "HTMLAnchorElement", "SVGElement", "DocumentFragment", "Text", "Comment",
    "Event", "CustomEvent", "MouseEvent", "KeyboardEvent", "InputEvent",
    "MutationObserver", "ResizeObserver", "getComputedStyle",
    "requestAnimationFrame", "cancelAnimationFrame", "localStorage",
    // Pickr (the colour picker) is a UMD bundle and reaches for `self`.
    "self", "HTMLCanvasElement", "Image", "DOMParser", "XMLHttpRequest", "CSS",
];

for (const key of globals) {
    const value = dom.window[key];
    if (value === undefined) continue;
    // `navigator` and friends are getter-only on globalThis in newer node.
    try {
        globalThis[key] = value;
    } catch {
        Object.defineProperty(globalThis, key, {value, configurable: true, writable: true});
    }
}

// On Windows a bare absolute path is not a valid ESM specifier.
/*
 * Svelte's transitions drive the Web Animations API, which jsdom does not
 * implement. Every setting row is wrapped in an in:slide, so without this the
 * first mount throws "element.animate is not a function".
 *
 * The fake finishes immediately, which is what a test wants anyway: no waiting
 * on 200ms of easing to read a committed value.
 */
if (!dom.window.Element.prototype.animate) {
    dom.window.Element.prototype.animate = function () {
        const animation = {
            currentTime: 0,
            startTime: 0,
            playbackRate: 1,
            playState: "finished",
            effect: {getComputedTiming: () => ({duration: 0})},
            finished: Promise.resolve(),
            onfinish: null,
            oncancel: null,
            play() {},
            pause() {},
            reverse() {},
            finish() {},
            cancel() {
                if (typeof animation.oncancel === "function") animation.oncancel();
            },
        };
        queueMicrotask(() => {
            if (typeof animation.onfinish === "function") animation.onfinish();
        });
        return animation;
    };
}

if (!dom.window.Element.prototype.getAnimations) {
    dom.window.Element.prototype.getAnimations = () => [];
}

const harness = await import(pathToFileURL(path.join(outDir, "headless.js")).href);

// ---------------------------------------------------------------- helpers

const {document} = dom.window;

// The HUD's SpaceSeperatedNames setting decides whether a row reads
// "AirWalker" or "Air Walker", so match on the name with spaces removed.
const squash = (text) => (text ?? "").replace(/\s+/g, "");

function toggle(root, settingName) {
    const label = [...root.querySelectorAll("label.switch-container")]
        .find((l) => squash(l.querySelector(".name")?.textContent) === squash(settingName));
    if (!label) {
        const seen = [...root.querySelectorAll("label.switch-container .name")]
            .map((n) => n.textContent.trim());
        throw new Error(`no switch named "${settingName}"; the pane shows [${seen}]`);
    }
    label.querySelector("input[type=checkbox]").click();
    harness.flushSync();
}

const settle = async () => {
    for (let i = 0; i < 8; i++) {
        await harness.tick();
        await new Promise((r) => setTimeout(r, 0));
        harness.flushSync();
    }
};

function booleanModule() {
    return {
        name: "Harness",
        valueType: "CONFIGURABLE",
        value: [
            {name: "AirWalker", valueType: "BOOLEAN", value: false},
            {name: "SwingSpeed", valueType: "BOOLEAN", value: false},
        ],
    };
}

// ---------------------------------------------------------------- cases

const results = [];

function check(name, actual, expected) {
    const pass = JSON.stringify(actual) === JSON.stringify(expected);
    results.push({name, pass, actual, expected});
    console.log(`${pass ? "  ok  " : "  FAIL"}  ${name}`);
    if (!pass) {
        console.log(`          expected ${JSON.stringify(expected)}`);
        console.log(`          actual   ${JSON.stringify(actual)}`);
    }
}

/*
 * Regression: the ClickGUI committed only the first setting change per module
 * selection (docs/known-issues.md). SettingsPane bound `bind:setting` into a
 * `filter()` copy, so after the first save's refetch every later change went
 * into an array nothing sends.
 *
 * The shape of the test is the shape of the bug: change things *without
 * reselecting the module*. Reselecting is what used to make it work again.
 */
console.log("\nSettingsPane — every change reaches the backend\n");

const target = document.createElement("div");
document.body.appendChild(target);
harness.mountSettingsPane(target, "Harness", booleanModule());
await settle();

toggle(target, "AirWalker");
await settle();
toggle(target, "AirWalker");
await settle();
toggle(target, "AirWalker");
await settle();
toggle(target, "SwingSpeed");
await settle();

check("four changes commit four times", harness.commits().length, 4);
check("AirWalker toggles on every click", harness.committedValues("AirWalker"), [true, false, true, true]);
check("a second setting still commits", harness.committedValues("SwingSpeed"), [false, false, false, true]);

// ---------------------------------------------------------------- report

fs.rmSync(outDir, {recursive: true, force: true});

const failed = results.filter((r) => !r.pass);
console.log(`\n${results.length - failed.length}/${results.length} passed\n`);
process.exit(failed.length === 0 ? 0 : 1);
