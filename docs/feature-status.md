# Feature status

Every item from the approved feature list, and where it actually stands.

Written because "we decided that already" is worth nothing if the decision only
exists in a conversation. Anything marked deferred here is a decision, not an
oversight, and carries the reason it was made.

Status meanings:

- **done** — built and checked in a running client, not merely compiled
- **present** — already existed in the fork; verified rather than rebuilt
- **deferred** — deliberately not built, with the reason given
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

## Performance — 9/11

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
| **VulkanMod profile** | **blocked** | no 26.2 release exists; newest is 26.1.2 |
| **ModernFix** | **blocked** | no 26.2 Fabric build; newest Fabric is 1.21.1 |
| ~~Starlight~~, ~~OptiFine~~ | excluded | both conflict with Sodium |

Neither blocker is a scoping decision. Both are absent from Modrinth, checked
rather than assumed. The VulkanMod profile exists by name and installs nothing,
with the reason written where its entry would go. Verifying that every UI
element renders under Vulkan cannot be done while it cannot be installed.

## HUD and visual — 20/27

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
| **Item physics** | **blocked** | no Fabric mod with a 26.2 build exists to merge or bundle |
| 3D skins | done | bundled: 3D Skin Layers `1.11.2`; see the licence note below |
| **Glint colorizer** | **deferred** | only 26.2 option is ZEEG, which requires Mod Menu; see below |
| **2D items** | **blocked** | same: nothing on Modrinth builds for 26.2 |
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

What is left in that group is genuinely blocked rather than deferred. Item
physics and 2D items have no Fabric mod with a 26.2 build at all - checked
against Modrinth, not assumed - so there is nothing to merge and nothing to
bundle, and building either from scratch means the entity and item renderer
surgery that silently broke four modules once already.

Glint colorizer is a near miss worth recording. ZEEG is the only 26.2 option,
it is MIT with source, and it would be mergeable except that it declares Mod
Menu as a required dependency and reaches item rendering through eight mixins.
Bundling it would mean bundling Mod Menu, which is a second settings UI - the
exact thing the bundled-mod config bridge exists to avoid - and merging it
means eight mixins into item rendering for a mod with 322 downloads. Skipped
on the strategy note that says to skip rather than sink effort into a corner.

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
| **MumbleLink / TeamSpeak** | **blocked** | JNI shared memory, and no 26.2 Fabric mod exists to bundle |

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
| Pack Organizer, Pack Display | A Svelte UI project in its own right |
| Knockback Trainer | A practice tool, in scope but a feature batch of its own - flagged rather than started |
| Better Sounds, Color Saturation, Fog Customizer | Fog and ambience are already `CustomAmbience`; the other two are shader work for cosmetic gain |
| Momentum, Snaplook, Auto Text Hot Key, Playtime, FPS | Already covered by the speedometer, `QuickPerspectiveSwap`, `Macros`, `{session.uptime}` and `{session.fps}` |

## The backend gap

Five items across three categories are blocked on the same missing thing: the
marketplace, cross-server chat, the screenshot uploader, and the update and
auto-config paths already gated behind `BACKEND_CONFIGURED`.

`docs/backend-contract.md` in the launcher repo specifies it. Nothing serves it.
It is the single largest unblock available.
