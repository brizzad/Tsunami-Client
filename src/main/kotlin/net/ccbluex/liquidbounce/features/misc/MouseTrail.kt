/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.misc

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.MouseRotationEvent
import net.ccbluex.liquidbounce.event.events.MouseTrailEvent
import net.ccbluex.liquidbounce.event.handler

/**
 * Feeds mouse motion to the HUD for the keystrokes mouse tracker.
 *
 * [MouseRotationEvent] fires once per mouse movement, which is far too often to put on the
 * interop socket - every other streamed event is a discrete state change, not a continuous
 * signal. Deltas are accumulated and flushed at [INTERVAL_MS] instead, so the socket sees a
 * bounded rate no matter how fast the mouse is polled.
 *
 * Nothing is sent while the player is not turning: the flush happens inside the rotation
 * handler, so a still mouse costs exactly zero messages rather than a heartbeat of zeroes.
 */
object MouseTrail : EventListener {

    /** ~30Hz. Fast enough to read as motion, slow enough to stay negligible on the socket. */
    private const val INTERVAL_MS = 33L

    private var accumulatedX = 0.0
    private var accumulatedY = 0.0
    private var lastFlush = 0L

    @Suppress("unused")
    private val rotationHandler = handler<MouseRotationEvent> { event ->
        accumulatedX += event.cursorDeltaX
        accumulatedY += event.cursorDeltaY

        val now = System.currentTimeMillis()
        if (now - lastFlush < INTERVAL_MS) {
            return@handler
        }

        lastFlush = now
        EventManager.callEvent(MouseTrailEvent(accumulatedX, accumulatedY))
        accumulatedX = 0.0
        accumulatedY = 0.0
    }
}
