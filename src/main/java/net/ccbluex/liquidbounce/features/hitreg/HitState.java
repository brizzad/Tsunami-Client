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
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The state a hit has to be judged against, and the small helpers that read it.
 *
 * Trimmed from upstream's {@code Hitreg}, which also carried fight analytics,
 * ping readouts and alert bookkeeping. What survives is what {@link Hit} needs
 * to classify a swing, plus enough history to know whether the last one was
 * ghosted.
 */
public final class HitState {

    private HitState() {
    }

    /** How long the server gets to confirm a hit before it counts as ghosted. */
    private static final long CONFIRMATION_WINDOW_MS = 500L;

    /** A swing under this gap cannot have dealt damage, so only feedback is due. */
    private static final long DAMAGE_COOLDOWN_MS = 475L;

    public static LivingEntity target;
    public static int lastTarget;
    public static int tick;
    public static long lastAttack;
    public static int lastAttackTick;

    public static boolean fighting;
    public static boolean withinFight;
    public static boolean bothAlive;
    public static boolean targetHasShield;
    public static boolean targetIsBlocking;

    public static boolean newTarget = true;
    public static boolean hitByAnother;
    public static boolean hitWasFarFromPrevious;
    public static boolean wasGhosted;

    /** Whether the client, not the server, drew the last hit. */
    public static boolean lastHitHandled;

    public static Vec3 lastAttackLocation = Vec3.ZERO;

    // Sprint state. A hit briefly clears the client's sprint flag while W is
    // still held, so the flag on its own is not a sprint reset.
    public static boolean sprintIsReset = true;
    private static boolean wasMovingForward;
    private static boolean wasCrouching;
    private static boolean usedItem;
    private static boolean lastHitWasSpecial;

    /** Timestamps of hits awaiting the server's verdict, oldest first. */
    private static final Deque<Long> pending = new ArrayDeque<>();

    public static boolean tooEarlyForDamage(long sinceLastHit) {
        return sinceLastHit < DAMAGE_COOLDOWN_MS;
    }

    /**
     * Per-tick bookkeeping. Only the sprint tracking is subtle.
     *
     * Sprint is what separates a knockback hit from an ordinary one, and the
     * client's own sprint flag is not enough to tell: it flickers off on every
     * hit while the key stays down. A real sprint reset is a fresh forward
     * input, standing up out of a crouch, or an item use just after the swing.
     */
    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        tick++;

        boolean movingForward = client.options.keyUp.isDown();
        if (movingForward && !wasMovingForward) {
            sprintIsReset = true;
            usedItem = true;
        }
        wasMovingForward = movingForward;

        boolean crouching = client.player.isCrouching();
        if (!crouching && wasCrouching) {
            sprintIsReset = true;
            usedItem = true;
        }
        wasCrouching = crouching;

        // Using an item within a tick or two of the swing keeps the sprint;
        // otherwise a special hit is what ended it. Upstream braced this so the
        // else binds to the inner branch, and that behaviour is kept here.
        if (tick - lastAttackTick <= 2) {
            if (client.player.isUsingItem()) {
                usedItem = true;
            } else if (sprintIsReset && !usedItem && lastHitWasSpecial) {
                sprintIsReset = false;
            }
        }

