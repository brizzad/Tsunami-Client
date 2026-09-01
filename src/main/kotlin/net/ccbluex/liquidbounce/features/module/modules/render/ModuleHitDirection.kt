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
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * HitDirection
 *
 * Draws a short arc around the crosshair pointing at whatever just hit you, for
 * as long as it takes to turn.
 *
 * ## Why this is not a radar
 *
 * The only thing it can point at is the source of damage you have already
 * taken, and only for a second or two after the fact. That is information the
 * game already gave you - vanilla ships exactly this angle as
 * `LivingEntity.getHurtDir` and uses it to tilt the hurt camera, which is a
 * cruder version of the same signal. Nothing here reveals a player who has not
 * hit you, and it goes quiet the moment the fight does.
 *
 * A directional indicator for damage already taken is standard in this
 * category; a directional indicator for players who have not attacked is not,
 * and is not what this is.
 */
object ModuleHitDirection : ClientModule("HitDirection", ModuleCategories.RENDER) {

    /** How long an indicator stays up after the hit, in milliseconds. */
    private val duration by int("Duration", 1500, 250..5000)

    /** Distance from the crosshair to the arc, in scaled pixels. */
    private val radius by int("Radius", 30, 10..120)

    /** Half-width of the arc, in degrees. */
    private val spread by int("Spread", 14, 4..60)

    /** Thickness of the arc, in scaled pixels. */
    private val thickness by int("Thickness", 3, 1..10)

    private val indicatorColor by color("Color", Color4b(0xFF, 0x3B, 0x30, 0xFF))

    /** Fade the arc out over its lifetime instead of dropping it abruptly. */
    private val fade by boolean("Fade", true)

    /**
     * The most recent hits, newest last. Several are kept because being hit by
     * two people at once is the case where one arrow is least useful.
     */
    private val hits = ArrayDeque<Hit>()

    private const val MAX_TRACKED = 4

    /** Segments per arc. Enough that the curve does not read as a polygon. */
    private const val SEGMENTS = 12

    private var lastHurtTime = 0

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        hits.clear()
        lastHurtTime = 0
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        val player = mc.player ?: return@handler

        // hurtTime counts down from hurtDuration, so a fresh hit is a rise. Polling
        // it here rather than hooking the damage path keeps this to one read of a
        // public field the game already maintains for the hurt camera.
        if (player.hurtTime > lastHurtTime) {
            record(player.getHurtDir())
        }
        lastHurtTime = player.hurtTime

        val now = System.currentTimeMillis()
        hits.removeAll { now - it.at > duration }

        if (hits.isEmpty()) {
            return@handler
        }

        val context = event.context
        val centreX = context.guiWidth() / 2
        val centreY = context.guiHeight() / 2

        // Where the player is facing now, so the arc tracks as they turn.
        val viewYaw = player.yRot

        for (hit in hits) {
            val age = (now - hit.at).toDouble() / duration
            val alpha = if (fade) {
                (indicatorColor.a * (1.0 - age)).roundToInt().coerceIn(0, 0xFF)
            } else {
                indicatorColor.a
            }

            if (alpha == 0) {
                continue
            }

            val argb = Color4b(indicatorColor.r, indicatorColor.g, indicatorColor.b, alpha).argb

            // Screen up is the direction the player faces, so the offset between
            // the recorded attack angle and the current view is the arc's bearing.
            val bearing = Math.toRadians((hit.yaw - viewYaw).toDouble())

            for (step in 0..SEGMENTS) {
                val offset = Math.toRadians(
                    (-spread + step * (2.0 * spread / SEGMENTS)).toDouble()
                )
                val angle = bearing + offset

                val px = centreX + (sin(angle) * radius).roundToInt()
                val py = centreY - (cos(angle) * radius).roundToInt()

                context.fill(px, py, px + thickness, py + thickness, argb)
            }
        }
    }

    private fun record(yaw: Float) {
        hits.addLast(Hit(yaw, System.currentTimeMillis()))
        while (hits.size > MAX_TRACKED) {
            hits.removeFirst()
        }
    }

    /**
     * @param yaw the world-space angle the damage came from, as the game reported it
     */
    private data class Hit(val yaw: Float, val at: Long)

}
