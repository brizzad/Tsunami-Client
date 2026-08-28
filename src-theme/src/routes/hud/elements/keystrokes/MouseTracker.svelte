<script lang="ts">
    import {onDestroy, onMount} from "svelte";
    import {listen} from "../../../../integration/ws";
    import type {MouseTrailEvent} from "../../../../integration/events";

    // Ported from Clean Keystrokes (LGPL-3.0, ItsPasi) - same accumulate/decay/trail model,
    // redrawn on a canvas here because Tsunami's HUD is a web overlay rather than an
    // immediate-mode Minecraft renderer.
    const SENSITIVITY = 0.02;
    const DECAY = 0.8;
    const TRAIL_MS = 400;
    const SIZE = 50;

    let canvas: HTMLCanvasElement;
    let frame: number;

    // Deltas arrive throttled at ~30Hz; the render loop runs at display rate and decays
    // between them, so motion stays smooth rather than stepping once per message.
    let pendingX = 0, pendingY = 0;
    let smoothX = 0, smoothY = 0;
    let trail: { x: number; y: number; t: number }[] = [];

    listen("mouseTrail", (e: MouseTrailEvent) => {
        pendingX += e.dx;
        pendingY += e.dy;
    });

    function draw(now: number) {
        frame = requestAnimationFrame(draw);
        if (!canvas) return;

        smoothX = smoothX * DECAY + pendingX * SENSITIVITY;
        smoothY = smoothY * DECAY + pendingY * SENSITIVITY;
        pendingX = 0;
        pendingY = 0;

        const half = SIZE / 2;
        const x = Math.max(-half, Math.min(half, smoothX)) + half;
        const y = Math.max(-half, Math.min(half, smoothY)) + half;

        trail.push({x, y, t: now});
        trail = trail.filter(p => now - p.t < TRAIL_MS);

        const ctx = canvas.getContext("2d");
        if (!ctx) return;
        ctx.clearRect(0, 0, SIZE, SIZE);

        const accent = getComputedStyle(canvas).getPropertyValue("color") || "#fff";
        ctx.strokeStyle = accent;
        ctx.lineWidth = 1.5;
        ctx.lineJoin = "round";
        ctx.lineCap = "round";

        // Older points fade out, so the stroke reads as direction rather than a static scribble.
        for (let i = 1; i < trail.length; i++) {
            const p = trail[i], prev = trail[i - 1];
            ctx.globalAlpha = 1 - (now - p.t) / TRAIL_MS;
            ctx.beginPath();
            ctx.moveTo(prev.x, prev.y);
            ctx.lineTo(p.x, p.y);
            ctx.stroke();
        }

        ctx.globalAlpha = 1;
        ctx.beginPath();
        ctx.arc(x, y, 2, 0, Math.PI * 2);
        ctx.fillStyle = accent;
        ctx.fill();
    }

    onMount(() => {
        frame = requestAnimationFrame(draw);
    });

    onDestroy(() => cancelAnimationFrame(frame));
</script>

<div class="tracker">
    <canvas bind:this={canvas} width={SIZE} height={SIZE}></canvas>
</div>

<style lang="scss">
  .tracker {
    background-color: var(--keystrokes-background-color);
    border-radius: 5px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 2px;
    color: var(--keystrokes-active-color);
  }

  canvas {
    display: block;
  }
</style>
