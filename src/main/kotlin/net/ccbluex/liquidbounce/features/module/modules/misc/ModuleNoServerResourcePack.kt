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
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket

/**
 * NoServerResourcePack
 *
 * Stops a server replacing your resource pack.
 *
 * A forced pack can rewrite fonts, sounds and every texture you rely on, and
 * on a slow connection it is a multi-megabyte download before you can play.
 * Vanilla's own setting refuses the pack, and servers that require one then
 * disconnect you.
 *
 * Two modes, and the honest one is the default:
 *
 *  - [Mode.DECLINE] tells the server the truth: the pack was declined. A
 *    server that requires it will disconnect you, which is its right.
 *
 *  - [Mode.PRETEND] tells the server the pack loaded when it did not. This
 *    keeps you connected to servers that would otherwise kick you, and it is
 *    a lie told to the server.
 *
 * PRETEND deliberately is not the default. Tsunami's stated scope excludes
 * "anything that reports false information to a server", and this mode is
 * exactly that, even though what it protects is your own machine's textures
 * rather than any advantage in play. Choosing it should be a decision
 * somebody makes on purpose, having read what it does.
 *
 * The whole module is off by default.
 */
object ModuleNoServerResourcePack : ClientModule("NoServerResourcePack", ModuleCategories.MISC) {

    enum class Mode(override val tag: String) : Tagged {
        /** Answer truthfully, and accept being disconnected. */
        DECLINE("Decline"),

        /** Answer that it loaded. Keeps you connected; is not true. */
        PRETEND("Pretend"),
    }

    private val mode by enumChoice("Mode", Mode.DECLINE)

    /** Says in chat what was refused, so a missing texture pack is not a mystery. */
    private val notify by boolean("Notify", true)

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (event.origin != TransferOrigin.INCOMING) {
            return@handler
        }

        val packet = event.packet as? ClientboundResourcePackPushPacket ?: return@handler

        // Stop the client acting on it: no download, no prompt, no swap.
        event.cancelEvent()

        val connection = mc.connection ?: return@handler

        when (mode) {
            Mode.DECLINE -> {
                connection.send(
                    ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.DECLINED)
                )
            }

            Mode.PRETEND -> {
                // The sequence a real client sends on success. Servers that
                // gate entry on the pack accept this and let you in.
                connection.send(
                    ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.ACCEPTED)
                )
                connection.send(
                    ServerboundResourcePackPacket(
                        packet.id(),
                        ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED
                    )
                )
            }
        }

        if (notify) {
            chat("Refused the server's resource pack (${mode.tag})")
        }
    }

}
