# Regression harness

Tools for checking that the fork did not silently break something. They exist
because the strip broke four modules in a way nothing else caught: `NoBob`,
`FullBright`, `BetterTab` and `AntiBlind` each shared a mixin method with a
module we removed, the whole method was deleted, and the modules went on
loading, enabling and appearing in the HUD while doing nothing at all.

A green build proves nothing about that class of bug. Neither does the client
starting. The only things that catch it are the static audit below and actually
looking at the screen.

## Static — run this on every change

```sh
node scripts/audit-mixins.mjs
```

Diffs every `.java` and `.kt` file against upstream and reports any **removed
line that names a module we kept**. Exits non-zero on anything unreviewed, so
it can gate CI.

Correct trims — dropping one condition from an expression shared with a removed
module — legitimately delete a line naming a kept module. Those live in
`scripts/audit-baseline.txt`. After reviewing a new finding and confirming the
module still works:

```sh
node scripts/audit-mixins.mjs --baseline
```

Set `TSUNAMI_UPSTREAM` to compare against a different upstream commit (defaults
to the fork point).

## Runtime — needs the client running

Start it first: `./gradlew runClient`

### Module registry

```sh
node scripts/harness/verify-modules.mjs <path-to-run-log>
```

Asks the running client what it actually registered, via its local REST interop
server, and compares against `scripts/keep.txt`. The port and session code
change every launch, so both are scraped from the log.

### Driving the window

```powershell
powershell -File scripts/harness/window.ps1 -Action shot  -Out shot.png
powershell -File scripts/harness/window.ps1 -Action click -X 283 -Y 256 -Out after.png
powershell -File scripts/harness/window.ps1 -Action key   -Scan 0x36 -Out gui.png   # right shift
powershell -File scripts/harness/window.ps1 -Action type  -Text "FullBright"
```

Finds the window by title, moves it to a known 1280x800 position so pixel
coordinates are stable, and captures just that window.

Two traps worth knowing:

- **Send keys as scancodes.** `keybd_event` with virtual key codes does not
  reach GLFW; the client ignores it.
- **The ClickGUI search box swallows keystrokes.** While it has focus, a right
  shift types into the field instead of closing the GUI, and W/S never reach
  the player. Clear the field and press escape, then confirm the GUI is really
  closed before measuring anything.

### Measuring

```powershell
powershell -File scripts/harness/measure.ps1 -Mode luma -A shot.png -X 60 -Y 430 -W 1100 -H 260
powershell -File scripts/harness/measure.ps1 -Mode diff -A a.png -B b.png -X 900 -Y 560 -W 300 -H 230
```

`luma` gives mean luminance over a region; `diff` gives mean absolute pixel
difference between two frames.

```powershell
powershell -File scripts/harness/burst.ps1 -Prefix out_ -Frames 8 -DelayMs 110 -Scan 0x11
```

Holds a key (default `0x11` = W) and captures a burst, for measuring motion.

## Worked examples

These are the two measurements that confirmed the restored modules.

### FullBright

Disable every other module first — `CustomAmbience` in particular also changes
lighting. Load at night. Hold the ClickGUI state **identical** across both
captures and measure a region **below** the GUI panel, because the panel is
bright and will otherwise dominate the average.

|                 | mean luminance |
| --------------- | -------------: |
| FullBright off  |          29.42 |
| FullBright on   |          49.48 |

### NoBob

Harder, because view bob only happens while walking and walking also moves the
scene. Two approaches do **not** work:

- Walking into a wall. `walkDist` only advances on real movement, so being
  blocked means no bob either — both conditions read zero.
- Any measurement taken while the ClickGUI holds focus. The player never moves
  and everything reads zero, which looks like a pass.

Always confirm the player actually moved by checking the full-frame diff is
non-zero before trusting a result.

What works is the ratio of held-item motion to whole-scene motion. Bob moves the
item in screen space independently of the world, so the ratio isolates it:

|                        | item ÷ scene motion |
| ---------------------- | ------------------: |
| bob active (NoBob off) |    3.96 3.95 3.83 4.25 |
| NoBob on               | 1.34 1.13 1.10 1.20 0.81 |

Roughly 4x versus roughly 1x. The ratio matters rather than the absolute
numbers, because the two runs walk at different speeds. Ignore frames where the
scene diff is near zero — the ratio is noise there.
