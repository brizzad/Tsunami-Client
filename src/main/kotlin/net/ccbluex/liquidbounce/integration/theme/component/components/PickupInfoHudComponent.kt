/*
 * This file is part of Tsunami, a fork of LiquidBounce
 * (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 * Copyright (c) 2026 Tsunami contributors
 *
 * Tsunami is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tsunami is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tsunami. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.integration.theme.component.components

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import kotlin.math.roundToInt

/**
 * PickupInfo
 *
 * A running tally of what has just entered or left your inventory, beside the
 * hotbar.
 *
 * Vanilla gives no feedback on a pickup at all past the item appearing in a
 * slot you may not be looking at, which makes "did that drop actually go in"
 * a question you answer by opening the inventory. It matters most where the
 * pickup is contested or fast: mining a vein, clearing a mob drop, or checking
 * whether a shulker actually took the stack.
 *
 * Losses are shown too, in a different colour, because "did I just lose that"
 * is the same question in the other direction.
 *
 * ## How it knows
 *
 * By diffing your own inventory once a tick against its previous contents. It
 * reads nothing but the inventory the client is already holding, and sends
 * nothing anywhere.
 *
 * ## Was a module until 2026-09-02
 *
 * Positioned by an anchor enum and two offset spinners; now dragged in the HUD
 * editor. Its bind came with it, because [HudComponent] gained one in the same
 * commit.
 */
object PickupInfoHudComponent : NativeHudComponent(
    "PickupInfo",
    enabled = false,
    alignment = Alignment(
        horizontalAlignment = Alignment.ScreenAxisX.LEFT,
        horizontalOffset = 4,
        verticalAlignment = Alignment.ScreenAxisY.BOTTOM,
        verticalOffset = 60,
    ),
    description = "A running tally of what just entered or left your inventory.",
) {

    /** How long an entry stays on screen, in milliseconds. */
    private val duration by int("Duration", 4000, 500..20_000)

    /** Show items leaving the inventory as well as arriving. */
    private val showLosses by boolean("ShowLosses", true)

    /** Most rows drawn at once. */
    private val maxEntries by int("MaxEntries", 6, 1..16)

    private val gainColor by color("GainColor", Color4b(0x4C, 0xD9, 0x64, 0xFF))
    private val lossColor by color("LossColor", Color4b(0xFF, 0x3B, 0x30, 0xFF))

    private const val LINE_HEIGHT = 10

    /** Previous tick's inventory, as item to total count. */
    private val previous = Object2IntOpenHashMap<Item>()

    private val entries = ArrayDeque<Entry>()

    private var primed = false

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        // Joining a world fills the inventory in one go; without this the first
        // tick reports the entire inventory as picked up.
        previous.clear()
        entries.clear()
        primed = false
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val player = mc.player ?: return@handler

        val current = Object2IntOpenHashMap<Item>()
        for (stack in player.inventory.nonEquipmentItems) {
            if (!stack.isEmpty) {
                current.addTo(stack.item, stack.count)
            }
        }
        for (stack in listOf(player.offhandItem)) {
            if (!stack.isEmpty) {
                current.addTo(stack.item, stack.count)
            }
        }

        if (!primed) {
            primed = true
            previous.putAll(current)
            return@handler
        }

        val seen = HashSet<Item>(current.keys)
        seen.addAll(previous.keys)

        val now = System.currentTimeMillis()
        for (item in seen) {
            val delta = current.getInt(item) - previous.getInt(item)
            if (delta == 0 || (delta < 0 && !showLosses)) {
                continue
            }

            record(item, delta, now)
        }

        previous.clear()
        previous.putAll(current)
    }

    /**
     * Merges with a live entry for the same item rather than stacking rows, so
     * mining a vein reads `+14 Iron Ore` instead of fourteen lines.
     */
    private fun record(item: Item, delta: Int, now: Long) {
        val existing = entries.firstOrNull { it.item === item && (delta > 0) == (it.delta > 0) }
        if (existing != null) {
            existing.delta += delta
            existing.at = now
            return
        }

        entries.addLast(Entry(item, delta, now))
        while (entries.size > maxEntries) {
            entries.removeFirst()
        }
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        val now = System.currentTimeMillis()
        entries.removeAll { now - it.at > duration }

        if (entries.isEmpty()) {
            return@handler
        }

        val context = event.context
        val font = mc.font

        val lines = entries.map { entry ->
            val sign = if (entry.delta > 0) "+" else ""
            val name = ItemStack(entry.item).hoverName.string
            "$sign${entry.delta} $name" to entry
        }

        val width = lines.maxOf { font.width(it.first) }
        val height = lines.size * LINE_HEIGHT

        val bounds = getGuiScaledBounds(width.toFloat(), height.toFloat())
        val x = bounds.xMin.toInt()
        val y = bounds.yMin.toInt()

        lines.forEachIndexed { index, (text, entry) ->
            val base = if (entry.delta > 0) gainColor else lossColor

            // Fade over the last quarter of the lifetime, so entries leave
            // rather than blink out.
            val age = (now - entry.at).toDouble() / duration
            val alpha = if (age < 0.75) {
                base.a
            } else {
                (base.a * (1.0 - age) * 4.0).roundToInt().coerceIn(0, 0xFF)
            }

            context.text(
                font,
                text,
                x,
                y + index * LINE_HEIGHT,
                Color4b(base.r, base.g, base.b, alpha).argb,
            )
        }
    }

    /**
     * What the editor measures.
     *
     * A tally is empty most of the time, and the HUD editor draws its drag handle from
     * the reported size - so an idle component would be one you could not pick up. The
     * sample row is about as wide as a real one gets.
     */
    private const val SAMPLE_LINE = "+64 Cobblestone"

    private fun currentLines(): List<String> {
        val now = System.currentTimeMillis()
        return entries.filter { now - it.at <= duration }
            .map { "${if (it.delta > 0) "+" else ""}${it.delta} ${ItemStack(it.item).hoverName.string}" }
    }

    override val guiScaledWidth: Float
        get() = currentLines().ifEmpty { listOf(SAMPLE_LINE) }.maxOf { mc.font.width(it) }.toFloat()

    override val guiScaledHeight: Float
        get() = (currentLines().ifEmpty { listOf(SAMPLE_LINE) }.size * LINE_HEIGHT).toFloat()

    init {
        registerComponentListen(this)
    }

    private class Entry(val item: Item, var delta: Int, var at: Long)

}
