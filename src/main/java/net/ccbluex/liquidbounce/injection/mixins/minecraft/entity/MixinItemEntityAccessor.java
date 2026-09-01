/*
 * This file is part of Tsunami, a fork of LiquidBounce
 * (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 * Copyright (c) 2026 Tsunami contributors
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

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the despawn clock of a dropped item.
 *
 * {@code ItemEntity.tick} increments {@code age} outside of any {@code isClientSide}
 * guard, so the client's copy counts along with the server's rather than sitting at
 * zero. It starts from when this client first saw the entity, which is the one
 * caveat worth knowing: an item that was already lying there when you arrived reads
 * younger than it is.
 *
 * @see net.ccbluex.liquidbounce.features.module.modules.render.ModuleItemDespawn
 */
@Mixin(ItemEntity.class)
public interface MixinItemEntityAccessor {
    @Accessor("age")
    int getAge();
}
