<script lang="ts">
    // The Tsunami mark, drawn from the real logo art rather than an approximation of it.
    //
    // The art is a solid silhouette, so it is embedded as a white-on-transparent image and
    // used as a luminance mask over a rect painted with [badgeFill]. That keeps the prop
    // contract the placeholder had - AnimatedLogo can still lay an animated gradient over
    // the mark, and Watermark can still pass a flat accent - which drawing the coloured PNG
    // directly would have thrown away.
    //
    // The viewBox stays 261.263 x 98 so every existing width/height pairing keeps its ratio.
    export let width = "261.263px";
    export let height = "98px";
    export let badgeFill = "var(--accent-color)";
    export let badgeTextFill = "var(--text-color)";
    export let badgeGroupClass = "";

    // Unique per instance: two logos on one page would otherwise share a mask id.
    const maskId = `tsunami-mark-${Math.random().toString(36).slice(2, 9)}`;
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
        <mask id={maskId}>
            <image
                href="data:image/png;base64,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"
                x="0"
                y="1"
                width="96"
                height="96"
            />
        </mask>
    </defs>

    <g class={badgeGroupClass}>
        <rect x="0" y="1" width="96" height="96" fill={badgeFill} mask={`url(#${maskId})`} />
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
