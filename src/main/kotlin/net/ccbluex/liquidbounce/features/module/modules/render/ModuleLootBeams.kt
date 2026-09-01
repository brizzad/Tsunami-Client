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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.utils.math.unaryMinus
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Rarity
import net.minecraft.world.phys.AABB

/**
 * LootBeams
 *
 * Puts a coloured beam over dropped items so they are findable in tall grass,
 * down a ravine, or wherever the mob you just killed fell.
 *
 * The item is already rendered and already in the frame - a dropped diamond is
 * a visible entity whether or not this is on. What it adds is a marker tall
 * enough to clear the terrain that is hiding a two-pixel-high sprite, which is
 * a legibility problem rather than an information one. Nothing here reads an
 * item you could not already walk up to and see.
 *
 * Colour follows the stack's own rarity by default, which is the same
 * classification the tooltip already prints in the same colours.
 */
object ModuleLootBeams : ClientModule("LootBeams", ModuleCategories.RENDER) {

    /** Beams past this are clutter, and the count is what costs frames. */
    private val range by float("Range", 48f, 8f..256f)

    private val beamWidth by float("Width", 0.2f, 0.02f..1f)
    private val beamHeight by float("Height", 3f, 0.5f..24f)

    private val colorMode by enumChoice("ColorMode", ColorMode.RARITY)

    /** Used when [ColorMode.FIXED] is picked. */
    private val fixedColor by color("Color", Color4b(0x1F, 0xA8, 0xFF, 0xFF))

    /** Fill opacity of the beam. The outline is always drawn at full alpha. */
    private val opacity by int("Opacity", 0x60, 0x10..0xFF)

    /**
     * Skip the ordinary. A world full of cobblestone drops is exactly the case
     * where a beam over every one of them is worse than none.
     */
    private val minimumRarity by enumChoice("MinimumRarity", RarityFilter.ANY)

    /**
     * Beams shrink with distance in screen space anyway; this fades them out so
     * a far-off field of drops does not read as a wall of colour.
     */
    private val fadeWithDistance by boolean("FadeWithDistance", true)

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val player = mc.player ?: return@handler
        val level = mc.level ?: return@handler

        val rangeSq = (range * range).toDouble()
        val eye = player.eyePosition

        event.renderEnvironment {
            val cam = camera.position()
            val halfWidth = beamWidth / 2.0

            // Filtered as a sequence rather than with a loop full of `continue`s: the
            // sequence is lazy, so it still walks the entity list once.
            val candidates = level.entitiesForRendering()
                .asSequence()
                .filterIsInstance<ItemEntity>()
                .filter { it.item.isEmpty.not() && minimumRarity.accepts(it.item.rarity) }
                .map { it to it.position().distanceToSqr(eye) }
                .filter { (_, distanceSq) -> distanceSq <= rangeSq }

            for ((entity, distanceSq) in candidates) {
                val stack = entity.item

                val base = when (colorMode) {
                    ColorMode.RARITY -> rarityColor(stack.rarity)
                    ColorMode.FIXED -> fixedColor
                }

                // Alpha is squared-distance based so the falloff matches how
                // quickly the beam shrinks on screen rather than being linear.
                val fade = if (fadeWithDistance) {
                    (1.0 - distanceSq / rangeSq).coerceIn(0.0, 1.0)
                } else {
                    1.0
                }

                val fillAlpha = (opacity * fade).toInt().coerceIn(0, 0xFF)
                if (fillAlpha == 0) {
                    continue
                }

                val pos = entity.position()
                val box = AABB(
                    pos.x - halfWidth, pos.y, pos.z - halfWidth,
                    pos.x + halfWidth, pos.y + beamHeight, pos.z + halfWidth,
                )

                drawBox(
                    box.move(-cam),
                    Color4b(base.r, base.g, base.b, fillAlpha),
                    Color4b(base.r, base.g, base.b, (0xFF * fade).toInt().coerceIn(0, 0xFF)),
                )
            }
        }
    }

    /**
     * The colours vanilla already prints the item's name in, so a beam and its
     * tooltip agree.
     */
    private fun rarityColor(rarity: Rarity): Color4b = when (rarity) {
        Rarity.COMMON -> Color4b(0xFF, 0xFF, 0xFF, 0xFF)
        Rarity.UNCOMMON -> Color4b(0xFF, 0xFF, 0x55, 0xFF)
        Rarity.RARE -> Color4b(0x55, 0xFF, 0xFF, 0xFF)
        Rarity.EPIC -> Color4b(0xFF, 0x55, 0xFF, 0xFF)
    }

    @Suppress("unused")
    enum class ColorMode(override val tag: String) : Tagged {
        RARITY("Rarity"),
        FIXED("Fixed")
    }

    @Suppress("unused")
    enum class RarityFilter(override val tag: String, private val floor: Rarity?) : Tagged {
        ANY("Any", null),
        UNCOMMON("Uncommon", Rarity.UNCOMMON),
        RARE("Rare", Rarity.RARE),
        EPIC("Epic", Rarity.EPIC);

        fun accepts(rarity: Rarity): Boolean = floor == null || rarity.ordinal >= floor.ordinal
    }

}
