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
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.features.misc.SessionStats
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories

/**
 * Stopwatch
 *
 * Times a run. Enabling starts the clock, disabling stops it, and the elapsed
 * time is available to the HUD as {session.stopwatch}.
 *
 * The toggle is the start button on purpose. A separate command would mean the
 * timer and its display could disagree about whether it is running, and a
 * stopwatch you cannot trust is worse than none.
 */
object ModuleStopwatch : ClientModule("Stopwatch", ModuleCategories.MISC) {

    /** Start from zero each time, rather than resuming where it stopped. */
    private val resetOnStart by boolean("ResetOnStart", true)

    override fun onEnabled() {
        if (resetOnStart) {
            SessionStats.resetStopwatch()
        }
        SessionStats.startStopwatch()
    }

    override fun onDisabled() {
        SessionStats.stopStopwatch()
    }

}
