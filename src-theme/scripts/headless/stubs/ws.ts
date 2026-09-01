/*
 * Stub for src/integration/ws.ts.
 *
 * The real module opens a WebSocket to the client the moment it is imported,
 * and theme_config.ts imports it transitively from almost everything. Nothing
 * here needs a live socket, so listeners are recorded and never fired.
 */

type Callback = (...args: any[]) => void;

const listeners = new Map<string, Callback[]>();

function add(name: string, callback: Callback) {
    const existing = listeners.get(name) ?? [];
    existing.push(callback);
    listeners.set(name, existing);
}

export function listen(name: string, callback: Callback) {
    add(name, callback);
}

export function listenAlways(name: string, callback: Callback) {
    add(name, callback);
}

/** Fire an event by hand, for a test that wants to prove a listener reacts. */
export function __emit(name: string, event: unknown) {
    listeners.get(name)?.forEach((callback) => callback(event));
}
