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

package net.ccbluex.liquidbounce.interfaces;

import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Addition to {@link net.minecraft.client.renderer.item.ItemStackRenderState}.
 *
 * <p>Exposes the baked quads of every render layer, which is the only way to tell a
 * flat item model from a genuine 3D one: a sprite's quads all sit in the same plane,
 * a block's do not.
 *
 * @see net.ccbluex.liquidbounce.features.module.modules.render.ModuleFlatItems
 */
@NullMarked
public interface ItemStackRenderStateAddition {

    List<BakedQuad>[] liquid_bounce$quads();

}
