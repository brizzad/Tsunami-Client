<script lang="ts">
    import {getModuleSettings, setModuleSettings, setModuleEnabled} from "../../integration/rest";
    import type {ConfigurableSetting} from "../../integration/types";
    import GenericSetting from "./setting/common/GenericSetting.svelte";
    import {convertToSpacedString, spaceSeperatedNames} from "../../theme/theme_config";

    export let name: string | null;
    export let enabled: boolean;
    export let description: string;

    let configurable: ConfigurableSetting | null = null;
    let loadedFor: string | null = null;

    $: if (name && name !== loadedFor) {
        loadedFor = name;
        load(name);
    }

    async function load(moduleName: string) {
        configurable = null;
        configurable = await getModuleSettings(moduleName);
    }

    async function save() {
        if (!name || !configurable) return;
        await setModuleSettings(name, configurable);
        configurable = await getModuleSettings(name);
    }

    async function toggle() {
        if (!name) return;
        await setModuleEnabled(name, !enabled);
    }

    /**
     * Bind and Hidden are handled by the header and the HUD respectively, so
     * showing them again in the body would be two controls for one value.
     */
    $: settings = configurable?.value.filter((v) => v.name !== "Bind" && v.name !== "Hidden") ?? [];
    $: bind = configurable?.value.find((v) => v.name === "Bind") ?? null;
</script>

<div class="pane">
    {#if !name}
        <div class="empty">
            <p>Select a module</p>
            <span>Its settings appear here.</span>
        </div>
    {:else}
        <header>
            <div class="title">
                <h2>{$spaceSeperatedNames ? convertToSpacedString(name) : name}</h2>
                <button class="toggle" class:on={enabled} onclick={toggle} type="button">
                    {enabled ? "Enabled" : "Disabled"}
                </button>
            </div>
            {#if description}
                <p class="description">{description}</p>
            {/if}
        </header>

        <div class="body">
            {#if bind}
                <div class="bind">
                    <GenericSetting path="clickgui.{name}" bind:setting={bind} on:change={save}/>
                </div>
            {/if}

            {#if configurable === null}
                <p class="muted">Loading…</p>
            {:else if settings.length === 0}
                <p class="muted">This module has no settings.</p>
            {:else}
                {#each settings as setting (setting.name)}
                    <GenericSetting path="clickgui.{name}" bind:setting={setting} on:change={save}/>
                {/each}
            {/if}
        </div>
    {/if}
</div>

<style lang="scss">
  .pane {
    display: flex;
    flex-direction: column;
    height: 100%;
    min-height: 0;
  }

  header {
    padding: 18px 20px 14px;
    border-bottom: 1px solid var(--clickgui-base-70-color);
  }

  .title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  h2 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: var(--clickgui-text-color);
  }

  .toggle {
    flex-shrink: 0;
    padding: 5px 12px;
    border: 1px solid color-mix(in srgb, var(--text-color) 22%, transparent);
    border-radius: 999px;
    background-color: transparent;
    color: var(--clickgui-text-dimmed-color);
    font-family: inherit;
    font-size: 11px;
    cursor: pointer;
    transition: background-color 120ms ease, border-color 120ms ease, color 120ms ease;

    &.on {
      background-color: var(--accent-color);
      border-color: var(--accent-color);
      color: #fff;
    }
  }

  .description {
    margin: 8px 0 0;
    font-size: 12px;
    line-height: 1.5;
    color: var(--clickgui-text-dimmed-color);
  }

  .body {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    padding: 14px 20px 20px;
  }

  .bind {
    margin-bottom: 10px;
    padding-bottom: 10px;
    border-bottom: 1px solid var(--clickgui-base-70-color);
  }

  .muted {
    font-size: 12px;
    color: var(--clickgui-text-dimmed-color);
  }

  .empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 6px;
    height: 100%;
    color: var(--clickgui-text-dimmed-color);

    p {
      margin: 0;
      font-size: 14px;
      color: var(--clickgui-text-color);
    }

    span {
      font-size: 12px;
    }
  }
</style>
