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
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * ShieldStatus
 *
 * Shows whether the shield is up, ready, or disabled, and how much of the
 * disable is left to run.
 *
 * An axe hit disables a shield for a few seconds. Vanilla communicates this
 * only by the shield icon greying slightly in the hotbar, which is unreadable
 * mid-fight, so the usual way to discover it is to raise a shield that is not
 * there and take the hit.
 *
 * Reads state the client already has. It does not change blocking, cooldowns,
 * or anything sent to the server.
 */
object ModuleShieldStatus : ClientModule("ShieldStatus", ModuleCategories.RENDER) {

    private val width by int("Width", 80, 20..300)
    private val height by int("Height", 5, 1..20)

    /** Distance above the hotbar, in scaled pixels. */
    private val offset by int("Offset", 58, 0..200)

    private val readyColor by color("ReadyColor", Color4b(0x33, 0xCC, 0x66, 0xC0))
    private val blockingColor by color("BlockingColor", Color4b(0x1F, 0xA8, 0xFF, 0xE0))
    private val disabledColor by color("DisabledColor", Color4b(0xE0, 0x3A, 0x3A, 0xE0))

    /** Hide the bar entirely when no shield is carried. */
    private val onlyWithShield by boolean("OnlyWithShield", true)

    private fun shieldStack(): ItemStack? {
        val player = mc.player ?: return null
        return sequenceOf(player.offhandItem, player.mainHandItem)
            .firstOrNull { it.`is`(Items.SHIELD) }
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        val player = mc.player ?: return@handler
        val stack = shieldStack()

        if (stack == null && onlyWithShield) {
            return@handler
        }

        val cooldowns = player.cooldowns
        val remaining = stack?.let { cooldowns.getCooldownPercent(it, event.tickDelta) } ?: 0f
        val disabled = stack != null && cooldowns.isOnCooldown(stack)

        val color = when {
            disabled -> disabledColor
            player.isBlocking -> blockingColor
            else -> readyColor
        }

        val guiWidth = event.context.guiWidth().toFloat()
        val guiHeight = event.context.guiHeight().toFloat()
        val x = (guiWidth - width) / 2f
        val y = guiHeight - offset

        with(event.context) {
            // Track behind the bar, so a nearly finished cooldown still reads as
            // "a shield exists and it is almost back" rather than as nothing.
            drawQuad(x - 1f, y - 1f, x + width + 1f, y + height + 1f, Color4b(0, 0, 0, 0x80))

            // While disabled the fill drains as the cooldown runs down; the
            // remaining fraction is the part still to wait for.
            val filled = if (disabled) width * remaining else width.toFloat()
            if (filled > 0f) {
                drawQuad(x, y.toFloat(), x + filled, (y + height).toFloat(), color)
            }
        }
    }

}