        updateFightState();
        expirePending();
    }

    public static void updateFightState() {
        Minecraft client = Minecraft.getInstance();

        bothAlive = client.player != null && target != null
                && client.player.isAlive() && target.isAlive()
                && !client.player.isSpectator() && !target.isSpectator();

        targetHasShield = target != null && target.isHolding(Items.SHIELD);
        // isUsingItem is also true while eating or drinking; isBlocking is the
        // one that means the shield is actually up.
        targetIsBlocking = targetHasShield && target.isBlocking();

        withinFight = bothAlive && distanceToTarget() <= 30;

        if (!withinFight) {
            fighting = false;
            pending.clear();
        }
    }

    /** Records the swing a hit is about to be predicted for. */
    public static void recordAttack(Hit hit, long now) {
        Minecraft client = Minecraft.getInstance();

        hitWasFarFromPrevious =
                lastAttackLocation.distanceToSqr(basePosition(client.player)) >= 2500;

        fighting = true;
        hitByAnother = hit.wasHitByAnother;
        newTarget = hit.wasNewTarget;
        lastAttackLocation = basePosition(client.player);
        lastAttack = now;
        lastTarget = hit.target.getId();
        lastAttackTick = tick;
        lastHitWasSpecial = !hit.tooEarlyForSpecial;
        usedItem = false;

        updateFightState();
    }

    /** A hit was drawn; the server now owes us a confirmation. */
    public static void expectConfirmation(long timestamp) {
        pending.addLast(timestamp);
    }

    /**
     * The server confirmed a hit we already drew.
     *
     * Called from the packet hook. The oldest pending hit is the one being
     * answered, so it is the one retired.
     */
    public static void confirmed() {
        if (!pending.isEmpty()) {
            pending.pollFirst();
        }
        wasGhosted = false;
    }

    /** Anything past the window without a confirmation was a ghost. */
    private static void expirePending() {
        long cutoff = System.currentTimeMillis() - CONFIRMATION_WINDOW_MS;
        while (!pending.isEmpty() && pending.peekFirst() < cutoff) {
            pending.pollFirst();
            wasGhosted = true;
        }
    }

    public static void reset() {
        target = null;
        fighting = false;
        withinFight = false;
        wasGhosted = false;
        lastHitHandled = false;
        lastAttackLocation = Vec3.ZERO;
        pending.clear();
    }

    // --- helpers inlined from upstream's MultiVersion -----------------------

    public static boolean isOnGround(Entity entity) {
        return entity.onGround();
    }

    /**
     * Vanilla suppresses the sweep when the player is moving faster than a
     * walk. 1.21.2 changed this from a walk-distance check to actual movement
     * against 2.5x speed; this is the current form.
     */
    public static boolean isMovingFast() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return false;
        }
        return client.player.getKnownMovement().horizontalDistanceSqr()
                >= Mth.square(client.player.getSpeed() * 2.5f);
    }

    public static boolean hasSharpness() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !client.player.getMainHandItem().isEnchanted()) {
            return false;
        }

        for (Holder<Enchantment> enchantment : client.player.getMainHandItem().getEnchantments().keySet()) {
            if (enchantment.getRegisteredName().equalsIgnoreCase("minecraft:sharpness")) {
                return true;
            }
        }
        return false;
    }

    public static Vec3 lerpedPosition(Entity entity) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || entity == null) {
            return Vec3.ZERO;
        }
        return entity.getPosition(client.getDeltaTracker().getGameTimeDeltaPartialTick(true));
    }

    public static Vec3 basePosition(Entity entity) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || entity == null) {
            return Vec3.ZERO;
        }
        return entity.position();
    }

    public static double distanceToTarget() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || target == null) {
            return Double.MAX_VALUE;
        }

        Vec3 a = basePosition(client.player);
        Vec3 b = basePosition(target);
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Crit and sharpness particles, spawned locally around the target. */
    public static void playParticles(SimpleParticleType particle, Entity entity) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || entity == null) {
            return;
        }

        Vec3 position = lerpedPosition(entity);
        for (int i = 0; i < 20; i++) {
            double x = Math.random() - 0.5;
            double y = Math.random() - 0.5;
            double z = Math.random() - 0.5;
            Vec3 direction = new Vec3(x, y, z).normalize();

            client.level.addParticle(particle,
                    position.x + x,
                    position.y + (entity.getBbHeight() / 2) + y,
                    position.z + z,
                    direction.x * 0.5,
                    direction.y * 0.5,
                    direction.z * 0.5);
        }
    }

    public static SimpleParticleType critParticle() {
        return ParticleTypes.CRIT;
    }

    public static SimpleParticleType sharpnessParticle() {
        return ParticleTypes.ENCHANTED_HIT;
    }
}
