/*
 * Bundle entry for the headless harness.
 *
 * Everything here runs inside the vite bundle, so it sees the real components
 * with the stubs swapped in by scripts/headless/run.mjs. The driver stays
 * plain JavaScript and only calls what this exports.
 */

import {mount, flushSync, tick} from "svelte";
import SettingsPane from "../../src/routes/clickgui/SettingsPane.svelte";
import {__seed, __commits, __reset} from "../../src/integration/rest";

export {flushSync, tick};

/** Mount the real SettingsPane over a seeded module. */
export function mountSettingsPane(target: HTMLElement, name: string, configurable: unknown) {
    __reset();
    __seed(name, configurable);
    mount(SettingsPane, {target, props: {name, enabled: false, description: "harness"}});
}

/** What the UI has committed, in order. */
export function commits() {
    return (__commits as () => Array<{ name: string; settings: any }>)();
}

/**
 * The value the backend now holds for one setting of one module — read from
 * the last commit, which is what `modules.json` would have been written from.
 */
export function committedValues(settingName: string) {
    return commits().map((c) =>
        Array.isArray(c.settings?.value)
            ? c.settings.value.find((v: any) => v.name === settingName)?.value
            : undefined);
}
