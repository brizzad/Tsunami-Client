<script lang="ts">
    import {setModuleEnabled} from "../../integration/rest";
    import {convertToSpacedString, spaceSeperatedNames} from "../../theme/theme_config";

    export let name: string;
    export let enabled: boolean;
    export let description: string;
    export let selected: boolean;
    export let onSelect: (name: string) => void;

    /**
     * The switch must not select the row, and selecting the row must not
     * toggle the module. Conflating them is how you turn something on while
     * trying to read what it does.
     */
    async function toggle(event: Event) {
        event.stopPropagation();
        await setModuleEnabled(name, !enabled);
    }
</script>

<button
        class="row"
        class:selected
        class:enabled
        onclick={() => onSelect(name)}
        type="button"
>
    <span class="text">
        <span class="name">
            {$spaceSeperatedNames ? convertToSpacedString(name) : name}
        </span>
        <span class="description">{description}</span>
    </span>

    <!-- svelte-ignore a11y_click_events_have_key_events -->
    <span
            class="switch"
            class:on={enabled}
            role="switch"
            aria-checked={enabled}
            aria-label="Toggle {name}"
            tabindex="0"
            onclick={toggle}
            onkeydown={(e) => (e.key === "Enter" || e.key === " ") && toggle(e)}
    >
        <span class="knob"></span>
    </span>
</button>

<style lang="scss">
  .row {
    display: flex;
    align-items: center;
    gap: 12px;
    width: 100%;
    padding: 10px 14px;
    border: none;
    border-left: 2px solid transparent;
    border-radius: 8px;
    background-color: transparent;
    color: var(--clickgui-text-color);
    text-align: left;
    cursor: pointer;
    font-family: inherit;
    transition: background-color 120ms ease, border-color 120ms ease, transform 90ms ease;

    /* Acknowledges the click without moving anything around it. */
    &:active {
      transform: scale(0.995);
    }

    &:hover {
      background-color: var(--clickgui-module-hover-background-color);
    }

    &.selected {
      background-color: var(--clickgui-module-hover-background-color);
      border-left-color: var(--accent-color);
    }
  }

  .text {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
    flex: 1;
  }

  .name {
    font-size: 14px;
    font-weight: 500;
  }

  /* One line only: the settings pane is where the full text belongs. */
  .description {
    font-size: 11px;
    color: var(--clickgui-text-dimmed-color);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .switch {
    flex-shrink: 0;
    position: relative;
    width: 34px;
    height: 18px;
    border-radius: 9px;
    background-color: color-mix(in srgb, var(--text-color) 22%, transparent);
    transition: background-color 140ms ease;
    cursor: pointer;

    &.on {
      background-color: var(--accent-color);
    }

    &:focus-visible {
      outline: 2px solid var(--accent-color);
      outline-offset: 2px;
    }
  }

  .knob {
    position: absolute;
    top: 2px;
    left: 2px;
    width: 14px;
    height: 14px;
    border-radius: 50%;
    background-color: #fff;
    transition: transform 140ms cubic-bezier(0.2, 0, 0, 1);
  }

  .switch.on .knob {
    transform: translateX(16px);
  }
</style>
