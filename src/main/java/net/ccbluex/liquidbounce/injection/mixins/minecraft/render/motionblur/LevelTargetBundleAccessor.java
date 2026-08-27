package net.ccbluex.liquidbounce.injection.mixins.minecraft.render.motionblur;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import net.minecraft.client.renderer.LevelTargetBundle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelTargetBundle.class)
public interface LevelTargetBundleAccessor {

    @Accessor("main")
    ResourceHandle<RenderTarget> liquidbounce$getMain();

    @Accessor("main")
    void liquidbounce$setMain(ResourceHandle<RenderTarget> target);

    @Accessor("translucent")
    ResourceHandle<RenderTarget> liquidbounce$getTranslucent();

    @Accessor("translucent")
    void liquidbounce$setTranslucent(ResourceHandle<RenderTarget> target);

    @Accessor("itemEntity")
    ResourceHandle<RenderTarget> liquidbounce$getItemEntity();

    @Accessor("itemEntity")
    void liquidbounce$setItemEntity(ResourceHandle<RenderTarget> target);

    @Accessor("weather")
    ResourceHandle<RenderTarget> liquidbounce$getWeather();

    @Accessor("weather")
    void liquidbounce$setWeather(ResourceHandle<RenderTarget> target);

    @Accessor("particles")
    ResourceHandle<RenderTarget> liquidbounce$getParticles();

    @Accessor("particles")
    void liquidbounce$setParticles(ResourceHandle<RenderTarget> target);

    @Accessor("entityOutline")
    ResourceHandle<RenderTarget> liquidbounce$getEntityOutline();

    @Accessor("entityOutline")
    void liquidbounce$setEntityOutline(ResourceHandle<RenderTarget> target);
}