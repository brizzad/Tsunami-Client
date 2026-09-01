# Feature status

Every item from the approved feature list, and where it actually stands.

Written because "we decided that already" is worth nothing if the decision only
exists in a conversation. Anything marked deferred here is a decision, not an
oversight, and carries the reason it was made.

Status meanings:

- **done** — built and checked in a running client, not merely compiled
- **present** — already existed in the fork; verified rather than rebuilt
- **deferred** — deliberately not built, with the reason given
- **built** — merged and compiling, with every injection point verified against
  the deobfuscated jar, but not yet seen in a running client
- **blocked** — cannot be built at all right now, with the blocker given

---

## PvP and combat — 19/19

| Item | Status | Notes |
| --- | --- | --- |
| Keystrokes | present | HUD component |
| CPS counter | done | `{session.cps.left}`; 0 at rest, 1–5 clicking |
| Combo counter | done | `{session.combo}`; 0 → 2 on a target |
| Armor status | done | `ArmorHud`, merged from uku's Armor HUD (MIT); theme component still there too |
| Potion status | present | Effects HUD, with duration and amplifier |
| Reach display | done | measured to the hitbox, not the entity origin |
| Hitboxes | done | `Hitboxes`, ported from Combat Hitboxes `1.0.11` (Apache-2.0) |
| Hit color | present | `HitFX` |
| Damage tint | done | edges 21.11 / 21.17, centre 0.000 |
| Crosshair | present | `Crosshair` |
| FOV changer | present | `NoFOV` |
| Toggle sneak/sprint | present | `Sneak`, `Sprint` |
| Macros / keybinds | present | `Macros` + keybinds HUD |
| MLG cobweb helper | done | `MlgHelper`; marks the landing, places nothing |
| Freelook | present | `FreeLook` |
| Speedometer | done | walking mean 4.06 b/s against vanilla's 4.317 |
| Shield status | done | bundled: Walksy's Shield Statuses `4.1.8+26.2` |
| Potion timers | done | `PotionTimers`, merged from Status Effect Bars `1.0.12` (LGPL-3.0) |
| Better Hitreg | done | `BetterHitreg`, source merged from jasspir `1.0.7+26.2` (Apache-2.0) |
| ~~Auto-totem~~ | excluded | automates a reaction that decides fights |

**Shield status is the upstream mod, not ours.** A first attempt drew a bar
above the hotbar, which is not what the feature means: Shield Statuses tints
the shield itself, orange while disabled and fading back as the cooldown runs
down, so the state is where your eyes already are. That mod is MIT, mature and
has millions of installs, so the version worth shipping was always the real
one. The home-made module was deleted rather than left beside it.

**Three modules were written and then deleted for existing mods.** Shield
Statuses, AppleSkin and Jade all do their job better than what was built here,
and Shield Statuses was named in the request before a line was written. The
habit that caused it is worth naming: reaching for "build it" when the question
was "does this already exist". If a row above says "bundled", the answer was
that it existed.

**Four more mods were assessed for merging; two went in.** The test each
time was whether the mod actually carries its own logic.

* **Status Effect Bars** merged cleanly. It targets 26.2, uses Mojang
  mappings like this fork, and its whole feature is 11 files. It became
  `PotionTimers`, and it adds something the Effects HUD did not have: a
  depletion bar on the icon rather than a number beside it.
* **uku's Armor HUD** merged, as `ArmorHud`. The part worth taking was the
  layout maths - the widget has to dodge the offhand slot and the attack
  indicator, both of which move for a left-handed player. Its bobbing
  warning sprite and break sound need bundled assets and were left out; the
  warning is a tint instead.
* **Shield Statuses could not be merged.** Its rendering is here, but the
  state it renders - `isCoolingDown`, disabled, rising - lives in WalksyLib,
  a separate library it depends on. Merging would mean reimplementing that
  state manager, which is exactly the home-made shield readout deleted
  earlier this project. It stays bundled, where it already works.
* **No minimap was merged.** See the note above: this fork already ships a
  complete one. Xaero's is All Rights Reserved with no published source, so
  it can be neither merged nor mirrored. The one GPL-compatible 26.2
  alternative, Immersive Minimaps, is a small overlay on the vanilla locator
  bar with no waypoint system of its own - swapping it in would replace a
  full minimap and a working `Waypoints` module with less than both.

