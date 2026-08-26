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

import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.drawQuad
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.item.foodComponent

/**
 * FoodInfo
 *
 * Shows saturation, which the game tracks and never displays.
 *
 * Saturation is what actually decides whether the hunger bar moves: it drains
 * first and only then does hunger start to fall. Two players on the same
 * number of drumsticks can be in completely different states, and vanilla
 * gives no way to tell them apart. Eating at the wrong moment wastes the food.
 *
 * Purely informational, in the spirit of AppleSkin. It reads values the client
 * already has and draws them. Nothing about hunger, saturation, exhaustion or
 * eating is changed.
 */
object ModuleFoodInfo : ClientModule("FoodInfo", ModuleCategories.RENDER) {

    /** Current saturation, drawn as a slim bar over the hunger bar. */
    private val showSaturation by boolean("Saturation", true)

    /** What the held food would restore, drawn as a ghost ahead of the bar. */
    private val showHeldFood by boolean("HeldFoodPreview", true)

    private val saturationColor by color("SaturationColor", Color4b(0xF5, 0xC9, 0x42, 0xE0))
    private val previewColor by color("PreviewColor", Color4b(0xFF, 0xFF, 0xFF, 0x70))

    /**
     * Vanilla hunger bar geometry, in scaled pixels. The bar sits right of
     * centre, ten icons of nine pixels each, above the hotbar.
     */
    private const val ICONS = 10
    private const val ICON_WIDTH = 9
    private const val BAR_WIDTH = ICONS * ICON_WIDTH
    private const val BAR_BOTTOM_OFFSET = 39
    private const val MAX_FOOD = 20f

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        val player = mc.player ?: return@handler

        // The hunger bar is hidden in creative and spectator, so anything drawn
        // over it would float against nothing.
        if (player.isCreative || player.isSpectator) {
            return@handler
        }

        val food = player.foodData
        val guiWidth = event.context.guiWidth().toFloat()
        val guiHeight = event.context.guiHeight().toFloat()

        val right = guiWidth / 2f + 91f
        val left = right - BAR_WIDTH
        val top = guiHeight - BAR_BOTTOM_OFFSET

        with(event.context) {
            if (showSaturation) {
                val fraction = (food.saturationLevel / MAX_FOOD).coerceIn(0f, 1f)
                if (fraction > 0f) {
                    drawQuad(left, top, left + BAR_WIDTH * fraction, top + 1.5f, saturationColor)
                }
            }

            if (showHeldFood) {
                val heldFood = player.mainHandItem.foodComponent ?: player.offhandItem.foodComponent
                if (heldFood != null) {
                    // Where hunger would reach after eating, capped at full, so
                    // the ghost never runs past the end of the bar.
                    val current = food.foodLevel.toFloat()
                    val after = (current + heldFood.nutrition).coerceAtMost(MAX_FOOD)
                    if (after > current) {
                        val from = left + BAR_WIDTH * (current / MAX_FOOD)
                        val to = left + BAR_WIDTH * (after / MAX_FOOD)
                        drawQuad(from, top - 2f, to, top - 0.5f, previewColor)
                    }
                }
            }
        }
    }

}
