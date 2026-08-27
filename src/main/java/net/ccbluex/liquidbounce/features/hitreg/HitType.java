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

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;

/**
 * The kinds of hit vanilla distinguishes, and the sounds each one makes.
 *
 * This is the part worth having taken from upstream rather than written here.
 * Vanilla does not play "a hit sound" - a knockback hit, a critical, a sweep
 * and an ordinary hit each play a different set, and the wrong set is more
 * jarring than no early sound at all. The sets and their order below are
 * upstream's.
 */
public enum HitType {

    /** Swung again before the cooldown allowed damage. Feedback only, no hit. */
    TOO_EARLY(SoundEvents.PLAYER_ATTACK_WEAK),

    /** Sprint hit: the target gets launched. */
    KNOCKBACK(SoundEvents.PLAYER_ATTACK_KNOCKBACK,
            SoundEvents.PLAYER_ATTACK_STRONG,
            SoundEvents.PLAYER_HURT),

    /** Falling hit, 1.5x damage. */
    CRITICAL(SoundEvents.PLAYER_ATTACK_CRIT,
            SoundEvents.PLAYER_HURT),

    /** Grounded sword swing that catches everything around the target. */
    SWEEP(SoundEvents.PLAYER_ATTACK_SWEEP,
            SoundEvents.PLAYER_HURT),

    /** An ordinary hit at full cooldown. */
    FULL_PICK(SoundEvents.PLAYER_ATTACK_STRONG,
            SoundEvents.PLAYER_HURT),

    /** An ordinary hit before the cooldown finished. */
    HALF_PICK(SoundEvents.PLAYER_HURT);

    private final List<SoundEvent> sounds;

    HitType(SoundEvent... sounds) {
        this.sounds = Arrays.asList(sounds);
    }

    /**
     * Plays this hit's sounds at a position, locally.
     *
     * {@code playSound} with the player as the exempt listener is the client's
     * own playback path - nothing is sent to the server, and no other player
     * hears any of it.
     */
    public void playSounds(Vec3 location, float volume) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return;
        }

        for (SoundEvent sound : sounds) {
            // The hurt sound depends on what was hit: a player, a zombie and an
            // iron golem all sound different, and using the player one for all
            // of them is immediately noticeable.
            if (sound.equals(SoundEvents.PLAYER_HURT)) {
                sound = hurtSoundOfTarget();
            }

            client.level.playSound(client.player, location.x, location.y, location.z,
                    sound, SoundSource.PLAYERS, volume, 1f);
        }
    }

    public SoundEvent mainSound() {
        return sounds.getFirst();
    }

    public static HitType of(Hit hit) {
        if (hit.tooEarlyForDamage) {
            return TOO_EARLY;
        }
        if (hit.shouldKnockback) {
            return KNOCKBACK;
        }
        if (hit.shouldCrit) {
            return CRITICAL;
        }
        if (hit.shouldSweep) {
            return SWEEP;
        }
        if (hit.shouldFullPick) {
            return FULL_PICK;
        }
        if (hit.shouldHalfPick) {
            return HALF_PICK;
        }
        return null;
    }

    private static SoundEvent hurtSoundOfTarget() {
        LivingEntity target = HitState.target;
        if (target == null) {
            return SoundEvents.PLAYER_HURT;
        }
        return target.getHurtSound(target.damageSources().generic());
    }
}
