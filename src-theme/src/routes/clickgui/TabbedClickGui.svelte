<script lang="ts">
    import ClickGui from "./ClickGui.svelte";
    import GlobalSettings from "./tabs/GlobalSettings.svelte";
    import {gridSize, os, scaleFactor, snappingEnabled, darken} from "./clickgui_store";
    import type {ConfigurableSetting, TogglableSetting} from "../../integration/types";
    import {onMount} from "svelte";
    import {
        getClientInfo,
        getGameWindow,
        getModuleSettings,
        setHudEditorSelected,
        setTyping
    } from "../../integration/rest";
    import {listen} from "../../integration/ws";
    import type {ClickGuiValueChangeEvent, ScaleFactorChangeEvent} from "../../integration/events";
    import HudEditor from "./tabs/hud_editor/HudEditor.svelte";

    /*
     * Three views, one shell. Upstream floated a pill of tabs over the top of
     * everything, which is a second navigation system sitting on top of the one
     * inside the window - and it put "HUD Editor" and "Settings" at the same
     * level as the entire module list. They are now entries in the ClickGUI's
     * own sidebar, and this only decides which view is mounted.
     */
    type View = "modules" | "hud" | "settings";

    let view = $state<View>("modules");
    let minecraftScaleFactor = $state(2);
    let clickGuiScaleFactor = $state(1);

    $effect(() => {
        $scaleFactor = minecraftScaleFactor * clickGuiScaleFactor;
    });

    /*
     * HudEditor sets and clears the client's own hud-editor flag in its mount
     * and cleanup, so switching away from it is enough to leave edit mode.
     * Setting it here as well would fight with that.
     */
    function show(next: View) {
        view = next;
    }

    function applyValues(configurable: ConfigurableSetting) {
        const scaleValue = configurable.value.find(v => v.name === "Scale");
        const snappingValue = configurable.value.find(v => v.name === "Snapping") as TogglableSetting | undefined;

        if (scaleValue) {
            clickGuiScaleFactor = scaleValue.value as number;
        }

        if (snappingValue) {
            $snappingEnabled = snappingValue.value.find(v => v.name === "Enabled")?.value as boolean ?? true;
            $gridSize = snappingValue.value.find(v => v.name === "GridSize")?.value as number ?? 10;
        }
    }

    onMount(async () => {
        await setHudEditorSelected(false);

        $os = (await getClientInfo()).os;

        const gameWindow = await getGameWindow();
        minecraftScaleFactor = gameWindow.scaleFactor;

        const clickGuiSettings = await getModuleSettings("ClickGUI");
        applyValues(clickGuiSettings);

        await setTyping(false);
    });

    listen("scaleFactorChange", (e: ScaleFactorChangeEvent) => {
        minecraftScaleFactor = e.scaleFactor;
    });

    listen("clickGuiValueChange", (e: ClickGuiValueChangeEvent) => {
        applyValues(e.configurable);
    });
</script>

<div
        class="tabbed-clickgui"
        class:darken={$darken}
>
    {#if view === "modules"}
        <ClickGui
                onOpenHudEditor={() => show("hud")}
                onOpenSettings={() => show("settings")}
        />
    {:else if view === "hud"}
        <HudEditor onBack={() => show("modules")}/>
    {:else}
        <GlobalSettings onBack={() => show("modules")}/>
    {/if}
</div>

<style lang="scss">
  .tabbed-clickgui {
    overflow: hidden;
    position: absolute;
    inset: 0;
    transition: background-color 180ms ease;

    &.darken {
      background-color: var(--clickgui-overlay-background-color);
    }
  }
</style>
