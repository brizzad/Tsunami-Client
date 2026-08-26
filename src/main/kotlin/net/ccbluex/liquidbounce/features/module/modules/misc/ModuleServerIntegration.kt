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

import io.netty.buffer.ByteBuf
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

/**
 * ServerIntegration
 *
 * Answers a partnered server that asks what client this is.
 *
 * Servers running tournaments or tier tests need to know which client and
 * which features are in play. Today they guess, or they ban whole clients
 * because they cannot tell a HUD from an aimbot. A client that will simply
 * answer, when its owner allows it, is worth more to both sides than one that
 * hides.
 *
 * This is a hook, not a detector. It reports what this client is running and
 * nothing else: it does not inspect other players, does not judge anyone's
 * behaviour, and does not report anybody. Tsunami has no business deciding
 * who is cheating - that is the server's job, and this only gives the server
 * something honest to work from.
 *
 * Off by default, because it tells a server about your setup and that should
 * never begin without being asked for. When it is on, the answer is truthful;
 * a hook that lies would be worse than no hook at all.
 */
object ModuleServerIntegration : ClientModule("ServerIntegration", ModuleCategories.MISC) {

    /**
     * The channel a server opens the conversation on.
     *
     * Built from an explicit namespace and path. CustomPacketPayload.createType
     * takes a bare path and prepends "minecraft:", so handing it a namespaced
     * string throws while the class is initialising - which does not fail the
     * module, it fails the whole client before the main menu.
     */
    private val CHANNEL = CustomPacketPayload.Type<Payload>(
        Identifier.fromNamespaceAndPath(LiquidBounce.CLIENT_NAME.lowercase(), "integration")
    )

    /** Include the list of enabled modules, not just the client name. */
    private val shareModules by boolean("ShareModules", true)

    /** Say in chat when a server asks, so it is never silent. */
    private val notify by boolean("Notify", true)

    /**
     * A single UTF-8 string. Deliberately the simplest thing that can carry an
     * answer: a server integrating with this should not have to implement a
     * bespoke binary format to read it.
     */
    class Payload(val body: String) : CustomPacketPayload {
        override fun type(): CustomPacketPayload.Type<Payload> = CHANNEL

        companion object {
            val CODEC: StreamCodec<ByteBuf, Payload> = StreamCodec.of(
                { buf, value -> ByteBufCodecs.STRING_UTF8.encode(buf, value.body) },
                { buf -> Payload(ByteBufCodecs.STRING_UTF8.decode(buf)) }
            )
        }
    }

    private fun answer(): String {
        val header = "${LiquidBounce.CLIENT_NAME} ${LiquidBounce.clientVersion}"
        if (!shareModules) {
            return header
        }

        val enabled = ModuleManager.filter { it.enabled }.joinToString(",") { it.name }
        return "$header;$enabled"
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (event.origin != TransferOrigin.INCOMING) {
            return@handler
        }

        val packet = event.packet as? ClientboundCustomPayloadPacket ?: return@handler
        if (packet.payload.type() != CHANNEL) {
            return@handler
        }

        val connection = mc.connection ?: return@handler
        connection.send(ServerboundCustomPayloadPacket(Payload(answer())))

        if (notify) {
            chat("Answered this server's Tsunami integration request")
        }
    }

}
