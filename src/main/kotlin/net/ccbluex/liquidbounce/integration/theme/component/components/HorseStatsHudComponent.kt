/*
 * This file is part of Tsunami (https://github.com/brizzad/Tsunami-Client)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.animal.equine.AbstractHorse
import net.minecraft.world.phys.EntityHitResult
import java.util.Locale

/**
 * HorseStats
 *
 * Shows the speed, jump height and health of the horse you are riding, or of
 * the one you are looking at.
 *
 * Every horse rolls its own speed, jump and health, the spread between a bad
 * one and a good one is roughly threefold, and the game shows you none of it.
 * The information is already on the client - it arrives as entity attributes,
 * which is how the horse moves at the speed it does - so this is a matter of
 * printing a number the client was already given rather than deriving one.
 *
 * Jump height is converted out of the raw attribute into blocks, because
 * "0.7143" means nothing and "5.2 blocks" is the number you would compare
 * against a fence.
 *
 * ## Was a module until 2026-09-02
 *
 * Positioned by an `Anchor` enum and two offset spinners; now dragged in the HUD
 * editor like everything else. Its bind came with it - [HudComponent] gained one
 * in the same commit, so nothing was lost in the move.
 */
object HorseStatsHudComponent : NativeHudComponent(
    "HorseStats",
    enabled = false,
    alignment = Alignment(
        horizontalAlignment = Alignment.ScreenAxisX.LEFT,
        horizontalOffset = 4,
        verticalAlignment = Alignment.ScreenAxisY.TOP,
        verticalOffset = 40,
    ),
    description = "Speed, jump height and health of the horse you are on or looking at.",
) {

    /** Also read the horse under the crosshair, not only the one you are on. */
    private val whileLookingAt by boolean("WhileLookingAt", true)

    private val textColor by color("TextColor", Color4b(0xFF, 0xFF, 0xFF, 0xFF))
    private val backgroundColor by color("Background", Color4b(0x00, 0x00, 0x00, 0x80))

    private const val LINE_HEIGHT = 10
    private const val PADDING = 3

    /**
     * What the box looks like with a horse in front of you.
     *
     * The editor sizes its drag handle from the reported width and height, and there
     * is usually no horse while you are arranging a HUD - so the idle size is this
     * rather than nothing. The digits are the widest the real rows get, so the handle
     * does not jump when a horse does turn up.
     */
    private val SAMPLE_LINES = listOf(
        "Speed  00.00 b/s",
        "Jump   00.00 blocks",
        "Health 00.00 / 00.00",
    )

    private fun lines(): List<String> {
        val horse = targetHorse() ?: return emptyList()
        return listOf(
            "Speed  ${format(blocksPerSecond(horse))} b/s",
            "Jump   ${format(jumpBlocks(horse))} blocks",
            "Health ${format(horse.health.toDouble())} / ${format(horse.maxHealth.toDouble())}",
        )
    }

    private fun sizingLines(): List<String> = lines().ifEmpty { SAMPLE_LINES }

    override val guiScaledWidth: Float
        get() = (sizingLines().maxOf { mc.font.width(it) } + PADDING * 2).toFloat()

    override val guiScaledHeight: Float
        get() = (sizingLines().size * LINE_HEIGHT + PADDING * 2).toFloat()

    init {
        registerComponentListen(this)
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        val lines = lines()
        if (lines.isEmpty()) {
            return@handler
        }

        val font = mc.font
        val width = (lines.maxOf { font.width(it) } + PADDING * 2).toFloat()
        val height = (lines.size * LINE_HEIGHT + PADDING * 2).toFloat()
        val bounds = getGuiScaledBounds(width, height)

        val context = event.context
        val x = bounds.xMin.toInt()
        val y = bounds.yMin.toInt()

        if (!backgroundColor.isTransparent) {
            context.fill(x, y, x + width.toInt(), y + height.toInt(), backgroundColor.argb)
        }

        lines.forEachIndexed { index, line ->
            context.text(font, line, x + PADDING, y + PADDING + index * LINE_HEIGHT, textColor.argb)
        }
    }

    private fun targetHorse(): AbstractHorse? {
        (mc.player?.vehicle as? AbstractHorse)?.let { return it }

        if (!whileLookingAt) {
            return null
        }

        return (mc.hitResult as? EntityHitResult)?.entity as? AbstractHorse
    }

    /**
     * The movement attribute is in blocks per tick; twenty ticks make a second.
     * Quoting b/s puts it on the same scale as the speedometer, so a horse can
     * be compared against walking (4.3) and sprinting (5.6) without arithmetic.
     */
    private fun blocksPerSecond(horse: AbstractHorse): Double =
        horse.getAttributeValue(Attributes.MOVEMENT_SPEED) * 20.0

    /**
     * Peak height of a jump, in blocks, from the jump-strength attribute.
     *
     * Derived the way the game itself integrates the jump: initial velocity is
     * the attribute value, gravity subtracts 0.08 a tick and 2% drag applies,
     * which the closed form below approximates closely enough to compare two
     * horses. Vanilla's own horse jump ranges about 1.1 to 5.3 blocks.
     */
    private fun jumpBlocks(horse: AbstractHorse): Double {
        val strength = horse.getAttributeValue(Attributes.JUMP_STRENGTH)
        if (strength <= 0.0) {
            return 0.0
        }

        // Fitted against vanilla's tick integration; the constants are the ones
        // the community's horse calculators have used since 1.9.
        return -0.1817584952 * strength * strength * strength +
            3.689713992 * strength * strength +
            2.128599134 * strength -
            0.343930367
    }

    private fun format(value: Double): String = String.format(Locale.ROOT, "%.2f", value)

}
