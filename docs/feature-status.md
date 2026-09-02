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
| **Vulkan rendering** | done | vanilla's own backend, via `BundledMods` → `Vulkan`; see below |
| **ModernFix** | **blocked** | no 26.2 Fabric build; newest Fabric is 26.1.2, re-checked 2026-09-01 |
| Shader support | done | bundled: Iris `1.11.2+26.2-fabric` (LGPL-3.0), on by default and inert until a pack is chosen |
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

**Terrain was then checked in a world, and it renders.** The client's own
`POST /api/v1/client/worlds/join` loads a save without touching the UI - which is
the way round the title-screen problem, since Tsunami replaces the title screen
with its own browser one and that swallows vanilla's `--quickPlaySingleplayer`.
With a world open on each backend and the window captured both times, the Vulkan
frame is indistinguishable from the OpenGL frame at the same position. Frame rate
is comparable as well: 250-280 either way on a GTX 1660 Ti. No render or Sodium
exception appears in the log.

**One false alarm is worth recording, because the mistake is easy to repeat.**
The first Vulkan capture looked badly corrupted - a flat red plane over the
ground, blue bands, beams through the sky - and that was read as broken terrain.
It was not. It is what this client looks like with seventy modules on:
`LightLevels` paints ground a mob can spawn on, `ChunkBorders` draws the vertical
beams, `Waypoints` draws its marker. The OpenGL baseline showed exactly the same
picture. **Do not judge a render change from a screenshot without a same-scene
baseline on the other path** - a client this dense in overlays looks alarming
when it is working perfectly.

**The one real difference is the browser, not the world.** MCEF logs
`GPU acceleration only supports the OpenGL backend. Current backend: Vulkan`, and
the window title says `Accelerated Paint is ON` on OpenGL and does not on Vulkan.
The ClickGUI and themed HUD composite on the CPU under Vulkan. They work - both
captures show the HUD drawn correctly - but that is the reason not to make Vulkan
the default.

**Behaviour worth knowing: the ClickGUI value wins at startup.** Loading the
config fires the setting's `onChanged`, so the stored toggle is written back to
`options.txt` every launch. A backend chosen in vanilla's video settings survives
only until the next start. Observed rather than assumed - a session launched on
OpenGL with the toggle stored as on rewrote the option to `vulkan` while running.


**Four approved features were added on 2026-09-01, and one could not be.**

* **Shaders** ship as Iris. The old note said Iris was deferred because it wanted
  a newer Sodium than the bundle pinned. That was half wrong twice over. The
  Sodium pin was held at 0.9.0 on the belief that 0.9.1 was an alpha, and it is
  not - it is a release published 2026-07-08. And Iris never constrained the
  decision at all: both 1.11.1 and 1.11.2 declare `sodium: ["0.9.x"]` in their
  own jars, so either runs on either. The version ids Modrinth lists as
  dependencies are advisory, not what Fabric enforces. Sodium is now 0.9.1 and
  Iris 1.11.2.
* **Item physics** is bundled with CreativeCore. It cannot be merged -
  LGPL-2.1-*only* has no upgrade path into GPL-3 - and that costs a second jar,
  which is a deliberate call rather than an oversight. Worth knowing while
  testing: `FlatItems` also changes how dropped items draw, so judge one at a
  time.
* **The glint colorizer** is bundled rather than merged, even though CC0 permits
  merging. The mod is not the small tweak the licence implies: 2,059 lines
  across 34 files, eleven mixins plus a Sodium-specific one, its own GLSL
  outline shaders and its own accesswidener. It also settles the old question of
  what there is to hook in 26.2, where the glint class family is gone - it hooks
  `RenderType`/`RenderSetup`, `EquipmentLayerRenderer`, `ItemRenderState` and
  `ModelPart` instead.
* **Xaero's Minimap** is bundled, off by default. The licence check came first
  and settled the shape: All Rights Reserved with no published source, so there
  is no derivative-works grant and nothing to derive from, and the approved
  "port with minimal changes" is not available. Bundling stands on the footing
  3D Skin Layers already established - the user's own launcher fetches from the
  author's distribution, so nothing is redistributed here. **If Tsunami ever
  mirrors these artifacts, both entries have to be revisited first.** It is off
  by default because this fork already ships a complete minimap and enabling
  both draws two.
  * **Its radar ships off, and that is a scope decision.** `display_radar` draws
    players and mobs on the minimap whether or not you can see them, which is an
    entity locator through terrain. The setting is exposed rather than removed so
    the choice is visible and the player's own, but it is off and should stay so.
