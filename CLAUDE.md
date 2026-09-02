# Tsunami Client — orientation for a new session

**Read this file fully.** Product context, scope rules and the working
relationship live in `../Tsunami/CLAUDE.md` (the launcher repo) — read that too
if this session involves any decision beyond code.

The one-line version: this is a **legitimate QoL/performance Minecraft client**,
forked from LiquidBounce with 194 of its 238 modules deleted. The package
namespace is still `net.ccbluex.liquidbounce` on purpose. Cheat names in git
history and in `docs/module-inventory.md` are records of deletions.

## Build and verify

**`JAVA_HOME` on this machine points at a Temurin 8, and Gradle needs 17+.**
That is the first thing that will fail. JDK 25 is installed — the toolchain asks
for 25 (`gradle/libs.versions.toml`: `jdk = "25"`). Prefix every Gradle command:

```sh
export JDK25="/c/Program Files/Eclipse Adoptium/jdk-25.0.4.7-hotspot"

JAVA_HOME="$JDK25" ./gradlew compileJava compileKotlin   # ~15s warm
JAVA_HOME="$JDK25" ./gradlew runClient                   # the real test
node scripts/audit.mjs                                   # phone-home, SVG, kept modules
node scripts/audit-mixins.mjs                            # did a strip break a kept module
```

**Run `./gradlew clean build` before believing a green build.** A plain
`./gradlew build` here is incremental, and it will not recompile a file whose own
source has not changed - so a retained file that imports a *deleted* package stays
green locally forever. That is not hypothetical: `ClientboundRemoveEntitiesPacketAddition`
imported CrystalAura's triggerer and survived the strip on 2026-08-25 (whose commit
message says "mixins outstanding"), and every local build passed for eight days
while the tree did not compile from clean. It was caught on 2026-09-02, by CI, the
first time CI ever ran on Tsunami's code - see the branch note below.

No audit catches this class of fault. `phone-home`, `svg-xml`, `anchors` and
`kept-modules` all pass on a tree that will not compile; `kept-modules` looks for
deleted lines naming a *kept* module, which is the opposite direction. Only a clean
compile finds it.

**The default branch is `main`, and it is Tsunami** (changed 2026-09-02). It used
to be `nextgen`, and `origin/nextgen` is *upstream LiquidBounce* - it kept taking
CCBlueX commits through 2026-08-28 and still carries KillAura, Aimbot, XRay,
Scaffold and ESP. All the fork's work had only ever lived on feature branches, so
`build.yml` - which triggers on the default branch - had been compiling upstream's
tree rather than this one. `nextgen` is left in place as the upstream mirror it
already was. Do not merge it into `main`; it would restore the cheat modules.

**`./gradlew build` is green** as of 2026-09-02 - compile, `:test` and `:detekt`
all pass, from clean. Both things that used to break it are fixed: the orphaned `NoFall` test
(`545542955`) and four detekt findings in the bundled-mods bridge.

Keep it that way. `:detekt` runs as part of `build`, and two of its limits are
close: a file may hold **11 top-level functions** and a class 11, so a bridge that
adds helpers goes in its own `Bundled<Mod>Config.kt` beside the module rather than
on the end of `ModuleBundledMods.kt`. `LineConfigStore` is at 11 of 11.

Compiling is not verifying. Mixin targets are resolved at runtime, so a green
compile says nothing about whether an injection actually applies. Two ways to do
better, in increasing order of strength:

1. **Check the target against the deobfuscated jar** (fast, no game launch):

```sh
JAR=~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.2/minecraft-merged-deobf-26.2.jar
unzip -o -q "$JAR" 'net/minecraft/client/renderer/entity/ItemEntityRenderer.class' -d /tmp/mcv
"$JDK25/bin/javap" -p -c /tmp/mcv/net/minecraft/client/renderer/entity/ItemEntityRenderer.class | grep -E 'submit|mulPose'
```

   This confirms the method descriptor exists, the shadowed field is really
   private, and — critically for `@Redirect` — how many candidate call sites the
   target method contains. `defaultRequire` is 1.

2. **`runClient` and look at it.** This is the project's bar for "done".

**A `pre-commit` hook is installed** (`.git/hooks/pre-commit`, from
`scripts/hooks/pre-commit`) and runs `scripts/audit.mjs`, refusing the commit if
it fails. The launcher repo has the same one. `--no-verify` bypasses it.