**Two of that audit's deletions were then reversed.** Hitboxes and Better Hitreg
came back as modules, because "does a mod already do this" is only half the
test. The other half is whether the thing is an engine or a toggle. Sodium,
Jade and AppleSkin are engines: large, self-contained, nothing the client needs
to reach into. A hitbox colour and a hit-feedback switch are toggles, and a
toggle shipped as a jar sits outside the ClickGUI, outside profile export and
outside the keybind system - three things every other feature here has.
jasspir's mod is still the specification for what BetterHitreg does, and its
wording is quoted in the source: appearance only, actual hits unmodified.

## Performance — 10/12

| Item | Status | Notes |
| --- | --- | --- |
| FPS optimisation | done | Sodium |
| Entity rendering | done | EntityCulling |
| Occlusion culling | done | Sodium + EntityCulling |
| Lazy chunk loading | done | C2ME, off by default |
| Smooth lighting | done | Sodium |
| Fullbright / gamma | present | 29.42 → 49.48 mean luminance |
| RAM display | done | `{session.memory.percent}` |
| Sodium profile | done | default |
| Lithium, FerriteCore, ImmediatelyFast, EntityCulling, C2ME | done | versions pinned to real 26.2 Fabric builds |
| **Vulkan rendering** | built | vanilla's own backend, via `BundledMods` → `Vulkan`; see below |
| **ModernFix** | **blocked** | no 26.2 Fabric build; newest Fabric is 1.21.1 |
| **Shader support** | **deferred** | Iris `1.11.1+26.2-fabric` (LGPL-3.0) exists and fits; see below |
| ~~Starlight~~, ~~OptiFine~~ | excluded | both conflict with Sodium |

ModernFix is absent from Modrinth for 26.2 Fabric, re-checked on 2026-08-28 and
still absent. That is not a scoping decision, just a missing artifact.

**Vulkan stopped being a mod problem, which is why its row changed** (2026-09-01).
The blocked entry was for *VulkanMod*, a third-party renderer replacement, and
that still has no 26.2 build. It does not need one: **26.2 ships a Vulkan backend
in vanilla.** `com.mojang.blaze3d.vulkan.VulkanBackend` sits beside `GlBackend`
and `net.minecraft.client.PreferredGraphicsApi` picks between them. This fork was
already written for it: `MixinVulkanRenderPass` injects into the Vulkan path.

The switch is `BundledMods` → `Vulkan` → `Enabled`. It sets vanilla's
`preferredGraphicsBackend` and saves `options.txt`; the game reads that once at
startup, so it applies on the next launch. That is all it does.

**It was run, and the run overturned the design.** On an NVIDIA GTX 1660 Ti the
client starts on Vulkan 1.4.341 and logs `Using graphics backend Vulkan`, then
the full healthy-start sequence - `Launching Tsunami v0.40.1`,
`Reloaded theme 'Tsunami'`, `Initialized Browser API`,
`Tsunami has been successfully initialized` - with no exception anywhere in the
log.

The first implementation also had the launcher strip Sodium, Sodium Extra and
ImmediatelyFast on the way in, on the reasoning that a mod which replaces the
OpenGL renderer cannot survive a Vulkan one. **That was wrong, and the evidence
is direct:**

* Sodium `0.9.0+mc26.2` ships `DrawBackend` with `OPENGL`, `VK_MULTIDRAW` and
  `VK_INDIRECT`, a `chooseBackend()` that picks at runtime, and a
  `VKDrawContext` built on `org.lwjgl.vulkan.VkCommandBuffer`. Read out of the
  jar, not inferred. Sodium has a Vulkan renderer.
* ImmediatelyFast logged `Initializing ImmediatelyFast 1.16.0+26.2 ... with
  Vulkan 1.4.341`. It names the API it is running on.
* Sodium Extra only ever needed Sodium, which works.

So the filtering was removed and no mod is disabled. The lesson is the one this
document keeps recording: "these two things are incompatible" was a plausible
inference from how the mods used to work, and one `runClient` plus one `javap`
disproved it.

**The real cost is not a mod.** MCEF logs `GPU acceleration only supports the
OpenGL backend. Current backend: Vulkan`. The ClickGUI and themed HUD are a
Chromium browser, so under Vulkan they composite on the CPU. They still work -
the browser initialises and serves the theme - but that is the argument against
making Vulkan a default.

