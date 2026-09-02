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
 *
 * ## Was a module until 2026-09-02
 *
 * It drew at a corner chosen from an `Anchor` enum plus `OffsetX`/`OffsetY`
 * spinners, which is a positioning system nobody wants to use twice. As a
 * [NativeHudComponent] it is dragged in the HUD editor like every other element,
 * and its bind survived the move because [HudComponent] gained one in the same
 * commit.
 */
object PackDisplayHudComponent : NativeHudComponent(
    "PackDisplay",
    enabled = false,
    alignment = Alignment(
        horizontalAlignment = Alignment.ScreenAxisX.RIGHT,
        horizontalOffset = 4,
        verticalAlignment = Alignment.ScreenAxisY.BOTTOM,
        verticalOffset = 4,
    ),
    description = "Names the resource packs you have applied.",
) {

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

    /**
     * The rows as they will be drawn.
     *
     * Read by the size getters as well as by the renderer, because the editor needs a
     * box to drag before there is a frame to draw: a component whose reported size
     * disagreed with what it paints would be grabbable in the wrong place.
     */
    private fun visibleNames(): List<String> = mc.resourcePackRepository.selectedPacks
        .filter { includeBuiltIn || it.id !in BUILT_IN }
        .map { it.title.string }
        .take(maxEntries)

    /**
     * What the editor measures.
     *
     * Falls back to one sample row when no pack is applied, because the HUD editor
     * draws its drag handle from the reported width and height - and a component that
     * reports nothing is a component you cannot pick up. Arranging your HUD is exactly
     * the moment you are least likely to have a resource pack on, so returning zero
     * here would make this element unpositionable whenever anyone wanted to position it.
     */
    private fun sizingNames(): List<String> = visibleNames().ifEmpty { listOf("Resource pack") }

    override val guiScaledWidth: Float
        get() = (sizingNames().maxOf { mc.font.width(it) } + PADDING * 2).toFloat()

    override val guiScaledHeight: Float
        get() = (sizingNames().size * LINE_HEIGHT + PADDING * 2).toFloat()

    init {
        registerComponentListen(this)
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        val names = visibleNames()
        if (names.isEmpty()) {
            return@handler
        }

        val font = mc.font
        val width = (names.maxOf { font.width(it) } + PADDING * 2).toFloat()
        val height = (names.size * LINE_HEIGHT + PADDING * 2).toFloat()
        val bounds = getGuiScaledBounds(width, height)

        val context = event.context
        val x = bounds.xMin.toInt()
        val y = bounds.yMin.toInt()

        if (!backgroundColor.isTransparent) {
            context.fill(x, y, x + width.toInt(), y + height.toInt(), backgroundColor.argb)
        }

        names.forEachIndexed { index, name ->
            context.text(font, name, x + PADDING, y + PADDING + index * LINE_HEIGHT, textColor.argb)
        }
    }

}
