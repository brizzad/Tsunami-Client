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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.render.colorgrading;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.ccbluex.liquidbounce.render.colorgrading.ColorGradingEngine;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Grades the world frame once the level renderer is finished with it.
 *
 * Priority 900 puts this after {@code MixinLevelRendererMotionBlur} (priority 800)
 * in the tail, so motion blur resolves first and the grade is applied to the frame
 * the player actually sees rather than to one of blur's inputs.
 *
 * Deliberately its own mixin rather than another line inside the motion blur one.
 * Two features sharing an injection is how four modules in this fork were once
 * silently broken by a deletion.
 */
@Mixin(value = LevelRenderer.class, priority = 900)
public class MixinLevelRendererColorGrading {

    @Inject(method = "render", at = @At("HEAD"))
    private void tsunami$captureAllocator(
            GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker,
            boolean renderOutline, CameraRenderState cameraState,
            Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog,
            Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
        ColorGradingEngine.captureAllocator(resourceAllocator);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void tsunami$gradeFrame(
            GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker,
            boolean renderOutline, CameraRenderState cameraState,
            Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog,
            Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
        ColorGradingEngine.apply();
        ColorGradingEngine.clearFrameAllocator();
    }
}
