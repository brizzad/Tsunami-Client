/*
 * This file is part of Tsunami, a fork of LiquidBounce
 * (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 * Copyright (c) 2026 Tsunami contributors
 *
 * The feature set and rendering behaviour here are ported from Combat Hitboxes
 * (https://github.com/sootysplash/combat-hitboxes) by sootysplash, licensed
 * under the Apache License, Version 2.0. See the class documentation for why
 * this is a port onto a different render path rather than a source merge.
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

import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawLinesWithWidth
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3f
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.FireworkRocketEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3

/**
 * Hitboxes
 *
 * Draws the collision box the server actually resolves hits against, plus the
 * two things a plain box does not tell you: which entity your crosshair is on,
 * and where its eyes are.
 *
 * ## Ported from Combat Hitboxes, not merged from it
 *
 * The feature set, the defaults and the rendering behaviour are sootysplash's
 * [Combat Hitboxes](https://github.com/sootysplash/combat-hitboxes),
 * Apache-2.0. Its source could not be taken as-is: that mod works by cancelling
 * vanilla's own `EntityHitboxDebugRenderer.showHitboxes` and drawing its own
 * gizmos in place of it, so it only draws while F3+B debug rendering is on and
 * it inherits whatever gating vanilla puts on that.
 *
 * A ClickGUI module that silently does nothing until you press a debug key is
 * not a module, so the behaviour was ported onto the client's own world
 * renderer instead. Everything that mod configures is configurable here: the
 * per-component line widths, the distance falloff, the outline pass, hurt and
 * target colouring, the eye-height band and the look vector.
 *
 * This draws only. No box is changed, so it cannot change whether a hit lands.
 */
object ModuleHitboxes : ClientModule("Hitboxes", ModuleCategories.RENDER) {

    private val players by boolean("Players", true)
    private val mobs by boolean("Mobs", true)
    private val items by boolean("Items", false)
    private val fireworks by boolean("Fireworks", false)

    /**
     * The box of whatever the crosshair is on, drawn in its own colour.
     *
     * This is the part that matters in a fight and the part a plain box does
     * not do: which of the three players in front of you a swing will reach.
     */
    private val highlightTarget by boolean("HighlightTarget", true)
    private val targetColor by color("TargetColor", Color4b(0xFF, 0x3B, 0x30, 0xE0))

    /**
     * Colour the box while the entity is in its hurt flash.
     *
     * Reads as confirmation that a hit landed, in the place you are already
     * looking rather than in the corner of the screen.
     */
    private val highlightHurt by boolean("HighlightHurt", false)
    private val hurtColor by color("HurtColor", Color4b(0xFF, 0x3D, 0xE0, 0xE0))

    private val playerColor by color("PlayerColor", Color4b(0x1F, 0xA8, 0xFF, 0xB0))
    private val mobColor by color("MobColor", Color4b(0xFF, 0x8A, 0x3D, 0xB0))
    private val itemColor by color("ItemColor", Color4b(0xB0, 0xB0, 0xB0, 0x90))

    /** A flat band at eye height: the point a look vector is cast from. */
    private val eyeHeight by boolean("EyeHeight", true)
    private val eyeColor by color("EyeColor", Color4b(0x3D, 0xFF, 0x8A, 0xC0))

    /** Where the entity is actually looking, drawn two blocks out. */
    private val lookDirection by boolean("LookDirection", true)
    private val lookColor by color("LookColor", Color4b(0x1F, 0x6B, 0xFF, 0xC0))

    /**
     * A wider box drawn behind the real one.
     *
     * A single thin line disappears against terrain of a similar colour. The
     * outline gives every box a border so it reads against anything.
     */
    private val outline by boolean("Outline", false)
    private val outlineColor by color("OutlineColor", Color4b(0x00, 0x00, 0x00, 0xC0))
    private val outlineScale by float("OutlineScale", 2f, 1.5f..4f)

    /** Line width up close, and past [thinBeyond] where the lines start to crowd. */
    private val lineWidth by float("LineWidth", 2.5f, 0.5f..6f)
    private val thinBeyond by float("ThinBeyond", 32f, 4f..128f)
    private val thinLineWidth by float("ThinLineWidth", 1.5f, 0.5f..6f)

    /**
     * Beyond this the boxes are a smear of lines rather than information, and
     * every extra entity is another set of lines to push.
     */
    private val range by float("Range", 32f, 4f..128f)

    private fun targetedEntity(): Entity? =
        (mc.hitResult as? EntityHitResult)?.entity

