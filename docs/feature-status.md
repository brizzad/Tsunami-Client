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
| Armor status | present | HUD component |
| Potion status | present | Effects HUD, with duration and amplifier |
| Reach display | done | measured to the hitbox, not the entity origin |
| Hitboxes | done | box edges 10.88 / 8.78 against 3.56 control |
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
| Potion timers | present | Effects HUD already shows both |
| Better Hitreg | done | rendering only; see caveat below |
| ~~Auto-totem~~ | excluded | automates a reaction that decides fights |

**Shield status is the upstream mod, not ours.** A first attempt drew a bar
above the hotbar, which is not what the feature means: Shield Statuses tints
the shield itself, orange while disabled and fading back as the cooldown runs
down, so the state is where your eyes already are. That mod is MIT, mature and
has millions of installs, so the version worth shipping was always the real
one. The home-made module was deleted rather than left beside it.

**Better Hitreg caveat.** Its effect only exists under network latency, and
singleplayer has none, so there was nothing to measure. Verified structurally:
it never cancels or rewrites the attack, and touches no packet. The benefit is
unverified.

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
| WAILA | done | `BlockInfo`; named deepslate diamond ore correctly |
| Shiny pots | done | potion slots 6.159, empty slots 0.000 |
| Weather / time changer | present | `CustomAmbience`, visual only |
| TNT countdown | present | `TNTTimer` |
| Height / fall danger | done | covered by coords plus `MlgHelper` |
| Uptime | done | `{session.uptime}` |
| Stopwatch | done | 0 → 3 → 6, then held at 6 when stopped |
| Durability warning | done | combo 4 → 0.00, sword durability frozen |
| Fire overlay reduction | present | `AntiBlind` → `FireOpacity` |
| AppleSkin food HUD | done | saturation strip 3.874 against 0.614 control |
| Minimap | present | **not** ported from Xaero's; see below |
| **Motion blur** | **deferred** | needs a post-processing shader pipeline the client does not have |
| **Item physics** | **deferred** | entity renderer surgery, the category that silently broke four modules |
| **3D skins** | **deferred** | same |
| **Glint colorizer** | **deferred** | requires intercepting the enchantment glint render type |
| **2D items** | **deferred** | item model rendering rework |
| **Resource pack organiser** | **deferred** | a Svelte UI project in its own right |
| ~~Real-world clock~~ | excluded | as specified |

**The minimap is not a port of Xaero's, and should not become one.** The fork
already ships a complete minimap — `MinimapHudComponent` with its own chunk
renderer, heightmap manager and texture atlas — registered as a native HUD
component and covered by the GPL we are already bound by. Xaero's Minimap is
proprietary, distributed as a compiled mod, with no redistribution or
derivative-works grant. Porting it would be unnecessary and unsafe. This is a
closed decision, not a pending question.

The five deferred render items are deferred together because they share a
cause: each needs shader or renderer work whose cost is comparable to a whole
feature batch, for cosmetic polish. They are worth doing, and they are worth
doing deliberately rather than at the end of a long session.

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
| **Spotify** | **deferred** | needs an OAuth redirect and a callback listener, which belongs in the launcher |
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
| **WorldEdit CUI** | **blocked** | `DiscardedPayload` keeps only the channel id and throws the bytes away, so the CUI message cannot be read without a mixin into payload decoding |
| **Hypixel stat overlay** | **deferred** | needs a user API key |
| **Network level display** | **deferred** | same API, same key |
| **Game-specific timers** | **deferred** | same, plus per-gamemode parsing |
| **Auto-friend / GG / tip** | **deferred** | needs server-specific end-of-game detection |
| **MumbleLink / TeamSpeak** | **deferred** | native shared-memory access over JNI, a different toolchain from anything else here |

The four Hypixel-shaped items are deferred as one piece of work rather than
four: they share an API client, key storage and rate limiting, and building
them separately would mean three redundant copies of each.

---

## The backend gap

Five items across three categories are blocked on the same missing thing: the
marketplace, cross-server chat, the screenshot uploader, and the update and
auto-config paths already gated behind `BACKEND_CONFIGURED`.

`docs/backend-contract.md` in the launcher repo specifies it. Nothing serves it.
It is the single largest unblock available.
