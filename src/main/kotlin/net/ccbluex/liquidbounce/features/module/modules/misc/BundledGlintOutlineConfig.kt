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

import com.google.gson.JsonArray
import net.ccbluex.liquidbounce.features.bundled.JsonConfigStore
import net.ccbluex.liquidbounce.render.engine.type.Color4b

/** The mod's default outline colour, as its own config ships it. */
private val GLINT_DEFAULT = Color4b(210, 150, 248, 255)

/**
 * Reads the stored `[r, g, b]` outline colour.
 *
 * Top level rather than a member of [ModuleBundledMods.GlintOutline], for the
 * same construction-order reason as the other bridge helpers: the group reads
 * this while its own object is being built inside the enclosing object's
 * construction, and reaching back there gives a null at startup rather than a
 * compile error.
 *
 * Anything unreadable falls back to the mod's own default rather than throwing.
 * A config this client cannot parse is not a reason to refuse to start.
 */
internal fun glintOutlineColor(store: JsonConfigStore): Color4b {
    val stored = store.readArray("render_solid_outline_color_rgb") ?: return GLINT_DEFAULT
    if (stored.size() < 3) {
        return GLINT_DEFAULT
    }

    return runCatching {
        Color4b(stored.get(0).asInt, stored.get(1).asInt, stored.get(2).asInt, 255)
    }.getOrDefault(GLINT_DEFAULT)
}

/**
 * Builds the `[r, g, b]` array the mod expects.
 *
 * Alpha is dropped because the mod has no channel for it. The ClickGUI swatch
 * still offers one; it simply does not reach the config.
 */
internal fun glintRgb(color: Color4b): JsonArray = JsonArray().apply {
    add(color.r)
    add(color.g)
    add(color.b)
}
