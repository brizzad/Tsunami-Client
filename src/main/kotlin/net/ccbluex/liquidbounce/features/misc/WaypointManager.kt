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
package net.ccbluex.liquidbounce.features.misc

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.utils.client.logger
import java.io.File

/**
 * A saved place, per dimension.
 *
 * The dimension is part of the identity rather than decoration: overworld and
 * nether coordinates are different places that happen to share numbers, and a
 * waypoint that follows you through a portal is worse than no waypoint.
 */
data class Waypoint(
    val name: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val dimension: String,
    val color: Int = 0xFF1FA8FF.toInt(),
)

/**
 * Stores waypoints on disk, next to the rest of the client's configuration.
 *
 * Plain JSON in the client folder rather than inside the module config, so a
 * list of places a player has collected over months survives a settings reset
 * and can be copied between installs by hand.
 */
object WaypointManager {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val file: File get() = File(ConfigSystem.rootFolder, "waypoints.json")

    private val waypoints = mutableListOf<Waypoint>()
    private var loaded = false

    private fun ensureLoaded() {
        if (loaded) {
            return
        }
        loaded = true

        if (!file.exists()) {
            return
        }

        runCatching {
            val type = object : TypeToken<List<Waypoint>>() {}.type
            val stored: List<Waypoint> = gson.fromJson(file.readText(), type) ?: emptyList()
            waypoints.clear()
            waypoints += stored
        }.onFailure {
            logger.error("Failed to read waypoints from ${file.name}", it)
        }
    }

    private fun save() {
        runCatching {
            file.writeText(gson.toJson(waypoints))
        }.onFailure {
            logger.error("Failed to write waypoints to ${file.name}", it)
        }
    }

    fun all(): List<Waypoint> {
        ensureLoaded()
        return waypoints.toList()
    }

    fun inDimension(dimension: String): List<Waypoint> = all().filter { it.dimension == dimension }

    /** Replaces any waypoint of the same name, so re-marking a place updates it. */
    fun add(waypoint: Waypoint) {
        ensureLoaded()
        waypoints.removeIf { it.name.equals(waypoint.name, ignoreCase = true) }
        waypoints += waypoint
        save()
    }

    fun remove(name: String): Boolean {
        ensureLoaded()
        val removed = waypoints.removeIf { it.name.equals(name, ignoreCase = true) }
        if (removed) {
            save()
        }
        return removed
    }

    fun clear() {
        ensureLoaded()
        waypoints.clear()
        save()
    }

}
