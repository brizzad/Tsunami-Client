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

**`./gradlew build` fails on `:detekt`**, not on your change. Four style findings
in the bundled-mods bridge (`ModConfigStore.kt`, `ModuleBundledMods.kt`) fail the
task; `./gradlew build -x detekt` is green, and so are `compileTestKotlin` and
`:test` as of 2026-09-01. `docs/known-issues.md` has the four and the choice they
need. The older orphaned-`NoFall`-test failure this warning used to describe is
fixed (`545542955`).

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

**The dev client has the bundled mods.** `gradle/libs.versions.toml` carries
Sodium, Lithium, Iris and ViaFabricPlus as `maven.modrinth` dependencies, so
`runClient` really loads them and writes real config files. The mods that are
*not* in that catalogue - AppleSkin, Jade, MoreCulling and the rest - come only
from the launcher, so their groups still appear in the ClickGUI but
`ModConfigStore.isModLoaded` is false and the write is skipped with a chat
line. Read a bridge's keys from the launcher's game directory instead:
`%APPDATA%TsunamiTsunamiLauncherdatagameDirlocalconfig`.

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
* **Enum constants come from the jar**, not from the config sample: a config only shows
  the value currently selected. `javap -p` on the enum class lists them all. Ignore the
  synthetic `VALUES` field.
* **Changes apply at next launch.** Every mod there reads its config once at startup.
  That limitation is stated on the module rather than hidden.
* **Nested groups must not touch the enclosing object during construction.** The
  sub-groups are built inside the parent's own constructor, so reaching back for a
  parent member gets a null at startup, not a compile error. Give each group its own
  store, or put shared helpers at file top level - see the `jade*` helpers.

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
- **`./gradlew build` fails** on `:detekt`'s four style findings. See above.

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
