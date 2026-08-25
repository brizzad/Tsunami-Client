<script lang="ts">
    // Keeps the prop contract of the LiquidBounce logo it replaces, so
    // AnimatedLogo can still paint the mark with its animated gradient and
    // Watermark can still pass a flat accent colour.
    //
    // The viewBox is unchanged at 261.263 x 98 so every existing width/height
    // pairing keeps its aspect ratio.
    export let width = "261.263px";
    export let height = "98px";
    export let badgeFill = "var(--accent-color)";
    export let badgeTextFill = "var(--text-color)";
    export let badgeGroupClass = "";
</script>

<svg
    class="tsunami-logo"
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 261.263 98"
    aria-hidden="true"
    style={`width: ${width}; height: ${height};`}
>
    <defs>
        <slot name="defs" />
    </defs>

    <g class={badgeGroupClass}>
        <g
            transform="translate(2 4) scale(0.9)"
            fill="none"
            stroke={badgeFill}
            stroke-width="8.5"
            stroke-linecap="round"
            stroke-linejoin="round"
        >
            <path d="M12 58C12 30 34 12 58 12c18 0 30 12 30 28 0 12-9 21-21 21-9 0-16-7-16-16" />
            <g opacity="0.55">
                <path d="M6 77q11-10 22 0t22 0 22 0 22 0" />
                <path d="M6 91q11-10 22 0t22 0 22 0 22 0" />
            </g>
        </g>
    </g>

    <text
        x="104"
        y="60"
        fill={badgeTextFill}
        font-size="26"
        font-weight="600"
        letter-spacing="2"
        dominant-baseline="middle"
    >TSUNAMI</text>
</svg>

<style>
    .tsunami-logo text {
        font-family: inherit;
    }
</style>
