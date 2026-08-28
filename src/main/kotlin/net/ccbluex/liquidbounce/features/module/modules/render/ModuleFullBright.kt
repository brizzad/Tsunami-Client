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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.PlayerPostTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories

/**
 * A full bright module
 *
 * Allows you to see in the dark.
 */
/*
 * The NightVision mode was removed. It called player.addEffect(NIGHT_VISION) every tick,
 * which fabricates a status effect the server never granted - that is inventing a potion,
 * not adjusting a display setting, and it desyncs the client's own effect list too.
 *
 * The gamma mode is kept. Raising brightness is what every client in this space ships and
 * what servers here expect, but it is still a setting worth deciding on deliberately.
 */
object ModuleFullBright : ClientModule("FullBright", ModuleCategories.RENDER) {

    private val modes = choices(
        "Mode", FullBrightGamma, arrayOf<Mode>(
            FullBrightGamma
        )
    )

    object FullBrightGamma : Mode("Gamma") {

        override val parent: ModeValueGroup<Mode>
            get() = modes

        val brightness by int("Brightness", 15, 1..15)

        var gamma = 0.0F
            private set

        override fun enable() {
            gamma = mc.options.gamma().get().toFloat()
        }

        val tickHandler = handler<PlayerPostTickEvent> {
            if (gamma < brightness) {
                gamma = (gamma + 0.1F).coerceAtMost(brightness.toFloat())
            }
        }

    }

}
