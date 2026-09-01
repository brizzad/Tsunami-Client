# Headless theme harness

Mounts the theme's **real** Svelte components in jsdom and asserts on what they
commit to the backend.

```sh
cd src-theme
npm i --no-save jsdom
node scripts/headless/run.mjs
```

Exit code 0 if every case passes, 1 if any fails, 2 if jsdom is missing.

## Why it exists

`runClient` is this project's bar for "done" and nothing here replaces it. But
MCEF downloads ~150 MB after the game is already running before the ClickGUI
exists at all, and no click can be scripted inside it. So the loop for a theme
change was: build, launch, wait, click, read a JSON file by hand.

The ClickGUI first-setting-only bug is the argument for this existing. It
survived a review that checked rendering, filtering, counts, row toggles and
that every setting type appears — all of which still held. What nobody did was
change two settings in a row without reselecting, because doing it by hand is
tedious. It is three lines here, and it runs in about ten seconds.

## What it tests, and the shape of the test

`SettingsPane` — every change reaches the backend. The test changes a setting
three times and then a different setting, **without reselecting the module**,
and asserts each click produced a distinct commit. Reselecting is what used to
make the bug go away, so a test that reselects between changes proves nothing.

Verified the way `scripts/README.md` asks for: checked out the pre-fix
`SettingsPane.svelte`, confirmed the harness fails on it (exit 1, two cases
red with `[true,true,true,true]` where `[true,false,true,true]` was expected),
then put the fix back. A green run that has never been seen red is not evidence.

## How it works

`run.mjs` builds `entry.ts` with **vite**, using the project's own
`@sveltejs/vite-plugin-svelte`. Reusing the real build tool is the point: no
bespoke compile step to drift from what actually ships, TypeScript and SCSS are
handled, and everything including the Svelte runtime is bundled, so the loader
never has to be argued with about export conditions.

Two modules are swapped for stubs, matched on the tail of the import specifier
so every relative spelling is caught:

| Real module | Why it cannot run here |
| --- | --- |
| `integration/ws` | opens a WebSocket the moment it is imported |
| `integration/rest` | talks HTTP to the running client |

**The `rest` stub only implements what a test needs.** Everything else it must
export is generated at build time by reading the real `src/integration/rest.ts`
and emitting `() => []` for each name the stub does not define. A stub that has
to be hand-edited whenever somebody adds a rest call is a stub that breaks the
next person's build for a reason unrelated to their change. If a test needs one
of those to behave, implement it in `stubs/rest.ts` and generation stops for it.

The stub stores a **deep copy** on write and hands back fresh objects on read,
mirroring the real backend in the one respect that mattered: serving the same
object identities back would have hidden the bug this was written for.

## Things that bite, all of them already handled in `run.mjs`

- **jsdom needs a real origin.** Construct it with `url: "http://localhost/"` or
  `localStorage` throws `SecurityError` on an opaque origin.
- **Svelte transitions need the Web Animations API.** Every setting row is
  wrapped in an `in:slide`, and jsdom has no `element.animate`. There is a fake
  that finishes immediately, which is what a test wants anyway.
- **Pickr, the colour picker, is a UMD bundle that reaches for `self`.**
  It is pulled in through `GenericSetting`, so `self` must be a global.
- **On Windows an absolute path is not a valid ESM specifier.** Import the built
  bundle through `pathToFileURL`.
- **`navigator` is getter-only on `globalThis` in current node**, so copying
  jsdom's globals needs a `defineProperty` fallback.
- **Setting rows render as "Air Walker" or "AirWalker"** depending on the HUD's
  `SpaceSeperatedNames`. Match on the name with whitespace stripped.

## Adding a case

Seed a module, mount, drive the DOM, assert on `committedValues(...)`. Assert on
what reached the stub, never on what the DOM shows — the bug this was built for
put the right thing on screen and the wrong thing in the config, so a test that
reads the switch would have passed throughout.

jsdom is deliberately not in `package.json`: it is a few hundred files that
nothing shipped in the jar needs, and `--no-save` keeps it that way.
