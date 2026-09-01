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

import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.minecraft.client.gui.components.toasts.AdvancementToast
import net.minecraft.client.gui.components.toasts.NowPlayingToast
import net.minecraft.client.gui.components.toasts.RecipeToast
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.client.gui.components.toasts.Toast
import net.minecraft.client.gui.components.toasts.TutorialToast

/**
 * ToastControl
 *
 * Picks which pop-up notifications the game is allowed to show.
 *
 * Toasts occupy the top-right corner for five seconds at a time and stack four
 * deep. A recipe-unlock cascade after picking up one new item covers the part
 * of the screen the tab list and most HUD layouts live in, and vanilla offers
 * no way to stop it.
 *
 * Each kind is separate rather than one on/off switch, because they are not
 * equally worth hiding: the system toasts carry world-corruption and pack-load
 * failures, and switching those off is a decision worth making on its own. They
 * stay on by default for that reason.
 *
 * Filtering is on the queue, not the draw, so a hidden toast never takes one of
 * the four slots from a visible one.
 */
object ModuleToastControl : ClientModule("ToastControl", ModuleCategories.MISC) {

    /** "Recipe unlocked". The noisiest kind by a distance. */
    private val recipes by boolean("Recipes", false)

    /** Advancement pop-ups. */
    private val advancements by boolean("Advancements", true)

    /**
     * World load failures, pack errors, screenshot confirmations. On by default:
     * these are the ones that tell you something went wrong.
     */
    private val system by boolean("System", true)

    /** The first-run tutorial hints. */
    private val tutorial by boolean("Tutorial", false)

    /** The music track banner. */
    private val nowPlaying by boolean("NowPlaying", true)

    /** Anything a mod adds that is none of the above. */
    private val other by boolean("Other", true)

    /**
     * @see net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.toast.MixinToastManager
     */
    @JvmStatic
    fun shouldShow(toast: Toast): Boolean {
        if (!running) {
            return true
        }

        return when (toast) {
            is RecipeToast -> recipes
            is AdvancementToast -> advancements
            is SystemToast -> system
            is TutorialToast -> tutorial
            is NowPlayingToast -> nowPlaying
            else -> other
        }
    }

}