**Status is `built`, not `done`, and the remaining gap is specific: terrain has
not been seen drawn.** The client reaches the title screen and initialises
everything, but Tsunami replaces the title screen with its own browser screen,
which swallows vanilla's `--quickPlaySingleplayer`, so an automated run cannot
enter a world. Loading a world by hand and looking at it is what is left.

## HUD and visual — 21/27

| Item | Status | Notes |
| --- | --- | --- |
| Waypoints | done | beam + distance list, JSON on disk, per dimension |
| Coordinates | done | `{blockPosition.x}` |
| Direction HUD | done | `{session.direction.cardinal}` |
| Scoreboard / tab list | present | HUD component, `BetterTab` |
| Boss bar | present | hide via `AntiBlind` → `BOSS_BARS` |
| Chat customisation | present | `BetterChat` |
| Titles / nametags | present | `BetterTitle`, `Nametags` |
| Particle toggle | present | `Particles` |
| Chunk borders | done | frame diff 7.55 |
| Block outline | present | `BlockOutline` |
| Smooth zoom | present | `Zoom` |
| Item counter | done | `{session.held.count}` / `.total`, checked against 42 cobblestone |
| Item tracker | present | `InventoryTracker` |
| WAILA | done | bundled: Jade `26.2.11+fabric` |
| Shiny pots | done | potion slots 6.159, empty slots 0.000 |
| Weather / time changer | present | `CustomAmbience`, visual only |
| TNT countdown | present | `TNTTimer` |
| Height / fall danger | done | covered by coords plus `MlgHelper` |
| Uptime | done | `{session.uptime}` |
| Stopwatch | done | 0 → 3 → 6, then held at 6 when stopped |
| Durability warning | done | combo 4 → 0.00, sword durability frozen |
| Fire overlay reduction | present | `AntiBlind` → `FireOpacity` |
| AppleSkin food HUD | done | bundled: AppleSkin `3.0.10+mc26.2` |
| Minimap | present | **not** ported from Xaero's; see below |
| Motion blur | done | bundled: Natural Motion Blur `1.4.4` (LGPL-3.0), off by default |
| **Item physics** | **deferred** | ItemPhysic `1.8.15` does build for 26.2 now, but see below |
| 3D skins | done | bundled: 3D Skin Layers `1.11.2`; see the licence note below |
| **Glint colorizer** | **deferred** | ZEEG still needs Mod Menu, but it is no longer the only option; see below |
| 2D items | built | `FlatItems`, merged from beamingblue's Flat Items `1.1.1+26.2` (MIT) |
| **Resource pack organiser** | **deferred** | a Svelte UI project in its own right; no 26.2 mod either |
| ~~Real-world clock~~ | excluded | as specified |

**The minimap is not a port of Xaero's, and should not become one.** The fork
already ships a complete minimap — `MinimapHudComponent` with its own chunk
renderer, heightmap manager and texture atlas — registered as a native HUD
component and covered by the GPL we are already bound by. Xaero's Minimap is
proprietary, distributed as a compiled mod, with no redistribution or
derivative-works grant. Porting it would be unnecessary and unsafe. This is a
closed decision, not a pending question.

**Two of those five were wrong, and bundling fixed them.** Motion blur was
recorded as needing a post-processing pipeline the client does not have. It
does not need one: vanilla already has a post-effect chain, and Natural Motion
Blur uses it. 3D skins was deferred by association rather than on its own
merits. Both are now bundled and neither needed a line of render code here.

**That group was re-checked on 2026-08-28, and two thirds of it had gone
stale.** "No 26.2 build exists" was true when written and is a claim with a
shelf life; Modrinth moved and the doc did not. Re-check before trusting any
remaining blocker here.

**2D items is built.** `flat-items` `1.1.1+26.2` is MIT and its entire feature is two
mixins and a settings interface, so the source was merged as `FlatItems` rather
than bundled - upstream configures itself through Mod Menu, which this client
deliberately does not ship. The feared "entity and item renderer surgery" turned
out to be one `@Redirect` on the single `PoseStack.mulPose` call inside
`ItemEntityRenderer.submit`, plus an accessor for the baked quads. Both
injection points were verified against the deobfuscated 26.2 jar: `submit`
carries exactly one `mulPose` invocation, so the redirect is unambiguous.

