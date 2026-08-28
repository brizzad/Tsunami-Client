<script lang="ts">
    import Key from "./Key.svelte";
    import MouseKey from "./MouseKey.svelte";
    import MouseTracker from "./MouseTracker.svelte";
    import {onMount} from "svelte";
    import {getMinecraftKeybinds} from "../../../../integration/rest";
    import type {MinecraftKeybind, SessionStats} from "../../../../integration/types";
    import type {SessionStatsEvent} from "../../../../integration/events";
    import {listen} from "../../../../integration/ws";
    import type {HudKeystrokesSettings} from "../../components";

    export let settings: { [name: string]: any };

    let cSettings: HudKeystrokesSettings;

    $: cSettings = settings as HudKeystrokesSettings;

    let keyForward: MinecraftKeybind | undefined;
    let keyBack: MinecraftKeybind | undefined;
    let keyLeft: MinecraftKeybind | undefined;
    let keyRight: MinecraftKeybind | undefined;
    let keyJump: MinecraftKeybind | undefined;
    let keyAttack: MinecraftKeybind | undefined;
    let keyUse: MinecraftKeybind | undefined;
    let keySneak: MinecraftKeybind | undefined;
    let keySprint: MinecraftKeybind | undefined;

    let cps = {left: 0, right: 0};

    async function updateKeybinds() {
        const keybinds = await getMinecraftKeybinds();

        keyForward = keybinds.find(k => k.bindName === "key.forward");
        keyBack = keybinds.find(k => k.bindName === "key.back");
        keyLeft = keybinds.find(k => k.bindName === "key.left");
        keyRight = keybinds.find(k => k.bindName === "key.right");
        keyJump = keybinds.find(k => k.bindName === "key.jump");
        keyAttack = keybinds.find(k => k.bindName === "key.attack");
        keyUse = keybinds.find(k => k.bindName === "key.use");
        keySneak = keybinds.find(k => k.bindName === "key.sneak");
        keySprint = keybinds.find(k => k.bindName === "key.sprint");
    }

    onMount(updateKeybinds);

    listen("keybindChange", updateKeybinds);

    // Already measured client-side and pushed four times a second, so the counter here is a
    // readout rather than a second, disagreeing implementation.
    listen("sessionStats", (event: SessionStatsEvent) => {
        const session = event.session as SessionStats;
        cps = session.cps;
    });
</script>

<div class="keystrokes">
    <div class="movement">
        <Key key={keyForward} gridArea="a" />
        <Key key={keyLeft} gridArea="b" />
        <Key key={keyBack} gridArea="c" />
        <Key key={keyRight} gridArea="d" />
        <Key key={keyJump} gridArea="e" />
    </div>

    {#if cSettings?.showSneakSprint}
        <div class="modifiers">
            <Key key={keySneak} gridArea="h" />
            <Key key={keySprint} gridArea="i" />
        </div>
    {/if}

    {#if cSettings?.showMouseButtons}
        <div class="mouse">
            <MouseKey key={keyAttack} label="LMB" cps={cps.left} showCps={cSettings?.showCps ?? true} gridArea="f" />
            <MouseKey key={keyUse} label="RMB" cps={cps.right} showCps={cSettings?.showCps ?? true} gridArea="g" />
        </div>
    {/if}
    {#if cSettings?.showMouseTracker}
        <MouseTracker/>
    {/if}
</div>

<style lang="scss">
  .keystrokes {
    display: flex;
    flex-direction: column;
    gap: 5px;
  }

  .movement {
    display: grid;
    grid-template-areas:
      ". a ."
      "b c d"
      "e e e";
    grid-template-columns: repeat(3, 50px);
    gap: 5px;
  }

  /* Sneak and sprint share the mouse row's shape so the block stays square-edged. */
  .modifiers {
    display: grid;
    grid-template-areas: "h i";
    grid-template-columns: repeat(2, 1fr);
    gap: 5px;
  }

  .mouse {
    display: grid;
    grid-template-areas: "f g";
    grid-template-columns: repeat(2, 1fr);
    gap: 5px;
  }
</style>
