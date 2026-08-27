<script lang="ts">
    import type {SessionStatsEvent} from "../../../integration/events";
    import type {SessionStats} from "../../../integration/types";
    import {listen} from "../../../integration/ws";
    import type {HudCpsCounterSettings} from "../components";

    export let settings: { [name: string]: any };

    let cSettings: HudCpsCounterSettings;

    $: cSettings = settings as HudCpsCounterSettings;

    let cps = {left: 0, right: 0};

    // SessionStats already measures this over a sliding window and excludes clicks made while a
    // screen is open, so this element reads it rather than counting again and disagreeing.
    listen("sessionStats", (event: SessionStatsEvent) => {
        const session = event.session as SessionStats;
        cps = session.cps;
    });

    $: parts = cSettings?.buttons === "Both"
        ? [{value: cps.left, name: "L"}, {value: cps.right, name: "R"}]
        : cSettings?.buttons === "Right"
            ? [{value: cps.right, name: "R"}]
            : [{value: cps.left, name: "L"}];
</script>

<div class="cps-counter">
    {#each parts as part}
        <div class="entry">
            {#if cSettings?.showButtonName && parts.length > 1}
                <span class="name">{part.name}</span>
            {/if}
            <span class="value">{part.value}</span>
            {#if cSettings?.showLabel}
                <span class="label">CPS</span>
            {/if}
        </div>
    {/each}
</div>

<style lang="scss">
  .cps-counter {
    display: flex;
    gap: 10px;
    background-color: var(--keystrokes-background-color);
    color: var(--keystrokes-text-color);
    border-radius: 5px;
    padding: 4px 8px;
    font-size: 14px;
    font-weight: 500;
  }

  .entry {
    display: flex;
    align-items: baseline;
    gap: 4px;
  }

  .name {
    font-size: 10px;
    opacity: .6;
  }

  .label {
    font-size: 10px;
    font-weight: 400;
    opacity: .75;
  }
</style>
