package net.ccbluex.liquidbounce.injection.mixins.minecraft.render.motionblur;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Invoker("mainRenderTarget")
    RenderTarget liquidbounce$mainRenderTarget();
}