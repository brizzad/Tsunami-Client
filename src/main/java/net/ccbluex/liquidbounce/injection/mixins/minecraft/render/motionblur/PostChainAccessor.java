package net.ccbluex.liquidbounce.injection.mixins.minecraft.render.motionblur;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;

@Mixin(PostChain.class)
public interface PostChainAccessor {
    @Accessor List<PostPass> getPasses();
}