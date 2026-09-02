/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.utils.input

import net.ccbluex.liquidbounce.features.misc.Toggleable

/**
 * Something a key or a mouse button can toggle.
 *
 * Modules have carried a bind since upstream, and for a long time they were the only
 * thing that could: `ModuleManager` matched key events against `modules` and nothing
 * else. That became a problem the moment HUD elements stopped being modules. Making a
 * native HUD element draggable means turning it into a
 * [net.ccbluex.liquidbounce.integration.theme.component.HudComponent], which is a
 * `ToggleableValueGroup` rather than a `ClientModule` - so the element gained a drag
 * handle and silently lost its key.
 *
 * This is the shared half. Both sides already had `enabled` through [Toggleable];
 * adding `bind` here lets `ModuleManager` dispatch against either without knowing
 * which it is, and every HUD component - the native ones, the theme's own, and the
 * minimap - became bindable as a side effect.
 */
interface Bindable : Toggleable {

    /**
     * The key or mouse button that toggles this, and how it behaves while held.
     *
     * Unbound by default: `InputConstants.UNKNOWN` matches no event, so a component
     * nobody has bound costs one comparison per key press and does nothing.
     */
    val bind: InputBind

}
