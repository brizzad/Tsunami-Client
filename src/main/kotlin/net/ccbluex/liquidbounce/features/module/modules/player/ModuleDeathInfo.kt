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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.Waypoint
import net.ccbluex.liquidbounce.features.misc.WaypointManager
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.chat

/**
 * DeathInfo
 *
 * Records where you died, and optionally drops a waypoint on it.
 *
 * The death screen tells you what killed you and nothing about where, and the
 * coordinates are gone from the F3 screen by the time you have read the
 * message. Everything needed is on the client at the moment of death - it is
 * your own position - so this is a note taken at the right instant rather than
 * anything new being learned.
 *
 * The waypoint is written through the same [WaypointManager] the `.waypoint`
 * command uses, so a death marker is an ordinary waypoint: it shows in the
 * list, renders as a beam, and is removed the same way.
 */
object ModuleDeathInfo : ClientModule("DeathInfo", ModuleCategories.PLAYER) {

    /** Save a waypoint at the death position. */
    private val waypoint by boolean("Waypoint", true)

    /**
     * One waypoint reused for every death, or one per death kept forever.
     * Reused by default: a list of two hundred death markers is not a list.
     */
    private val keepHistory by boolean("KeepHistory", false)

    /** How many numbered markers to keep when [keepHistory] is on. */
    private val historySize by int("HistorySize", 5, 1..50)

    private val waypointColor by color("WaypointColor", Color4b(0xFF, 0x3B, 0x30, 0xFF))

    private const val WAYPOINT_NAME = "death"

    /**
     * Deaths seen so far, used only to number the history markers. Not
     * persisted: it is a label, not a statistic.
     */
    private var deathCount = 0

    /**
     * Death is detected by the health crossing to zero rather than by the death
     * screen appearing, because the screen can be skipped by an immediate
     * respawn and the position is already gone by then.
     */
    private var wasAlive = true

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        wasAlive = true
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val player = mc.player ?: return@handler

        val alive = player.health > 0f
        if (alive) {
            wasAlive = true
            return@handler
        }

        if (!wasAlive) {
            return@handler
        }
        wasAlive = false

        val x = player.blockX
        val y = player.blockY
        val z = player.blockZ

        chat("Died at $x, $y, $z.", this)

        if (!waypoint) {
            return@handler
        }

        val dimension = mc.level?.dimension()?.identifier()?.toString() ?: return@handler

        val name = if (keepHistory) {
            deathCount = (deathCount % historySize) + 1
            "$WAYPOINT_NAME-$deathCount"
        } else {
            WAYPOINT_NAME
        }

        // add() replaces a waypoint of the same name, which is what makes the
        // reused marker and the rotating history both fall out of one call.
        WaypointManager.add(Waypoint(name, x, y, z, dimension, waypointColor.argb))

        chat("Waypoint \"$name\" saved. Remove it with .waypoint remove $name", this)
    }

}
