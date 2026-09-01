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

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFlatItems;
import net.ccbluex.liquidbounce.interfaces.ItemStackRenderStateAddition;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * Draws dropped items flat and facing you, the way the game did before 1.8.
 *
 * <p>Vanilla spins a dropped stack on its vertical axis. This replaces that one
 * rotation - the {@code mulPose} call that applies it - with one that turns the
 * model to face the camera instead, and optionally strips every quad that is not
 * the front face so a sprite reads as a flat card rather than an extruded slab.
 *
 * <p>The redirect is the whole mechanism: nothing else about item rendering,
 * pickup, physics or collision is touched, and with the module off the original
 * rotation is applied unchanged.
 *
 * @see net.ccbluex.liquidbounce.features.module.modules.render.ModuleFlatItems
 * @see net.ccbluex.liquidbounce.injection.mixins.minecraft.render.flatitems
 */
@Mixin(ItemEntityRenderer.class)
public class MixinItemEntityRenderer {

    @Redirect(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;"
            + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V"
        )
    )
    private void flattenInsteadOfSpinning(PoseStack pose, Quaternionfc rotation,
                                          @Local(argsOnly = true) ItemEntityRenderState state,
                                          @Local(argsOnly = true) CameraRenderState camera) {
        var module = ModuleFlatItems.INSTANCE;

        if (!module.isActive() || state.count == 0) {
            pose.mulPose(rotation);
            return;
        }

        var lists = ((ItemStackRenderStateAddition) state.item).liquid_bounce$quads();

        // A model counts as 3D the moment any quad has real depth. The first such quad
        // settles it, so this returns rather than scanning the rest.
        for (var quads : lists) {
            for (var quad : quads) {
                if (Mth.abs(quad.position0().z() - quad.position2().z()) <= FLATNESS_EPSILON) {
                    continue;
                }

                if (!module.getAffect3D()) {
                    pose.mulPose(rotation);
                    return;
                }

                module.faceCamera(pose, state, camera);

                if (!module.getRenderSides()) {
                    keepFrontFaceOnly(lists);
                }

                if (module.getEnlarge3D()) {
                    pose.scale(2f, 2f, 2f);
                }

                return;
            }
        }

        module.faceCamera(pose, state, camera);

        if (!module.getRenderSides()) {
            keepFrontFaceOnly(lists);
        }
    }

    /**
     * A sprite is extruded to one sixteenth of a block, so anything thicker than that
     * is a real model rather than a flat icon.
     */
    @Unique
    private static final float FLATNESS_EPSILON = 0.0625f;

    /**
     * Drops every quad that is not the south face, which is the one pointing at the
     * camera once {@code faceCamera} has turned the model.
     *
     * <p>The surviving quads are rebuilt rather than kept, because the list is reused
     * by the renderer and removing in place is what keeps the iteration honest.
     */
    @Unique
    private static void keepFrontFaceOnly(List<BakedQuad>[] quadLists) {
        for (var quads : quadLists) {
            var it = quads.listIterator();

            while (it.hasNext()) {
                var quad = it.next();
                it.remove();

                if (quad.direction() == Direction.SOUTH) {
                    it.add(new BakedQuad(quad.position0(), quad.position1(), quad.position2(),
                        quad.position3(), quad.packedUV0(), quad.packedUV1(), quad.packedUV2(),
                        quad.packedUV3(), quad.direction(), quad.materialInfo()));
                }
            }
        }
    }

}