## Driving a running client over its own REST API

**This is the strongest verification available here, and it was not written
down before.** The ClickGUI is a browser you cannot script a click inside, but
it is only a client of the interop server the game itself runs - so anything
the ClickGUI can do, curl can do, against a real running client.

Pin the port and auth code, which are otherwise random per launch:

```sh
JAVA_HOME="$JDK25" LB_INTEROP_PORT=8099 LB_INTEROP_AUTH_CODE=devcode01   ./gradlew runClient > /tmp/runclient.log 2>&1 &

# wait for it, then every request carries ?lb_code=<code>
curl -s "http://127.0.0.1:8099/api/v1/client/modules?lb_code=devcode01"
curl -s "http://127.0.0.1:8099/api/v1/client/modules/settings?name=BundledMods&lb_code=devcode01"
```

The routes are whatever `src-theme/src/integration/rest.ts` calls - read it
rather than guessing, the shapes are not all alike. The three that matter:

| What | Route |
| --- | --- |
| List modules, with `enabled` | `GET /client/modules` |
| One module's settings tree | `GET /client/modules/settings?name=X` |
| Write it back | `PUT /client/modules/settings?name=X` |
| Toggle a module | `POST /client/modules/toggle` with `{name, enabled}` |

Auth is `?lb_code=` on the query string (or the `lb_auth` cookie); an
`Authorization` header is **not** accepted and returns
`{"reason":"Authentication required"}`.

What this buys: a settings change can be pushed exactly as the ClickGUI would
push it, and then checked where it actually landed - including in a bundled
mod's own config file under `run/config/`. That is a genuine end-to-end test of
a ClickGUI bridge without a single click.

**The dev client has the bundled mods, and as of 2026-09-02 it has nearly all of
them.** `gradle/libs.versions.toml` carries them as `maven.modrinth`
dependencies with `runtimeOnly` entries in `build.gradle.kts`, so `runClient`
really loads them and writes real config files - Sodium, Lithium, Iris,
ViaFabricPlus, Jade, AppleSkin, 3D Skin Layers, Xaero's Minimap, ItemPhysic,
Shield Statuses, Glint Outline, and now Replay Mod, WorldEdit CUI and Simple
Voice Chat.

**This is how you get a bridge's real keys: add the mod here and launch once.**
Replay Mod and Simple Voice Chat were bridged that way - neither had ever
written a config, and a guessed key is the exact failure
`verify-bridge-keys.mjs` exists to catch. WorldEdit CUI writes no config even
when loaded; it has no settings worth bridging (`javap` on
`CUIConfiguration` shows debug flags and colours, no enable).

For a mod that is *not* in the catalogue, its group still appears in the
ClickGUI but `ModConfigStore.isModLoaded` is false and the write is skipped
with only a chat line. Read its keys from the launcher's game directory
instead: `%APPDATA%TsunamiTsunamiLauncherdatagameDirlocalconfig`.

**Watch the mod id, not the Modrinth slug.** `isModLoaded` takes the id from
`fabric.mod.json`. `simple-voice-chat` is id `voicechat`; `3dskinlayers` is id
`skinlayers3d`; `enchantment-glint-outline` is id `enchant-outline`. Get it
wrong and everything looks fine while nothing is written.

**Float settings round-trip through a float.** Setting 0.7 + 0.1 from
JavaScript sends 0.7999999999999999 and reads back 0.8. That is the language,
not the client - compare with a tolerance or use values that are exact in
binary.

## Changing the theme — the ClickGUI and the HUD

The interface is a **Svelte 5** app in `src-theme/`, built into the jar. Its
toolchain is npm and vite, nothing to do with Gradle:

```sh
cd src-theme
npx vite build      # what the jar gets, ~8s
npm run check       # svelte-check; see the baseline below before believing it
```

**`npm run check` is red before you touch anything** — 21 errors in 7 files, all
of them HUD elements plus `menu/common/modal/Tabs.svelte`, mostly missing
`Hud*Settings` types (checked 2026-08-31). It is a diff, not a gate: note the
count before your change and confirm it did not grow. `npx vite build` *is* a
gate — it is clean apart from one unused-CSS warning in `menu/title/Title.svelte`.

`bun` is not installed on this machine. Neither is `python` or `python3` — those
are Microsoft Store shims that answer and then fail; **Python is `py`** (3.12.0).
For a scripted edit, `node -` with a heredoc is the path of least resistance.

