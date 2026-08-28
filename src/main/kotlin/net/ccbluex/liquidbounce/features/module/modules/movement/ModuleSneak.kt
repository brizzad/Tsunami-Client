/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.immuneToMagmaBlocks
import net.ccbluex.liquidbounce.utils.entity.isOnMagmaBlock
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.set
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput

/**
 * Sneak module
 *
 * Automatically sneaks all the time.
 */
/*
 * Only the input-level mode remains.
 *
 * The removed Vanilla and Switch modes drove sneaking with raw packets - forceSneak on the
 * player input packet, and the 1.21.5 start/stop sneaking pair - which is decoupled from
 * whether the game would let you sneak at all. That is what allowed sneaking with an
 * inventory screen open: something toggle-sneak could do years ago and cannot now. Setting
 * the movement input instead is exactly equivalent to holding the key, and stops when the
 * game stops accepting movement, which is the honest behaviour.
 */
object ModuleSneak : ClientModule("Sneak", ModuleCategories.MOVEMENT) {

    private val modes = choices<Mode>("Mode", Legit, arrayOf(Legit)).apply { tagBy(this) }
    private val notDuringMove by boolean("NotDuringMove", false)

    private object Legit : Mode("Legit") {

        private val onMagmaBlocksOnly by boolean("OnMagmaBlocksOnly", false)

        override val parent: ModeValueGroup<Mode>
            get() = modes

        @Suppress("unused")
        private val inputHandler = handler<MovementInputEvent> { event ->
            if (player.moving && notDuringMove) {
                return@handler
            }

            if (onMagmaBlocksOnly && (player.immuneToMagmaBlocks || !isOnMagmaBlock(event.directionalInput))) {
                return@handler
            }

            // Temporarily override sneaking
            event.sneak = true
        }

    }

    private fun isOnMagmaBlock(directionalInput: DirectionalInput): Boolean {
        val simulatedInput = SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(directionalInput)
        simulatedInput.set(jump = false)

        // Doesn't keep the player stuck at the edge of a magma block while sneaking
        simulatedInput.ignoreClippingAtLedge = true

        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(simulatedInput)
        simulatedPlayer.pos = player.position()

        simulatedPlayer.tick()
        val isOnMagmaBlockAfterOneTick = simulatedPlayer.boundingBox.isOnMagmaBlock()

        simulatedPlayer.tick()
        val isOnMagmaBlockAfterTwoTicks = simulatedPlayer.boundingBox.isOnMagmaBlock()

        return isOnMagmaBlockAfterOneTick || isOnMagmaBlockAfterTwoTicks
    }
}