**Item physics is now a licence problem rather than an availability one.**
ItemPhysic `1.8.15` builds for 26.2, so it can be bundled - but it cannot be
merged: it is LGPL-2.1-**only**, and without the "or later" clause there is no
upgrade path into a GPL-3.0 work. It also requires CreativeCore, so bundling it
costs two jars rather than one. That is a deliberate call to make, not a
blocker, which is why the row moved from blocked to deferred.

Glint colorizer was recorded as a near miss on ZEEG, which is still true -
ZEEG still declares Mod Menu as a *required* dependency, re-checked, and
bundling Mod Menu means a second settings UI. But ZEEG is no longer the only
26.2 option, which is what the old note got wrong. `enchantment-glint-outline`
`3.3` is **CC0-1.0** with 936k downloads, and Mod Menu is *optional* to it. CC0
is the most permissive licence there is, so it can be merged outright. Not
built here only because it was not what this batch was for.

The separate objection in the Feather audit below - that 26.2 has no glint
class left to inject into - deserves a re-check too, because these mods build
for 26.2 and therefore found something to hook. Whatever they hook is the
answer to that question.

## Social — 7/9

| Item | Status | Notes |
| --- | --- | --- |
| Friends list | present | `FriendManager` + `.friend` |
| Skin manager | present | `SkinChanger` |
| Cosmetics | present | `Wings`, `Hats`, rendered locally — no infrastructure to depend on |
| Server address / player count / ping | done | `{session.server.*}`, `{session.ping}` |
| Nick hider | present | `NameProtect` |
| Voice chat | done | Simple Voice Chat `fabric-2.6.22+26.2`, off by default |
| Replay | done | Replay Mod `26.2-2.6.27`, off by default |
| **Name history** | **blocked** | Mojang removed the endpoint |
| **Cross-server chat** | **deferred** | needs a chat server |
| **Screenshot uploader** | **deferred** | needs an image host |
| **Spotify** | **blocked** | Craftify is All Rights Reserved and has no 26.2 build; both are fatal |
| ~~IP protection~~, ~~launcher minigames~~ | excluded | as specified |

**Name history was tested, not assumed.**
`api.mojang.com/user/profiles/<uuid>/names` returns **404 NOT_FOUND**; the
session server still returns the current name only. The remaining route is
third-party aggregators, which means sending player UUIDs to someone else —
the exact category this fork spent a session removing. Not built.

## Mod tools — 3/3

| Item | Status | Notes |
| --- | --- | --- |
| Profile import / export | done | `.config export|import|profiles`, plain files |
| One-click Fabric mod install | present | launcher: install, list and delete custom mods |
| Anti-forced-resource-pack | done | `NoServerResourcePack`, two modes |
| **Community marketplace** | **deferred** | needs a backend |

## Trust — 1/1

`ServerIntegration` answers a partnered server asking what client this is. A
hook, not a detector: it reports this client and nothing else, never inspects
other players and never reports anybody. Off by default; truthful when on,
because a hook that lies is worse than none.

## Niche — 0/7, all dispositioned

| Item | Status | Notes |
| --- | --- | --- |
| Quickplay | present | `Macros` already binds a key to `/play …`; a second mechanism would be duplication |
| WorldEdit CUI | done | bundled: `worldedit-cui` `26.2+02` (EPL-2.0), off by default |
| **Hypixel stat overlay** | **deferred** | needs a user API key |
| **Network level display** | **blocked** | same API and key, and no LevelHead-style mod builds for 26.2 |
| **Game-specific timers** | **deferred** | same, plus per-gamemode parsing |
| **Auto-friend / GG / tip** | **deferred** | needs server-specific end-of-game detection |
| **MumbleLink / TeamSpeak** | **deferred** | `mumble-link-fabric` `0.13.2` (LGPL-3.0) does build for 26.2; the JNI work is upstream's, not ours |

The four Hypixel-shaped items are deferred as one piece of work rather than
four: they share an API client, key storage and rate limiting, and building
them separately would mean three redundant copies of each.

---

## Audited against Lunar Client

Lunar ships roughly seventy mods. Every one was checked against what this
fork already has, because the point of the exercise was to find gaps, not to
reimplement things twice.

**Almost all of it is already here.** Armor status, potion effects, keystrokes,
CPS, combo, reach, hitboxes, hit colour, hurt cam, crosshair, FOV, freelook,
toggle sneak and sprint, coordinates, direction, scoreboard, tab, titles,
nametags, chat, waypoints, zoom, block outline, chunk borders, shiny pots,
TNT countdown, stopwatch, day counter, item counter and tracker, memory, ping,
server address, nick hider, particles, weather and time, WAILA, 3D skins,
motion blur, WorldEdit CUI and Replay are all covered by a module, a HUD
component or a bundled mod.

