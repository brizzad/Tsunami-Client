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
import net.ccbluex.liquidbounce.utils.math.unaryMinus
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player

/**
 * Hitboxes
 *
 * Draws the collision box the server actually uses for each entity, which is
 * what a hit is resolved against. Vanilla only offers this behind F3+B, which
 * also turns on a pile of unrelated debug rendering.
 *
 * This draws only. It does not change any box, so it cannot change whether a
 * hit lands - it just stops you having to guess where the box is.
 */
object ModuleHitboxes : ClientModule("Hitboxes", ModuleCategories.RENDER) {

    private val players by boolean("Players", true)
    private val mobs by boolean("Mobs", true)
    private val items by boolean("Items", false)

    private val playerColor by color("PlayerColor", Color4b(0x1F, 0xA8, 0xFF, 0xB0))
    private val mobColor by color("MobColor", Color4b(0xFF, 0x8A, 0x3D, 0xB0))
    private val itemColor by color("ItemColor", Color4b(0xB0, 0xB0, 0xB0, 0x90))

    /**
     * Beyond this the boxes are a smear of lines rather than information, and
     * every extra entity is another set of lines to push.
     */
    private val range by float("Range", 32f, 4f..128f)

    private fun colorFor(entity: Entity): Color4b? = when {
        entity is Player -> if (players) playerColor else null
        entity is ItemEntity -> if (items) itemColor else null
        entity is LivingEntity -> if (mobs) mobColor else null
        else -> null
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val world = mc.level ?: return@handler
        val self = mc.player ?: return@handler
        val rangeSq = (range * range).toDouble()

        event.renderEnvironment {
            for (entity in world.entitiesForRendering()) {
                if (entity === self || entity.distanceToSqr(self) > rangeSq) {
                    continue
                }

                val color = colorFor(entity) ?: continue
                drawEntityBox(entity, color)
            }
        }
    }

    private fun WorldRenderEnvironment.drawEntityBox(entity: Entity, color: Color4b) {
        // The box is in world space; the render environment is camera-relative.
        drawBox(entity.boundingBox.move(-camera.position()), outlineColor = color)
    }

}
