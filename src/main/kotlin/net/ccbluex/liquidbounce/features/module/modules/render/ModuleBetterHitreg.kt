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
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.hitreg.Hit
import net.ccbluex.liquidbounce.features.hitreg.HitState
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories

/**
 * BetterHitreg
 *
 * Draws the feedback for a hit as soon as you swing, rather than waiting for
 * the server to confirm it. On a high-ping connection the flash and the sound
 * arrive long after the click, which makes combat feel unresponsive even when
 * every hit is landing.
 *
 * The logic behind this is merged from jasspir's Better Hitreg
 * (https://modrinth.com/mod/betterhitreg), Apache-2.0, and lives in
 * [net.ccbluex.liquidbounce.features.hitreg]. That package documents what was
 * taken and what was deliberately left out. This class is only the part that
 * puts it in the ClickGUI.
 *
 * Appearance only, in upstream's own words: "all custom hits are in appearance
 * only, your actual hits are not modified in any way, only the way they are
 * rendered and sound is affected".
 *
 * What this does NOT touch, deliberately:
 *
 *  - the attack packet, its timing, and its contents
 *  - whether the swing is sent at all
 *  - reach, hit detection, cooldown, or any check the server performs
 *
 * The server decides what landed exactly as it did before. If it disagrees,
 * its own result still arrives and still wins.
 *
 * Off by default. It changes what a fight looks like rather than what it is,
 * and that is the player's call rather than a default to inherit.
 */
object ModuleBetterHitreg : ClientModule("BetterHitreg", ModuleCategories.RENDER) {

    /** The hurt flash, applied locally on the swing. */
    val animation by boolean("Animation", true)

    /** The hit sound, played locally on the swing. */
    val sound by boolean("Sound", true)

    val volume by float("Volume", 1f, 0f..1f)

    /**
     * Play only the hurt sound, the way 1.8 did.
     *
     * 1.9 added the attack sounds that mark a crit or a sweep. Players who
     * learned to fight before that read those extra sounds as noise.
     */
    val legacySounds by boolean("LegacySounds", false)

    /** Crit and sharpness particles, spawned locally with the hit. */
    val particles by boolean("Particles", true)

    /** Spawn sharpness particles on every hit, not only with a sharpness weapon. */
    val sharpnessParticlesAlways by boolean("SharpnessParticlesAlways", false)

    /**
     * How long to wait before drawing, in milliseconds.
     *
     * Zero draws on the click. A small delay can sit better with the swing
     * animation, which is why the option exists at all.
     */
    val delay by int("Delay", 0, 0..200)

    /**
     * Skip the prediction when it is likely to be wrong.
     *
     * A new target, a target somebody else is hitting, a hit right after
     * travelling a long way, or a hit after the server ghosted the last one -
     * all cases where guessing draws something that did not happen.
     */
    val safeRegsOnly by boolean("SafeRegsOnly", true)

    /** Never predict against a player holding a shield, raised or not. */
    val ignoreShieldHolders by boolean("IgnoreShieldHolders", false)

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { event ->
        // Not cancelling, not rewriting, not delaying. The event carries on to
        // whatever normally sends the attack.
        Hit.capture(event.entity)?.load()
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        HitState.tick()
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        // Entity ids are only meaningful within a world, and a stale target
        // would be compared against whatever inherits its id next.
        HitState.reset()
    }

    override fun onDisabled() {
        HitState.reset()
    }
}
