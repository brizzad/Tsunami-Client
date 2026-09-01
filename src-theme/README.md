# Tsunami Default Theme

This directory contains the source code for the Tsunami default theme, built with
[Svelte](https://svelte.dev/). It is the in-game UI: the ClickGUI, the HUD and the
menu screens, rendered in an embedded browser and driven over the client's interop
websocket.

## Development

```bash
npm install
npm run build     # writes dist/, which the Gradle build copies into the jar
npm run check     # svelte-check
```

`npm run build` must be run before building the client if you have changed anything
under `src/` or `public/` — Gradle copies `dist/` as-is and will happily ship a stale
bundle otherwise.

## Components

HUD elements are declared in two places and need both: a manifest in
`public/components/`, and an entry in the `components` array of
`public/metadata.json`. A component with a manifest but no metadata entry is silently
absent from the HUD editor, with no error to explain why.

## Upstream

Tsunami is a fork of [LiquidBounce](https://github.com/CCBlueX/LiquidBounce), and this
theme descends from its default theme. The theming system, the interop protocol and the
component model are upstream's design; the styling, HUD elements and menu content here
are not. If you are building a theme for LiquidBounce itself rather than for Tsunami,
start from their repository instead: https://github.com/CCBlueX/LiquidBounce-Theme
