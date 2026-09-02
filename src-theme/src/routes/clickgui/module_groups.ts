/*
 * Display grouping for the ClickGUI.
 *
 * This is a **presentation concern only**. It does not touch how a module
 * registers itself, and the Kotlin side is unchanged - `ModuleCategories`
 * still says what it always said.
 *
 * Why it exists: upstream's categories sort modules by the kind of unfair
 * advantage they give, which is the right axis for a cheat client and a
 * useless one here. After the strip, the real distribution is:
 *
 *     Render 49 | Misc 13 | Player 4 | Movement 2 | World 1
 *     Combat 0  | Exploit 0 | Fun 0
 *
 * Three categories are empty, and more than two thirds of everything is in
 * "Render" - because once the cheats are gone, nearly every remaining module
 * is "draws something on screen". Sorting by that tells a player nothing.
 *
 * These groups sort by *what you would go looking for instead*. They are
 * landmarks in one scrollable list, not filters: every module is always on
 * screen and always reachable by scrolling or searching.
 */

/** Group order is display order. */
export const GROUP_ORDER = [
    "Combat",
    "Interface",
    "Visuals",
    "World",
    "Items",
    "Utility",
] as const;

/**
 * Module name -> group.
 *
 * A module missing from this map is **not hidden**. It falls back to its own
 * upstream category, which becomes a group of its own at the end of the list -
 * see `groupOf`. That way adding a module to the Kotlin side without touching
 * this file leaves it visible and searchable rather than silently dropped,
 * which is the failure mode a hand-maintained map like this would otherwise
 * have.
 */
const GROUPS: Record<string, string> = {
    // Combat - what happened in a fight, and what you need mid-fight.
    ArmorHud: "Combat",
    BetterHitreg: "Combat",
    Cooldowns: "Combat",
    Crosshair: "Combat",
    DamageParticles: "Combat",
    DamageTint: "Combat",
    HitDirection: "Combat",
    HitFX: "Combat",
    Hitboxes: "Combat",
    MlgHelper: "Combat",
    NoHurtCam: "Combat",
    PotionTimers: "Combat",
    ShinyPots: "Combat",
    TotemEffect: "Combat",

    // Interface - the client's own chrome and readouts.
    BetterChat: "Interface",
    BetterInventory: "Interface",
    BetterTab: "Interface",
    BetterTitle: "Interface",
    ClickGUI: "Interface",
    GUICloser: "Interface",
    HUD: "Interface",
    Nametags: "Interface",
    Notifier: "Interface",
    PackDisplay: "Interface",
    Stopwatch: "Interface",
    ToastControl: "Interface",

    // Visuals - how the world and your own view are drawn.
    Animations: "Visuals",
    AntiBlind: "Visuals",
    AutoF5: "Visuals",
    BlockOutline: "Visuals",
    ColorSaturation: "Visuals",
    CustomAmbience: "Visuals",
    FlatItems: "Visuals",
    FreeLook: "Visuals",
    FullBright: "Visuals",
    Hats: "Visuals",
    JumpEffect: "Visuals",
    MotionBlur: "Visuals",
    NoBob: "Visuals",
    NoFOV: "Visuals",
    NoSwing: "Visuals",
    Particles: "Visuals",
    QuickPerspectiveSwap: "Visuals",
    SkinChanger: "Visuals",
    SmoothCamera: "Visuals",
    Wings: "Visuals",
    Zoom: "Visuals",

    // World - reading the terrain you are standing in.
    ChunkBorders: "World",
    DeathInfo: "World",
    HorseStats: "World",
    LightLevels: "World",
    TNTTimer: "World",
    Waypoints: "World",

    // Items - what you are carrying and what is on the ground.
    DropProtect: "Items",
    DurabilityGuard: "Items",
    InventoryTracker: "Items",
    ItemDespawn: "Items",
    ItemScroller: "Items",
    ItemTags: "Items",
    LootBeams: "Items",
    PickupInfo: "Items",

    // Utility - the client looking after itself and after you.
    AntiExploit: "Utility",
    AutoReconnect: "Utility",
    AutoRespawn: "Utility",
    BundledMods: "Utility",
    Debug: "Utility",
    LogCleanup: "Utility",
    Macros: "Utility",
    NameProtect: "Utility",
    NoServerResourcePack: "Utility",
    ServerIntegration: "Utility",
    Sneak: "Utility",
    Sprint: "Utility",
    TextFieldProtect: "Utility",
};

/**
 * The group a module is displayed under.
 *
 * Falls back to the module's own category so nothing can go missing, and the
 * fallback is visible in the UI rather than silent - an unmapped module shows
 * under a group named after its category, which is a legible prompt to add it
 * here.
 */
export function groupOf(name: string, category: string): string {
    return GROUPS[name] ?? category;
}

/** Groups in display order: the curated ones first, then any fallbacks. */
export function orderGroups(present: Iterable<string>): string[] {
    const seen = new Set(present);
    const known = GROUP_ORDER.filter((g) => seen.has(g));
    const rest = [...seen].filter((g) => !GROUP_ORDER.includes(g as never)).sort();
    return [...known, ...rest];
}
