/*
 * This file is part of Tsunami, a fork of LiquidBounce
 * (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 * Copyright (c) 2026 Tsunami contributors
 *
 * Portions derived from BetterHitreg (https://github.com/jasspir/BetterHitreg)
 * Copyright (c) Jass, licensed under the Apache License, Version 2.0.
 * See package-info.java for what was taken and what was left behind.
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
package net.ccbluex.liquidbounce.features.hitreg;

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleBetterHitreg;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * One swing, and everything about the moment it was made.
 *
 * The state is captured at swing time rather than read later because most of
 * it has changed by the time the server answers: the player has landed, let go
 * of sprint, or swapped items. Judging the hit against the world as it is 100ms
 * later produces the wrong sound, which is the failure mode this whole class
 * exists to avoid.
 *
 * The classification in {@link #load()} is upstream's, and is a reimplementation
 * of vanilla's own attack rules. Do not simplify it without checking against
 * {@code Player.attack} - each clause is a case vanilla actually distinguishes.
 */
public class Hit {

    /** Vanilla's hurt flash length, in ticks. */
    public static final int HURT_DURATION = 10;

    public LivingEntity target;
    public float cooldown;

    // State at the moment of the swing.
    public boolean tooEarlyForDamage;
    public boolean tooEarlyForSpecial;
    public boolean hadShield;
    public boolean wasBlocked;
    public boolean wasSprinting;
    public boolean wasMovingFast;
    public boolean wasMovingForward;
    public boolean wasFalling;
    public boolean wasOnGround;
    public boolean wasClimbing;
    public boolean wasTouchingWater;
    public boolean wasInVehicle;
    public boolean wasBlind;
    public boolean wasInvisible;
    public boolean wasHoldingSword;
    public boolean swordHadSharpness;
    public boolean sprintWasReset;
    public boolean wasNewTarget;
    public boolean wasHitByAnother;

    // What the classification concluded.
    public boolean shouldKnockback;
    public boolean shouldCrit;
    public boolean shouldSweep;
    public boolean shouldPick;
    public boolean shouldFullPick;
    public boolean shouldHalfPick;

    public SoundEvent expectedSound;
    public HitType type;
    public long timestamp;