**One real gap, and it was worth filling: `Cooldowns`.** Lunar treats this as a
headline mod and updated it in 2026; nothing on Modrinth provides it for
Fabric 26.2. It was written here rather than merged for that reason, and it
was cheap because the fork already carried `MixinItemCooldowns`, which exposes
the start, end and current tick of any cooldown. Verified in game: a goat horn
reads `Goat Horn 4.8s` with a bar draining behind it.

Deliberately not taken from Lunar:

| Lunar mod | Why not |
| --- | --- |
| Clock | Excluded in the brief - a real-world clock was cut as useless |
| Scrollable Tooltips | Only 26.2 mod is Skyblock-specific under a custom licence; building it means hooking tooltip render and scroll for very little |
| Menu Blur | `inventory-blur` is CC-BY-NC-4.0. NC cannot be merged into GPL, and taking an NC dependency is a bad fit for a project that may take donations |
| Item Physics, 2D Items, Glint Colorizer | No 26.2 Fabric build exists; see the render section above |
| Screenshot Uploader | Needs an image host - backend gap below |
| Hypixel Bedwars/Mods/Quickplay, UHC Overlay, Team View, PvP Info | Server-specific, and the Hypixel set needs a user API key |
| Mumble Link | JNI shared memory, no 26.2 mod |
| Pack Organizer | A Svelte UI project in its own right |
| ~~Pack Display~~ | **Built since**, as `PackDisplay`. Naming the applied packs is a four-line read of `PackRepository`; only the organiser is a UI project |
| Knockback Trainer | A practice tool, in scope but a feature batch of its own - flagged rather than started |
| Fog Customizer | Already `CustomAmbience` - `FogValueGroup` sets colour, environmental and render-distance bands, sky and cloud ends |
| Better Sounds | Reverb needs OpenAL EFX, which is a project rather than a module |
| ~~Color Saturation~~ | **Built since.** Recorded here as "shader work for cosmetic gain", which was wrong twice over: the fork already had a post-effect pipeline from MotionBlur, and colour grading is the single most-requested setting these clients ship. It is now `ColorSaturation` |
| Momentum, Snaplook, Auto Text Hot Key, Playtime, FPS | Already covered by the speedometer, `QuickPerspectiveSwap`, `Macros`, `{session.uptime}` and `{session.fps}` |

## Audited against Feather Client

