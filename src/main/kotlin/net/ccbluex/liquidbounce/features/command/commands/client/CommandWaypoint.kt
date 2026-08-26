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
package net.ccbluex.liquidbounce.features.command.commands.client

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.misc.Waypoint
import net.ccbluex.liquidbounce.features.misc.WaypointManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc

/**
 * Waypoint command
 *
 * Marks the place you are standing, so you can find it again.
 *
 *   .waypoint add <name>      mark where you are standing
 *   .waypoint remove <name>   forget it
 *   .waypoint list            everything saved, per dimension
 *   .waypoint clear           forget all of them
 */
object CommandWaypoint : Command.Factory {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("waypoint")
            .alias("wp")
            .hub()
            .requiresIngame()
            .subcommand(
                CommandBuilder.begin("add")
                    .parameter(ParameterBuilder.begin<String>("name").required().build())
                    .handler {
                        val name = args[0] as String
                        val player = mc.player ?: return@handler
                        val dimension = mc.level?.dimension()?.identifier()?.toString() ?: return@handler

                        WaypointManager.add(
                            Waypoint(
                                name = name,
                                x = player.blockX,
                                y = player.blockY,
                                z = player.blockZ,
                                dimension = dimension
                            )
                        )

                        chat("Saved '$name' at ${player.blockX} ${player.blockY} ${player.blockZ}")
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder.begin("remove")
                    .parameter(ParameterBuilder.begin<String>("name").required().build())
                    .handler {
                        val name = args[0] as String
                        if (WaypointManager.remove(name)) {
                            chat("Removed waypoint '$name'")
                        } else {
                            chat("No waypoint called '$name'")
                        }
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder.begin("list")
                    .handler {
                        val all = WaypointManager.all()
                        if (all.isEmpty()) {
                            chat("No waypoints saved")
                            return@handler
                        }

                        for (waypoint in all) {
                            // The dimension is shown because two waypoints can
                            // share coordinates and mean different places.
                            chat(
                                "${waypoint.name}: ${waypoint.x} ${waypoint.y} ${waypoint.z} " +
                                    "(${waypoint.dimension.substringAfter(':')})"
                            )
                        }
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder.begin("clear")
                    .handler {
                        WaypointManager.clear()
                        chat("Cleared all waypoints")
                    }
                    .build()
            )
            .build()
    }

}
