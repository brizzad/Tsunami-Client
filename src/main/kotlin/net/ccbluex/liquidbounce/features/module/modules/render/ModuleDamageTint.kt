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

/**
 * DamageTint
 *
 * Reddens the edges of the screen as health drops, so a low-health state is
 * visible without reading the health bar mid-fight.
 *
 * This is the readout vanilla lacks: the vanilla low-health cue is the hearts
 * shaking, which is small, in the corner of vision, and easy to miss while
 * looking at an opponent.
 *
 * Drawn as a border rather than a full-screen wash on purpose. Tinting the
 * whole screen red hides the thing you are fighting.
 */
object ModuleDamageTint : ClientModule("DamageTint", ModuleCategories.RENDER) {

    /** Health at or below which the tint starts, as a fraction of maximum. */
    private val threshold by float("Threshold", 0.5f, 0.1f..1f)

    /** Opacity when health reaches zero. Never fully opaque. */
    private val strength by int("Strength", 140, 10..220)

    private val tint by color("Color", Color4b(0xC8, 0x14, 0x14, 0xFF))

    /** How far in from each edge the fade reaches, in pixels. */
    private val depth by int("Depth", 90, 20..400)

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        val player = mc.player ?: return@handler

        // Absorption is not part of the ratio: hearts from a totem or a golden
        // apple are extra, and letting them clear the tint would hide that the
        // real health underneath is still low.
        val max = player.maxHealth
        if (max <= 0f) {
            return@handler
        }

        val ratio = (player.health / max).coerceIn(0f, 1f)
        if (ratio >= threshold) {
            return@handler
        }

        // Full strength at zero health, nothing at the threshold.
        val severity = ((threshold - ratio) / threshold).coerceIn(0f, 1f)
        val alpha = (strength * severity).toInt().coerceIn(0, 255)
        if (alpha == 0) {
            return@handler
        }

        val width = event.context.guiWidth().toFloat()
        val height = event.context.guiHeight().toFloat()
        val edge = depth.toFloat().coerceAtMost(minOf(width, height) / 2f)

        // Drawn as a handful of graduated bands rather than per-pixel strips.
        // Per-pixel would be several hundred quads every frame for a border
        // nobody looks at directly.
        val bands = 12
        val step = edge / bands

        with(event.context) {
            for (i in 0 until bands) {
                val a = (alpha * (1f - i.toFloat() / bands)).toInt().coerceIn(0, 255)
                if (a == 0) continue

                val band = Color4b(tint.r, tint.g, tint.b, a)
                val near = i * step
                val far = near + step

                drawQuad(0f, near, width, far, band)                       // top
                drawQuad(0f, height - far, width, height - near, band)     // bottom
                drawQuad(near, 0f, far, height, band)                      // left
                drawQuad(width - far, 0f, width - near, height, band)      // right
            }
        }
    }

}
