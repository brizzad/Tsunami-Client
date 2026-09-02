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

import net.ccbluex.liquidbounce.utils.client.isOlderThan1_9
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.ccbluex.liquidbounce.utils.item.getCooldown
import net.minecraft.world.item.ItemStack

/**
 * Cooldowns
 *
 * Lists every item currently on cooldown, with the time left on each.
 *
 * Vanilla already darkens a hotbar slot while its item is cooling down, which
 * answers "can I use this" and nothing else. In a fight the question is "how
 * long until I can", and the difference between an ender pearl coming back in
 * 0.3s and 0.9s decides whether you commit. This puts a number on it, and
 * covers items that are not in your hotbar at all.
 *
 * ## Why this is written here rather than merged
 *
 * Lunar Client ships this as one of its headline mods and there is no Fabric
 * equivalent with a 26.2 build - checked against Modrinth, which returns
 * nothing for a cooldown HUD on this version. It was cheap to write because
 * the hard part already existed: the fork carries a `MixinItemCooldowns` that
 * exposes the start, end and current tick of any cooldown, so this module is
 * only selection and drawing.
 *
 * Reads state the game already gave you and draws it. It cannot start, stop or
 * shorten a cooldown, and does not touch the server's copy of one.
 */
object CooldownsHudComponent : NativeHudComponent(
    "Cooldowns",
    enabled = false,
    alignment = Alignment(
        horizontalAlignment = Alignment.ScreenAxisX.LEFT,
        horizontalOffset = 4,
        verticalAlignment = Alignment.ScreenAxisY.TOP,
        verticalOffset = 110,
    ),
    description = "Item cooldowns, with a bar draining as each one runs down.",
) {

    /**
     * Nothing has an item cooldown before 1.9 - no ender pearl throw, no chorus fruit, no
     * shield. The readout would be a permanently empty list rather than a wrong one.
     */
    override val inapplicableOnProtocol: String?
        get() = if (isOlderThan1_9) "Item cooldowns do not exist below 1.9" else null


    /** Row height. Also the icon size, so the two stay aligned. */
    private const val ROW_HEIGHT = 18

    /**
     * Search the whole inventory, not just the hotbar.
     *
     * A pearl in your inventory is one hotbar swap away, so its cooldown is
     * still worth knowing. Off means only what you could throw right now.
     */
    private val wholeInventory by boolean("WholeInventory", true)

    /** Draw a bar draining alongside the number. */
    private val showBar by boolean("ShowBar", true)

    private val barColor by color("BarColor", Color4b(0x1F, 0xA8, 0xFF, 0xFF))
    private val textColor by color("TextColor", Color4b(0xFF, 0xFF, 0xFF, 0xFF))

    /**
     * Hide anything shorter than this, in tenths of a second.
     *
     * Most cooldowns in the game are a tick or two long and flicker on screen
     * for less time than it takes to read them. The ones worth showing are the
     * ones you would wait for.
     */
    private val minDuration by int("MinDuration", 5, 0..100)

    /** Most rows to draw, so a full inventory cannot fill the screen. */
    private val maxEntries by int("MaxEntries", 6, 1..12)

    override val guiScaledWidth: Float
        get() = ROW_WIDTH.toFloat()

    /**
     * One row per live cooldown, and one when there are none - the editor draws its
     * drag handle from this, and you arrange a HUD standing still with nothing on
     * cooldown.
     */
    override val guiScaledHeight: Float
        get() {
            val player = mc.player ?: return ROW_HEIGHT.toFloat()
            val rows = collect(player.inventory.nonEquipmentItems + player.offhandItem).size
            return (rows.coerceAtLeast(1) * ROW_HEIGHT).toFloat()
        }

    init {
        registerComponentListen(this)
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        val player = mc.player ?: return@handler
        val cooldowns = collect(player.inventory.nonEquipmentItems + player.offhandItem)

        if (cooldowns.isEmpty()) {
            return@handler
        }

        val context = event.context
        val rows = cooldowns.size

        val bounds = getGuiScaledBounds(ROW_WIDTH.toFloat(), (rows * ROW_HEIGHT).toFloat())
        val x = bounds.xMin.toInt()
        val top = bounds.yMin.toInt()

        cooldowns.forEachIndexed { index, entry ->
            val y = top + index * ROW_HEIGHT

            if (showBar) {
                // Background first, so the draining bar reads against it even
                // over bright terrain.
                context.fill(x, y, x + ROW_WIDTH, y + ROW_HEIGHT - 2, BAR_BACKGROUND.argb)
                context.fill(
                    x, y,
                    x + (ROW_WIDTH * entry.remainingFraction).toInt(), y + ROW_HEIGHT - 2,
                    barColor.argb,
                )
            }

            context.text(
                mc.font,
                entry.label,
                x + TEXT_INSET,
                y + 4,
                textColor.argb,
            )
        }
    }

    /**
     * One row per cooldown group, not per stack.
     *
     * Every ender pearl in the inventory shares a cooldown, so without the
     * dedupe a stack of sixteen would be sixteen identical rows.
     */
    private fun collect(stacks: List<ItemStack>): List<Entry> {
        val player = mc.player ?: return emptyList()
        val manager = player.cooldowns
        val seen = HashSet<Pair<Int, Int>>()

        val candidates = if (wholeInventory) stacks else stacks.take(9) + player.offhandItem

        // A lazy chain rather than a loop of early exits: the sequence stops
        // pulling as soon as `take` has enough, so it still walks no further
        // than the old `break` did.
        val result = candidates.asSequence()
            .filter { !it.isEmpty }
            .mapNotNull { stack -> manager.getCooldown(stack)?.let { stack to it } }
            .mapNotNull { (stack, cooldown) ->
                val total = cooldown.endTick() - cooldown.startTick()
                val remaining = cooldown.endTick() - cooldown.currentTick()

                if (total <= 0 || remaining <= 0 || remaining < minDuration * 2) {
                    return@mapNotNull null
                }

                // Items sharing a cooldown group share its exact window, so the
                // start/end pair identifies the group without needing the
                // group id, which vanilla keeps to itself.
                if (!seen.add(cooldown.startTick() to cooldown.endTick())) {
                    return@mapNotNull null
                }

                Entry(
                    label = "%s  %.1fs".format(stack.hoverName.string, remaining / 20.0f),
                    remainingFraction = remaining.toFloat() / total.toFloat(),
                    remaining = remaining,
                )
            }
            .take(maxEntries)
            .toList()

        // Soonest first: the one about to come back is the one being waited on.
        return result.sortedBy { it.remaining }
    }


    /** Left inset for the label, so the bar stays readable underneath it. */
    private const val TEXT_INSET = 6
    private const val ROW_WIDTH = 96
    private val BAR_BACKGROUND = Color4b(0x00, 0x00, 0x00, 0x90)

    private data class Entry(
        val label: String,
        val remainingFraction: Float,
        val remaining: Int,
    )
}
