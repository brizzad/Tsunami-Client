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

import net.minecraft.world.damagesource.DamageSource;

/**
 * A hit the client is applying itself, ahead of the server.
 *
 * Wrapping the real source rather than inventing one keeps the damage type
 * intact for anything that reads it, while giving the mixin a marker it can
 * match on to run the flash and nothing else - no health change, no death, no
 * knockback. Those remain entirely the server's to decide.
 */
public class OnlyAnimate extends DamageSource {

    public final DamageSource wrapped;

    public OnlyAnimate(DamageSource wrapped) {
        super(wrapped.typeHolder());
        this.wrapped = wrapped;
    }
}
