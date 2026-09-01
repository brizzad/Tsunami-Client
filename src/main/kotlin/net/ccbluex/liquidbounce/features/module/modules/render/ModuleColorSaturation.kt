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
import net.ccbluex.liquidbounce.event.events.GameRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.colorgrading.ColorGradingConfig
import net.ccbluex.liquidbounce.render.colorgrading.ColorGradingEngine

/**
 * ColorSaturation
 *
 * Colour grading for the world: saturation, vibrance, contrast, brightness,
 * gamma and white balance.
 *
 * Every client in this space ships this and Minecraft ships nothing like it -
 * vanilla's only colour control is a brightness slider that lifts the whole
 * lightmap. Players run it for two different reasons and both are worth
 * supporting: pushing saturation up makes team colours, potion swirls and ore
 * textures separate at a glance, and pulling it down takes the edge off a
 * texture pack that is louder than the person using it wants.
 *
 * ## What it does and does not touch
 *
 * The grade runs at the tail of the level render, so it covers the world and
 * stops there. The HUD, chat, the inventory and every screen keep their real
 * colours - a desaturated interface is an unreadable one, and the numbers on
 * your armour bar are not what anybody is adjusting this for.
 *
 * It is a per-pixel function of the finished frame. It cannot make anything
 * visible that was not already drawn: a block hidden behind another block was
 * never in the frame to grade. That is the line between this and a wallhack,
 * and it is a structural one rather than a promise - the shader has one input
 * sampler and it is the colour buffer.
 *
 * @see net.ccbluex.liquidbounce.render.colorgrading.ColorGradingEngine
 */
object ModuleColorSaturation : ClientModule("ColorSaturation", ModuleCategories.RENDER) {

    /**
     * Named grades, for when "a bit more colour" is the whole request.
     *
     * Defaults to [Preset.CUSTOM] so the sliders below are live out of the box -
     * a slider that visibly does nothing because a preset is overriding it is a
     * bug report waiting to happen. The slider defaults are the Vivid grade, so
     * switching the module on with nothing touched looks the same either way.
     */
    private val preset by enumChoice("Preset", Preset.CUSTOM)

    private val saturation by float("Saturation", 1.25f, 0f..3f)
    private val vibrance by float("Vibrance", 0.1f, -1f..1f)
    private val contrast by float("Contrast", 1.05f, 0.25f..2.5f)
    private val brightness by float("Brightness", 1f, 0.25f..2f)

    /** Above 1.0 lifts the shadows without washing out the highlights. */
    private val gamma by float("Gamma", 1f, 0.25f..2.5f)

    /** Positive is warmer (more red, less blue), negative cooler. */
    private val temperature by float("Temperature", 0f, -0.5f..0.5f)

    /** Positive pushes green, negative magenta. The other white-balance axis. */
    private val tint by float("Tint", 0f, -0.5f..0.5f)

    @Suppress("unused")
    private val syncHandler = handler<GameRenderEvent> {
        ColorGradingEngine.setConfig(currentConfig())
    }

    private fun currentConfig(): ColorGradingConfig = when (preset) {
        Preset.CUSTOM -> ColorGradingConfig(
            saturation, vibrance, contrast, brightness, gamma, temperature, tint
        )
        else -> preset.config
    }

    override fun onRegistration() {
        // The engine is driven from a mixin that has no idea whether the module is on,
        // and ClientModule makes onToggled final with no disable hook to override. So
        // the engine pulls the running state instead: a flag pushed from a handler
        // would latch on, because the handlers stop firing the moment it is switched
        // off and nothing would ever push the false.
        ColorGradingEngine.bindEnabled { running }
    }

    /**
     * Named grades. The numbers are conservative on purpose: a preset that
     * clips the highlights on a bright day is one nobody leaves enabled.
     */
    @Suppress("unused")
    enum class Preset(
        override val tag: String,
        val config: ColorGradingConfig
    ) : Tagged {
        /** A light lift. What most people mean by "turn the saturation up". */
        VIVID("Vivid", ColorGradingConfig(1.25f, 0.1f, 1.05f, 1f, 1f, 0f, 0f)),

        /** Heavier, for texture packs that are already flat. */
        PUNCHY("Punchy", ColorGradingConfig(1.5f, 0.2f, 1.15f, 1.02f, 1.05f, 0.03f, 0f)),

        /** Takes the shout out of a loud pack without going grey. */
        MUTED("Muted", ColorGradingConfig(0.8f, 0f, 0.95f, 1f, 1f, 0f, 0f)),

        /** Warm, slightly lifted shadows. Easier on a long session. */
        WARM("Warm", ColorGradingConfig(1.1f, 0.05f, 1f, 1f, 1.1f, 0.12f, 0f)),

        /** Cool and contrasty. */
        COOL("Cool", ColorGradingConfig(1.1f, 0.05f, 1.1f, 1f, 1f, -0.12f, 0f)),

        /** Luma only. Useful for checking contrast in a build. */
        GREYSCALE("Greyscale", ColorGradingConfig(0f, 0f, 1f, 1f, 1f, 0f, 0f)),

        /** Hands control to the sliders above. */
        CUSTOM("Custom", ColorGradingConfig.IDENTITY)
    }

}
