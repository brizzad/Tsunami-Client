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

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.FpsChangeEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MouseButtonEvent
import net.ccbluex.liquidbounce.event.events.SessionStatsEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.ping
import net.ccbluex.liquidbounce.utils.text.hideSensitiveAddress
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Readouts a player wants on screen that are not part of the vanilla player
 * state: clicks per second, hit combo, reach, speed, memory and so on.
 *
 * These deliberately do not live on [net.ccbluex.liquidbounce.integration
 * .interop.protocol.rest.v1.game.PlayerData]. That type is built by
 * `fromPlayer` for the local player *and* for the current target, so anything
 * client-global added to it would make the target HUD report the local
 * player's FPS and CPS as though they belonged to the opponent.
 *
 * Everything here is measured from what the client already does. Nothing is
 * sent anywhere, and none of it changes what is sent to a server.
 */
object SessionStats : EventListener {

    /** Clicks inside this window count toward CPS. */
    private const val CPS_WINDOW_MS = 1000L

    /**
     * A hit continues a combo if it lands within this long of the previous one.
     * Long enough to survive a missed swing, short enough that walking away and
     * coming back reads as a new fight.
     */
    private const val COMBO_TIMEOUT_MS = 4000L

    /** Ticks between pushes to the interface. Four a second is plenty to read. */
    private const val PUSH_INTERVAL_TICKS = 5

    private val startedAt = System.currentTimeMillis()

    private val leftClicks = ArrayDeque<Long>()
    private val rightClicks = ArrayDeque<Long>()

    private var comboTargetId: Int? = null
    private var lastHitAt = 0L

    var combo = 0
        private set
    var bestCombo = 0
        private set

    /** Distance of the most recent attack, in blocks, or 0 when nothing was hit yet. */
    var reach = 0.0
        private set

    /**
     * Stopwatch, started and stopped by ModuleStopwatch rather than by a
     * command, so the HUD toggle and the timer are the same switch.
     */
    private var stopwatchStartedAt: Long? = null
    private var stopwatchAccumulated = 0L

    fun startStopwatch() {
        if (stopwatchStartedAt == null) {
            stopwatchStartedAt = System.currentTimeMillis()
        }
    }

    fun stopStopwatch() {
        stopwatchStartedAt?.let { stopwatchAccumulated += System.currentTimeMillis() - it }
        stopwatchStartedAt = null
    }

    fun resetStopwatch() {
        stopwatchAccumulated = 0L
        stopwatchStartedAt = if (stopwatchStartedAt != null) System.currentTimeMillis() else null
    }

    private val stopwatchSeconds: Long
        get() {
            val running = stopwatchStartedAt?.let { System.currentTimeMillis() - it } ?: 0L
            return (stopwatchAccumulated + running) / 1000
        }

    private var fps = 0
    private var lastPos: Triple<Double, Double, Double>? = null
    private var speed = 0.0
    private var tickCounter = 0

    private fun prune(clicks: ArrayDeque<Long>, now: Long) {
        while (clicks.isNotEmpty() && now - clicks.first() > CPS_WINDOW_MS) {
            clicks.removeFirst()
        }
    }

    @Suppress("unused")
    private val fpsHandler = handler<FpsChangeEvent> { event ->
        fps = event.fps
    }

    @Suppress("unused")
    private val mouseHandler = handler<MouseButtonEvent> { event ->
        // Clicks made while a screen is open are interface clicks, not attacks.
        if (event.screen != null || !event.isPressed) {
            return@handler
        }

        val now = System.currentTimeMillis()
        when {
            event.isLeftButton -> leftClicks.addLast(now)
            event.isRightButton -> rightClicks.addLast(now)
        }
    }

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { event ->
        val player = mc.player ?: return@handler
        val now = System.currentTimeMillis()

        // To the nearest point of the hitbox, not to the entity origin.
        // Origin distance reads about 1.5 blocks long on a normal hit, which
        // would show 4.5 for a hit that vanilla only allows out to 3.0 - a
        // reach display that lies in the direction of looking impressive.
        reach = sqrt(event.entity.boundingBox.distanceToSqr(player.eyePosition))

        val sameTarget = comboTargetId == event.entity.id
        val inTime = now - lastHitAt <= COMBO_TIMEOUT_MS
        combo = if (sameTarget && inTime) combo + 1 else 1

        comboTargetId = event.entity.id
        lastHitAt = now
        bestCombo = maxOf(bestCombo, combo)
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val player = mc.player ?: return@handler
        val now = System.currentTimeMillis()

        prune(leftClicks, now)
        prune(rightClicks, now)

        if (combo > 0 && now - lastHitAt > COMBO_TIMEOUT_MS) {
            combo = 0
            comboTargetId = null
        }

        // Horizontal blocks per second. Taken from actual position change rather
        // than deltaMovement, so it reads true when movement is server-corrected.
        val pos = Triple(player.x, player.y, player.z)
        lastPos?.let { (lx, _, lz) ->
            speed = hypot(player.x - lx, player.z - lz) * 20.0
        }
        lastPos = pos

        if (++tickCounter < PUSH_INTERVAL_TICKS) {
            return@handler
        }
        tickCounter = 0

        EventManager.callEvent(SessionStatsEvent(snapshot()))
    }