    public Hit() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Captures the swing. Returns null when the target is not something a hit
     * can be predicted for.
     */
    public static Hit capture(Entity attacked) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null
                || !(attacked instanceof LivingEntity living)
                || attacked instanceof ArmorStand
                || !attacked.isAlive()
                || attacked.isInvulnerable()) {
            return null;
        }

        HitState.target = living;

        long now = System.currentTimeMillis();
        long sinceLastHit = now - HitState.lastAttack;

        Hit hit = new Hit();
        hit.target = living;
        hit.cooldown = client.player.getAttackStrengthScale(0.5f);
        hit.tooEarlyForDamage = HitState.tooEarlyForDamage(sinceLastHit);
        hit.tooEarlyForSpecial = hit.cooldown <= 0.9f;
        hit.hadShield = living.isHolding(Items.SHIELD);
        hit.wasBlocked = living.isBlocking();
        hit.wasSprinting = client.player.isSprinting();
        hit.wasFalling = client.player.fallDistance > 0;
        hit.wasOnGround = HitState.isOnGround(client.player);
        hit.wasClimbing = client.player.onClimbable();
        hit.wasTouchingWater = client.player.isInWater();
        hit.wasInVehicle = client.player.isPassenger();
        hit.wasBlind = client.player.hasEffect(MobEffects.BLINDNESS);
        hit.wasHoldingSword = client.player.getMainHandItem().is(ItemTags.SWORDS);
        hit.wasMovingFast = HitState.isMovingFast();
        hit.wasMovingForward = client.options.keyUp.isDown();
        hit.swordHadSharpness = HitState.hasSharpness();
        hit.sprintWasReset = HitState.sprintIsReset;
        hit.wasNewTarget = HitState.lastTarget != attacked.getId();
        hit.wasInvisible = attacked.isInvisible();
        // Their invulnerability running while we have not swung in a second
        // means somebody else is hitting them, so our prediction would be
        // competing with theirs.
        hit.wasHitByAnother = attacked.invulnerableTime > 10 && sinceLastHit >= 1000;

        if (!hit.tooEarlyForDamage) {
            HitState.recordAttack(hit, now);
        }

        return hit;
    }

    /**
     * Works out what kind of hit this was, and schedules the feedback.
     *
     * Every clause mirrors vanilla:
     * knockback wins over everything, a crit needs an unassisted fall, a sweep
     * needs a sword and a standing start, and what is left is an ordinary hit
     * split by whether the cooldown had finished.
     */
    public void load() {
        shouldKnockback = !tooEarlyForSpecial && wasSprinting && sprintWasReset;

        shouldCrit = !tooEarlyForSpecial && !shouldKnockback && wasFalling && !wasOnGround
                && !wasClimbing && !wasTouchingWater && !wasInVehicle && !wasBlind;

        // 1.21.2 added the forward-movement clause; before that it was only the
        // speed check.
        shouldSweep = !tooEarlyForSpecial && !shouldKnockback && wasHoldingSword
                && wasOnGround && !wasMovingFast && !wasMovingForward;

        shouldPick = !shouldKnockback && !shouldCrit && !shouldSweep;
        shouldFullPick = !tooEarlyForSpecial && shouldPick;
        shouldHalfPick = !shouldFullPick && shouldPick;

        type = HitType.of(this);
        if (type == null) {
            return;
        }
        expectedSound = type.mainSound();

        // Decided once, here. The target may raise a shield or leave range
        // before the feedback runs, and a hit that was predictable at swing time
        // should still be drawn the way it was judged.
        boolean handled = shouldHandle();

        if (!tooEarlyForDamage) {
            HitState.lastHitHandled = handled;
            if (handled) {
                HitState.expectConfirmation(timestamp);
            }
        }

        if (handled) {
            HitScheduler.schedule(ModuleBetterHitreg.INSTANCE.getDelay(), this::run);
        }
    }

    /**
     * Whether the client should draw this hit rather than wait for the server.
     *
     * The exclusions are all cases where the prediction would probably be
     * wrong: a raised shield eats the hit, an unfamiliar target has no history
     * to judge against, and a target somebody else is hitting will be animating
     * for reasons that are not us.
     */
    private boolean shouldHandle() {
        ModuleBetterHitreg module = ModuleBetterHitreg.INSTANCE;

        if (!module.getRunning()) {
            return false;
        }
        if (!HitState.withinFight || HitState.targetIsBlocking) {
            return false;
        }
        if (module.getSafeRegsOnly()
                && (HitState.newTarget || HitState.wasGhosted
                    || HitState.hitByAnother || HitState.hitWasFarFromPrevious)) {
            return false;
        }
        return !module.getIgnoreShieldHolders() || !HitState.targetHasShield;
    }

    /** Applies the feedback: the flash, the sound, the particles. */
    public void run() {
        if (target == null || type == null) {
            return;
        }

        ModuleBetterHitreg module = ModuleBetterHitreg.INSTANCE;

        if (module.getAnimation() && !tooEarlyForDamage) {
            // The marker source runs the flash and nothing else. Health,
            // knockback and death stay the server's to decide.
            target.handleDamageEvent(new OnlyAnimate(target.damageSources().generic()));
        }

        if (module.getSound()) {
            Vec3 location = HitState.lerpedPosition(target);
            float volume = module.getVolume();

            if (module.getLegacySounds()) {
                // 1.8 had no attack sounds at all, only the hurt sound.
                if (!tooEarlyForDamage) {
                    HitType.HALF_PICK.playSounds(location, volume);
                }
            } else {
                type.playSounds(location, volume);
            }
        }

        if (module.getParticles()) {
            if (shouldCrit) {
                HitState.playParticles(HitState.critParticle(), target);
            }
            if (swordHadSharpness || module.getSharpnessParticlesAlways()) {
                HitState.playParticles(HitState.sharpnessParticle(), target);
            }
        }
    }
}
