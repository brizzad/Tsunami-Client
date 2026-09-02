<script lang="ts">
    import type {Snippet} from "svelte";
    import {fade} from "svelte/transition";
    import {quintOut} from "svelte/easing";

    let { title, icon, onBack, children } = $props<{
        title: string;
        icon?: string;
        onBack?: () => void;
        children: Snippet;
    }>();
</script>

<div class="window" transition:fade|global={{duration: 200, easing: quintOut}}>
    <div class="title">
        {#if onBack}
            <button class="back" type="button" onclick={onBack} aria-label="Back to modules">
                <span class="chevron"></span>
                Modules
            </button>
        {/if}
        {#if icon}
            <img
                    class="icon"
                    src="img/clickgui/icon-{icon}.svg"
                    alt="icon"
            />
        {/if}
        <span class="title-text">{title}</span>
    </div>
    <div class="content">
        {@render children()}
    </div>
</div>

<style lang="scss">

  .window {
    position: fixed;
    top: 70px;
    left: 50%;
    transform: translateX(-50%);
    width: min(1040px, 92vw);
    --window-max-height: 78vh;
    background-color: var(--clickgui-base-90-color);
    max-height: var(--window-max-height, none);
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 24px 60px rgba(0, 0, 0, 0.55);
    backdrop-filter: blur(18px);
    user-select: none;
  }

  .title {
    display: grid;
    grid-template-columns: max-content max-content 1fr;
    align-items: center;
    column-gap: 12px;
    background-color: transparent;
    padding: 14px 18px;
    font-size: 15px;
    font-weight: 600;
    color: var(--clickgui-text-color);
    border-bottom: 1px solid var(--clickgui-base-70-color);
  }

  .title-text {
    font-weight: 600;
  }

  /* The way back to the module list, now that these are not tabs. */
  .back {
    display: flex;
    align-items: center;
    gap: 7px;
    padding: 5px 11px 5px 8px;
    border: none;
    border-radius: 999px;
    background-color: var(--clickgui-base-70-color);
    color: var(--clickgui-text-dimmed-color);
    font-family: inherit;
    font-size: 12px;
    font-weight: 500;
    cursor: pointer;
    transition: background-color 120ms ease, color 120ms ease;

    &:hover {
      background-color: var(--clickgui-module-hover-background-color);
      color: var(--clickgui-text-color);
    }
  }

  .chevron {
    width: 7px;
    height: 7px;
    border-left: 1.5px solid currentColor;
    border-bottom: 1.5px solid currentColor;
    transform: rotate(45deg);
  }

  .content {
    padding: 14px 18px 20px;
    overflow: auto;
    max-height: calc(var(--window-max-height, 9999px) - 60px);
  }
</style>
