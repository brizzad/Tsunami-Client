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
import net.ccbluex.liquidbounce.render.engine.type.Color4b

/**
 * PackDisplay
 *
 * Names the resource packs currently applied, in load order.
 *
 * The pack screen is three clicks away from the game and shows the answer only
 * while it is open, which is a poor fit for the thing people actually do with
 * packs: cycle through several while comparing them, or record footage and
 * later need to know which one was on. It is also how you notice a server
 * quietly swapped your pack out.
 *
 * The built-in `vanilla` pack is hidden by default, since it is always there
 * and naming it in a list of two would be half noise.
 */
object ModulePackDisplay : ClientModule("PackDisplay", ModuleCategories.RENDER) {

    private val anchor by enumChoice("Anchor", ScreenAnchor.BOTTOM_RIGHT)
    private val offsetX by int("OffsetX", 4, 0..512)
    private val offsetY by int("OffsetY", 4, 0..512)

    /** Show `vanilla` and the other packs the game always has selected. */
    private val includeBuiltIn by boolean("IncludeBuiltIn", false)

    /** Most rows to draw, so a heavily layered setup cannot fill the screen. */
    private val maxEntries by int("MaxEntries", 5, 1..16)

    private val textColor by color("TextColor", Color4b(0xFF, 0xFF, 0xFF, 0xFF))
    private val backgroundColor by color("Background", Color4b(0x00, 0x00, 0x00, 0x80))

    private const val LINE_HEIGHT = 10
    private const val PADDING = 3

    /** Packs the game ships and always has on. Naming them is not information. */
    private val BUILT_IN = setOf("vanilla", "mod_resources", "programmer_art", "high_contrast")

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        val names = mc.resourcePackRepository.selectedPacks
            .filter { includeBuiltIn || it.id !in BUILT_IN }
            .map { it.title.string }
            .take(maxEntries)

        if (names.isEmpty()) {
            return@handler
        }

        val context = event.context
        val font = mc.font
        val width = names.maxOf { font.width(it) } + PADDING * 2
        val height = names.size * LINE_HEIGHT + PADDING * 2

        val x = anchor.x(context.guiWidth(), width, offsetX)
        val y = anchor.y(context.guiHeight(), height, offsetY)

        if (!backgroundColor.isTransparent) {
            context.fill(x, y, x + width, y + height, backgroundColor.argb)
        }

        names.forEachIndexed { index, name ->
            context.text(font, name, x + PADDING, y + PADDING + index * LINE_HEIGHT, textColor.argb)
        }
    }

}