Feather publishes its own mod list in its server API documentation
(<https://docs.feathermc.com/server-api/mods/>) — 93 mod ids with display names
and descriptions. That is the whole surface, from the vendor, rather than a
review site's summary, so it was checked line by line rather than sampled.

**Roughly two thirds were already here.** Animations, armour status, auto
perspective, auto text, block overlay, boss bar, brightness, combo, coordinates,
CPS, crosshair, custom chat, damage indicator, direction, FOV changer, FPS,
hitbox, inventory, item counter, keystrokes, mousestrokes, motion blur,
nametags, nick hider, perspective, ping, playtime, potion effects, reach
display, saturation, scoreboard, server address, snaplook, speed meter,
stopwatch, subtitles, tablist, time changer, title tweaker, TNT timer, toggle
sprint, totem, viewmodel, voice, waypoints, weather changer and zoom all map
onto an existing module, HUD component or bundled mod.

### Built from this audit — thirteen modules

| Feather mod | Built as | Notes |
| --- | --- | --- |
| `colorSaturation` | `ColorSaturation` | Post-process grade over the world frame: saturation, vibrance, contrast, brightness, gamma and white balance, plus six presets. Runs on the same `PostChain` machinery MotionBlur brought in |
| `lootBeams` | `LootBeams` | Rarity-coloured beam over dropped items, with a rarity floor and distance fade |
| `itemdespawn` | `ItemDespawn` | Outline that warms toward red and then flashes as the despawn clock runs out |
| `lightleveloverlay` | `LightLevels` | Marks ground a hostile mob can spawn on, with a hard marker budget so range cannot stall the render thread |
| `horses` | `HorseStats` | Speed in b/s and jump in blocks, read from the horse's own attributes |
| `packdisplay` | `PackDisplay` | Names the applied resource packs, built-ins hidden by default |
| `hitindicator` | `HitDirection` | Arc pointing at the source of damage already taken, for a second or two |
| `itemInfo1` | `PickupInfo` | Running tally of what entered or left the inventory, losses included |
| `toastcontrol` | `ToastControl` | Per-kind toast filter, cancelled at the queue so a hidden toast takes no slot |
| `reconnect` | `AutoReconnect` | Bounded retries with optional back-off; refuses to retry a disconnect that reads as a ban or kick |
| `culllogs` | `LogCleanup` | Deletes rotated `.log.gz` archives past an age. Dry run by default, never touches `latest.log` |
| `dropprevention` | `DropProtect` | Refuses the drop key on enchanted, rare, renamed or damageable items, and says so |
| `deathInfo` | `DeathInfo` | Death coordinates in chat plus an optional waypoint, written through the existing `WaypointManager` |

Two readouts were added to `SessionStats` rather than as modules, because that
is where `{session.fps}`, `{session.ping}` and `{session.memory.percent}`
already live:

* **`{session.tps}`** (Feather's `tps`) — measured, not asked for. A vanilla
  server sends a time update every 20 ticks, so the median gap between them is
  the tick rate. The ticking-state packet was rejected as a source: it reports
  the rate the server was *configured* with, which stays at 20 while a server
  lags. A server that suppresses time updates leaves this at 0 rather than at a
  fabricated number.
* **`{session.cpu}`** (part of Feather's `systemresources`, which Tsunami only
  covered for memory) — this process' CPU share, read reflectively so a JVM
  without `com.sun.management` reports -1 instead of failing.

### Already covered, so not rebuilt

| Feather mod | Where it already is |
| --- | --- |
| `customfog` | `CustomAmbience` → `FogValueGroup`: colour override, environmental and render-distance bands, sky and cloud ends. Clear water is `AntiBlind` → `LIQUIDS_FOG` |
| `shulkertooltips` | Vanilla. `ItemContainerContents` implements `TooltipProvider` and prints the contents itself; only Feather's graphical grid is extra, and a bespoke `ClientTooltipComponent` is not worth it for a layout change |
| `subtitles` | Already replaced by the theme HUD through `ClosedCaptionsEvent`; `MixinSubtitleOverlay` wraps the vanilla path in four places |
| `blockIndicator` | Jade, bundled |
| `armorBar`, `hearts` | `ArmorHud`. The compact single-row health rewrite would mean cancelling `Gui.extractPlayerHealth`, which `HudComponentTweak.DISABLE_STATUS_BAR` already claims |
| `autohidehud` | `AntiBlind`'s `DoRender` set plus the HUD component tweaks |
| `teamtracker`, `mobOverlay` | Out of scope — both are entity locators through terrain, which is the wallhack line |
| `jumpreset`, `totem` (auto) | Out of scope — automating or coaching a reaction that decides a fight |
| `time` | Excluded in the brief; a real-world clock was cut as useless |

### Checked and left alone, with the reason

| Feather mod | Why not |
| --- | --- |
| `glint` | **There is no glint class in 26.2 at all** — the whole `net.minecraft.client...glint` family is gone, checked against the deobfuscated jar rather than assumed. The earlier ZEEG note still stands, but the real blocker is now that there is no obvious injection point to colour |
| `uiScaling` | Feather ships this with "USE WITH CAUTION" in its own description. `Window.calculateScale` is injectable, but a GUI scale that goes wrong is unrecoverable without editing options.txt by hand |
| `backups` | `LevelStorageAccess.makeWorldBackup` runs from the world-selection screen with the world closed. Calling it mid-session needs a forced save first or the archive is a torn one, and a backup you cannot trust is worse than none. Worth doing properly, as its own piece of work |
| `soundfilters` | Reverb is OpenAL EFX. Same answer as Lunar's Better Sounds |
| `screenshot` | Vanilla already prints a clickable "saved as" line. Clipboard image copy would mean AWT, which does not coexist with LWJGL on macOS |
| `customf3`, `customadvancementsscreen`, `darkmode`, `searchkeybind`, `elytras`, `playerModel`, `camera` | Screen-chrome polish. In scope and cheap individually, but none of them is a gap anybody has named |
| `hypixel`, `uhcoverlay`, `tiertagger` | Server-specific, and two of the three need a user API key — the same batch already deferred in the niche section |
| `itemPhysic`, `packOrganizer` | Unchanged from the render section above |

## Everything is configurable in the ClickGUI

The strategy doc has one hard rule: every mod, merged or bundled, is configured
through Tsunami's ClickGUI rather than its own screen. That was true of the
merged modules and only a third true of the bundled ones, so it was audited
end to end.

**Merged modules were already right.** Each is a `ClientModule`, so each has a
toggle and a keybind for free, and the settings were already complete:
BetterHitreg 9, Hitboxes ~20, ArmorHud 15, PotionTimers 8, ColorSaturation 8,
FlatItems 4, MotionBlur 3. What BetterHitreg leaves out - the metronome, the
combat analytics - is scope, recorded in its `package-info.java`, not an
oversight.

**Bundled mods were the gap: five of twenty had any ClickGUI presence.** Now
eleven do, 209 settings in all.

| Mod | Settings | Note |
| --- | ---: | --- |
| Sodium | 14 | was 5; the other nine are real options from its own page |
| Sodium Extra | 45 | was unbridged - FPS readout, animations, particles, toasts |
| Jade | 83 | already done |
| MoreCulling | 12 | TOML, needed `LineConfigStore` |
| BadOptimizations | 12 | `key: value` text |
| ShieldStatuses | 12 | array-of-named-records, needed `NamedRecordConfigStore` |
| AppleSkin | 9 | JSON5; was skipped on a false claim about NeoForge TOML |
| Ixeris | 8 | TOML |
| SkinLayers | 6 | already done |
| ImmediatelyFast | 4 | already done |
| EntityCulling | 4 | already done |

**Not bridged, with the reason:**

- **Lithium, FerriteCore** - nothing a player should see. Their properties
  files are mixin kill-switches for debugging, empty by default.
- **C2ME, Replay Mod, WorldEdit CUI** - off by default and have never written
  a config, so there are no keys to read. Needs a launch each; a guessed key
  is the exact failure `scripts/verify-bridge-keys.mjs` exists to catch.
- **Simple Voice Chat** - deliberate hold. Half-bridging a microphone and a
  push-to-talk key is how someone transmits while believing they are muted.
- **ViaFabricPlus** - **no technical reason.** Its `settings.json` is ordinary
  nested JSON that `JsonConfigStore` already handles. Simply not done.

**The limit that cannot be fixed here:** a jar cannot be unloaded at runtime,
so for most bundled mods the ClickGUI reconfigures but cannot switch off - that
is the launcher's per-build mod list. Shield Statuses and Ixeris are the
exceptions, because they carry their own enable flags.

**Verified in a running client, not compiled.** The ClickGUI cannot be clicked
by a script, but it is only a client of the interop server the game runs, so
every setting was pushed through the same `PUT /client/modules/settings` the
ClickGUI uses and read back. 205 bundled settings committed; Sodium's writes
were followed all the way into `run/config/sodium-options.json`. The fourteen
Feather modules were checked the same way: 14/14 toggles, 81/81 settings.
`CLAUDE.md` has the recipe.

## The backend gap — half closed, 2026-09-01

There is a backend now. `https://brizzad.github.io/tsunami-api` serves the build
catalogue, launch manifests, mod lists and changelog as static JSON from GitHub
Pages, generated from the `tsunami-api` repo. The launcher reads it: driven on
2026-09-01 it logged `Fetched 1 build(s) from https://brizzad.github.io/tsunami-api`,
with `local_build.rs` kept as a fallback for when the host is unreachable.

**That unblocks two of the five and none of the other three**, and the split is
worth understanding rather than re-checking each time:

| Item | Now |
| --- | --- |
| Update path, auto-config (`BACKEND_CONFIGURED`) | **unblocked** — these needed a catalogue to read, and there is one |
| Marketplace | **still blocked** — needs writes |
| Cross-server chat | **still blocked** — needs a live service, not files |
| Screenshot uploader | **still blocked** — needs somewhere to put an image |

The three that remain are blocked on the *same* thing as each other and a
different thing from what was just built. Static files can serve a catalogue
forever at no cost; they cannot accept an upload, hold a socket open, or store
a message. Those three need a real service with a real bill, and standing up
the catalogue did not move them an inch closer. Do not read "the backend
exists" as "the backend gap is closed".

**The next step is on the launcher side, not here:** the client jar is still not
published anywhere, so a launch depends on a file copied into `custom_mods` by
hand. `docs/backend-contract.md` in the launcher repo has the options.
