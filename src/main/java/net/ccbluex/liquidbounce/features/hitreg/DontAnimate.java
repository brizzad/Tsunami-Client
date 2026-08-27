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
 * The server's confirmation of a hit the client already showed.
 *
 * Without this the player sees the flash twice: once when they swing, once
 * when the confirmation lands a ping later. The marker tells the mixin to
 * apply everything the packet means except the animation, which already ran.
 */
public class DontAnimate extends DamageSource {

    public final DamageSource wrapped;

    public DontAnimate(DamageSource wrapped) {
        super(wrapped.typeHolder());
        this.wrapped = wrapped;
    }
}
