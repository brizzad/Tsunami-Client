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
import net.ccbluex.liquidbounce.injection.mixins.minecraft.entity.MixinItemEntityAccessor
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.utils.math.unaryMinus
import net.minecraft.world.entity.item.ItemEntity
import kotlin.math.sin

/**
 * ItemDespawn
 *
 * Outlines dropped items that are running out of time, and flashes the ones
 * about to go.
 *
 * A dropped stack lives 6000 ticks - five minutes - and vanilla gives no sign
 * of where in that it is. The thing this prevents is the specific and very
 * common loss of walking back to a death pile thirty seconds too late, having
 * had no way to know.
 *
 * ## The one caveat, stated plainly
 *
 * The client's copy of the age counter starts when *this client* first saw the
 * entity, not when it was dropped. Your own drops are exact. Something that
 * was already lying there when you rendered the chunk reads younger than it
 * is, so the warning comes late rather than early. There is no packet that
 * carries the real age, so this is a floor on what any client can know rather
 * than a shortcut taken here.
 *
 * @see net.ccbluex.liquidbounce.injection.mixins.minecraft.entity.MixinItemEntityAccessor
 */
object ModuleItemDespawn : ClientModule("ItemDespawn", ModuleCategories.RENDER) {

    /** Vanilla's despawn age for a dropped stack, in ticks. */
    private const val LIFETIME_TICKS = 6000

    private const val TICKS_PER_SECOND = 20

    /** How long before despawn the outline appears, in seconds. */
    private val warnAt by int("WarnSeconds", 60, 5..280)

    /** How long before despawn the outline starts flashing, in seconds. */
    private val flashAt by int("FlashSeconds", 10, 1..60)

    private val range by float("Range", 48f, 8f..256f)

    /** Colour at the moment the warning starts. Fades toward [urgentColor]. */
    private val warnColor by color("WarnColor", Color4b(0xFF, 0xC1, 0x07, 0xFF))

    /** Colour as the clock runs out. */
    private val urgentColor by color("UrgentColor", Color4b(0xFF, 0x3B, 0x30, 0xFF))

    /** Fill the box as well as outlining it. */
    private val fill by boolean("Fill", true)

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val player = mc.player ?: return@handler
        val level = mc.level ?: return@handler

        val rangeSq = (range * range).toDouble()
        val eye = player.eyePosition

        // One phase for every item on screen, so they flash together rather than
        // each on its own clock. A field of items blinking out of step is noise.
        val phase = (System.currentTimeMillis() % 1000L) / 1000.0
        val flashAlpha = (0.35 + 0.65 * (0.5 + 0.5 * sin(phase * 2.0 * Math.PI))).toFloat()

        event.renderEnvironment {
            val cam = camera.position()

            // Selected as a lazy sequence rather than in a loop of `continue`s. Still
            // one pass over the entity list, and the despawn maths reads in one place.
            val expiring = level.entitiesForRendering()
                .asSequence()
                .filterIsInstance<ItemEntity>()
                .filter { !it.item.isEmpty && it.position().distanceToSqr(eye) <= rangeSq }
                .map { it to (LIFETIME_TICKS - (it as MixinItemEntityAccessor).age) }
                .filter { (_, remainingTicks) -> remainingTicks > 0 }
                .map { (entity, remainingTicks) -> entity to remainingTicks.toFloat() / TICKS_PER_SECOND }
                .filter { (_, remainingSeconds) -> remainingSeconds <= warnAt }

            for ((entity, remainingSeconds) in expiring) {
                // 0 at the moment the warning starts, 1 at despawn.
                val urgency = (1f - remainingSeconds / warnAt).coerceIn(0f, 1f)
                val base = warnColor.interpolateTo(urgentColor, urgency.toDouble())

                val alpha = if (remainingSeconds <= flashAt) {
                    (base.a * flashAlpha).toInt().coerceIn(0, 0xFF)
                } else {
                    base.a
                }

                val outline = Color4b(base.r, base.g, base.b, alpha)
                val face = if (fill) Color4b(base.r, base.g, base.b, alpha / 4) else null

                drawBox(entity.boundingBox.inflate(0.05).move(-cam), face, outline)
            }
        }
    }

}
