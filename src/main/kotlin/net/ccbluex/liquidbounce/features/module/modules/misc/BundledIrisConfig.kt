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
 * Reads the stored colour space, falling back rather than throwing.
 *
 * Top level rather than a member of [ModuleBundledMods.Iris], for the same
 * reason as the Jade and Sodium Extra helpers: the group reads it while its own
 * object is being constructed, which happens inside the enclosing object's
 * construction, and reaching into a half-built object there gives a null at
 * startup rather than a compile error.
 */
internal fun irisColorSpace(store: ModConfigStore): IrisColorSpace {
    val stored = store.readString("colorSpace") ?: return IrisColorSpace.SRGB
    return IrisColorSpace.entries.firstOrNull { it.key == stored } ?: IrisColorSpace.SRGB
}