**Never `bind:` into a `$:` derived array.** This one mistake caused the worst
bug the client has had, described under live issues below.
`bind:setting={configurable.value[i]}` writes back; `bind:setting={setting}`
over `$: settings = configurable.value.filter(...)` writes into a copy that
nothing reads, and the change is lost with nothing logged. `GlobalSettings.svelte`
and `menu/common/setting/WrappedSetting.svelte` are the patterns to copy.

Related and still live: every setting editor does `const cSetting = setting` at
init and then `setting = {...cSetting}`. That capture never updates, so after the
parent refetches, the child goes on displaying its own stale copy.

### Driving theme components headlessly

`runClient` is the bar, but MCEF downloads ~150 MB before the UI exists at all
and you cannot script a click inside it. There is a harness that mounts the real
components in jsdom and asserts on what they **commit**:

```sh
cd src-theme
npm i --no-save jsdom          # deliberately not a package.json dependency
node scripts/headless/run.mjs  # 0 pass, 1 fail, 2 jsdom missing
```

It builds with vite and the project's own svelte plugin, stubbing `integration/ws`
(opens a socket on import) and `integration/rest` (talks HTTP). The rest stub
generates its unimplemented exports from the real module, so adding a rest call
does not break it.

It currently covers the ClickGUI binding regression, and it has been seen red:
checked out against the pre-fix `SettingsPane.svelte` it fails with exit 1.
`scripts/headless/README.md` has the rest, including the six environment traps
that are already handled — and the rule for new cases, which is to **assert on
what reached the backend, never on what the DOM shows**. The bug it was built
for drew the right thing on screen and stored the wrong thing.

## Merging a mod's source in as a module — the recipe

This is the main way features get added here. It is the documented strategy and
Nathan's stated preference: an established mod has already solved edge cases a
fresh implementation will miss. See `../Tsunami/tsunami-modules-vs-mods.md` for
merge-vs-bundle, and `docs/feature-status.md` for what is already covered.

**The licence gate comes first.** Merging source into this GPL-3.0 codebase
needs MIT, Apache-2.0, CC0, LGPL-3.0, GPL-3.0 or LGPL-2.1-**or-later**. These
cannot be merged: All Rights Reserved, any `-NC` or `-ND` Creative Commons,
EPL-2.0, MPL-2.0, LGPL-2.1-**only** (no upgrade path to GPL-3). Bundling a jar
from Modrinth is *not* redistribution and does not require this — that is how
3D Skin Layers and WorldEdit CUI ship despite unmergeable licences.

Worked examples to copy, in decreasing order of how closely they match a typical
job: `ModuleFlatItems`, `ModuleBetterHitreg`, `ModuleArmorHud`,
`ModulePotionTimers`, `ModuleMotionBlur`.

The shape:

1. **The mod's real logic** goes in `features/<name>/` (or `render/<name>/` for
   render engines). Mixins go in
   `injection/mixins/minecraft/<area>/<name>/`, in **Java**, not Kotlin.
2. **A `package-info.java`** in that package records: origin URL and licence, why
   it permits inclusion in GPLv3, why the source was merged rather than the jar
   bundled, what changed in the merge, and what was deliberately left out. Copy
   the tone from `features/hitreg/package-info.java` — it is the model.
3. **`Module<Name>.kt`** in `features/module/modules/<category>/` is the ClickGUI
   face and nothing else. It carries the settings and the KDoc explaining the
   feature to a human.
4. **Drop the mod's own config layer.** Its settings interface, Mod Menu hook,
   config file and keybinds all go. This is non-negotiable: the strategy doc
   requires everything be configurable through Tsunami's ClickGUI, and it is what
   earns the feature keybinds and profile export.
5. **The module's own `running` flag replaces upstream's `enabled` setting.** A
   Tsunami module that is off is already not running.

Conventions that will bite you:

- Duck interfaces live in `interfaces/` as `<Class>Addition.java`, and every
  injected member is prefixed `liquid_bounce$`.
- Settings are `by boolean(...)`, `by int(...)`, `by color(...)`,
  `by enumChoice(...)`. Enums implement `Tagged` with a `tag` string.
- **Never name an accessor `getFoo()` beside a property `foo`** — the Kotlin
  getter has that JVM signature already and the compile fails with "platform
  declaration clash". Make the property public and let Java call its getter.
