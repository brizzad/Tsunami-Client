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

package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.blaze3d.vertex.PoseStack
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState
import net.minecraft.client.renderer.state.level.CameraRenderState
import org.joml.Quaternionf
import kotlin.math.asin
import kotlin.math.atan2

/**
 * FlatItems
 *
 * Draws dropped items flat and facing you, the way the game did before 1.8.
 *
 * Vanilla spins every dropped stack on its vertical axis, so an item on the
 * ground spends half of each rotation edge-on and unreadable. Flat items were
 * the old behaviour, and the reason people still ask for them is not nostalgia:
 * a floor covered in drops is legible at a glance instead of a field of
 * flickering slivers.
 *
 * The 2D-items option from the approved feature list. It was recorded as
 * blocked on the grounds that no Fabric mod built for this version - that was
 * true when it was checked and is not any more, so the source was merged rather
 * than the feature written from scratch.
 *
 * Merged from beamingblue's Flat Items
 * (https://modrinth.com/mod/flat-items), MIT. The rendering is upstream's; this
 * class is the ClickGUI face and holds the billboard maths that used to live in
 * its settings enum.
 *
 * ## Scope
 *
 * Appearance only. Nothing about what an item is, where it lies, when it
 * despawns or whether you can pick it up changes, and with the module off the
 * vanilla rotation is applied untouched.
 *
 * @see net.ccbluex.liquidbounce.injection.mixins.minecraft.render.flatitems
 */
object ModuleFlatItems : ClientModule("FlatItems", ModuleCategories.RENDER) {

    /**
     * Flatten block models and other genuinely 3D items too.
     *
     * Off by default: a flattened block reads as a painted square and is harder
     * to tell from an item than the spinning cube is. Upstream defaults it off
     * for the same reason.
     */
    val affect3D by boolean("Affect3DModels", false)

    /**
     * Keep the sides of the model instead of stripping everything but the front
     * face. Leaves a shallow slab that still faces you - subtler, and closer to
     * modern vanilla than to 1.7.
     */
    val renderSides by boolean("RenderSides", false)

    /**
     * Scale a flattened 3D model up to match the size of a real 2D sprite, which
     * otherwise renders noticeably smaller beside it.
     */
    val enlarge3D by boolean("Enlarge3DModels", false)

    /** What a flattened item turns to face. */
    private val billboard by enumChoice("Billboard", Facing.PLAYER)

    enum class Facing(override val tag: String) : Tagged {
        /**
         * Face the camera's position, so an item stays square-on as you walk
         * around it. Correct from any angle, and the one to want.
         */
        PLAYER("Player"),

        /**
         * Face the screen plane instead. Every item on screen sits parallel,
         * which looks tidier in a straight line and wrong from the corner of
         * the eye.
         */
        SCREEN("Screen")
    }

    fun isActive() = running

    /**
     * Replaces vanilla's spin with a rotation that turns the model toward the
     * camera.
     *
     * [PLAYER][Facing.PLAYER] aims at the camera's actual position, so the yaw
     * comes from the horizontal offset and the pitch from the vertical one.
     * [SCREEN][Facing.SCREEN] reuses the camera's own orientation, which is one
     * multiply and no trigonometry.
     */
    fun faceCamera(pose: PoseStack, state: ItemEntityRenderState, camera: CameraRenderState) {
        when (billboard) {
            Facing.PLAYER -> {
                val toCamera = camera.pos.subtract(state.x, state.y, state.z).normalize()

                pose.mulPose(
                    Quaternionf().rotateYXZ(
                        atan2(toCamera.x, toCamera.z).toFloat(),
                        asin(-toCamera.y).toFloat(),
                        0f
                    )
                )
            }

            Facing.SCREEN -> pose.mulPose(camera.orientation)
        }
    }

}
