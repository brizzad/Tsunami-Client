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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.render.flatitems;

import net.ccbluex.liquidbounce.interfaces.ItemStackRenderStateAddition;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

/**
 * Reads the baked quads back out of a render state.
 *
 * {@code layers} is private and there is no getter, so this is the access the
 * flattening needs to decide whether a model is flat already.
 *
 * @see net.ccbluex.liquidbounce.features.module.modules.render.ModuleFlatItems
 */
@Mixin(ItemStackRenderState.class)
public class MixinItemStackRenderState implements ItemStackRenderStateAddition {

    @Shadow
    private ItemStackRenderState.LayerRenderState[] layers;

    @Override
    @SuppressWarnings("unchecked")
    public List<BakedQuad>[] liquid_bounce$quads() {
        var lists = new List[this.layers.length];

        for (int i = 0; i < lists.length; i++) {
            lists[i] = this.layers[i].prepareQuadList();
        }

        return lists;
    }

}
