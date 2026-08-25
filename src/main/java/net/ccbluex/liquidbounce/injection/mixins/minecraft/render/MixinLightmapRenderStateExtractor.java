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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFullBright;
import net.ccbluex.liquidbounce.features.module.modules.render.DoRender;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@NullMarked
@Mixin(LightmapRenderStateExtractor.class)
public abstract class MixinLightmapRenderStateExtractor {


    // Turns off blinking when the darkness effect is active.
    @ModifyVariable(method = "extract", at = @At(value = "STORE"), name = "darknessEffectScaleOption")
    private float injectAntiDarkness(float darknessEffectScaleOption) {
        return ModuleAntiBlind.canRender(DoRender.DARKNESS) ? darknessEffectScaleOption : 0f;
    }


    /**
     * FullBright's gamma override.
     *
     * Upstream shared this injection with XRay under the name
     * injectXRayFullBright, so removing XRay took FullBright's gamma with it.
     * Only the XRay half is gone.
     *
     * Target:
     * <pre>
     *     float brightnessOption = ((Double)this.minecraft.options.gamma().get()).floatValue();
     * </pre>
     */
    @ModifyVariable(method = "extract", at = @At(value = "STORE"), name = "brightnessOption")
    private float injectFullBrightGamma(float brightnessOption) {
        if (ModuleFullBright.FullBrightGamma.INSTANCE.getRunning()) {
            return ModuleFullBright.FullBrightGamma.INSTANCE.getGamma();
        }

        return brightnessOption;
    }
}