- Mixins call modules as `ModuleFoo.INSTANCE.bar()`.
- MixinExtras (`@Local`, etc.) is available and used.
- Keep the GPL file header on every new file. Never remove an upstream one.

Registration — **all three, or the module silently does nothing**:

- `features/module/ModuleManager.kt`: the import, and the entry in the list
- `resources/liquidbounce.mixins.json`: every new mixin under `client`
- Check for duplicate entries while you are in there; one shipped recently

**Every module needs a description in `resources/liquidbounce/lang/en_us.json`**,
keyed `tsunami.module.<lowerCamelName>.description`. Without it the client logs
`missing fallback description key` on startup and the ClickGUI shows the module
with no description. Verified: all 395 module keys are present and only a newly
added module was missing one.

## Bridging a bundled mod into the ClickGUI

The strategy doc has one hard rule: every bundled mod must be configurable through
Tsunami's ClickGUI, not its own screen. `ModuleBundledMods` is where that lives, with
a `ValueGroup` per mod backed by `ModConfigStore`.

* **Read the real keys.** Launch once, then read the config the mod actually wrote
  (`gameDir/local/config/...`), or the field names in its jar. Do not guess - every
  group in that file says where its keys came from.
* **A mod that has never written its config will silently drop your writes.**
  Every store refuses to create a file the mod has not made, because inventing a
  schema produces something the mod cannot load. That is right by default and it
  is invisible: the ClickGUI stores the value and says "Saved". Two things now
  guard it. `ModConfigStore.write` **returns `Boolean`**, and `applyTo` tells the
  player when a write was skipped instead of claiming it saved. And
  `ModConfigStore.json(file, seed)` takes a complete set of defaults, so a bridge
  that has read the mod's whole config class can create a valid file on the first
  change. **Only pass a seed when you have read the entire schema out of the
  jar** - Player Health Indicators is the worked example, three fields and their
  defaults taken from `javap` on its `Config` class. A partial seed is worse than
  none.
* **Enum constants come from the jar**, not from the config sample: a config only shows
  the value currently selected. `javap -p` on the enum class lists them all. Ignore the
  synthetic `VALUES` field.
* **Changes apply at next launch.** Every mod there reads its config once at startup.
  That limitation is stated on the module rather than hidden.
* **Nested groups must not touch the enclosing object during construction.** The
  sub-groups are built inside the parent's own constructor, so reaching back for a
  parent member gets a null at startup, not a compile error. Give each group its own
  store, or put shared helpers at file top level - see the `jade*` helpers.

### Every mod that is not buying frames must be toggleable

**Nathan's rule, stated 2026-09-02 after finding Jade and 3D Skin Layers on by
default with no way to turn them off:** if a mod is not helping FPS, it must have
an off switch in the ClickGUI. A pure optimisation with nothing on screen does
not need one - there is nothing to see, so there is nothing to turn off.

Sorting a bundled mod is one question: **can the player see it?** Sodium,
Lithium, FerriteCore, ImmediatelyFast, EntityCulling, MoreCulling,
BadOptimizations, C2ME and Ixeris buy frames and draw nothing - no switch. Cloth
Config, WalksyLib and CreativeCore are libraries - no switch. Everything else
draws or does something, and needs one.

AppleSkin is the one that looks like an optimisation and is not: it draws a
saturation overlay, an exhaustion underlay and food values on tooltips. Judge by
what reaches the screen, not by the mod's reputation or its config format.

### Finding a mod's off switch - the recipe

Do this in order. It is written down because the sentence it replaces -
"the rest reconfigure only, that is a property of the mods" - was in
`feature-status.md` for weeks and was simply wrong.

1. **Read the mod's own config file off disk.** Not its wiki, not its
   Modrinth page. `run/config/...` after a `runClient`.
2. **If it has never written one, add it to the dev catalogue and launch.**
   A version ref and library entry in `gradle/libs.versions.toml`, a
   `runtimeOnly` in `build.gradle.kts`, then `runClient`. That is how Replay
   Mod and Simple Voice Chat were bridged. **Never guess a key** -
   `verify-bridge-keys.mjs` exists because guessed keys fail silently.
3. **A single master flag?** Surface it as `Enabled` at the **top of the
   group**. Jade always had one - `general.displayTooltip` - and it was already
   bridged, as **"Tooltip"**, three levels down under `General`. A real off
   switch named after the sub-feature it happens to control is invisible.
   **Name it `Enabled` and put it first.**