* **Spotify still cannot be done.** Craftify has no 26.2 build - its newest is
  1.21.11 - and it is All Rights Reserved. Either alone would block it.

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
| Player health | done | bundled: Player Health Indicators `1.1.2` (MIT); replaced the `Nametags` Health text part |
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
| Item physics | done | bundled: ItemPhysic `1.8.15` + CreativeCore `2.14.16`; bundle-only, LGPL-2.1-only |
| 3D skins | done | bundled: 3D Skin Layers `1.11.2`; see the licence note below |
| Glint colorizer | done | bundled: Enchantment Glint Outline `3.3` (CC0); see the merge-vs-bundle note below |
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
| **Spotify** | **blocked** | Craftify is All Rights Reserved and stops at 1.21.11; re-checked 2026-09-01, both still fatal |
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

**Player health moved from a nametag part to the real mod (2026-09-02).**
`NametagTextFormatter` had a `HEALTH` part printing `18 HP` in green/yellow/red.
It was removed at Nathan's direction and replaced by **Player Health Indicators**
`1.1.2` (MIT), which draws vanilla heart sprites on the player instead of text
above them.

The fork matters here. The original `playerhealthindicators` by Gaider10 stops at
1.21, so a fork is unavoidable; there are two, and only one is acceptable:

* `player-health-indicators-unofficial` - **bundled.** MIT, no dependencies
  beyond Fabric API, a faithful continuation.
* `player-health-indicators-invisible-support` - **rejected**, despite being the
  more popular of the two at 258k downloads. Its entire reason for existing is to
  show hearts for players under invisibility. That reveals a player you were not
  meant to see, which is the entity-locator line.

**It cannot see through terrain, and that was read out of the jar rather than
assumed.** The renderer submits with `RenderTypes.entityCutout`, which is
depth-tested, so a wall occludes the hearts exactly as it occludes the player.
The old nametag part needed `Nametags -> RequireLineOfSight` to get the same
property, because nametags draw in screen space after the world.

Three settings bridged, keys and defaults from `javap` on `Config` and the file
name from its constant pool: `Enabled` (`renderingEnabled`), `HeartStacking`,
`HeartOffset`.

**Crystal optimisers: one of four, off by default, and the reasoning is the
point.** Nathan asked for "any crystal optimizer". None of them is what the name
suggests - **not one is a rendering or FPS optimisation.** Every mod in the
category cuts the delay on placing and breaking end crystals. The four available
for 26.2 Fabric:

| Mod | What it does | Licence | Verdict |
| --- | --- | --- | --- |
| Marlow's | removes a broken crystal locally instead of waiting for the server | MIT | **bundled, off by default** |
| Client Side Crystals | renders your own placement instantly, "to look like zero ping" | ARR | not bundled |
| Kind's | handles place *and* break client-side so actions feel instant | ARR | not bundled |
| FastCrystal | **removes all interact and attack cooldowns**, duplicates attack packets | ARR | **excluded** |

FastCrystal and Kind's are the excluded category outright - removing an attack
cooldown is automating a reaction that decides a fight, which `CLAUDE.md` lists
as permanently out of scope. They are not bundled at any setting.

Marlow's was chosen and it is still a judgement call rather than an obvious one,
so here is the whole basis. It is the only MIT option. It has 4.4M downloads. And
uniquely in the category it ships a **server opt-out protocol** - `OptOutPacket`,
`ChallengePacket`, `ChallengeResponsePacket`, `OptOutCache` in the jar - so a
partnered server can tell the client to disable it and the client complies. That
is the same shape as `ServerIntegration` and it is the strongest fair-play
signal available here.

It is **off by default** because it is latency compensation on a fight-deciding
action rather than a readout of something the game already told you. Shipping it
switched off puts the choice in front of the player instead of making it for
them, the same call as Xaero's radar.

