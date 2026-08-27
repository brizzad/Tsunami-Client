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
 * GPL as part of Tsunami. See EffectBarRenderer for what was taken.
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

/**
 * Remembers how long an effect originally lasted.
 *
 * Vanilla only tracks the time an effect has left, which is enough to print a
 * number and not enough to draw a bar: a bar needs to know what full looks
 * like. The mixin on {@code MobEffectInstance} records the duration at the
 * moment the effect is created and keeps it through refreshes and upgrades.
 */
public interface MobEffectInstanceDuck {

    int tsunami$getMaxDuration();

    void tsunami$setMaxDuration(int maxDuration);
}
