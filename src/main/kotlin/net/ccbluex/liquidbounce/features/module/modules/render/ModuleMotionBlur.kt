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

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.GameRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.motionblur.MotionBlurConfig
import net.ccbluex.liquidbounce.render.motionblur.MotionBlurEngine

/**
 * MotionBlur module
 *
 * Blends motion across frames to cut the stroboscopic stutter of high-framerate panning.
 *
 * The rendering engine is a port of Natural Motion Blur (LGPL-3.0, relicensed to GPL-3.0
 * here as its licence permits) and lives in [net.ccbluex.liquidbounce.render.motionblur].
 * That engine is driven entirely by mixins on the level/game renderer; this module is the
 * ClickGUI surface for it, mirroring its values into [MotionBlurConfig] each frame.
 *
 * [VELOCITY][BlurAlgorithm.VELOCITY] is the default because it is the only mode that
 * reconstructs real per-pixel motion - it rebuilds world position from the depth buffer and
 * reprojects against the previous frame's matrices, so geometry smears along its actual
 * screen-space path. The blending modes are cheaper but only ever average whole frames,
 * which reads as ghosting rather than blur.
 */
object ModuleMotionBlur : ClientModule("MotionBlur", ModuleCategories.RENDER) {

    private val algorithm by enumChoice("Algorithm", BlurAlgorithm.VELOCITY)

    /**
     * Ignored by [BlurAlgorithm.HYBRID], which derives its own strength per frame.
     */
    private val strength by float("Strength", 0.5f, 0f..1f)

    /**
     * Above the monitor's refresh rate each frame is on screen for less time, so a fixed
     * blend reads as progressively weaker. Scaling by FPS/refresh keeps the effect constant.
     * Only [BlurAlgorithm.VELOCITY] supports it; the engine ignores it otherwise.
     */
    private val refreshRateScaling by boolean("RefreshRateScaling", true)

    /**
     * The engine caches a compiled [net.minecraft.client.renderer.PostChain] per algorithm,
     * so switching modes has to drop those caches or the old chain keeps rendering.
     */
    private var lastAlgorithm: BlurAlgorithm? = null

    @Suppress("unused")
    private val syncHandler = handler<GameRenderEvent> {
        pushSettings()
    }

    private fun pushSettings() {
        val config = MotionBlurConfig.get()
        config.motionBlurStrength = strength
        config.refreshRateScaling = refreshRateScaling
        config.blurAlgorithm = algorithm.engineValue

        if (lastAlgorithm != algorithm) {
            lastAlgorithm = algorithm
            MotionBlurEngine.invalidate()
        }
    }

    override fun onRegistration() {
        // The engine pulls the running state instead of having it pushed in. ClientModule
        // makes onToggled final and offers no disable hook, and a module's handlers stop
        // firing the moment it is switched off - so a pushed flag would latch on forever.
        MotionBlurConfig.bindEnabled { running }
    }

    enum class BlurAlgorithm(
        override val tag: String,
        val engineValue: MotionBlurConfig.BlurAlgorithm
    ) : Tagged {
        VELOCITY("Velocity", MotionBlurConfig.BlurAlgorithm.VELOCITY_BASED),
        FRAME_BLENDING("FrameBlending", MotionBlurConfig.BlurAlgorithm.FRAME_BLENDING),
        HYBRID("Hybrid", MotionBlurConfig.BlurAlgorithm.HYBRID_BLENDING),
        ACCUMULATION_MAX("AccumulationMax", MotionBlurConfig.BlurAlgorithm.ACCUMULATION_MAX),
        ACCUMULATION_MIX("AccumulationMix", MotionBlurConfig.BlurAlgorithm.ACCUMULATION_MIX)
    }
}
