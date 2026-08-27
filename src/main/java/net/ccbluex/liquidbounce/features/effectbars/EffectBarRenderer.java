/*
 * This file is part of Tsunami, a fork of LiquidBounce
 * (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 * Copyright (c) 2026 Tsunami contributors
 *
 * Derived from Status Effect Bars (https://github.com/A5b84/status-effect-bars)
 * Copyright (c) A5b84, licensed under the GNU Lesser General Public License v3.
 * LGPL-3.0 permits use in a GPL-3.0 work; this file is distributed under the
 * GPL as part of Tsunami.
 *
 * Taken: the depletion-bar geometry and the rules for when a bar is worth
 * drawing at all. Left behind: upstream configuration screen, its Mod Menu
 * integration, and the vertical/reversed layout permutations - Tsunami exposes
 * the two that matter through the ClickGUI instead.
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
package net.ccbluex.liquidbounce.features.effectbars;

import net.ccbluex.liquidbounce.features.module.modules.render.ModulePotionTimers;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.Nullable;

/**
 * Draws a depletion bar across the bottom of a status effect icon.
 *
 * Vanilla shows the remaining time as text next to the icon, which is precise
 * and slow to read. A bar is the opposite: you cannot tell 8 seconds from 9,
 * but you can tell "nearly gone" at a glance mid-fight, which is the only
 * question being asked at the time.
 */
public final class EffectBarRenderer {

    private EffectBarRenderer() {
    }

    /** The size of a status effect icon on the HUD. */
    public static final int ICON_SIZE = 24;

    /** The size of a status effect icon in the inventory screen. */
    public static final int INVENTORY_ICON_SIZE = 32;

    public static void render(
            GuiGraphicsExtractor graphics,
            @Nullable DeltaTracker deltaTracker,
            MobEffectInstance effect,
            int x,
            int y,
            int size) {

        ModulePotionTimers module = ModulePotionTimers.INSTANCE;
        if (!module.getRunning()) {
            return;
        }

        // An effect measured in minutes has nothing useful to show as a bar -
        // it sits at full for the entire time you are looking at it.
        if (effect.isInfiniteDuration() || effect.getDuration() > module.getMaxDurationTicks()) {
            return;
        }

        MobEffectInstanceDuck duck = (MobEffectInstanceDuck) effect;
        int maxDuration = duck.tsunami$getMaxDuration();
        if (maxDuration <= 0) {
            return;
        }

        // A beacon refreshes its effects constantly, so a bar for one that was
        // just applied would be permanently full and permanently distracting.
        int age = maxDuration - effect.getDuration();
        if (effect.isAmbient() && age < module.getMinAmbientAgeTicks()) {
            return;
        }

        float tickDelta = deltaTracker != null
                ? deltaTracker.getGameTimeDeltaPartialTick(false)
                : 0f;

        float progress = (effect.getDuration() - tickDelta) / (float) maxDuration;
        progress = Mth.clamp(progress, 0f, 1f);

        int thickness = module.getThickness();
        int startX = x + module.getPadding();
        int endX = x + size - module.getPadding();
        int middleX = Mth.lerpInt(progress, startX, endX);

        int topY = y + size - thickness;
        int bottomY = y + size;

        graphics.fill(startX, topY, middleX, bottomY, colorFor(effect));
        graphics.fill(middleX, topY, endX, bottomY, module.getBackgroundColor().argb());
    }

    /**
     * Bar colour.
     *
     * Following the effect's own colour keeps the bar tied to the icon it sits
     * under, which matters when four of them are stacked; a single flat colour
     * turns them into one block of noise.
     */
    private static int colorFor(MobEffectInstance effect) {
        ModulePotionTimers module = ModulePotionTimers.INSTANCE;

        if (!module.getUseEffectColor()) {
            return module.getBarColor().argb();
        }

        // The effect colour carries no alpha of its own, so the configured
        // alpha is kept and only the hue comes from the effect.
        int rgb = effect.getEffect().value().getColor() & 0xFFFFFF;
        int alpha = module.getBarColor().argb() & 0xFF000000;
        return alpha | rgb;
    }
}
