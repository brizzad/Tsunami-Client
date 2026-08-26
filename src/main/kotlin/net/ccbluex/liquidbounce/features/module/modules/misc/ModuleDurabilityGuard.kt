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

import net.ccbluex.liquidbounce.event.CancellableEvent
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.BlockAttackEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PlayerInteractItemEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.client.notification
import net.minecraft.world.item.ItemStack

/**
 * DurabilityGuard
 *
 * Refuses to swing, mine or use a tool that is one or two hits from breaking,
 * so a mended netherite pickaxe does not vanish because of a stray click.
 *
 * Losing a heavily invested tool this way is entirely a slip: nobody chooses
 * to break their good pickaxe on cobblestone. The item is gone permanently and
 * there is no undo, which is what makes it worth guarding.
 *
 * This blocks an action rather than performing one. It never swaps items,
 * never repairs anything, and never acts on its own - the worst it does is
 * make you notice, which is the point.
 *
 * Off by default. It cancels inputs, and in a fight a refused swing is worse
 * than a broken sword, so turning it on should be a deliberate choice.
 */
object ModuleDurabilityGuard : ClientModule("DurabilityGuard", ModuleCategories.MISC) {

    /** Remaining durability at or below which the item is protected. */
    private val threshold by int("Threshold", 15, 1..200)

    private val stopAttacking by boolean("StopAttacking", true)
    private val stopMining by boolean("StopMining", true)
    private val stopUsing by boolean("StopUsing", true)

    private val notify by boolean("Notify", true)

    /** Notifications are rate limited; a blocked mine repeats every tick. */
    private const val NOTIFY_INTERVAL_MS = 3000L
    private var lastNotify = 0L

    private fun ItemStack.isNearlyBroken(): Boolean {
        if (isEmpty || !isDamageableItem) {
            return false
        }
        return maxDamage - damageValue <= threshold
    }

    private fun guard(event: CancellableEvent, enabled: Boolean) {
        if (!enabled) {
            return
        }

        val player = mc.player ?: return
        val stack = player.mainHandItem
        if (!stack.isNearlyBroken()) {
            return
        }

        event.cancelEvent()

        if (!notify) {
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastNotify < NOTIFY_INTERVAL_MS) {
            return
        }
        lastNotify = now

        notification(
            title = "DurabilityGuard",
            message = "${stack.hoverName.string} has ${stack.maxDamage - stack.damageValue} durability left",
            severity = NotificationEvent.Severity.INFO
        )
    }

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { guard(it, stopAttacking) }

    @Suppress("unused")
    private val mineHandler = handler<BlockAttackEvent> { guard(it, stopMining) }

    @Suppress("unused")
    private val useHandler = handler<PlayerInteractItemEvent> { guard(it, stopUsing) }

}
