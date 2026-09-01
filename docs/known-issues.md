# Known issues

Defects found and reproduced here, not yet fixed. Each entry says how to
reproduce it and how it was confirmed, so nobody has to rediscover it.

---

## ClickGUI commits only the first setting change per module selection

**Fixed and committed (`035a0d046`); not yet checked in a running client.**
Kept here until someone confirms it in game, because that is this project's bar
for done.

After you selected a module in the ClickGUI, the **first** setting you changed
was applied and persisted. Every change after that flipped the switch on screen
but never reached the backend. Selecting another module and coming back
resynced it, and the next single change worked again.

So a player who opened a module and adjusted four things got one of them, and
the UI told them all four took. Nothing was logged.

### The cause

Two things had to be true at once, which is why it survived review.

**One — the settings each-block bound into a copy.** `SettingsPane.svelte` did:

```svelte
$: settings = configurable?.value.filter((v) => v.name !== "Bind" && v.name !== "Hidden") ?? [];

{#each settings as setting (setting.name)}
    <GenericSetting bind:setting={setting} on:change={save}/>
{/each}
```

`filter()` returns a **new array** holding the same object references. A
`bind:` writeback assigns into that array, not into `configurable.value`, and
`save()` sends `configurable`. So any child that *reassigns* its prop writes
into a dead end.

**Two — every setting editor reassigns its prop, from a stale capture.** The
upstream idiom, here in `BooleanSetting.svelte`, is:

```js
const cSetting = setting as BooleanSetting;   // captured once, at init
function handleChange() {
    setting = { ...cSetting };                // reassignment, not mutation
    dispatch("change");
}
```

`cSetting` is a `const` grabbed when the component mounts. The inner control
mutates *that object*.

Together they produce exactly the observed behaviour:

1. First change — `cSetting` still **is** `configurable.value[i]`, so mutating
   it in place reaches `configurable` despite the dead-end writeback.
   `save()` sends it. It commits.
2. `save()` then refetches: `configurable = await getModuleSettings(name)`,
   which replaces every setting object with a fresh one from the server.
3. The keyed each sees unchanged keys, so it **reuses** the child components
   rather than recreating them — and `cSetting` still points at the discarded
   object from step 1.
4. Every later change mutates that orphan. The writeback goes to the filtered
   copy. `configurable.value` is untouched, and `save()` sends it unchanged.

That is why the doc's earlier note was right that `Value.set` was innocent: the
change never arrived at it.

It also explains the resync. Selecting another module sets `configurable = null`,
which empties the list and destroys the children; coming back rebuilds them, so
`cSetting` is captured fresh and one more change gets through.

### The fix

Bind to the real array by index, so the writeback lands where `save()` reads:

```svelte
$: settingIndices = configurable
    ? configurable.value.flatMap((v, i) =>
        v.name === "Bind" || v.name === "Hidden" ? [] : [i])
    : [];

{#each settingIndices as i (configurable.value[i].name)}
    <GenericSetting bind:setting={configurable.value[i]} on:change={save}/>
{/each}
```

The Bind row had the same fault, from `find()`, and is fixed the same way.

This is not a new idea — `tabs/GlobalSettings.svelte` and
`menu/common/setting/WrappedSetting.svelte` both already bind
`something.value[i]`, and neither has ever shown the bug. `SettingsPane.svelte`
was the only place binding into a derived value, and it was the one file the
single-window rework (`1e8b9f2a`) added that touches settings.

### How it was verified

Compiled the real before-and-after shapes with the project's own Svelte 5.33.9
and drove them in jsdom, clicking one setting three times and then a second
setting, without reselecting — the reproduction below, as code. Committed
values per click:

| | AirWalker | SwingSpeed |
| --- | --- | --- |
| before | `true, true, true, true` | `false, false, false, false` |
| after | `true, false, true, true` | `false, false, false, true` |

Before, clicks 2 and 3 never move the value and the fourth click is lost too.
After, every click commits. `npx vite build` is clean.

**Still to do: confirm it in game**, with the reproduction below.

### Reproducing it

1. `./gradlew runClient`, load a world, open the ClickGUI.
2. Render → **Animations** → toggle **AirWalker**. It commits.
3. Toggle **AirWalker** again, and again.
4. Read the stored value:

```sh
node -e "const fs=require('fs');const j=JSON.parse(fs.readFileSync('run/Tsunami/modules.json','utf8'));const f=(n,name)=>{if(n&&typeof n==='object'){if(n.name===name)return n;for(const v of Object.values(n)){const r=f(v,name);if(r)return r;}}return null;};console.log(f(j,'Animations').value.find(v=>v.name==='AirWalker'));"
```

The switch on screen and the stored value should now agree at every step.

`run/Tsunami/modules.json` is the authority here, not the screen. Force a save
first by toggling something on a *different* module, since the config is written
on a change rather than continuously.

### The latent half, deliberately left alone

The stale `const cSetting` capture is still there, in all 22 setting editors.
With the writeback repaired it no longer loses changes — the `{...cSetting}`
spread carries the new value into the real array — but it does mean a child goes
on displaying its own copy after a refetch. If the backend ever answers with a
value different from the one sent (a clamped int, a choice that resets its
siblings), the row will show the sent value rather than the stored one.

Not fixed here because it is upstream's idiom across 22 files, and this fork
keeps upstream's structure so merges stay possible. Worth doing as its own
piece of work, with `const` becoming `$:` and each editor re-checked.

---

## `./gradlew build` fails on detekt

**Severity: medium.** CI's second job runs the full `./gradlew build`, so the
tree is red there until these are resolved.

**The NoFall orphan that used to be this entry is fixed** (`545542955`, "Get the
test suite running again"). Verified on 2026-09-01: `compileTestKotlin` and
`:test` are both green. What is left is a different task failing.

`:detekt` reports four style violations, all in the bundled-mods ClickGUI bridge:

| File | Finding |
| --- | --- |
| `features/bundled/ModConfigStore.kt:352` | `write` has cognitive complexity 17; the maximum is 16 |
| `features/module/modules/misc/ModuleBundledMods.kt:21` | 13 functions in the file; the maximum is 11 |
| `ModuleBundledMods.kt:218` | line longer than the configured maximum |
| `ModuleBundledMods.kt:249` | line longer than the configured maximum |

### Reproducing it

```sh
./gradlew detekt
```

`./gradlew build -x detekt` is green, so this is the only thing between the tree
and a passing build.

### The fix

The two long lines are trivial. The other two are a judgement call rather than a
mechanical fix: `ModuleBundledMods.kt` grows a function per bridged mod, so it
will cross the file limit again with the next bridge, and `ModConfigStore.write`
is branchy because it dispatches over the four config shapes in use. Either
split them along those seams — one file per group of bridges, one writer per
shape — or raise the two thresholds in the detekt config deliberately. Do not
suppress them file-by-file, which hides the next instance too.

### The lesson from the orphan this entry used to describe

Worth keeping now that the defect is gone. `audit.mjs` reported **clean**
throughout: it checks that kept modules still have the code they depend on, not
that deleted modules left no test behind. A `kept-tests` check would have caught
it, and does not exist yet.
