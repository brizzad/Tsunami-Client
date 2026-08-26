<script lang="ts">
    import type {GroupedModules, Module} from "../../integration/types";
    import {fade, scale} from "svelte/transition";
    import {onMount} from "svelte";
    import {getModules} from "../../integration/rest";
    import {groupByCategory} from "../../integration/util";
    import {listen} from "../../integration/ws";
    import ScaledClickGuiContent from "./ScaledClickGuiContent.svelte";
    import ModuleRow from "./ModuleRow.svelte";
    import SettingsPane from "./SettingsPane.svelte";

    let categories: GroupedModules = {};
    let modules: Module[] = [];
    let activeCategory: string | null = null;
    let selectedName: string | null = null;
    let query = "";
    let searchInput: HTMLInputElement;

    onMount(async () => {
        await refresh();
        activeCategory = Object.keys(categories)[0] ?? null;

        // Focused on open, so the menu can be driven by typing the name of the
        // thing you came here for. Without this the first keystrokes go to the
        // game behind it, which at best does nothing and at worst moves you.
        searchInput?.focus();
    });

    // A module toggled by a keybind, a command or the row itself has to be
    // reflected here, or the switch shows the opposite of the truth.
    listen("moduleToggle", refresh);

    async function refresh() {
        modules = await getModules();
        categories = groupByCategory(modules);
    }

    $: searching = query.trim().length > 0;

    $: visible = (() => {
        const needle = query.trim().toLowerCase();

        // Search looks across every category. Hunting for a module whose name
        // you half-remember is the entire point of a search box, and making
        // you pick the right category first defeats it.
        const pool = searching
            ? modules
            : (activeCategory ? categories[activeCategory] ?? [] : []);

        const matches = searching
            ? pool.filter((m) =>
                m.name.toLowerCase().includes(needle) ||
                (m.description ?? "").toLowerCase().includes(needle) ||
                (m.aliases ?? []).some((a) => a.toLowerCase().includes(needle)))
            : pool;

        return [...matches].sort((a, b) => a.name.localeCompare(b.name));
    })();

    $: selected = modules.find((m) => m.name === selectedName) ?? null;

    function enabledIn(category: string, all: GroupedModules): number {
        return (all[category] ?? []).filter((m) => m.enabled).length;
    }
</script>

<ScaledClickGuiContent>
    <div class="backdrop" transition:fade|global={{duration: 150}}>
        <div class="window" transition:scale|global={{duration: 180, start: 0.98}}>
            <header class="titlebar">
                <span class="brand">Tsunami</span>

                <input
                        class="search"
                        type="text"
                        placeholder="Search modules"
                        bind:value={query}
                        bind:this={searchInput}
                        spellcheck="false"
                        autocomplete="off"
                />
            </header>

            <div class="layout">
                <nav class="sidebar">
                    {#each Object.keys(categories) as category (category)}
                        <button
                                class="category"
                                class:active={!searching && activeCategory === category}
                                on:click={() => { query = ""; activeCategory = category; }}
                                type="button"
                        >
                            <span>{category}</span>
                            {#if enabledIn(category, categories) > 0}
                                <span class="count">{enabledIn(category, categories)}</span>
                            {/if}
                        </button>
                    {/each}
                </nav>

                <section class="list">
                    {#if searching}
                        <p class="hint">
                            {visible.length} result{visible.length === 1 ? "" : "s"}
                        </p>
                    {/if}

                    {#each visible as module (module.name)}
                        <ModuleRow
                                name={module.name}
                                enabled={module.enabled}
                                description={module.description}
                                selected={selectedName === module.name}
                                onSelect={(n) => (selectedName = n)}
                        />
                    {:else}
                        <p class="hint">Nothing here.</p>
                    {/each}
                </section>

                <aside class="settings">
                    <SettingsPane
                            name={selectedName}
                            enabled={selected?.enabled ?? false}
                            description={selected?.description ?? ""}
                    />
                </aside>
            </div>
        </div>
    </div>
</ScaledClickGuiContent>

<style lang="scss">
  .backdrop {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: var(--clickgui-overlay-background-color);
  }

  /*
   * One window instead of loose panels. Draggable per-category panels suit a
   * client whose users rearrange constantly; here it mostly means nothing is
   * ever where you left it, and every category overlaps another.
   */
  .window {
    display: flex;
    flex-direction: column;
    width: min(1000px, 88vw);
    height: min(620px, 84vh);
    border-radius: 14px;
    overflow: hidden;
    background-color: var(--clickgui-base-90-color);
    box-shadow: 0 24px 60px rgba(0, 0, 0, 0.55);
    backdrop-filter: blur(18px);
  }

  .titlebar {
    display: flex;
    align-items: center;
    gap: 20px;
    padding: 14px 18px;
    border-bottom: 1px solid var(--clickgui-base-70-color);
  }

  .brand {
    font-size: 15px;
    font-weight: 700;
    letter-spacing: 0.08em;
    text-transform: uppercase;
    color: var(--accent-color);
  }

  .search {
    flex: 1;
    padding: 8px 12px;
    border: 1px solid transparent;
    border-radius: 8px;
    /* Needs a visible surface: over a dark backdrop a translucent black
       field is indistinguishable from the panel behind it, and reads as a
       label rather than something you can type into. */
    background-color: color-mix(in srgb, var(--text-color) 8%, transparent);
    color: var(--clickgui-text-color);
    font-family: inherit;
    font-size: 13px;

    &::placeholder {
      color: var(--clickgui-text-dimmed-color);
    }

    &:focus {
      outline: none;
      border-color: var(--accent-color);
    }
  }

  .layout {
    display: grid;
    grid-template-columns: 168px minmax(0, 1fr) 300px;
    flex: 1;
    min-height: 0;
  }

  .sidebar {
    display: flex;
    flex-direction: column;
    gap: 2px;
    padding: 12px 10px;
    border-right: 1px solid var(--clickgui-base-70-color);
    overflow-y: auto;
  }

  .category {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    padding: 9px 12px;
    border: none;
    border-radius: 8px;
    background-color: transparent;
    color: var(--clickgui-text-dimmed-color);
    font-family: inherit;
    font-size: 13px;
    text-align: left;
    cursor: pointer;
    transition: background-color 120ms ease, color 120ms ease;

    &:hover {
      background-color: var(--clickgui-module-hover-background-color);
      color: var(--clickgui-text-color);
    }

    &.active {
      background-color: var(--accent-subtle-background-color);
      color: var(--clickgui-text-color);
    }
  }

  /* How many modules in a category are on, without having to open it. */
  .count {
    min-width: 18px;
    padding: 1px 5px;
    border-radius: 999px;
    background-color: var(--accent-color);
    color: #fff;
    font-size: 10px;
    text-align: center;
  }

  .list {
    display: flex;
    flex-direction: column;
    gap: 2px;
    padding: 12px 10px;
    overflow-y: auto;
  }

  .settings {
    border-left: 1px solid var(--clickgui-base-70-color);
    background-color: var(--clickgui-base-70-color);
    min-height: 0;
  }

  .hint {
    margin: 6px 4px;
    font-size: 12px;
    color: var(--clickgui-text-dimmed-color);
  }
</style>