**It has no config, so it has no ClickGUI bridge.** No config class exists in the
jar - checked, not assumed. Its only switch is the launcher's mod list, which
removes the jar rather than quieting it. That is the documented answer for a mod
with no off flag of its own.

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

**Bundled mods were the gap: five of twenty-four had any ClickGUI presence.**
Now fifteen do, 243 settings in all.

| Mod | Settings | Note |
| --- | ---: | --- |
| Jade | 83 | already done |
| Sodium Extra | 45 | was unbridged - FPS readout, animations, particles, toasts |
| Sodium | 14 | was 5; the other nine are real options from its own page |
| MoreCulling | 12 | TOML, needed `LineConfigStore` |
| BadOptimizations | 12 | `key: value` text |
| ShieldStatuses | 12 | array-of-named-records, needed `NamedRecordConfigStore` |
| ItemPhysic | 10 | nested JSON; `vanillaRendering` is a real off switch |
| AppleSkin | 9 | JSON5; was skipped on a false claim about NeoForge TOML |
| XaerosMinimap | 9 | `key = value`, needed `equalsSeparated`; radar ships off |
| Ixeris | 8 | TOML |
| GlintOutline | 8 | flat JSON; colour is `[r, g, b]`, needed `readArray` |
| Iris | 7 | `.properties`; `enableShaders` is a real off switch |
| SkinLayers | 6 | already done - and silently broken until 2026-09-01, see below |
| ImmediatelyFast | 4 | already done |
| EntityCulling | 4 | already done |

**Two bridges were writing nothing, and the failure mode is worth naming.**
`applyTo` gates every write on `isModLoaded(modId)`, which takes the **Fabric**
mod id from `fabric.mod.json` - not the Modrinth slug the launcher installs by.
Where the two differ, the key still resolves, the ClickGUI stores the value and
shows it back, `verify-bridge-keys.mjs` still passes, and the write is dropped
with a chat line. Nothing says the setting did nothing.

`3dskinlayers` should have been `skinlayers3d` - the slug and the id are
reversed - so all six SkinLayers settings had never written anything since the
bridge was built. `enchantment-glint-outline` should have been
`enchant-outline`, caught the same way before it shipped.

`verify-bridge-keys.mjs` now checks every bridge id against the fabric mod id of
an installed jar, and distinguishes "not installed here" from "no jar declares
this, but one nearby does" - the second is the bug and fails the run.

**Not bridged, with the reason:**

- **Lithium, FerriteCore** - nothing a player should see. Their properties
  files are mixin kill-switches for debugging, empty by default.
- **Cloth Config, WalksyLib, CreativeCore** - libraries, not features. They are
  in the bundle because MoreCulling, Shield Statuses and ItemPhysic will not
  load without them.
- **C2ME** - off by default, an optimisation, and nothing on screen to switch.
- **WorldEdit CUI** - loaded in the dev catalogue on 2026-09-02 and it writes no
  config at all. `javap` on `CUIConfiguration` shows `debugMode`,
  `promiscuous`, `clearAllOnKey` and a list of colours - **no enable flag
  exists**, so there is nothing to bridge. It also draws nothing unless you are
  on a WorldEdit server with a selection made. The toggle is the launcher's mod
  list.
- **ViaFabricPlus** - has no on/off either. Its `settings.json` holds
  `selected-protocol-version`, and when that is the native version - the
  default - the mod is inert. The meaningful control is a protocol selector, not
  a switch, and that is the flagship version-switching feature rather than
  something to turn off.

**Two of these were bridged on 2026-09-02, and the way it was done is the point:**

- **Simple Voice Chat** - the old note called this a deliberate hold, and the
  reasoning still stands for the *fine* settings: half-bridging a microphone and
  a push-to-talk key is how someone transmits while believing they are muted.
  But the mod has a single flag, `disabled`, whose own comment reads "both sound
  and microphone off". That is the opposite of the hazard - one unambiguous
  master - so `VoiceChat -> Enabled` bridges it and nothing else. Everything
  finer stays in the mod's own screen, where the state on display is the state
  that is live.
- **Replay Mod** - had never written a config, so it was added to the dev
  catalogue and launched once, which is how a key gets read rather than guessed.
  It has no single enable flag; `Enabled` is synthesised over
  `recording.recordSingleplayer`, `recordServer` and `autoStartRecording`, so
  off means it records nothing - which is what off means for a recorder.

