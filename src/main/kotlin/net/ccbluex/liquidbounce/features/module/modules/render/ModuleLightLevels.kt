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
import net.ccbluex.liquidbounce.render.drawPlane
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.LightLayer

/**
 * LightLevels
 *
 * Marks the ground where a hostile mob can spawn.
 *
 * Since 1.18 the rule is a single number - block light zero - and vanilla
 * exposes it only through the F3 screen, one block at a time, for whichever
 * block you happen to be standing on. Lighting a base is therefore a game of
 * walking every square metre and reading a debug line, which is why this is one
 * of the most-installed mods there is.
 *
 * This is world data about your own surroundings, in the same category as the
 * chunk borders module: it does not tell you where any entity is, only what the
 * blocks you can already see would allow.
 */
object ModuleLightLevels : ClientModule("LightLevels", ModuleCategories.RENDER) {

    /**
     * Since 1.18, hostile mobs spawn only at block light 0. The setting exists
     * because older servers - which this client can join through its protocol
     * translation - used a threshold of 7.
     */
    private val spawnThreshold by int("SpawnThreshold", 0, 0..15)

    /** A second, laxer band drawn in the caution colour. 0 turns it off. */
    private val cautionThreshold by int("CautionThreshold", 7, 0..15)

    /** Radius in blocks around the player, on the horizontal axes. */
    private val horizontalRange by int("HorizontalRange", 24, 4..48)

    /** How far above and below the player to scan. */
    private val verticalRange by int("VerticalRange", 4, 1..16)

    private val spawnColor by color("SpawnColor", Color4b(0xFF, 0x3B, 0x30, 0x90))
    private val cautionColor by color("CautionColor", Color4b(0xFF, 0xC1, 0x07, 0x70))

    /** Lifts the marker off the block face so it does not z-fight with it. */
    private const val SURFACE_OFFSET = 0.01

    /**
     * A hard ceiling on markers drawn per frame. A 48-block radius over 16
     * levels is 150k candidate positions; without this, turning the range up
     * is a way to stall the render thread rather than a setting.
     */
    private val maxMarkers by int("MaxMarkers", 2048, 64..8192)

    @Suppress("unused", "detekt.CognitiveComplexMethod")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val player = mc.player ?: return@handler
        val level = mc.level ?: return@handler

        val originX = player.blockX
        val originY = player.blockY
        val originZ = player.blockZ

        val floor = maxOf(originY - verticalRange, level.minY)
        val ceiling = minOf(originY + verticalRange, level.minY + level.height - 1)

        var drawn = 0
        val cursor = BlockPos.MutableBlockPos()

        event.renderEnvironment {
            for (x in -horizontalRange..horizontalRange) {
                for (z in -horizontalRange..horizontalRange) {
                    for (y in floor..ceiling) {
                        if (drawn >= maxMarkers) {
                            return@renderEnvironment
                        }

                        cursor.set(originX + x, y, originZ + z)

                        if (!isSpawnableSurface(cursor)) {
                            continue
                        }

                        val light = level.getBrightness(LightLayer.BLOCK, cursor)
                        val paint = when {
                            light <= spawnThreshold -> spawnColor
                            cautionThreshold > spawnThreshold && light <= cautionThreshold -> cautionColor
                            else -> continue
                        }

                        withPositionRelativeToCamera(
                            cursor.x.toDouble(),
                            cursor.y.toDouble() + SURFACE_OFFSET,
                            cursor.z.toDouble(),
                        ) {
                            drawPlane(1f, 1f, paint, paint)
                        }

                        drawn++
                    }
                }
            }
        }
    }

    /**
     * Somewhere a mob could actually stand: two blocks of room, on top of a
     * face solid enough to hold it.
     *
     * Without the headroom check every hole in a wall lights up, and the
     * overlay stops being readable at exactly the point it should be useful.
     */
    private fun isSpawnableSurface(pos: BlockPos): Boolean {
        val level = mc.level ?: return false

        val below = pos.below()
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            return false
        }

        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty &&
            level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty
    }

}