4. **No single flag, but a complete set?** Synthesise one. `Enabled` writes
   every flag in the set at once, and each individual setting writes
   `enabled && it` so the master keeps winning. 3D Skin Layers (seven layer
   flags), AppleSkin (eight `show*` flags) and Replay Mod (three recording
   flags) all work this way. Default the master from whether *any* member is
   currently on, so someone who turned them all off by hand is not overridden.
5. **Neither exists?** Say so in `feature-status.md` with what you checked.
   WorldEdit CUI has no enable flag at all (`javap` on `CUIConfiguration`:
   debug flags and colours), and ViaFabricPlus has a protocol selector rather
   than a switch. For those the toggle is the launcher's **Recommended mods**
   list in `VersionSelect.svelte`, where every bundled mod except Sodium is
   `required: false` and unticking it means the jar is never installed. That is
   the stronger toggle anyway - it removes rather than quiets.

Two things that will bite you while doing this:

* **Reading a sibling setting inside `onChanged` needs an explicit type.**
  `val enabled: Boolean by boolean(...)`, not `val enabled by boolean(...)`.
  Without it the compiler reports "Type checking has run into a recursive
  problem" - the delegate's type depends on a lambda that depends on the
  delegate. Every gated setting in the group needs the annotation too.
* **The mod id is not the Modrinth slug.** `simple-voice-chat` is id
  `voicechat`, `3dskinlayers` is `skinlayers3d`, `enchantment-glint-outline`
  is `enchant-outline`. `applyTo` gates on the id, so getting it wrong stores
  and redisplays the value while writing nothing. Read it from the jar's
  `fabric.mod.json` and let `verify-bridge-keys.mjs` confirm it.

### Proving a switch works

A `PUT` that round-trips proves nothing - that is exactly what the two dead
bridges did for months. Push the setting through the interop server the way the
ClickGUI would, then **read the mod's own config file back off disk**, and do it
in both directions so you know the restore path works too:

```sh
curl -s "http://127.0.0.1:8099/api/v1/client/modules/settings?name=BundledMods&lb_code=devcode01" -o bm.json
# flip the group's Enabled to false in bm.json, then:
curl -s -X PUT "http://127.0.0.1:8099/api/v1/client/modules/settings?name=BundledMods&lb_code=devcode01"   -H "Content-Type: application/json" --data-binary @bm.json
# then read run/config/<the mod's own file> and check the keys actually moved
```

**`runClient` leaves the game JVM alive when the Gradle wrapper is killed**, and
it keeps `LB_INTEROP_PORT`. A stale client answering on 8099 looks exactly like
a fresh one, and you will test the old jar without noticing - that happened here.
Kill it by command line before relaunching:

```sh
# PowerShell
Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
  Where-Object { $_.CommandLine -match 'net.fabricmc|knot' } |
  ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
```

### Dotted paths, and the trap Jade sprang

`ModConfigStore` addresses JSON with dotted paths, where a dot means descend. Jade
breaks that: inside its `plugin.minecraft` object, `harvest_tool.effective_tool` is one
flat key containing a dot, not two levels. Splitting naively creates bogus nesting and
leaves the real key untouched - a setting that looks saved and does nothing.

`JsonConfigStore` therefore splits only on **unescaped** dots. Write a literal dot in a
key name as `\\.` in Kotlin source. Jade's config also lives in a subdirectory,
`jade/jade.json`, which `ModConfigStore.json()` handles as a relative path.

Worth doing for any new bridge: resolve every key you added against the mod's real
config file before trusting it. That catches typos and bad escaping in one pass - it
found nothing wrong in the 83 Jade keys, but only because it was run.
## Version switching, and why merging is the safe path

Mid-launch version switching is the flagship feature and the brief says not to
break it. It is also the thing most likely to be misunderstood, so:

**Switching version does not swap the Minecraft client.** The launcher offers
exactly one build - `request_builds` returns a single `Build` pinned to
`MC_VERSION = "26.2"`. ViaFabricPlus translates the *protocol* so you can join a
1.8.9 server; the client jar, its classes and every mixin stay 26.2.

So **a mixin can never fail to apply because of a version switch.** Its target
class is always loaded. What changes is *semantics* - whether the thing a module
draws exists, or means the same, on the negotiated protocol.

For that there is a first-class hook on `ClientModule`:

