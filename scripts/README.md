
## Bridged config keys

```sh
node scripts/verify-bridge-keys.mjs [--config-dir <dir>]
```

`ModuleBundledMods` writes into config files other mods own. A mistyped path
there does not fail the build and does not log: the write lands on a key the
mod never reads, the ClickGUI shows the value it just stored, and the setting
silently does nothing. That is the Jade `harvest_tool.effective_tool` trap, and
it is the reason `CLAUDE.md` asks for every new bridge to be resolved against a
real config.

This does that automatically. It reads the keys back out of the Kotlin,
attributes each to its group's store, and checks it exists in a config the mod
itself wrote. It understands all four shapes in use: JSON with dotted paths,
JSON5 with comments, the TOML and `key: value` files behind `LineConfigStore`,
and the array-of-named-records Shield Statuses uses.

Default config dir is the launcher's game directory, which is where a real
launch writes them:
`%APPDATA%\Tsunami\TsunamiLauncher\data\gameDir\local\config`. A mod that is
off by default has never written one, and its keys are reported as unverified
rather than failed.

Exits non-zero on any key that does not resolve. It also names any group whose
keys it could not see at all, so an unchecked bridge is reported rather than
silently passing.

Same rule as the audit: break a key on purpose, confirm it fails, put it back.
