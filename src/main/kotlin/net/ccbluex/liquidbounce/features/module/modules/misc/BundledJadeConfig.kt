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

import net.ccbluex.liquidbounce.features.bundled.ModConfigStore

/**
 * Jade's config store and the readers [ModuleBundledMods.Jade] is built from.
 *
 * A file of its own rather than a tail on `ModuleBundledMods.kt`: every bridged mod
 * brings a handful of these, and kept together they outgrow what detekt allows in one
 * file. One file per mod is the seam that keeps scaling.
 */

internal const val JADE_ID = "jade"

/**
 * Jade's config, which lives in a subdirectory rather than beside the others.
 *
 * Top level rather than a member of [ModuleBundledMods.Jade] on purpose: the nested
 * groups read it while their own objects are being constructed, which happens inside
 * the enclosing object's own construction. Reaching back into a half-built object
 * there is how this becomes a null at startup rather than a compile error.
 */
internal val jadeStore = ModConfigStore.json("jade/jade.json")

internal fun jadeBool(key: String, fallback: Boolean) = jadeStore.readBoolean(key) ?: fallback

internal fun jadeInt(key: String, fallback: Int) = jadeStore.readInt(key) ?: fallback

internal fun jadeFloat(key: String, fallback: Float) = jadeStore.readFloat(key) ?: fallback

/**
 * Reads a stored enum, matching on the name Jade writes rather than the ClickGUI label.
 *
 * An unknown value falls back rather than throwing: Jade may add a constant in a later
 * version, and a config this client cannot parse is not a reason to refuse to start.
 */
internal fun <T> jadeEnum(entries: List<T>, key: String, fallback: T): T
    where T : Enum<T>, T : ConfigKeyed =
    jadeStore.readString(key)?.let { raw -> entries.firstOrNull { it.key == raw } } ?: fallback
