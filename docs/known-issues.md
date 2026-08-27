# Known issues

Defects found and reproduced here, not yet fixed. Each entry says how to
reproduce it and how it was confirmed, so nobody has to rediscover it.

---

## ClickGUI commits only the first setting change per module selection

**Severity: high.** It affects every setting in the client, not one module.

After you select a module in the ClickGUI, the **first** setting you change is
applied and persisted. Every change after that flips the switch on screen but
never reaches the backend. Selecting another module and coming back resyncs it,
and the next single change works again.

So a player who opens a module and adjusts four things gets one of them, and the
UI tells them all four took. Nothing is logged.

### Reproducing it

1. `./gradlew runClient`, load a world, open the ClickGUI.
2. Render → **Animations** → toggle **AirWalker**. It commits.
3. Toggle **AirWalker** again, and again.
4. Read the stored value:

```sh
node -e "const fs=require('fs');const j=JSON.parse(fs.readFileSync('run/Tsunami/modules.json','utf8'));const f=(n,name)=>{if(n&&typeof n==='object'){if(n.name===name)return n;for(const v of Object.values(n)){const r=f(v,name);if(r)return r;}}return null;};console.log(f(j,'Animations').value.find(v=>v.name==='AirWalker'));"
```

The switch on screen and the stored value disagree after step 3.

`run/Tsunami/modules.json` is the authority here, not the screen. Force a save
first by toggling something on a *different* module, since the config is written
on a change rather than continuously.

### What is known

- It is **not specific to nested value groups**. `AirWalker` is a plain
  top-level boolean on `Animations`, and it reproduces there.
- It is **not specific to modules added recently**. `Animations` is upstream's,
  untouched by this fork.
- The backend is behaving correctly. `Value.set` skips no-ops and fires its
  listeners on every real change (`config/types/Value.kt`), and
  `ValueChangedEvent` is emitted. The changes that go missing never arrive at
  `set` at all - the stored value simply never moves.
- So the fault is on the theme side, in what the ClickGUI sends after it has
  already sent one change for the selected module.

### Where it came from

The single-window ClickGUI rework, commit `1e8b9f2a`. That commit was verified
for rendering, filtering, counts, row toggles and that every setting *type*
appears - all of which still hold. What it did not cover was changing two
settings in a row without reselecting, which is the case that fails.

### How it was found

While verifying `BundledMods`, whose settings write through to a bundled mod's
own config file. The first toggle wrote to `config/sodium-options.json` and
logged it; later toggles did nothing. The bridge was suspected first, and
cleared by reproducing the same pattern on `Animations`, which has no such
listener.
