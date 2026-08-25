<div align="center">
<p>
    <img width="200" src="src-theme/public/img/tsunami-logo.svg" alt="Tsunami">
</p>
</div>

Tsunami is a free and open-source mixin-based Minecraft client built on the
Fabric API, focused on performance and quality of life.

It is a fork of [LiquidBounce](https://github.com/CCBlueX/LiquidBounce) by
CCBlueX, with the cheats removed. What remains is the rendering, HUD,
configuration and module framework, plus the modules that make the game nicer to
play without playing it for you.

## Scope

The line Tsunami draws is whether a feature automates **skill expression** or
merely removes **busywork**.

Automating something that takes no skill is quality of life. Respawning after
death is a keypress with no decision in it, so AutoRespawn belongs here.
Automating something that decides an encounter is a cheat, however convenient.
Swapping a totem into your off-hand at the right moment is a real skill that
separates players, so it does not.

Feature requests for combat or movement automation are out of scope, and so are
reach, timing, and anything that reports false information to a server.

## Why the mod id is still `liquidbounce`

`src/main/resources/fabric.mod.json` declares `"id": "liquidbounce"`, and that
is deliberate rather than a missed rename.

The mod id is a resource namespace, not a brand. It is what every asset and
translation key in the jar resolves against — `liquidbounce.module.*` in the
language files, `resources/liquidbounce/**` on disk, and the paths the theme's
HTTP server hands out. Renaming it means rewriting several thousand keys and
every resource path in one commit, and getting one wrong produces a missing
translation or a 404 at runtime rather than a build error.

The user-visible name is set separately and is already Tsunami: `CLIENT_NAME` in
`LiquidBounce.kt`, and `"name"` in the same `fabric.mod.json`.

## Relationship to LiquidBounce

Tsunami is a GPLv3 fork and stays one. Upstream keeps the credit for the
framework this is built on.

Anything that pointed at CCBlueX's infrastructure has been disconnected, because
a fork silently talking to upstream's servers is both a privacy problem and
theirs to pay for. Endpoints resolve to `.invalid`, the Discord application id
is unset, and CI publishes nowhere. Where a real Tsunami destination does not
exist yet, the value is left empty with a comment rather than pointed somewhere
plausible. `scripts/audit.mjs` enforces this.

## Issues

Tsunami has no issue tracker yet. It is a fork in progress, not something to
file LiquidBounce bugs against — please do not send Tsunami's bugs upstream.

## License

This project is subject to the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.en.html). This
does only apply for source code located directly in this clean repository. During the development and compilation
process, additional source code may be used to which we have obtained no rights. Such code is not covered by the GPL
license.

For those who are unfamiliar with the license, here is a summary of its main points. This is by no means legal advice
nor legally binding.

*Actions that you are allowed to do:*

- Use
- Share
- Modify

*If you do decide to use ANY code from the source:*

- **You must disclose the source code of your modified work and the source code you took from this project. This means
  you are not allowed to use code from this project (even partially) in a closed-source (or even obfuscated)
  application.**
- **Your modified application must also be licensed under the GPL**

## Setting up a workspace

Tsunami uses Gradle, and needs JDK 25 and [Node.js](https://nodejs.org) for the
[theme](src-theme).

1. Clone the repository with its submodules: `git clone --recurse-submodules <url>`
2. `cd` into the local repository.
3. Run `./gradlew genSources` for a better development experience (optional).
4. Open the folder as a Gradle project in your preferred IDE.
5. Run the client: `./gradlew runClient`

## Checks

```sh
node scripts/audit.mjs
```

Run before committing; a pre-commit hook and a CI workflow both run it. It
checks for CCBlueX hosts left in code, SVGs that will not parse, and code that a
kept module depends on going missing.

Every check exists because that mistake was made here and shipped with a green
build. Four modules once loaded, enabled and appeared in the HUD while doing
nothing at all, because a mixin method they shared with a deleted module was
removed whole. See [scripts/harness/README.md](scripts/harness/README.md) for
how to verify a module actually does something, which is harder than it sounds
and is not the same as it compiling.

## Additional libraries

### Mixins

Mixins can be used to modify classes at runtime before they are loaded. Tsunami uses it to inject its code into the
Minecraft client. This way, none of Mojang's copyrighted code is shipped. If you want to learn more about it, check out
its [Documentation](https://docs.spongepowered.org/5.1.0/en/plugin/internals/mixins.html).

## Contributing

Contributions are welcome. Please read the scope section above first: a
well-built feature that automates skill expression will still be declined.
