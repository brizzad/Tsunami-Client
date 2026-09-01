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

import net.ccbluex.liquidbounce.config.types.list.Tagged

/**
 * Which corner a native overlay readout hangs off, and the arithmetic for
 * turning that plus an offset into a top-left pixel.
 *
 * Shared because every readout module here had been writing the same four-way
 * `when` twice - once for x, once for y - and the two are easy to get subtly
 * out of step. The tags match what these modules already serialised, so an
 * existing config keeps its corner.
 */
@Suppress("unused")
enum class ScreenAnchor(override val tag: String) : Tagged {
    TOP_LEFT("TopLeft"),
    TOP_RIGHT("TopRight"),
    BOTTOM_LEFT("BottomLeft"),
    BOTTOM_RIGHT("BottomRight");

    val isRight: Boolean
        get() = this == TOP_RIGHT || this == BOTTOM_RIGHT

    val isBottom: Boolean
        get() = this == BOTTOM_LEFT || this == BOTTOM_RIGHT

    /** Left edge of a [contentWidth]-wide block, [offset] in from the anchored side. */
    fun x(screenWidth: Int, contentWidth: Int, offset: Int): Int =
        if (isRight) screenWidth - offset - contentWidth else offset

    /** Top edge of a [contentHeight]-tall block, [offset] in from the anchored side. */
    fun y(screenHeight: Int, contentHeight: Int, offset: Int): Int =
        if (isBottom) screenHeight - offset - contentHeight else offset
}
