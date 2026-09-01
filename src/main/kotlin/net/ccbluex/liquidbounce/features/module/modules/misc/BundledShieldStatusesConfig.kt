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

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.features.bundled.ModConfigStore
import net.ccbluex.liquidbounce.render.engine.type.Color4b

/** Shield Statuses' config store, colour readers and the WalksyLib colour record. */

internal const val SHIELD_ID = "shieldstatus"

internal const val SHIELD_SELF_ONLY = "Color/General Options/Self State Only"
internal const val SHIELD_INTERPOLATE = "Color/General Options/Interpolate Shield Color"
internal const val SHIELD_GRAYSCALE = "Color/General Options/Grayscale Shield Texture"
internal const val SHIELD_CUSTOM_ENABLED = "Color/Enabled Shield Options/Custom Enabled Shield Color"
internal const val SHIELD_ENABLED_COLOR = "Color/Enabled Shield Options/Enabled Color"
internal const val SHIELD_CUSTOM_ACTIVE = "Color/Using Shield Options/Custom Active Shield Color"
internal const val SHIELD_ACTIVE_COLOR = "Color/Using Shield Options/Active Color"
internal const val SHIELD_CUSTOM_RISING = "Color/Rising Shield Options/Custom Rising Shield Color"
internal const val SHIELD_RISING_COLOR = "Color/Rising Shield Options/Rising Color"
internal const val SHIELD_CUSTOM_DISABLED = "Color/Disabled Shield Options/Custom Disabled Shield Color"
internal const val SHIELD_DISABLED_COLOR = "Color/Disabled Shield Options/Disabled Color"

/** Top level for the same construction-order reason as the Jade helpers. */
internal val shieldStore = ModConfigStore.namedRecords("shieldstatus.json")

internal fun shieldWrite(pair: Pair<String, Any>) =
    ModuleBundledMods.applyTo(shieldStore, SHIELD_ID, pair)

/** Reads a WalksyLib colour record back into a [Color4b]. */
internal fun shieldColor(path: String, fallback: Color4b): Color4b {
    val stored = shieldStore.readObject(path) ?: return fallback

    return runCatching {
        Color4b(
            stored.get("r").asInt,
            stored.get("g").asInt,
            stored.get("b").asInt,
            stored.get("a").asInt,
        )
    }.getOrDefault(fallback)
}

/**
 * Builds the colour record WalksyLib expects.
 *
 * `ColorTypeAdapter` in the jar reads all of `r`, `g`, `b`, `a`, `value`,
 * `hue`, `saturation`, `brightness`, `rainbow`, `rainbowSpeed`, `pulse` and
 * `pulseSpeed`, so writing only the channels would leave the packed int and
 * the HSB triple describing the *previous* colour. Its own picker edits in
 * HSB, so a stale triple is what it would show you.
 *
 * Rainbow and pulse are animation modes rather than colours; they are written
 * off, because there is no ClickGUI control for them here and silently leaving
 * a colour animating would contradict the swatch the player just set.
 */
internal fun walksyColor(color: Color4b): JsonObject {
    val hsb = FloatArray(3)
    java.awt.Color.RGBtoHSB(color.r, color.g, color.b, hsb)

    val packed = (color.a shl 24) or (color.r shl 16) or (color.g shl 8) or color.b

    return JsonObject().apply {
        addProperty("r", color.r)
        addProperty("g", color.g)
        addProperty("b", color.b)
        addProperty("a", color.a)
        addProperty("value", packed)
        addProperty("hue", hsb[0])
        addProperty("saturation", hsb[1])
        addProperty("brightness", hsb[2])
        addProperty("rainbow", false)
        addProperty("rainbowSpeed", 5)
        addProperty("pulse", false)
        addProperty("pulseSpeed", 5)
    }
}
