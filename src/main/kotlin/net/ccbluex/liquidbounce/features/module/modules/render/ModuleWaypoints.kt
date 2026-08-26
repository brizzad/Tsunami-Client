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
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.WaypointManager
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.utils.math.unaryMinus
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.ccbluex.liquidbounce.utils.text.textOf
import net.minecraft.ChatFormatting
import net.minecraft.world.phys.AABB
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Waypoints
 *
 * Marks saved places with a beam you can see from a distance, and lists them
 * by how far away they are.
 *
 * Managed with `.waypoint add|remove|list|clear`. Storage is a plain JSON file
 * in the client folder, so a list collected over months survives a settings
 * reset. See [WaypointManager].
 *
 * The name and distance are drawn as a flat list rather than floating over the
 * beam, because there is no world-to-screen projection helper in the render
 * code and inventing one for a label is not worth the surface it adds. The
 * beam says where, the list says which and how far.
 */
object ModuleWaypoints : ClientModule("Waypoints", ModuleCategories.RENDER) {

    /** Beams past this are noise; the list still names them. */
    private val beamRange by float("BeamRange", 256f, 16f..2048f)

    private val beamWidth by float("BeamWidth", 0.35f, 0.05f..2f)

    /** How far the beam reaches above and below the marked block. */
    private val beamHeight by int("BeamHeight", 48, 4..320)

    private val showList by boolean("ShowList", true)
    private val listX by int("ListX", 6, -600..600)
    private val listY by int("ListY", 120, -300..600)

    private val fontRenderer
        get() = FontManager.FONT_RENDERER

    private fun currentDimension(): String? = mc.level?.dimension()?.identifier()?.toString()

    @Suppress("unused")
    private val worldHandler = handler<WorldRenderEvent> { event ->
        val player = mc.player ?: return@handler
        val dimension = currentDimension() ?: return@handler
        val here = WaypointManager.inDimension(dimension)
        if (here.isEmpty()) {
            return@handler
        }

        val rangeSq = (beamRange * beamRange).toDouble()

        event.renderEnvironment {
            val cam = camera.position()

            for (waypoint in here) {
                val dx = waypoint.x + 0.5 - player.x
                val dz = waypoint.z + 0.5 - player.z
                if (dx * dx + dz * dz > rangeSq) {
                    continue
                }

                val half = beamWidth / 2.0
                val box = AABB(
                    waypoint.x + 0.5 - half, (waypoint.y - beamHeight).toDouble(), waypoint.z + 0.5 - half,
                    waypoint.x + 0.5 + half, (waypoint.y + beamHeight).toDouble(), waypoint.z + 0.5 + half
                )

                val color = Color4b(waypoint.color)
                drawBox(box.move(-cam), Color4b(color.r, color.g, color.b, 0x40), color)
            }
        }
    }

    @Suppress("unused")
    private val overlayHandler = handler<OverlayRenderEvent> { event ->
        if (!showList) {
            return@handler
        }

        val player = mc.player ?: return@handler
        val dimension = currentDimension() ?: return@handler

        val sorted = WaypointManager.inDimension(dimension)
            .map { it to sqrt(distanceSq(it.x + 0.5, it.y.toDouble(), it.z + 0.5, player.x, player.y, player.z)) }
            .sortedBy { it.second }

        if (sorted.isEmpty()) {
            return@handler
        }

        with(event.context) {
            val vanillaScale = fontRenderer.scaleToVanillaFont

            sorted.forEachIndexed { index, (waypoint, distance) ->
                val line = textOf(
                    waypoint.name.asPlainText(ChatFormatting.WHITE),
                    "  ${distance.roundToInt()}m".asPlainText(ChatFormatting.GRAY)
                )

                fontRenderer.draw(line) {
                    x = listX.toFloat()
                    y = listY + index * 11f
                    shadow = true
                    scale = vanillaScale
                }
            }
        }
    }

    private fun distanceSq(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Double {
        val dx = x1 - x2
        val dy = y1 - y2
        val dz = z1 - z2
        return dx * dx + dy * dy + dz * dz
    }

}
