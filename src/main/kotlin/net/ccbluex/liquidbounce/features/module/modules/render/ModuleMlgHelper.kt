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
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.ccbluex.liquidbounce.utils.math.unaryMinus
import net.minecraft.world.item.Items
import net.minecraft.world.phys.AABB

/**
 * MlgHelper
 *
 * While falling, marks where you are going to land and whether the landing
 * will hurt. Judging that mid-air is the hard part of an MLG, and the vanilla
 * screen gives you nothing to judge it with.
 *
 * It shows you the landing spot. It does not place anything, does not aim,
 * does not swap your hotbar, and does not act on your behalf in any way. The
 * save is still yours to make and still yours to miss - automating the
 * placement would be automating the whole skill, which is the line this
 * client does not cross.
 */
object ModuleMlgHelper : ClientModule("MlgHelper", ModuleCategories.RENDER) {

    /** Ticks of fall to simulate ahead. About four seconds. */
    private const val LOOKAHEAD_TICKS = 80

    /** Below this much predicted damage the landing is not worth marking. */
    private val minimumDamage by float("MinimumDamage", 3f, 0f..20f)

    private val safeColor by color("SafeColor", Color4b(0x33, 0xCC, 0x66, 0xA0))
    private val hurtColor by color("HurtColor", Color4b(0xFF, 0xC1, 0x07, 0xC0))
    private val fatalColor by color("FatalColor", Color4b(0xE0, 0x3A, 0x3A, 0xD0))

    /** Marks the landing green when something that saves you is in hand. */
    private val checkHeldItem by boolean("CheckHeldItem", true)

    private val SAVING_ITEMS = setOf(Items.WATER_BUCKET, Items.COBWEB, Items.HAY_BLOCK, Items.SLIME_BLOCK)

    private fun holdingSave(): Boolean {
        val player = mc.player ?: return false
        return player.inventory.nonEquipmentItems.any { it.item in SAVING_ITEMS }
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val player = mc.player ?: return@handler

        // Only while actually falling, and not when something already caught
        // us - a boat, a slow-falling potion, water below.
        if (player.onGround() || player.deltaMovement.y >= 0.0 || player.isFallFlying) {
            return@handler
        }

        val prediction = FallingPlayer.fromPlayer(player).findCollision(LOOKAHEAD_TICKS) ?: return@handler
        val landing = prediction.pos ?: return@handler

        // Vanilla takes one heart per block past the first three.
        val fallDistance = player.y - landing.y
        val damage = (fallDistance - 3.0).toFloat()
        if (damage < minimumDamage) {
            return@handler
        }

        val color = when {
            checkHeldItem && holdingSave() -> safeColor
            damage >= player.health -> fatalColor
            else -> hurtColor
        }

        val box = AABB(
            landing.x.toDouble(), landing.y.toDouble() + 1.0, landing.z.toDouble(),
            landing.x + 1.0, landing.y + 1.05, landing.z + 1.0
        )

        event.renderEnvironment {
            drawBox(box.move(-camera.position()), color, color)
        }
    }

}
