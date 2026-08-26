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

import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.ccbluex.liquidbounce.utils.text.textOf
import net.minecraft.ChatFormatting
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult

/**
 * BlockInfo
 *
 * Names whatever the crosshair is on, block or entity, the way WAILA does.
 *
 * Telling deepslate from tuff, or one copper oxidation stage from the next, is
 * otherwise a matter of squinting - and being wrong costs a wasted trip or a
 * ruined build palette. F3 answers it, but F3 covers a third of the screen.
 *
 * Reads the crosshair target the client already computes for its own
 * highlight, and draws its name.
 */
object ModuleBlockInfo : ClientModule("BlockInfo", ModuleCategories.RENDER) {

    private val showEntities by boolean("Entities", true)

    /** Adds the registry id, which is what commands and resource packs use. */
    private val showIdentifier by boolean("Identifier", false)

    /** Adds how hard the block is to break, relative to stone. */
    private val showHardness by boolean("Hardness", false)

    private val x by int("X", 0, -600..600)
    private val y by int("Y", 30, -300..600)

    private val scale by float("Scale", 1f, 0.5f..3f)

    private val fontRenderer
        get() = FontManager.FONT_RENDERER

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        val level = mc.level ?: return@handler
        val hit = mc.hitResult ?: return@handler

        val lines = mutableListOf<net.minecraft.network.chat.Component>()

        when (hit) {
            is BlockHitResult -> {
                val state = level.getBlockState(hit.blockPos)
                if (state.isAir) {
                    return@handler
                }

                lines += state.block.name.copy().withStyle(ChatFormatting.WHITE)

                if (showIdentifier) {
                    lines += net.minecraft.core.registries.BuiltInRegistries.BLOCK
                        .getKey(state.block).toString().asPlainText(ChatFormatting.GRAY)
                }

                if (showHardness) {
                    val hardness = state.getDestroySpeed(level, hit.blockPos)
                    lines += textOf(
                        "Hardness: ".asPlainText(ChatFormatting.GRAY),
                        // -1 is vanilla's marker for unbreakable, which reads
                        // better as a word than as a negative number.
                        (if (hardness < 0f) "unbreakable" else String.format("%.1f", hardness))
                            .asPlainText(ChatFormatting.AQUA)
                    )
                }
            }

            is EntityHitResult -> {
                if (!showEntities) {
                    return@handler
                }
                lines += hit.entity.name.copy().withStyle(ChatFormatting.WHITE)
            }

            else -> return@handler
        }

        val centre = event.context.guiWidth() / 2f

        with(event.context) {
            val vanillaScale = fontRenderer.scaleToVanillaFont

            lines.forEachIndexed { index, line ->
                fontRenderer.draw(line) {
                    this.x = centre + this@ModuleBlockInfo.x
                    this.y = this@ModuleBlockInfo.y + index * 11f
                    shadow = true
                    this.scale = vanillaScale * this@ModuleBlockInfo.scale
                }
            }
        }
    }

}
