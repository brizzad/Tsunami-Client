/*
 * Stub for src/integration/rest.ts.
 *
 * Stands in for the client's HTTP backend with an in-memory store, and — the
 * whole point — records every payload the UI commits, so a test can assert on
 * what actually reached the backend rather than on what the screen says.
 *
 * It deliberately mirrors the real backend in the one respect that mattered
 * for the ClickGUI binding bug: `setModuleSettings` stores a deep copy, so a
 * later `getModuleSettings` hands back *fresh objects*. Serving the same
 * object identities back would hide exactly the class of fault this exists to
 * catch.
 */

const modules = new Map<string, any>();
const commits: Array<{ name: string; settings: any }> = [];

const copy = <T>(value: T): T => JSON.parse(JSON.stringify(value));

export async function getModuleSettings(name: string) {
    // theme_config.ts asks for "HUD" on import; anything unseeded gets an
    // empty configurable rather than a crash.
    return copy(modules.get(name) ?? {name, value: []});
}

export async function setModuleSettings(name: string, settings: any) {
    commits.push({name, settings: copy(settings)});
    modules.set(name, copy(settings));
}

export async function setModuleEnabled(name: string, enabled: boolean) {
    commits.push({name, settings: {enabled}});
}

/** Seed a module's settings as the backend would hold them. */
export function __seed(name: string, configurable: any) {
    modules.set(name, copy(configurable));
}

/** Every payload committed so far, oldest first. */
export function __commits() {
    return commits;
}

export function __reset() {
    modules.clear();
    commits.length = 0;
}