```kotlin
override val inapplicableOnProtocol: String?
    get() = if (isOlderThan1_9) "Item cooldowns do not exist below 1.9" else null
```

- It gates `running` and deliberately **not** `enabled`, so the player's toggle
  survives. The module pauses on one server and returns on the next.
- The string is shown to the player. Say what is missing, not a version number.
- Predicates live in `utils/client/ProtocolUtil.kt`: `isEqual1_8`,
  `isOlderThanOrEqual1_8`, `isOlderThan1_9`, `isOlderThan1_11`, `protocolVersion`.
- Used by `ModuleCooldowns` and `ModuleTotemEffect`. Copy those.

**This is the argument for merging over bundling.** A merged module gets this
hook, a ClickGUI entry and profile export. A bundled jar gets none of them: it
runs on every protocol regardless, with no gate and no way to tell the player it
is inert. The embedded mods are the managed half.

When adding a module, ask one question: *does what this draws exist, and mean the
same, back to 1.8?* Three possible answers:

1. **Yes** - no gate. Rendering that follows client state (item entities,
   post-processing, armour, status effects) is almost always this.
2. **It does not exist below version X** - gate it with `inapplicableOnProtocol`.
3. **It exists but the rule differs** - a gate is wrong, because the module is not
   inapplicable. Expose the difference as a setting and document why, the way
   `ModuleLightLevels.SpawnThreshold` does for the pre-1.18 light rule of 7.

`FlatItems` is case 1 and needs no gate. Not yet confirmed against a real 1.8.9
server - that is a `runClient` check.

## Running it as a player sees it

Facts that cost a whole debugging round because they were not written down:

* **The ClickGUI is bound to Right Shift** (`ModuleClickGui`,
  `bind = InputConstants.KEY_RSHIFT`). There is no menu button anywhere - not in
  the launcher, not in game. Chat command prefix is `.` (`.help`).
* **The whole interface is a browser.** The ClickGUI and the themed HUD are a
  Svelte app in `src-theme/`, built into the jar at
  `resources/liquidbounce/themes/tsunami/`, and rendered by MCEF (Chromium).
  **MCEF downloads ~150 MB at runtime on first launch, after the game is already
  running.** Until it finishes there is no ClickGUI *and* no HUD. On a fresh
  machine this looks identical to a completely broken client.
* A healthy start logs, in order: `Launching Tsunami v<version>`,
  `(MCEF) Chromium Embedded Framework initialized`,
  `Successfully initialized browser`, `Reloaded theme 'Tsunami'`,
  `Initialized Browser API`. If those are present the UI layer is fine and the
  problem is elsewhere.
* Logs: dev runs write to `run/logs/latest.log`; a launcher-installed client
  writes to
  `%APPDATA%\Tsunami\TsunamiLauncher\data\gameDir\local\logs\latest.log`.
  **If that second file does not exist, the game never started** - check the
  launcher's own log instead. That distinction settles "did it even run".
* Benign log noise, do not chase: Realms/401 `Failed to fetch user properties`
  on an offline account, `Missing sound for event: tsunami:bonk`, and
  `Error loading class: traben/entity_model_features` or `mezz/jei` (other mods
  probing for optional integrations).

## Live issues to know about

- **The ClickGUI first-setting-only bug is fixed and committed (`035a0d046`),
  but has not been seen in a running client yet.** It affected every setting in
  the client: only the first change per module selection reached the backend.
  `SettingsPane.svelte` bound `bind:setting` into a `filter()` copy, so the
  writeback never reached the `configurable` that `save()` sends. It now binds
  `configurable.value[i]`, the way `GlobalSettings.svelte` always has.
  **Never `bind:` into a `$:` derived array** — that is the whole bug. Cause,
  fix and the jsdom before/after are in `docs/known-issues.md`; the in-game
  check is still outstanding.
- **`./gradlew build` is green**, including `:detekt`. See the note above on keeping it so.

## Verifying a feature before calling it done

`docs/feature-status.md` defines the vocabulary and it is deliberate:

- **done** — built and checked in a running client, not merely compiled
- **built** — merged, compiling, injection points verified against the
  deobfuscated jar, but not yet seen in a running client
- **present** — already existed in the fork; verified rather than rebuilt
- **deferred** — deliberately not built, with the reason
- **blocked** — cannot be built right now, with the blocker

Update the row when you finish, and record *why* for anything you decline. A
decision that only exists in a conversation is worth nothing — that is the whole
reason that document exists.
