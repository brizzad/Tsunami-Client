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

/** Sodium Extra's config store and readers. See [BundledJadeConfig] for why this is its own file. */

internal const val SODIUM_EXTRA_ID = "sodium-extra"

/**
 * Sodium Extra's config store and helpers.
 *
 * Top level rather than members of [ModuleBundledMods.SodiumExtra], for the same
 * reason as the Jade helpers above: the nested groups read it while their own
 * objects are being constructed, which happens inside the enclosing object's
 * construction. Reaching back into a half-built object there gives a null at
 * startup rather than a compile error.
 */
internal val sodiumExtraStore = ModConfigStore.json("sodium-extra-options.json")

internal fun seBool(key: String, fallback: Boolean) = sodiumExtraStore.readBoolean(key) ?: fallback

internal fun seInt(key: String, fallback: Int) = sodiumExtraStore.readInt(key) ?: fallback

/** Matches on the constant Sodium Extra writes, falling back rather than throwing. */
internal fun <T> seEnum(entries: List<T>, key: String, fallback: T): T where T : Enum<T>, T : ConfigKeyed {
    val stored = sodiumExtraStore.readString(key) ?: return fallback
    return entries.firstOrNull { it.key == stored } ?: fallback
}

internal fun seWrite(pair: Pair<String, Any>) =
    ModuleBundledMods.applyTo(sodiumExtraStore, SODIUM_EXTRA_ID, pair)
