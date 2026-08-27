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
import net.ccbluex.liquidbounce.render.engine.type.Color4b

/**
 * PotionTimers
 *
 * Draws a depletion bar under every status effect icon, on the HUD and in the
 * inventory.
 *
 * Vanilla already prints the remaining time as text. That is precise and slow:
 * you can read "0:08" but not while someone is hitting you. A bar answers the
 * only question actually being asked mid-fight - is this about to run out -
 * without reading anything.
 *
 * Merged from A5b84's Status Effect Bars
 * (https://modrinth.com/mod/status-effect-bars), LGPL-3.0, which permits use
 * inside a GPL-3.0 work. The geometry and the rules for when a bar is worth
 * drawing are upstream's; this class is the ClickGUI face and
 * [net.ccbluex.liquidbounce.features.effectbars.EffectBarRenderer] is the
 * drawing.
 *
 * ## It forces the vanilla effect icons back on
 *
 * The theme HUD ships an Effects component, and while that is enabled it
 * cancels vanilla's own effect overlay outright. These bars are drawn onto
 * those vanilla icons, so with the tweak in force this module would load,
 * enable, and draw nothing at all - the exact failure mode that once hid four
 * dead modules in this fork. Enabling it therefore suppresses that tweak, and
 * the vanilla icons come back. If you would rather keep only the themed
 * panel, leave this off.
 */
object ModulePotionTimers : ClientModule("PotionTimers", ModuleCategories.RENDER) {

    /**
     * Longest effect that still gets a bar, in seconds.
     *
     * An eight-minute fire resistance sits at full for the whole time you are
     * looking at it, so the bar is noise. Anything short enough to actually run
     * out during a fight is worth drawing.
     */
    private val maxDuration by int("MaxDuration", 60, 5..600)

    /**
     * How long a beacon effect must have been running before it gets a bar,
     * in seconds.
     *
     * A beacon reapplies its effects constantly, so a freshly applied one is
     * always near full and always about to be refreshed. Waiting a moment
     * stops the icons flickering with bars that mean nothing.
     */
    private val minAmbientAge by int("MinAmbientAge", 5, 0..60)

    /** Bar height in pixels. */
    val thickness by int("Thickness", 2, 1..6)

    /** Inset from each side of the icon. */
    val padding by int("Padding", 1, 0..8)

    /**
     * Colour the bar to match the effect itself.
     *
     * With four effects stacked, one flat colour turns the icons into a single
     * block. Following each effect keeps the bar attached to what it describes.
     */
    val useEffectColor by boolean("UseEffectColor", true)

    /** Used when [useEffectColor] is off, and always for the alpha channel. */
    val barColor by color("BarColor", Color4b(0x1F, 0xA8, 0xFF, 0xFF))

    /** The part already elapsed. */
    val backgroundColor by color("BackgroundColor", Color4b(0x00, 0x00, 0x00, 0x90))

    /** Also draw the bars in the inventory effect list. */
    val inInventory by boolean("InInventory", true)

    // Ticks, not seconds, for the renderer: durations arrive in ticks and
    // converting once here beats converting on every icon of every frame.
    val maxDurationTicks: Int get() = maxDuration * 20
    val minAmbientAgeTicks: Int get() = minAmbientAge * 20

}
