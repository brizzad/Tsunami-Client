package net.ccbluex.liquidbounce.injection.mixins.minecraft.render.motionblur;
import net.minecraft.client.renderer.ShaderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShaderManager.class)
public interface ShaderManagerAccessor {
    @Accessor ShaderManager.CompilationCache getCompilationCache();
}