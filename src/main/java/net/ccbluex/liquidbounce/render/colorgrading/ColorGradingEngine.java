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

package net.ccbluex.liquidbounce.render.colorgrading;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.ccbluex.liquidbounce.injection.mixins.minecraft.render.motionblur.PostChainAccessor;
import net.ccbluex.liquidbounce.injection.mixins.minecraft.render.motionblur.PostPassAccessor;
import net.ccbluex.liquidbounce.injection.mixins.minecraft.render.motionblur.ShaderManagerAccessor;
import net.ccbluex.liquidbounce.render.motionblur.ClientRenderTargets;
import net.ccbluex.liquidbounce.render.motionblur.GpuBufferUtil;
import net.ccbluex.liquidbounce.render.motionblur.ManagedUniformBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Runs the {@code liquidbounce:color_grading} post chain over the finished world
 * frame.
 *
 * <h2>Why this borrows from the motion blur package</h2>
 *
 * {@link GpuBufferUtil}, {@link ManagedUniformBuffer} and {@link ClientRenderTargets}
 * are plumbing for "get a std140 uniform block onto the GPU and find the main render
 * target", with nothing motion-blur-shaped about them. They live in that package
 * because it was the first feature that needed them. Copying them here would mean two
 * copies of the reflective render-target lookup drifting apart across a Minecraft
 * update, which is worse than the coupling.
 *
 * The three shader accessors are borrowed for the same reason. Nothing in this class
 * touches motion blur's own state: separate {@link PostChain}, separate UBO, separate
 * injection point.
 *
 * <h2>Where it runs</h2>
 *
 * At the tail of {@code LevelRenderer.render}, so it grades the world and leaves the
 * HUD, chat and every open screen at their real colours. Desaturating the interface
 * along with the world is how these effects end up unreadable.
 */
public final class ColorGradingEngine {

    /** Eight floats. std140 rounds a uniform block up to a multiple of 16 bytes. */
    private static final int UBO_SIZE = 32;

    private static final String UNIFORM_BLOCK = "ColorGradingUniforms";
    private static final String CHAIN_NAME = "color_grading";

    private static final ManagedUniformBuffer gradingUBO =
            new ManagedUniformBuffer(UNIFORM_BLOCK, UBO_SIZE);

    private static GraphicsResourceAllocator frameAllocator = null;
    private static boolean loadErrorLogged = false;

    private static ColorGradingConfig config = ColorGradingConfig.IDENTITY;

    /**
     * Pulled rather than pushed. A module's event handlers stop firing the moment it
     * is switched off, so a flag it pushed on its last frame would latch on forever
     * and the grade would never come back off.
     */
    private static BooleanSupplier enabled = () -> false;

    private ColorGradingEngine() {}

    public static void captureAllocator(GraphicsResourceAllocator allocator) {
        frameAllocator = allocator;
    }

    public static void clearFrameAllocator() {
        frameAllocator = null;
    }

    /**
     * Pushed by the module every frame rather than pulled, because the module is the
     * only thing that knows whether it is running and what the sliders say.
     */
    public static void setConfig(ColorGradingConfig next) {
        config = next == null ? ColorGradingConfig.IDENTITY : next;
    }

    public static void bindEnabled(BooleanSupplier supplier) {
        enabled = supplier == null ? () -> false : supplier;
    }

    public static void apply() {
        if (frameAllocator == null || !enabled.getAsBoolean() || config.isIdentity()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        PostChain chain = loadChain(client);
        if (chain == null) {
            return;
        }

        List<PostPass> passes = ((PostChainAccessor) chain).getPasses();
        if (passes.isEmpty()) {
            return;
        }

        Map<String, GpuBuffer> uniformBuffers = ((PostPassAccessor) passes.getFirst()).getCustomUniforms();
        if (!uniformBuffers.containsKey(UNIFORM_BLOCK)) {
            return;
        }

        GpuBuffer ubo = gradingUBO.put(chain, uniformBuffers, UNIFORM_BLOCK);
        ColorGradingConfig snapshot = config;

        try {
            GpuBufferUtil.writeStd140(ubo, UBO_SIZE, b -> {
                b.putFloat(snapshot.saturation());
                b.putFloat(snapshot.vibrance());
                b.putFloat(snapshot.contrast());
                b.putFloat(snapshot.brightness());
                b.putFloat(snapshot.gamma());
                b.putFloat(snapshot.temperature());
                b.putFloat(snapshot.tint());
                b.putFloat(0.0f);
            });
        } catch (RuntimeException e) {
            if (gradingUBO.resetIfClosed(e)) {
                return;
            }
            throw e;
        }

        RenderTarget main = ClientRenderTargets.getMain(client);
        try {
            chain.process(main, frameAllocator);
        } catch (RuntimeException e) {
            if (gradingUBO.resetIfClosed(e)) {
                return;
            }
            throw e;
        }
    }

    private static PostChain loadChain(Minecraft client) {
        try {
            ShaderManager.CompilationCache cache =
                    ((ShaderManagerAccessor) client.getShaderManager()).getCompilationCache();
            if (cache == null) {
                return null;
            }

            PostChain chain = cache.getOrLoadPostChain(
                    Identifier.fromNamespaceAndPath("liquidbounce", CHAIN_NAME),
                    LevelTargetBundle.MAIN_TARGETS);

            loadErrorLogged = false;
            return chain;
        } catch (Exception e) {
            if (!loadErrorLogged) {
                loadErrorLogged = true;
                System.err.println("[Tsunami] Failed to load the colour grading shader: " + e.getMessage());
            }
            return null;
        }
    }
}
