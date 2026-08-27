package net.ccbluex.liquidbounce.injection.mixins.minecraft.render.motionblur;

import net.ccbluex.liquidbounce.render.motionblur.MotionBlurEngine;
import net.ccbluex.liquidbounce.render.motionblur.MotionBlurConfig;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 800)
public class MixinLevelRendererMotionBlur {

    @Unique private final Matrix4f prevModelView     = new Matrix4f();
    @Unique private final Matrix4f prevProjection    = new Matrix4f();
    @Unique private final Matrix4f scratchModelView  = new Matrix4f();
    @Unique private final Matrix4f scratchProjection = new Matrix4f();
    @Unique private double prevCamX, prevCamY, prevCamZ;
    @Unique private boolean previousFrameReady = false;

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(
            GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker,
            boolean renderOutline, CameraRenderState cameraState,
            Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog,
            Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {

        MotionBlurConfig config = MotionBlurConfig.get();
        boolean blurActive = config.isEnabled() && config.getEffectiveMotionBlurStrength() != 0.0f;
        boolean needsVelocityState = blurActive && config.usesVelocityBlur();

        double cx = cameraState.pos.x();
        double cy = cameraState.pos.y();
        double cz = cameraState.pos.z();

        if (!blurActive) {
            MotionBlurEngine.clearFrameAllocator();
            liquidbounce$rememberCurrentFrameState(modelViewMatrix, cameraState.projectionMatrix, cx, cy, cz);
            return;
        }

        MotionBlurEngine.captureAllocator(resourceAllocator);
        MotionBlurEngine.beginFrame();

        if (!needsVelocityState) {
            liquidbounce$rememberCurrentFrameState(modelViewMatrix, cameraState.projectionMatrix, cx, cy, cz);
            return;
        }

        scratchModelView.set(modelViewMatrix);
        scratchProjection.set(cameraState.projectionMatrix);

        if (!previousFrameReady) {
            MotionBlurEngine.setFrameMotionBlur(
                    scratchModelView, scratchModelView,
                    scratchProjection, scratchProjection,
                    0.0f, 0.0f, 0.0f);

            liquidbounce$rememberCurrentFrameState(scratchModelView, scratchProjection, cx, cy, cz);
            return;
        }

        float dx = (float)(cx - prevCamX);
        float dy = (float)(cy - prevCamY);
        float dz = (float)(cz - prevCamZ);

        MotionBlurEngine.setFrameMotionBlur(
                scratchModelView, prevModelView,
                scratchProjection, prevProjection,
                dx, dy, dz);

        liquidbounce$rememberCurrentFrameState(scratchModelView, scratchProjection, cx, cy, cz);
    }

    @Unique
    private void liquidbounce$rememberCurrentFrameState(Matrix4fc modelViewMatrix, Matrix4fc projectionMatrix, double cx, double cy, double cz) {
        prevModelView.set(modelViewMatrix);
        prevProjection.set(projectionMatrix);
        prevCamX = cx;
        prevCamY = cy;
        prevCamZ = cz;
        previousFrameReady = true;
    }

    @Inject(method = "submitEntities", at = @At("HEAD"), require = 0)
    private void liquidbounce$beforeSubmitEntities(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector output, CallbackInfo ci) {
        MotionBlurEngine.applyPreEntityVelocityOnly(liquidbounce$shouldUseSpecialSingleBlur());
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void liquidbounce$onRenderLevelTail(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
        MotionBlurConfig config = MotionBlurConfig.get();
        boolean specialSingleBlur = liquidbounce$shouldUseSpecialSingleBlur();

        if (config.blurAlgorithm == MotionBlurConfig.BlurAlgorithm.HYBRID_BLENDING) {
            if (!specialSingleBlur) {
                MotionBlurEngine.applyPostRenderVelocityOnly();
            }
        } else if (config.blurAlgorithm == MotionBlurConfig.BlurAlgorithm.VELOCITY_BASED && !specialSingleBlur) {
            MotionBlurEngine.applyPostRenderVelocityOnly();
        }
    }

    @Unique
    private boolean liquidbounce$shouldUseSpecialSingleBlur() {
        Minecraft client = Minecraft.getInstance();
        if (client.options.getCameraType() != CameraType.FIRST_PERSON) return true;
        return client.player != null && client.player.isPassenger();
    }
}