    /**
     * The current readouts.
     *
     * Shared by the tick push and by GET /api/v1/client/session, so the number
     * the HUD shows and the number a test samples can never drift apart.
     */
    fun snapshot(): SessionStatsData {
        val player = mc.player
        val runtime = Runtime.getRuntime()
        val used = runtime.totalMemory() - runtime.freeMemory()

        return SessionStatsData(
            fps = fps,
            cps = CpsData(leftClicks.size, rightClicks.size),
            combo = combo,
            bestCombo = bestCombo,
            reach = reach,
            speed = speed,
            ping = player?.ping ?: 0,
            memory = MemoryData(
                used = used / 1024 / 1024,
                max = runtime.maxMemory() / 1024 / 1024,
                percent = (used.toDouble() / runtime.maxMemory() * 100.0).roundToInt()
            ),
            uptime = (System.currentTimeMillis() - startedAt) / 1000,
            stopwatch = stopwatchSeconds,
            held = HeldItemData(
                count = player?.mainHandItem?.count ?: 0,
                total = player?.let { p ->
                    val held = p.mainHandItem
                    if (held.isEmpty) 0 else p.inventory.nonEquipmentItems
                        .filter { it.item == held.item }
                        .sumOf { it.count }
                } ?: 0
            ),
            // Overworld clock rather than the local one, so the day counter keeps
            // counting while in the Nether or the End.
            day = (player?.level()?.overworldClockTime ?: 0L) / 24000L,
            direction = DirectionData.of(player?.yRot ?: 0f),
            server = ServerData(
                address = mc.currentServer?.ip?.hideSensitiveAddress() ?: "Singleplayer",
                players = mc.connection?.onlinePlayers?.size ?: 1
            )
        )
    }

    /**
     * Dropped when leaving a world so a new session does not inherit the last
     * one's combo or reach.
     */
    fun reset() {
        leftClicks.clear()
        rightClicks.clear()
        combo = 0
        bestCombo = 0
        comboTargetId = null
        reach = 0.0
        speed = 0.0
        lastPos = null
    }

}

data class CpsData(val left: Int, val right: Int)

/**
 * How much of the held item is in hand and how much of it is carried in total,
 * which is the number that matters when you are part way through a wall.
 */
data class HeldItemData(val count: Int, val total: Int)

data class MemoryData(val used: Long, val max: Long, val percent: Int)

data class ServerData(val address: String, val players: Int)

data class DirectionData(val cardinal: String, val axis: String) {
    companion object {
        private val CARDINALS = arrayOf("S", "SW", "W", "NW", "N", "NE", "E", "SE")
        private val AXES = arrayOf("+Z", "+Z -X", "-X", "-Z -X", "-Z", "-Z +X", "+X", "+Z +X")

        fun of(yaw: Float): DirectionData {
            // Minecraft yaw is 0 at south and grows clockwise. Offset by half a
            // sector so each label covers the 45 degrees centred on it.
            val index = (((yaw % 360 + 360) % 360) / 45.0f).let {
                (Math.round(it) % 8)
            }
            return DirectionData(CARDINALS[index], AXES[index])
        }
    }
}

data class SessionStatsData(
    val fps: Int,
    val cps: CpsData,
    val combo: Int,
    val bestCombo: Int,
    val reach: Double,
    val speed: Double,
    val ping: Int,
    val memory: MemoryData,
    val uptime: Long,
    val stopwatch: Long,
    val held: HeldItemData,
    val day: Long,
    val direction: DirectionData,
    val server: ServerData,
)
