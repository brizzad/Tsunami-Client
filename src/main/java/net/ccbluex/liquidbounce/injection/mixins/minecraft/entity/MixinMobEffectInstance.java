/*
 * This file is part of Tsunami, a fork of LiquidBounce
 * (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 * Copyright (c) 2026 Tsunami contributors
 *
 * Derived from Status Effect Bars (https://github.com/A5b84/status-effect-bars)
 * Copyright (c) A5b84, licensed under the GNU Lesser General Public License v3.
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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity;

import net.ccbluex.liquidbounce.features.effectbars.MobEffectInstanceDuck;
import net.minecraft.world.effect.MobEffectInstance;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Records how long an effect originally lasted, so a bar has a full to measure
 * against.
 *
 * All three hooks exist because an effect can gain time three different ways,
 * and missing any one of them leaves a bar that reads past full or snaps
 * backwards: created outright, copied from another instance, or taken over by
 * a stronger one.
 */
@Mixin(MobEffectInstance.class)
public abstract class MixinMobEffectInstance implements MobEffectInstanceDuck {

    @Unique
    private int tsunami$maxDuration;

    @Shadow
    private int duration;

    @Inject(
        method = "<init>(Lnet/minecraft/core/Holder;IIZZZLnet/minecraft/world/effect/MobEffectInstance;)V",
        at = @At("RETURN")
    )
    private void hookInit(CallbackInfo ci) {
        tsunami$maxDuration = duration;
    }

    @Inject(method = "setDetailsFrom", at = @At("RETURN"))
    private void hookCopyFrom(MobEffectInstance copy, CallbackInfo ci) {
        tsunami$maxDuration = ((MobEffectInstanceDuck) copy).tsunami$getMaxDuration();
    }

    @Inject(
        method = "update",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/effect/MobEffectInstance;duration:I",
            opcode = Opcodes.PUTFIELD
        )
    )
    private void hookUpgrade(MobEffectInstance takeOver, CallbackInfoReturnable<Boolean> cir) {
        tsunami$maxDuration = ((MobEffectInstanceDuck) takeOver).tsunami$getMaxDuration();
    }

    @Override
    public int tsunami$getMaxDuration() {
        return tsunami$maxDuration;
    }

    @Override
    public void tsunami$setMaxDuration(int maxDuration) {
        this.tsunami$maxDuration = maxDuration;
    }
}
