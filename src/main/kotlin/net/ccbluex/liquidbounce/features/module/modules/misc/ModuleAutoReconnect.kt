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

import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.ServerConnectEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.DisconnectedScreen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.multiplayer.resolver.ServerAddress

/**
 * AutoReconnect
 *
 * Rejoins the last server after a disconnect, counting down on the disconnect
 * screen so the attempt is visible and can be stopped.
 *
 * The case this is for is a restart or a network blip during a queue: the
 * server comes back in thirty seconds and the cost of not noticing is losing a
 * place in it. Reconnecting is the same click a player would make anyway, with
 * no timing or judgement in it, which is what keeps it on the quality-of-life
 * side of the line.
 *
 * ## Deliberate limits
 *
 * It gives up after a bounded number of tries rather than hammering the server
 * indefinitely - an unattended client retrying forever is somebody else's
 * bandwidth problem. It also refuses to retry a disconnect that reads as a ban
 * or a kick, because rejoining after those is not a connection problem to solve
 * and doing it automatically is exactly the behaviour that gets a client
 * blocked.
 */
object ModuleAutoReconnect : ClientModule("AutoReconnect", ModuleCategories.MISC) {

    /** Seconds to wait before the first attempt. */
    private val delay by int("Delay", 5, 1..60)

    /**
     * How many tries before giving up and leaving the screen alone. Zero would
     * be a client that retries forever, which is not offered.
     */
    private val maxAttempts by int("MaxAttempts", 5, 1..20)

    /**
     * Add the previous wait to each retry, so a server that is down gets a
     * lengthening gap instead of a fixed drumbeat.
     */
    private val backOff by boolean("BackOff", true)

    /**
     * Do not retry when the disconnect message looks like a ban or a kick.
     * Leaving this off means reconnecting into a ban screen repeatedly.
     */
    private val skipKicks by boolean("SkipKicks", true)

    /**
     * Not gated on being in a world. [ClientModule.running] requires `inGame`,
     * which is false by definition on the screen this module exists to act on.
     */
    override val running: Boolean
        get() = enabled

    private var lastServer: ServerData? = null
    private var lastAddress: ServerAddress? = null

    private var ticksRemaining = 0
    private var attempts = 0
    private var armedFor: DisconnectedScreen? = null

    /** Phrases that mean "do not come back", in the languages servers actually use. */
    private val KICK_MARKERS = listOf(
        "banned", "ban", "kick", "blacklist", "whitelist", "not whitelisted", "suspended"
    )

    @Suppress("unused")
    private val connectHandler = handler<ServerConnectEvent> { event ->
        lastServer = event.serverInfo
        lastAddress = event.address
        attempts = 0
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val screen = mc.gui.screen()

        if (screen !is DisconnectedScreen) {
            // Left the disconnect screen, by hand or by reconnecting. Disarm so a
            // later, unrelated disconnect starts its own countdown.
            armedFor = null
            return@handler
        }

        if (armedFor !== screen) {
            armedFor = screen

            if (!shouldRetry(screen)) {
                ticksRemaining = 0
                return@handler
            }

            attempts++
            if (attempts > maxAttempts) {
                ticksRemaining = 0
                return@handler
            }

            val waitSeconds = if (backOff) delay * attempts else delay
            ticksRemaining = waitSeconds * TICKS_PER_SECOND
        }

        if (ticksRemaining <= 0) {
            return@handler
        }

        ticksRemaining--
        if (ticksRemaining > 0) {
            return@handler
        }

        reconnect()
    }

    private fun shouldRetry(screen: DisconnectedScreen): Boolean {
        if (lastServer == null || lastAddress == null) {
            return false
        }

        if (!skipKicks) {
            return true
        }

        // The screen renders the reason; reading what it says is the only signal
        // available, since the disconnect packet carries no machine-readable cause.
        val reason = screen.narrationMessage.string.lowercase()
        return KICK_MARKERS.none { it in reason }
    }

    private fun reconnect() {
        val server = lastServer ?: return
        val address = lastAddress ?: return

        ConnectScreen.startConnecting(
            // Where the player lands if they cancel, or if the connection fails again.
            JoinMultiplayerScreen(TitleScreen()),
            mc,
            address,
            server,
            false,
            null,
        )
    }

    private const val TICKS_PER_SECOND = 20

}
