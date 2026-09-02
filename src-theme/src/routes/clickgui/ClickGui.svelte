<script lang="ts">
    import type {Module} from "../../integration/types";
    import {onMount} from "svelte";
    import {scale} from "svelte/transition";
    import {cubicOut} from "svelte/easing";
    import {getModules} from "../../integration/rest";
    import {listen} from "../../integration/ws";
    import ScaledClickGuiContent from "./ScaledClickGuiContent.svelte";
    import ModuleRow from "./ModuleRow.svelte";
    import SettingsPane from "./SettingsPane.svelte";
    import {groupOf, orderGroups} from "./module_groups";

    export let onOpenHudEditor: () => void = () => {};
    export let onOpenSettings: () => void = () => {};

    type Section = { group: string; modules: Module[] };

    let modules: Module[] = [];
    let selectedName: string | null = null;
    let query = "";
    let onlyEnabled = false;
    let searchInput: HTMLInputElement;
    let listEl: HTMLElement;

    onMount(async () => {
        await refresh();

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
    }

    $: searching = query.trim().length > 0;

    $: matching = (() => {
        const needle = query.trim().toLowerCase();

        return modules.filter((m) => {
            if (onlyEnabled && !m.enabled) {
                return false;
            }

            if (!needle) {
                return true;
            }

            return m.name.toLowerCase().includes(needle)
                || (m.description ?? "").toLowerCase().includes(needle)
                || (m.aliases ?? []).some((a) => a.toLowerCase().includes(needle));
        });
    })();

    /*
     * One list, always. Groups are headings inside it rather than filters that
     * hide the rest - the whole point of the rework is that you can scroll from
     * the first module to the last without choosing a category first.
     */
    $: sections = (() => {
        const byGroup = new Map<string, Module[]>();

        for (const module of matching) {
            const group = groupOf(module.name, module.category);
            const bucket = byGroup.get(group);
            if (bucket) {
                bucket.push(module);
            } else {
                byGroup.set(group, [module]);
            }
        }

        return orderGroups(byGroup.keys()).map((group): Section => ({
            group,
            modules: [...(byGroup.get(group) ?? [])].sort((a, b) => a.name.localeCompare(b.name)),
        }));
    })();

    $: selected = modules.find((m) => m.name === selectedName) ?? null;
    $: enabledCount = modules.filter((m) => m.enabled).length;

    function enabledIn(section: Section): number {
        return section.modules.filter((m) => m.enabled).length;
    }

    /*
     * The sidebar scrolls the list rather than filtering it. Someone after "the
     * combat ones" gets taken there and can keep scrolling past into the next
     * group, which is the whole point of a browsable list.
     */
    function jumpTo(group: string) {
        const target = listEl?.querySelector<HTMLElement>(`[data-group="${CSS.escape(group)}"]`);
        target?.scrollIntoView({behavior: "smooth", block: "start"});
    }

    function clearFilters() {
        query = "";
        onlyEnabled = false;
        listEl?.scrollTo({top: 0, behavior: "smooth"});
    }
</script>

<ScaledClickGuiContent>
    <div class="stage">
    <div class="window" transition:scale|global={{duration: 170, start: 0.97, opacity: 0, easing: cubicOut}}>
        <header class="titlebar">
            <span class="brand">Tsunami</span>

            <div class="search-wrap">
                <input
                        class="search"
                        type="text"
                        placeholder="Search modules"
                        bind:value={query}
                        bind:this={searchInput}
                        spellcheck="false"
                        autocomplete="off"
                />
                {#if searching}
                    <button class="clear" type="button" onclick={() => (query = "")} aria-label="Clear search">
                        &times;
                    </button>
                {/if}
            </div>
        </header>

        <div class="layout">
            <nav class="sidebar">
                <p class="nav-label">Modules</p>

                <button
                        class="nav-item"
                        class:active={!onlyEnabled}
                        type="button"
                        onclick={clearFilters}
                >
                    <span>All</span>
                    <span class="count muted">{modules.length}</span>
                </button>

                <button
                        class="nav-item"
                        class:active={onlyEnabled}
                        type="button"
                        onclick={() => (onlyEnabled = true)}
                >
                    <span>Enabled</span>
                    <span class="count">{enabledCount}</span>
                </button>

                {#if sections.length > 0}
                    <p class="nav-label">Jump to</p>

                    {#each sections as section (section.group)}
                        <button class="nav-item jump" type="button" onclick={() => jumpTo(section.group)}>
                            <span>{section.group}</span>
                            <span class="count muted">{section.modules.length}</span>
                        </button>
                    {/each}
                {/if}

                <div class="nav-spacer"></div>

                <p class="nav-label">Client</p>

                <button class="nav-item" type="button" onclick={onOpenHudEditor}>
                    <span>HUD Editor</span>
                </button>

                <button class="nav-item" type="button" onclick={onOpenSettings}>
                    <span>Settings</span>
                </button>
            </nav>

            <section class="list" bind:this={listEl}>
                {#if searching || onlyEnabled}
                    <p class="hint">
                        Showing {matching.length} of {modules.length}
                    </p>
                {/if}

                {#each sections as section (section.group)}
                    <div class="section" data-group={section.group}>
                        <div class="section-header">
                            <span class="section-title">{section.group}</span>
                            <span class="section-meta">
                                {#if enabledIn(section) > 0}{enabledIn(section)} on{/if}
                            </span>
                        </div>

                        {#each section.modules as module (module.name)}
                            <ModuleRow
                                    name={module.name}
                                    enabled={module.enabled}
                                    description={module.description}
                                    selected={selectedName === module.name}
                                    onSelect={(n) => (selectedName = n)}
                            />
                        {/each}
                    </div>
                {:else}
                    <div class="empty">
                        <p class="empty-title">Nothing matches</p>
                        <p class="hint">Try a different search, or clear the filters.</p>
                        <button class="empty-action" type="button" onclick={clearFilters}>Show everything</button>
                    </div>
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
  /*
   * One window, one list. Upstream drew a draggable panel per category, which
   * suits a client whose users rearrange constantly; here it meant nothing was
   * ever where you left it and every category overlapped another. The single
   * window replaced that, and this pass removed the last of the siloing - the
   * sidebar no longer decides what you are allowed to see.
   */
  /* Centres the window inside the scaled full-viewport stage. */
  .stage {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .window {
    display: flex;
    flex-direction: column;
    /* Percentages of the stage, not vh. The stage is already a scaled virtual
       viewport - ScaledClickGuiContent sizes it 2/scale * 100vw|vh and then
       scales it down - so a vh here resolves against the real screen and the
       window runs off the bottom of it. Seen in a running client. */
    width: min(1040px, 92%);
    height: min(640px, 88%);
    border-radius: 16px;
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

  .search-wrap {
    position: relative;
    flex: 1;
    display: flex;
  }

  .search {
    flex: 1;
    padding: 8px 32px 8px 12px;
    border: 1px solid transparent;
    border-radius: 8px;
    /* Needs a visible surface: over a dark backdrop a translucent black
       field is indistinguishable from the panel behind it, and reads as a
       label rather than something you can type into. */
    background-color: color-mix(in srgb, var(--text-color) 8%, transparent);
    color: var(--clickgui-text-color);
    font-family: inherit;
    font-size: 13px;
    transition: border-color 140ms ease, background-color 140ms ease;

    &::placeholder {
      color: var(--clickgui-text-dimmed-color);
    }

    &:focus {
      outline: none;
      border-color: var(--accent-color);
      background-color: color-mix(in srgb, var(--text-color) 12%, transparent);
    }
  }

  .clear {
    position: absolute;
    right: 6px;
    top: 50%;
    transform: translateY(-50%);
    width: 20px;
    height: 20px;
    border: none;
    border-radius: 50%;
    background-color: transparent;
    color: var(--clickgui-text-dimmed-color);
    font-size: 16px;
    line-height: 1;
    cursor: pointer;
    transition: color 120ms ease, background-color 120ms ease;

    &:hover {
      color: var(--clickgui-text-color);
      background-color: var(--clickgui-module-hover-background-color);
    }
  }

  .layout {
    display: grid;
    grid-template-columns: 184px minmax(0, 1fr) 312px;
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

  .nav-label {
    margin: 10px 12px 4px;
    font-size: 10px;
    font-weight: 600;
    letter-spacing: 0.09em;
    text-transform: uppercase;
    color: var(--clickgui-text-dimmed-color);

    &:first-child {
      margin-top: 2px;
    }
  }

  .nav-spacer {
    flex: 1;
    min-height: 12px;
  }

  .nav-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    padding: 8px 12px;
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

  /* Jump links are navigation, not state - they must not look selected. */
  .jump {
    font-size: 12.5px;
  }

  .count {
    min-width: 18px;
    padding: 1px 6px;
    border-radius: 999px;
    background-color: var(--accent-color);
    color: #fff;
    font-size: 10px;
    font-variant-numeric: tabular-nums;
    text-align: center;

    &.muted {
      background-color: transparent;
      color: var(--clickgui-text-dimmed-color);
    }
  }

  .list {
    display: flex;
    flex-direction: column;
    padding: 8px 10px 24px;
    overflow-y: auto;
    scroll-behavior: smooth;
  }

  .section {
    display: flex;
    flex-direction: column;
    gap: 2px;
    scroll-margin-top: 8px;
  }

  /* Sticky, so you always know where you are in a list this long. */
  .section-header {
    position: sticky;
    top: 0;
    z-index: 1;
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    margin-top: 14px;
    padding: 6px 14px;
    background-color: var(--clickgui-base-90-color);
    backdrop-filter: blur(8px);
  }

  .section-title {
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 0.09em;
    text-transform: uppercase;
    color: var(--clickgui-text-color);
  }

  .section-meta {
    font-size: 10px;
    color: var(--clickgui-text-dimmed-color);
    font-variant-numeric: tabular-nums;
  }

  .settings {
    border-left: 1px solid var(--clickgui-base-70-color);
    background-color: var(--clickgui-base-70-color);
    min-height: 0;
  }

  .hint {
    margin: 6px 14px;
    font-size: 12px;
    color: var(--clickgui-text-dimmed-color);
  }

  .empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 4px;
    flex: 1;
    padding: 40px 20px;
    text-align: center;
  }

  .empty-title {
    margin: 0;
    font-size: 14px;
    font-weight: 500;
    color: var(--clickgui-text-color);
  }

  .empty-action {
    margin-top: 10px;
    padding: 7px 14px;
    border: 1px solid var(--accent-color);
    border-radius: 8px;
    background-color: transparent;
    color: var(--accent-color);
    font-family: inherit;
    font-size: 12px;
    cursor: pointer;
    transition: background-color 120ms ease;

    &:hover {
      background-color: var(--accent-subtle-background-color);
    }
  }
</style>
