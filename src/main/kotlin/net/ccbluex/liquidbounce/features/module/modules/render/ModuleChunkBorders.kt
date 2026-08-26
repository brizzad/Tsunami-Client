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

import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.drawLinesWithWidth
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3f
import net.ccbluex.liquidbounce.render.renderEnvironment

/**
 * ChunkBorders
 *
 * Draws where the chunk you are standing in begins and ends.
 *
 * Chunk boundaries decide a lot that is otherwise invisible: where mob
 * spawning caps apply, how far a farm's simulation reaches, where a redstone
 * contraption stops being loaded. Vanilla shows them behind F3+G, alongside a
 * screenful of debug text.
 *
 * Draws lines at positions computed from the player's coordinates. Reads
 * nothing that is not already on screen.
 */
object ModuleChunkBorders : ClientModule("ChunkBorders", ModuleCategories.RENDER) {

    private const val CHUNK = 16

    private val borderColor by color("BorderColor", Color4b(0x1F, 0xA8, 0xFF, 0xD0))
    private val gridColor by color("GridColor", Color4b(0xFF, 0xFF, 0xFF, 0x50))

    private val lineWidth by float("LineWidth", 2f, 0.5f..6f)

    /** How far above and below the player the vertical edges reach. */
    private val span by int("Span", 32, 4..128)

    /** Draws the 16x16 grid inside the chunk as well as its outline. */
    private val showGrid by boolean("ShowGrid", false)

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val player = mc.player ?: return@handler
        val level = mc.level ?: return@handler

        // Chunk origin: floor to a multiple of 16, which is where the boundary
        // actually falls regardless of which side of zero you are on.
        val originX = Math.floorDiv(player.blockX, CHUNK) * CHUNK
        val originZ = Math.floorDiv(player.blockZ, CHUNK) * CHUNK

        val bottom = (player.y - span).coerceAtLeast(level.minY.toDouble())
        val top = (player.y + span).coerceAtMost((level.minY + level.height).toDouble())

        event.renderEnvironment {
            val cam = camera.position()

            val points = ArrayList<Vec3f>(8)
            fun vertical(x: Int, z: Int) {
                points += Vec3f((x - cam.x).toFloat(), (bottom - cam.y).toFloat(), (z - cam.z).toFloat())
                points += Vec3f((x - cam.x).toFloat(), (top - cam.y).toFloat(), (z - cam.z).toFloat())
            }

            // The four corners of the chunk, floor to ceiling.
            vertical(originX, originZ)
            vertical(originX + CHUNK, originZ)
            vertical(originX, originZ + CHUNK)
            vertical(originX + CHUNK, originZ + CHUNK)

            drawLinesWithWidth(borderColor.argb, lineWidth, *points.toTypedArray())

            if (showGrid) {
                val grid = ArrayList<Vec3f>(128)
                val y = (player.y - cam.y).toFloat()
                for (i in 1 until CHUNK) {
                    grid += Vec3f((originX + i - cam.x).toFloat(), y, (originZ - cam.z).toFloat())
                    grid += Vec3f((originX + i - cam.x).toFloat(), y, (originZ + CHUNK - cam.z).toFloat())
                    grid += Vec3f((originX - cam.x).toFloat(), y, (originZ + i - cam.z).toFloat())
                    grid += Vec3f((originX + CHUNK - cam.x).toFloat(), y, (originZ + i - cam.z).toFloat())
                }
                drawLinesWithWidth(gridColor.argb, lineWidth * 0.5f, *grid.toTypedArray())
            }
        }
    }

}
