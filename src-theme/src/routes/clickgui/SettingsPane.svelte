<script lang="ts">
    import {getModuleSettings, setModuleSettings, setModuleEnabled} from "../../integration/rest";
    import type {ConfigurableSetting} from "../../integration/types";
    import GenericSetting from "./setting/common/GenericSetting.svelte";
    import {convertToSpacedString, spaceSeperatedNames} from "../../theme/theme_config";
    import {fly} from "svelte/transition";
    import {cubicOut} from "svelte/easing";

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
     * Indices into `configurable.value`, never copies of the settings themselves.
     *
     * `bind:setting` has to write back into the array `save()` sends, and a
     * `filter()`/`find()` result is a different array holding the same object
     * references. A child that reassigns its prop - which every setting editor
     * does, as `setting = {...cSetting}` - then writes into that copy, and the
     * change never reaches `configurable`. Binding to `configurable.value[i]`
     * is what `GlobalSettings.svelte` already does, for this reason.
     *
     * Bind and Hidden are handled by the header and the HUD respectively, so
     * showing them again in the body would be two controls for one value.
     */
    $: settingIndices = configurable
        ? configurable.value.flatMap((v, i) =>
            v.name === "Bind" || v.name === "Hidden" ? [] : [i])
        : [];
    $: bindIndex = configurable?.value.findIndex((v) => v.name === "Bind") ?? -1;
</script>

<div class="pane">
    {#if !name}
        <div class="empty">
            <p>Select a module</p>
            <span>Its settings appear here.</span>
        </div>
    {:else}
        {#key name}
        <div class="content" in:fly|global={{duration: 150, y: 6, easing: cubicOut}}>
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
            {#if configurable && bindIndex >= 0}
                <div class="bind">
                    <GenericSetting
                            path="clickgui.{name}"
                            bind:setting={configurable.value[bindIndex]}
                            on:change={save}
                    />
                </div>
            {/if}

            {#if configurable === null}
                <p class="muted">Loading…</p>
            {:else if settingIndices.length === 0}
                <p class="muted">This module has no settings.</p>
            {:else}
                {#each settingIndices as i (configurable.value[i].name)}
                    <GenericSetting
                            path="clickgui.{name}"
                            bind:setting={configurable.value[i]}
                            on:change={save}
                    />
                {/each}
            {/if}
        </div>
        </div>
        {/key}
    {/if}
</div>

<style lang="scss">
  .pane {
    display: flex;
    flex-direction: column;
    height: 100%;
    min-height: 0;
  }

  /* Wrapper for the keyed fade; it must not change the pane's own layout. */
  .content {
    display: flex;
    flex-direction: column;
    min-height: 0;
    flex: 1;
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
