package net.ccbluex.liquidbounce.injection.mixins.minecraft.render.motionblur;

import net.ccbluex.liquidbounce.render.motionblur.MotionBlurEngine;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRendererMotionBlur {

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void liquidbounce$afterRenderLevel(DeltaTracker deltaTracker, CallbackInfo ci) {
        MotionBlurEngine.applyDeferredTemporalBlur();
        MotionBlurEngine.clearFrameAllocator();
    }
}