    /**
     * Colour precedence, matching Combat Hitboxes: hurt beats target, target
     * beats the entity kind. The most transient state wins, because that is the
     * one you would otherwise miss.
     */
    private fun colorFor(entity: Entity, targeted: Entity?): Color4b? {
        if (highlightHurt && entity is LivingEntity && entity.hurtTime != 0) {
            return hurtColor
        }
        if (highlightTarget && entity === targeted) {
            return targetColor
        }

        return when (entity) {
            is Player -> if (players) playerColor else null
            is ItemEntity -> if (items) itemColor else null
            is FireworkRocketEntity -> if (fireworks) itemColor else null
            is LivingEntity -> if (mobs) mobColor else null
            else -> null
        }
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val world = mc.level ?: return@handler
        val self = mc.player ?: return@handler
        val rangeSq = (range * range).toDouble()
        val thinBeyondSq = (thinBeyond * thinBeyond).toDouble()
        val targeted = targetedEntity()
        val partialTicks = event.partialTicks

        event.renderEnvironment {
            val cameraPosition = camera.position()

            for (entity in world.entitiesForRendering()) {
                if (entity === self) {
                    continue
                }

                val distanceSq = entity.distanceToSqr(self)
                if (distanceSq > rangeSq) {
                    continue
                }

                val color = colorFor(entity, targeted) ?: continue
                val width = if (distanceSq > thinBeyondSq) thinLineWidth else lineWidth

                // Interpolated, so the box tracks the entity between ticks
                // rather than stepping along behind it.
                val lerped = entity.getPosition(partialTicks)
                val offset = lerped.subtract(entity.position()).subtract(cameraPosition)
                val box = entity.boundingBox.move(offset)

                if (outline) {
                    drawBoxOutline(box, outlineColor, width * outlineScale)
                }
                drawBoxOutline(box, color, width)

                if (eyeHeight && entity is LivingEntity) {
                    drawEyeBand(box, entity, eyeColor, width)
                }

                if (lookDirection) {
                    val eye = lerped
                        .add(0.0, entity.eyeHeight.toDouble(), 0.0)
                        .subtract(cameraPosition)
                    val view = entity.getViewVector(partialTicks)
                    drawSegment(eye, eye.add(view.scale(2.0)), lookColor, width)
                }
            }
        }
    }

    /** The twelve edges of a box, as line pairs. */
    private fun WorldRenderEnvironment.drawBoxOutline(box: AABB, color: Color4b, width: Float) {
        val x0 = box.minX.toFloat()
        val y0 = box.minY.toFloat()
        val z0 = box.minZ.toFloat()
        val x1 = box.maxX.toFloat()
        val y1 = box.maxY.toFloat()
        val z1 = box.maxZ.toFloat()

        drawLinesWithWidth(
            color.argb, width,
            // bottom face
            Vec3f(x0, y0, z0), Vec3f(x1, y0, z0),
            Vec3f(x1, y0, z0), Vec3f(x1, y0, z1),
            Vec3f(x1, y0, z1), Vec3f(x0, y0, z1),
            Vec3f(x0, y0, z1), Vec3f(x0, y0, z0),
            // top face
            Vec3f(x0, y1, z0), Vec3f(x1, y1, z0),
            Vec3f(x1, y1, z0), Vec3f(x1, y1, z1),
            Vec3f(x1, y1, z1), Vec3f(x0, y1, z1),
            Vec3f(x0, y1, z1), Vec3f(x0, y1, z0),
            // uprights
            Vec3f(x0, y0, z0), Vec3f(x0, y1, z0),
            Vec3f(x1, y0, z0), Vec3f(x1, y1, z0),
            Vec3f(x1, y0, z1), Vec3f(x1, y1, z1),
            Vec3f(x0, y0, z1), Vec3f(x0, y1, z1),
        )
    }

    /** A flat rectangle around the box at the entity's eye level. */
    private fun WorldRenderEnvironment.drawEyeBand(
        box: AABB,
        entity: Entity,
        color: Color4b,
        width: Float,
    ) {
        val y = (box.minY + entity.eyeHeight).toFloat()
        val x0 = box.minX.toFloat()
        val z0 = box.minZ.toFloat()
        val x1 = box.maxX.toFloat()
        val z1 = box.maxZ.toFloat()

        drawLinesWithWidth(
            color.argb, width,
            Vec3f(x0, y, z0), Vec3f(x1, y, z0),
            Vec3f(x1, y, z0), Vec3f(x1, y, z1),
            Vec3f(x1, y, z1), Vec3f(x0, y, z1),
            Vec3f(x0, y, z1), Vec3f(x0, y, z0),
        )
    }

    private fun WorldRenderEnvironment.drawSegment(
        from: Vec3,
        to: Vec3,
        color: Color4b,
        width: Float,
    ) {
        drawLinesWithWidth(
            color.argb, width,
            Vec3f(from.x.toFloat(), from.y.toFloat(), from.z.toFloat()),
            Vec3f(to.x.toFloat(), to.y.toFloat(), to.z.toFloat()),
        )
    }

}
