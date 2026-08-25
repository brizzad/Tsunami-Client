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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.EntityEquipmentChangeEvent;
import net.ccbluex.liquidbounce.event.events.EntityHealthUpdateEvent;
import net.ccbluex.liquidbounce.event.events.PlayerAfterJumpEvent;
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.*;
import net.ccbluex.liquidbounce.features.module.modules.render.DoRender;
import net.ccbluex.liquidbounce.features.module.modules.render.animations.ModuleAnimations;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.ccbluex.liquidbounce.features.module.modules.render.hitfx.ModuleHitFX;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection;
import net.ccbluex.liquidbounce.utils.client.SilentHotbar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends MixinEntity {

    @Shadow
    public boolean jumping;

    @Shadow
    public int noJumpDelay;

    @Shadow
    public abstract float getJumpPower();

    @Shadow
    public abstract void jumpFromGround();

    @Shadow
    public abstract boolean hasEffect(Holder<MobEffect> effect);

    @Shadow
    public abstract void tick();

    @Shadow
    public abstract void swing(InteractionHand hand, boolean sendToSwingingEntity);

    @Shadow
    public abstract void setHealth(float health);

    @Shadow
    public abstract boolean isFallFlying();

    @Shadow
    protected abstract boolean canGlide();

    @Shadow
    public abstract float getHealth();

    @Shadow
    public abstract float getMaxHealth();

    @ModifyReturnValue(method = "getMainHandItem", at = @At("RETURN"))
    private ItemStack applySilentHotbarForMainHand(ItemStack original) {
        var player = Minecraft.getInstance().player;
        if ((Object) this == player) {
            return player.getInventory().getNonEquipmentItems().get(SilentHotbar.INSTANCE.getServersideSlot());
        }

        return original;
    }



    @Unique
    private PlayerJumpEvent jumpEvent;

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void hookJumpEvent(CallbackInfo ci) {
        if (!liquid_bounce$isClientPlayer()) {
            return;
        }

        jumpEvent = EventManager.INSTANCE.callEvent(new PlayerJumpEvent(getJumpPower(), this.getYRot()));
        if (jumpEvent.isCancelled()) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getJumpPower()F"))
    private float hookJumpEvent(float original) {
        // Replaces ((Object) this) != MinecraftClient.getInstance().player
        if (jumpEvent == null) {
            return original;
        }

        return jumpEvent.getMotion();
    }

    @ModifyExpressionValue(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"))
    private float hookJumpYaw(float original) {
        // Replaces ((Object) this) != MinecraftClient.getInstance().player
        if (jumpEvent == null) {
            return original;
        }

        return jumpEvent.getYaw();
    }

    @Inject(method = "jumpFromGround", at = @At("RETURN"))
    private void hookAfterJumpEvent(CallbackInfo ci) {
        jumpEvent = null;

        if (!liquid_bounce$isClientPlayer()) {
            return;
        }

        EventManager.INSTANCE.callEvent(PlayerAfterJumpEvent.INSTANCE);
    }

    /**
     * Hook velocity rotation modification
     * <p>
     * Jump according to modified rotation. Prevents detection by movement sensitive anticheats.
     */
    @ModifyExpressionValue(method = "jumpFromGround", at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 hookFixRotation(Vec3 original) {
        var rotation = RotationManager.INSTANCE.getCurrentRotation();
        var rotationTarget = RotationManager.INSTANCE.getActiveRotationTarget();

        if (!liquid_bounce$isClientPlayer()) {
            return original;
        }

        if (rotationTarget == null || rotationTarget.getMovementCorrection() == MovementCorrection.OFF || rotation == null) {
            return original;
        }

        float yaw = rotation.yaw() * Mth.DEG_TO_RAD;

        return new Vec3(-Mth.sin(yaw) * 0.2F, 0.0, Mth.cos(yaw) * 0.2F);
    }



    @Unique
    private boolean previousElytra = false;


    /**
     * Gliding using modified-rotation
     */
    @ModifyExpressionValue(method = "updateFallFlyingMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getXRot()F"))
    private float hookModifyFallFlyingPitch(float original) {
        if (!liquid_bounce$isClientPlayer()) {
            return original;
        }

        var rotation = RotationManager.INSTANCE.getCurrentRotation();
        var rotationTarget = RotationManager.INSTANCE.getActiveRotationTarget();

        if (rotation == null || rotationTarget == null || rotationTarget.getMovementCorrection() == MovementCorrection.OFF) {
            return original;
        }

        return rotation.pitch();
    }

    @Inject(method = "spawnItemParticles", at = @At("HEAD"), cancellable = true)
    private void hookEatParticles(ItemStack itemStack, int count, CallbackInfo ci) {
        if (itemStack.getComponents().has(DataComponents.FOOD) && !ModuleAntiBlind.canRender(DoRender.EAT_PARTICLES)) {
            ci.cancel();
        }
    }

    /**
     * Gliding using modified-rotation
     */
    @ModifyExpressionValue(method = "updateFallFlyingMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 hookModifyFallFlyingRotationVector(Vec3 original) {
        if (!liquid_bounce$isClientPlayer()) {
            return original;
        }

        var rotation = RotationManager.INSTANCE.getCurrentRotation();
        var rotationTarget = RotationManager.INSTANCE.getActiveRotationTarget();

        if (rotation == null || rotationTarget == null || rotationTarget.getMovementCorrection() == MovementCorrection.OFF) {
            return original;
        }

        return rotation.directionVector();
    }

    @Unique
    private boolean previousIsGliding = false;


    @Inject(method = "setHealth", at = @At("HEAD"))
    private void hookSetHealth(float health, CallbackInfo callbackInfo) {
        var oldHealth = this.getHealth();
        var maxHealth = this.getMaxHealth();
        var newHealth = Math.clamp(health, 0.0F, maxHealth);

        if (oldHealth != newHealth) {
            EventManager.INSTANCE.callEvent(new EntityHealthUpdateEvent((LivingEntity) (Object) this, oldHealth, newHealth, maxHealth));
        }
    }

    @Inject(method = "setItemSlot", at = @At("HEAD"))
    private void hookEquipmentChange(EquipmentSlot slot, ItemStack itemStack, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new EntityEquipmentChangeEvent((LivingEntity) (Object) this, slot, itemStack));
    }

    @ModifyExpressionValue(method = "getCurrentSwingDuration", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/SwingAnimation;duration()I"), require = 0)
    private int hookSwingSpeed(int duration) {
        var animations = ModuleAnimations.INSTANCE;
        return animations.getRunning() && liquid_bounce$isClientPlayer() ? animations.getSwingDuration() : duration;
    }

    @ModifyExpressionValue(method = "handleDamageEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getHurtSound(Lnet/minecraft/world/damagesource/DamageSource;)Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent hookHitFxSound(SoundEvent original) {
        if (liquid_bounce$isClientPlayer() && ModuleHitFX.INSTANCE.getRunning()) {
            var hitFxSound = ModuleHitFX.INSTANCE.getSelfSound();
            if (hitFxSound != null) {
                return hitFxSound;
            }
        }

        return original;
    }

}