Its mod id is `voicechat`, not the Modrinth slug `simple-voice-chat`. That is
the third time that mismatch has come up here and the reason
`verify-bridge-keys.mjs` runs: 14/14 ids now match an installed jar.

**The limit that cannot be fixed here:** a jar cannot be unloaded at runtime, so
for a bundled mod the ClickGUI can only reconfigure, not remove - what it ships
at all is the launcher's per-build mod list.

Twelve can be switched off from the ClickGUI, which is as close to a toggle as a
bundled jar gets:

| Mod | The switch | What off means |
| --- | --- | --- |
| Shield Statuses | its own enable flag | stops tinting |
| Ixeris | per-platform enable flags | input handling returns to vanilla |
| Iris | `enableShaders` | rendering goes straight back to Sodium |
| ItemPhysic | `vanillaRendering` | dropped items render the vanilla way |
| Xaero's Minimap | `display_minimap` | draws nothing; the built-in minimap remains |
| Glint Outline | `enabled` | the vanilla glint comes back |
| Jade | `general.displayTooltip` | the overlay does not draw |
| 3D Skin Layers | every layer flag at once | layers fall back to flat vanilla |
| AppleSkin | every `show*` flag at once | food HUD and tooltips go back to vanilla |
| Simple Voice Chat | `disabled` | both sound and microphone off |
| Replay Mod | every recording flag at once | records nothing |
| Player Health Indicators | `renderingEnabled` | no hearts drawn |

**The last two were added on 2026-09-02, and the sentence that used to stand
here - "the rest reconfigure only, that is a property of the mods, not of the
bridge" - was wrong twice over.** Nathan reported both as un-toggleable and both
were, but not for the reason recorded:

* **Jade has a true master flag and it was already bridged.** `displayTooltip`
  false means the overlay does not draw at all. It was exposed as **"Tooltip"**,
  three levels down under `Jade -> General`, where nobody looking to turn Jade
  off would think to look. Same key, same write, moved to the top of the group
  and named `Enabled`. Nothing about the mod prevented this.
* **3D Skin Layers has no single flag, but it does have a complete set** - one
  per body part. `Enabled` is synthesised: off writes all seven layer keys
  false in one click and every layer falls back to the flat vanilla rendering
  the mod replaced. That is a real off switch, just spelled in seven booleans.

The lesson is the one this document keeps recording. "That is a property of the
mods" was an inference, and reading the mods' own config files off disk - which
is what the bridge audit was *for* - disproves it in both cases. Two more are
the same shape. **AppleSkin** was done for the same reason - it is not an
optimisation, it draws a saturation overlay, an exhaustion underlay and food
values on tooltips, and a mod you can see is a mod you can turn off.
**BadOptimizations** was deliberately left alone: it is pure optimisation with
nothing on screen, so there is nothing to switch off. That is Nathan's rule and
it is the right cut - **if a mod is not buying frames, it must be toggleable.**

Genuinely reconfigure-only, having checked rather than assumed: **Sodium**,
**Sodium Extra**, **ImmediatelyFast**, **EntityCulling** and **MoreCulling**.
These are pure optimisations with no off flag anywhere in their config. For
them the toggle is the launcher's own **Recommended mods** list
(`VersionSelect.svelte`), where every bundled mod except Sodium is
`required: false` and can be unticked so the jar is never installed. That is
the better answer for all of them anyway - it removes the jar rather than
quieting it.

**Verified in a running client, not compiled.** The ClickGUI cannot be clicked
by a script, but it is only a client of the interop server the game runs, so
every setting was pushed through the same `PUT /client/modules/settings` the
ClickGUI uses - and then, which is the part that matters, the mod's own config
file was read back off disk. A PUT that round-trips proves only that the client
stored the value, which is exactly what the two dead bridges did for months.

Re-run on 2026-09-01 across the whole bundle: **15/15 groups, 243 settings, every
group's own config file observed changing.** On the module side: 74 registered,
72 of 72 toggled off and back with the state read back each time, 74 of 74
settings trees readable, 1072 leaf settings, and no missing-description warnings.
ClickGui and HUD are not toggled - turning either off removes the interface being
tested through. `CLAUDE.md` has the recipe.

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
