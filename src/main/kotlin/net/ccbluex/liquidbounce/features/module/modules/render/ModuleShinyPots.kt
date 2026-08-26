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

import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * ShinyPots
 *
 * Gives potions the enchantment shimmer, so they stand out in a hotbar of
 * similar-looking bottles.
 *
 * Potion types are told apart by a small colour swatch inside an identically
 * shaped bottle, which is legible at leisure and not at all mid-fight. The
 * shimmer marks the slot rather than the type, which is the part that matters
 * when reaching for it.
 *
 * A rendering flag, and only for how the item is drawn. Nothing about what the
 * potion is or does changes.
 */
object ModuleShinyPots : ClientModule("ShinyPots", ModuleCategories.RENDER) {

    private val potions by boolean("Potions", true)
    private val splash by boolean("SplashPotions", true)
    private val lingering by boolean("LingeringPotions", true)

    /** Totems too: the same "did I actually grab it" problem. */
    private val totems by boolean("Totems", false)

    @JvmStatic
    fun shouldShine(stack: ItemStack): Boolean {
        if (!running) {
            return false
        }

        return when (stack.item) {
            Items.POTION -> potions
            Items.SPLASH_POTION -> splash
            Items.LINGERING_POTION -> lingering
            Items.TOTEM_OF_UNDYING -> totems
            else -> false
        }
    }

}
