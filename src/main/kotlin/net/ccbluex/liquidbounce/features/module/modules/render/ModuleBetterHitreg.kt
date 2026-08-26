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

import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.LivingEntity

/**
 * BetterHitreg
 *
 * Shows the feedback for a hit as soon as you swing, instead of waiting for
 * the server to say so. On a high-ping connection the flash and the sound
 * arrive long after the click, which makes combat feel unresponsive even when
 * every hit is landing.
 *
 * Appearance only, in the same sense as jasspir's Better Hitreg
 * (https://modrinth.com/mod/betterhitreg), whose description is explicit that
 * "all custom hits are in appearance only, your actual hits are not modified
 * in any way, only the way they are rendered and sound is affected".
 *
 * What this module does NOT touch, deliberately:
 *
 *  - the attack packet, its timing, and its contents
 *  - whether the swing is sent at all
 *  - reach, hit detection, cooldown, or any check the server performs
 *
 * The server decides what landed exactly as it did before. If the server
 * disagrees, its own result still arrives and still wins; the only thing that
 * changed is that the client stopped waiting to draw and play a sound.
 *
 * Off by default. It changes what a fight looks like rather than what it is,
 * and that is the player's call to make rather than a default to inherit.
 */
object ModuleBetterHitreg : ClientModule("BetterHitreg", ModuleCategories.RENDER) {

    /** Red flash on the target, applied locally on the swing. */
    private val flash by boolean("Flash", true)

    /** Hit sound, played locally on the swing. */
    private val sound by boolean("Sound", true)

    private val volume by float("Volume", 1f, 0f..1f)

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { event ->
        // Explicitly not cancelling, not rewriting, not delaying. The event
        // continues to whatever normally sends the attack.
        val target = event.entity as? LivingEntity ?: return@handler
        val level = mc.level ?: return@handler

        if (flash && target.hurtTime == 0) {
            // Same values the game sets when the server confirms a hit. When
            // that confirmation lands it simply refreshes the timer.
            target.hurtDuration = HURT_DURATION
            target.hurtTime = HURT_DURATION
        }

        if (sound && volume > 0f) {
            level.playLocalSound(
                target,
                SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS,
                volume,
                1f
            )
        }
    }

    /** Vanilla's hurt flash length, in ticks. */
    private const val HURT_DURATION = 10

}
