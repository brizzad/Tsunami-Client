<script lang="ts">
    import {listen} from "../../../../integration/ws";
    import type {MouseButtonEvent} from "../../../../integration/events";
    import type {MinecraftKeybind} from "../../../../integration/types";

    export let gridArea: string;
    export let key: MinecraftKeybind | undefined;
    export let label: string;
    export let cps: number;
    export let showCps: boolean;

    let active = false;

    // Matched on the bind's own translation key rather than a hardcoded button number, so a
    // player who has moved attack onto a side button still lights the right tile.
    listen("mouseButton", (e: MouseButtonEvent) => {
        if (e.key !== key?.key.translationKey) {
            return;
        }

        // Mouse buttons have no repeat action, unlike keys: 1 is press, 0 is release.
        active = e.action === 1;
    });
</script>

<div class="key mouse-key" style="grid-area: {gridArea};" class:active>
    <span class="label">{label}</span>
    {#if showCps}
        <span class="cps">{cps} CPS</span>
    {/if}
</div>

<style lang="scss">
  .key {
    height: 50px;
    background-color: var(--keystrokes-background-color);
    color: var(--keystrokes-text-color);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    border-radius: 5px;
    font-size: 14px;
    font-weight: 500;
    transition: ease box-shadow .2s;
    position: relative;
    box-shadow: inset 0 0 0 0 var(--keystrokes-active-color);
    text-align: center;
    line-height: 1.1;

    &.active {
      box-shadow: inset 0 0 0 25px var(--keystrokes-active-color);
    }
  }

  .cps {
    font-size: 10px;
    font-weight: 400;
    opacity: .75;
  }
</style>
